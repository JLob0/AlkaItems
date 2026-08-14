package com.alkacode.items.service;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.effect.EffectType;
import com.alkacode.items.listener.ItemEffectMovementListener;
import com.alkacode.items.model.ItemEffect;
import com.alkacode.items.model.ItemTemplate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task periodica (config: effect-tick-interval) que cobre o que nao e evento-driven:
 * sincroniza ON_PASSIVE (presenca no inventario) e reexecuta PARTICLE/SOUND com
 * "interval-ticks" configurado enquanto a fonte (armadura/mao) esta ativa. Tambem
 * delega pro {@link ItemEffectMovementListener#tick()} (reset de allowFlight do
 * DOUBLE_JUMP ao tocar o chao).
 */
public final class EffectTickTask extends BukkitRunnable {

    private final AlkaItemsServices services;
    private final ItemEffectMovementListener movementListener;
    private long counter;

    public EffectTickTask(AlkaItemsServices services, ItemEffectMovementListener movementListener) {
        this.services = services;
        this.movementListener = movementListener;
    }

    @Override
    public void run() {
        counter++;
        movementListener.tick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncPassive(player);
            fireIntervalEffects(player);
            services.enchantService.updateArmorSetStatus(player);
            fireDue(player, services.enchantService.activeSetIntervalEffects(player));
        }
    }

    private void syncPassive(Player player) {
        Set<String> ids = new HashSet<>();
        for (ItemStack item : player.getInventory().getContents()) {
            String id = services.pdc.getTemplateId(item);
            if (id == null) {
                continue;
            }
            ItemTemplate template = services.itemsConfig.get(id);
            if (template != null && !template.effectsPassive().isEmpty()) {
                ids.add(id);
            }
        }
        services.effectService.syncPassive(player, ids, services.itemsConfig::get);
    }

    private void fireIntervalEffects(Player player) {
        var equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }
        List<ItemStack> sources = new ArrayList<>(List.of(equipment.getArmorContents()));
        sources.add(equipment.getItemInMainHand());

        for (ItemStack item : sources) {
            String id = services.pdc.getTemplateId(item);
            if (id == null) {
                continue;
            }
            ItemTemplate template = services.itemsConfig.get(id);
            if (template == null) {
                continue;
            }
            fireDue(player, template.effectsOnEquip());
            fireDue(player, template.effectsOnHold());
        }
    }

    private void fireDue(Player player, List<ItemEffect> effects) {
        for (ItemEffect effect : effects) {
            if (effect.type() != EffectType.PARTICLE && effect.type() != EffectType.SOUND) {
                continue;
            }
            int interval = effect.params().getInt("interval-ticks", 0);
            if (interval > 0 && counter % interval == 0) {
                services.effectService.fireOneShot(player, effect);
            }
        }
    }
}
