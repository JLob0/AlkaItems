package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.enchant.EnchantTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Soulbound (nao dropa ao morrer) + ON_DEATH (dispara pra todo item do inventario que
 * tenha um encantamento customizado com esse trigger, antes dos drops serem calculados). */
public final class ItemDeathListener implements Listener {

    private final AlkaItemsServices services;
    private final Map<UUID, List<ItemStack>> pendingRestore = new ConcurrentHashMap<>();

    public ItemDeathListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                services.enchantService.trigger(EnchantTrigger.ON_DEATH, player, item, null, event);
            }
        }

        List<ItemStack> kept = new ArrayList<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (services.pdc.isSoulbound(drop)) {
                kept.add(drop);
                iterator.remove();
            }
        }
        if (!kept.isEmpty()) {
            pendingRestore.put(player.getUniqueId(), kept);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        List<ItemStack> kept = pendingRestore.remove(event.getPlayer().getUniqueId());
        if (kept == null || kept.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        for (ItemStack item : kept) {
            var leftover = player.getInventory().addItem(item);
            leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        }
    }
}
