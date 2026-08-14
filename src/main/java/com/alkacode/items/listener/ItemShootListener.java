package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.enchant.EnchantTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ON_SHOOT (no lancamento) e ON_PROJECTILE_HIT (no impacto) - o item que disparou so
 * existe no momento do lancamento, entao o resultado do build() naquele instante e
 * guardado por UUID do projetil ate ele acertar algo ou o mapa ser limpo (entrada
 * some sozinha apos o hit, nao ha limpeza periodica - flechas nao ficam vivas por
 * muito tempo, o mapa nao cresce sem limite na pratica).
 */
public final class ItemShootListener implements Listener {

    private final AlkaItemsServices services;
    private final Map<UUID, ItemStack> pendingProjectiles = new ConcurrentHashMap<>();

    public ItemShootListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        services.enchantService.trigger(EnchantTrigger.ON_SHOOT, player, bow, null, event);
        if (bow != null && services.pdc.isAlkaItem(bow)) {
            pendingProjectiles.put(event.getProjectile().getUniqueId(), bow);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ItemStack bow = pendingProjectiles.remove(projectile.getUniqueId());
        if (bow == null || !(projectile.getShooter() instanceof Player player)) {
            return;
        }
        LivingEntity target = event.getHitEntity() instanceof LivingEntity living ? living : null;
        services.enchantService.trigger(EnchantTrigger.ON_PROJECTILE_HIT, player, bow, target, event);
    }
}
