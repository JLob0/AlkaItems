package com.alkacode.items.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-dependency para o ItemsAdder (dev.lone.itemsadder.api.CustomStack) via
 * reflection - identico ao com.alkacode.vips.hook.ItemsAdderHook ja verificado no
 * ecossistema. {@code material:} em items.yml aceita "itemsadder:algum_id" - ver
 * {@link #isCustomId(String)}.
 */
public final class ItemsAdderHook {

    private static final String CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack";
    private static final String PREFIX = "itemsadder:";

    private final JavaPlugin plugin;
    private final Plugin itemsAdder;

    public ItemsAdderHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
    }

    public boolean isAvailable() {
        return itemsAdder != null && itemsAdder.isEnabled();
    }

    public static boolean isCustomId(String material) {
        return material != null && material.toLowerCase(java.util.Locale.ROOT).startsWith(PREFIX);
    }

    public static String stripPrefix(String material) {
        return isCustomId(material) ? material.substring(PREFIX.length()) : material;
    }

    public ItemStack getItemStack(String namespacedId) {
        if (!isAvailable() || namespacedId == null || namespacedId.isBlank()) {
            return null;
        }
        Object customStack = HookReflection.invokeStatic(plugin.getLogger(), "ItemsAdder", CUSTOM_STACK_CLASS,
                "getInstance", new Class<?>[]{String.class}, namespacedId);
        Object stack = HookReflection.invokeInstance(plugin.getLogger(), "ItemsAdder", customStack, "getItemStack",
                new Class<?>[0]);
        return stack instanceof ItemStack item ? item : null;
    }

    public boolean isItemsAdderItem(ItemStack item) {
        return customStackOf(item) != null;
    }

    public String getNamespacedId(ItemStack item) {
        Object customStack = customStackOf(item);
        if (customStack == null) {
            return null;
        }
        Object id = HookReflection.invokeInstance(plugin.getLogger(), "ItemsAdder", customStack, "getNamespacedID",
                new Class<?>[0]);
        return id instanceof String s ? s : null;
    }

    private Object customStackOf(ItemStack item) {
        if (!isAvailable() || item == null) {
            return null;
        }
        return HookReflection.invokeStatic(plugin.getLogger(), "ItemsAdder", CUSTOM_STACK_CLASS, "byItemStack",
                new Class<?>[]{ItemStack.class}, item);
    }
}
