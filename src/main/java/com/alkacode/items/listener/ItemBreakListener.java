package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.enchant.EnchantTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class ItemBreakListener implements Listener {

    private final AlkaItemsServices services;

    public ItemBreakListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        services.enchantService.trigger(EnchantTrigger.ON_BLOCK_BREAK, player, tool, null, event);
    }
}
