package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.enchant.EnchantTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public final class ItemFishListener implements Listener {

    private final AlkaItemsServices services;

    public ItemFishListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        services.enchantService.trigger(EnchantTrigger.ON_FISH, player, rod, null, event);
    }
}
