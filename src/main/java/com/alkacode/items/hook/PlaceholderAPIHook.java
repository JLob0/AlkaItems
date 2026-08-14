package com.alkacode.items.hook;

import com.alkacode.items.config.ItemsConfig;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.util.ItemPdc;
import com.alkacode.items.util.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * %alkaitems_item_in_mainhand/offhand%, %alkaitems_item_helmet/chestplate/leggings/boots%,
 * %alkaitems_has_enchant_<id>%, %alkaitems_enchant_level_<id>%, %alkaitems_total_templates%.
 * "unique_items_collected" do spec original NAO foi implementado (exigiria persistencia
 * por jogador que este plugin deliberadamente nao tem - ver memoria do projeto).
 */
public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final ItemsConfig itemsConfig;
    private final ItemPdc pdc;

    public PlaceholderAPIHook(JavaPlugin plugin, ItemsConfig itemsConfig, ItemPdc pdc) {
        this.plugin = plugin;
        this.itemsConfig = itemsConfig;
        this.pdc = pdc;
    }

    @Override
    public @NotNull String getIdentifier() { return "alkaitems"; }

    @Override
    public @NotNull String getAuthor() { return "AlkaStudio"; }

    @Override
    public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        String key = params.toLowerCase(java.util.Locale.ROOT);
        PlayerInventory inv = player.getInventory();

        if (key.equals("total_templates")) {
            return String.valueOf(itemsConfig.all().size());
        }
        if (key.equals("item_in_mainhand")) {
            return templateDisplay(inv.getItemInMainHand());
        }
        if (key.equals("item_in_offhand")) {
            return templateDisplay(inv.getItemInOffHand());
        }
        if (key.equals("item_helmet")) {
            return templateDisplay(inv.getHelmet());
        }
        if (key.equals("item_chestplate")) {
            return templateDisplay(inv.getChestplate());
        }
        if (key.equals("item_leggings")) {
            return templateDisplay(inv.getLeggings());
        }
        if (key.equals("item_boots")) {
            return templateDisplay(inv.getBoots());
        }
        if (key.startsWith("has_enchant_")) {
            String id = key.substring("has_enchant_".length());
            return String.valueOf(pdc.hasEnchant(inv.getItemInMainHand(), id));
        }
        if (key.startsWith("enchant_level_")) {
            String id = key.substring("enchant_level_".length());
            return String.valueOf(pdc.getEnchantLevel(inv.getItemInMainHand(), id));
        }
        return null;
    }

    private String templateDisplay(ItemStack item) {
        String id = pdc.getTemplateId(item);
        if (id == null) {
            return "";
        }
        ItemTemplate template = itemsConfig.get(id);
        return template != null ? TextUtil.plain(template.name()) : id;
    }
}
