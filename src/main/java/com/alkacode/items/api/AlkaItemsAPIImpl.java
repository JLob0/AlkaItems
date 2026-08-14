package com.alkacode.items.api;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.service.ItemService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AlkaItemsAPIImpl implements AlkaItemsAPI {

    private final AlkaItemsServices services;

    public AlkaItemsAPIImpl(AlkaItemsServices services) {
        this.services = services;
    }

    @Override
    public boolean giveItem(Player player, String templateId, int amount) {
        return services.itemService.giveItem(player, templateId, amount) == ItemService.GiveResult.SUCCESS;
    }

    @Override
    public boolean hasItem(Player player, String templateId) {
        if (templateId == null) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (templateId.equalsIgnoreCase(services.pdc.getTemplateId(item))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemTemplate getTemplate(String id) {
        return services.itemsConfig.get(id);
    }

    @Override
    public CustomEnchantment getEnchant(String id) {
        return services.enchantsConfig.get(id);
    }

    @Override
    public boolean hasEnchant(ItemStack item, String enchantId) {
        return services.pdc.hasEnchant(item, enchantId);
    }

    @Override
    public int getEnchantLevel(ItemStack item, String enchantId) {
        return services.pdc.getEnchantLevel(item, enchantId);
    }
}
