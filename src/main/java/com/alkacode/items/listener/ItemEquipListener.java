package com.alkacode.items.listener;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.model.ItemTemplate;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * ON_EQUIP/ON_UNEQUIP via PlayerArmorChangeEvent (Paper) - cobre troca por click,
 * drag, dispenser e comando /item, sem precisar reimplementar deteccao de slot em
 * cima de InventoryClickEvent cru como o spec original sugeria. Esse evento NAO e
 * cancelavel (o item ja foi fisicamente equipado quando ele dispara) - se
 * vip-required/rank-required falhar, a peca e devolvida ao inventario no mesmo
 * tick em vez de bloqueada preventivamente (simplificacao documentada, mesma classe
 * de escolha do escopo pratico combinado com o usuario pra este plugin).
 */
public final class ItemEquipListener implements Listener {

    private final AlkaItemsServices services;

    public ItemEquipListener(AlkaItemsServices services) {
        this.services = services;
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();

        String oldId = services.pdc.getTemplateId(event.getOldItem());
        if (oldId != null) {
            ItemTemplate oldTemplate = services.itemsConfig.get(oldId);
            if (oldTemplate != null) {
                services.effectService.removeAll(player, oldTemplate, EffectTrigger.ON_EQUIP);
            }
        }

        String newId = services.pdc.getTemplateId(event.getNewItem());
        if (newId == null) {
            return;
        }
        ItemTemplate newTemplate = services.itemsConfig.get(newId);
        if (newTemplate == null) {
            return;
        }

        if (!meetsRequirements(player, newTemplate)) {
            returnToInventory(player, event);
            return;
        }

        services.effectService.applyAll(player, newTemplate, EffectTrigger.ON_EQUIP);
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

    private void returnToInventory(Player player, PlayerArmorChangeEvent event) {
        var equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }
        switch (event.getSlot()) {
            case HEAD -> equipment.setHelmet(event.getOldItem());
            case CHEST -> equipment.setChestplate(event.getOldItem());
            case LEGS -> equipment.setLeggings(event.getOldItem());
            case FEET -> equipment.setBoots(event.getOldItem());
            default -> { }
        }
        var leftover = player.getInventory().addItem(event.getNewItem());
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }
}
