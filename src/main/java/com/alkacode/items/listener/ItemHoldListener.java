package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.model.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** ON_HOLD/ON_UNHOLD - mao principal (troca de hotbar slot, cancelavel de verdade -
 * diferente do equip de armadura) e mao secundaria (swap-hand). */
public final class ItemHoldListener implements Listener {

    private final AlkaItemsServices services;

    public ItemHoldListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        ItemTemplate newTemplate = templateOf(newItem);
        if (newTemplate != null && !meetsRequirements(player, newTemplate)) {
            event.setCancelled(true);
            return;
        }

        swap(player, oldItem, newItem);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Apos o swap, offhand vira o item que estava na mao principal e vice-versa -
        // troca so a mao principal (offhand nao dispara efeitos ON_HOLD neste escopo,
        // so main-hand, igual ao restante do plugin considera "segurar").
        ItemTemplate oldMainTemplate = templateOf(event.getMainHandItem());
        ItemTemplate newMainTemplate = templateOf(event.getOffHandItem());
        if (newMainTemplate != null && !meetsRequirements(player, newMainTemplate)) {
            event.setCancelled(true);
            return;
        }
        if (oldMainTemplate != null) {
            services.effectService.removeAll(player, oldMainTemplate, EffectTrigger.ON_HOLD);
        }
        if (newMainTemplate != null) {
            services.effectService.applyAll(player, newMainTemplate, EffectTrigger.ON_HOLD);
        }
    }

    private void swap(Player player, ItemStack oldItem, ItemStack newItem) {
        ItemTemplate oldTemplate = templateOf(oldItem);
        if (oldTemplate != null) {
            services.effectService.removeAll(player, oldTemplate, EffectTrigger.ON_HOLD);
        }
        ItemTemplate newTemplate = templateOf(newItem);
        if (newTemplate != null) {
            services.effectService.applyAll(player, newTemplate, EffectTrigger.ON_HOLD);
        }
    }

    private ItemTemplate templateOf(ItemStack item) {
        String id = services.pdc.getTemplateId(item);
        return id != null ? services.itemsConfig.get(id) : null;
    }

    private boolean meetsRequirements(Player player, ItemTemplate template) {
        if (template.hasVipRequirement() && !services.requirementHook.hasVip(player, template.vipRequired())) {
            services.sendMessage(player, "items.equip.denied-vip", Map.of("tier", template.vipRequired()));
            return false;
        }
        if (template.hasRankRequirement() && !services.requirementHook.hasRank(player, template.rankRequired())) {
            services.sendMessage(player, "items.equip.denied-rank", Map.of());
            return false;
        }
        return true;
    }
}
