package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** KNOCKBACK_IMMUNITY (efeito de item/set bonus) - {@code io.papermc.paper.event.entity.EntityKnockbackEvent}
 * e real e cancelavel no Paper 1.21.8 (confirmado via javap no jar; a versao legada
 * {@code org.bukkit.event.entity.EntityKnockbackEvent} esta deprecated-for-removal, evitada de
 * proposito), cobre TODAS as causas (ataque, explosao, etc) - nao precisou do fallback via
 * NMS/ProtocolLib que o spec original cogitou. */
public final class KnockbackImmunityListener implements Listener {

    private final AlkaItemsServices services;

    public KnockbackImmunityListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (event.getEntity() instanceof Player player && services.effectService.hasKnockbackImmunity(player)) {
            event.setCancelled(true);
        }
    }
}
