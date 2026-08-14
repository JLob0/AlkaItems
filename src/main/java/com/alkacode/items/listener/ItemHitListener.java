package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.enchant.EnchantTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/** ON_HIT (arma na mao), ON_DAMAGE_TAKEN (qualquer peca de armadura equipada), ON_KILL (arma na
 * mao no momento da morte da vitima). Prioridade HIGH pra rodar depois de reducoes/imunidades
 * normais mas antes do dano ser efetivamente aplicado (DAMAGE/CRITICAL/DODGE mutam o evento). */
public final class ItemHitListener implements Listener {

    private final AlkaItemsServices services;

    public ItemHitListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        services.enchantService.trigger(EnchantTrigger.ON_HIT, attacker, weapon, victim, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        var equipment = victim.getEquipment();
        if (equipment == null) {
            return;
        }
        for (ItemStack armorPiece : equipment.getArmorContents()) {
            services.enchantService.trigger(EnchantTrigger.ON_DAMAGE_TAKEN, victim, armorPiece, victim, event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        services.enchantService.trigger(EnchantTrigger.ON_KILL, killer, weapon, event.getEntity(), event);
    }
}
