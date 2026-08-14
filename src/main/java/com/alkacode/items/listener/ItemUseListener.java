package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.model.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** ON_USE - so mao principal (evita disparar 2x no clique com as duas maos, mesmo trick que o vanilla usa
 * pra nao comer 2 itens de uma vez com EquipmentSlot.OFF_HAND). */
public final class ItemUseListener implements Listener {

    private final AlkaItemsServices services;

    public ItemUseListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String id = services.pdc.getTemplateId(item);
        if (id == null) {
            return;
        }
        ItemTemplate template = services.itemsConfig.get(id);
        if (template == null || template.effectsOnUse().isEmpty()) {
            return;
        }
        if (template.hasVipRequirement() && !services.requirementHook.hasVip(player, template.vipRequired())) {
            services.sendMessage(player, "items.equip.denied-vip", Map.of("tier", template.vipRequired()));
            event.setCancelled(true);
            return;
        }
        if (template.hasRankRequirement() && !services.requirementHook.hasRank(player, template.rankRequired())) {
            services.sendMessage(player, "items.equip.denied-rank", Map.of());
            event.setCancelled(true);
            return;
        }
        services.effectService.runUseEffects(player, template);
    }
}
