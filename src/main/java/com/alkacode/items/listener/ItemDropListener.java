package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Map;

public final class ItemDropListener implements Listener {

    private final AlkaItemsServices services;

    public ItemDropListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (services.pdc.isSoulbound(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            services.sendMessage(event.getPlayer(), "items.soulbound-cannot-drop", Map.of());
        }
    }
}
