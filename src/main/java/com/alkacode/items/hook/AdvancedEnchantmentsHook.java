package com.alkacode.items.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Soft-dependency para o AdvancedEnchantments (net.advancedplugins.ae.api.AEAPI) via
 * reflection - assinaturas reais confirmadas via javap, ver [[reference-advancedenchantments-api]]
 * e com.alkacode.anvil.enchant.AdvancedEnchantmentsWrapper (AlkaAnvil, mesma sessao).
 * AlkaItems NAO substitui o AE - coexiste (um item pode ter encantamentos AlkaItems E
 * AE ao mesmo tempo). Escopo aqui e so aplicar/consultar, sem a migracao/import
 * completa de definicoes de encantamento do spec original (deferido).
 */
public final class AdvancedEnchantmentsHook {

    private static final String API_CLASS = "net.advancedplugins.ae.api.AEAPI";

    private final JavaPlugin plugin;
    private final boolean available;

    public AdvancedEnchantmentsHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().getPlugin("AdvancedEnchantments") != null
                && Bukkit.getPluginManager().isPluginEnabled("AdvancedEnchantments");
    }

    public boolean isAvailable() {
        return available;
    }

    /** Aplica (level > 0) ou remove (level <= 0) um encantamento AE - retorna o ItemStack resultante
     * (a API do AE devolve um item NOVO em vez de mutar in-place). */
    public ItemStack applyEnchant(ItemStack item, String aeName, int level) {
        if (!available) {
            return item;
        }
        Object result = level <= 0
                ? HookReflection.invokeStatic(plugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                        "removeEnchantment", new Class<?>[]{ItemStack.class, String.class}, item, aeName)
                : HookReflection.invokeStatic(plugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                        "applyEnchant", new Class<?>[]{String.class, int.class, ItemStack.class}, aeName, level, item);
        return result instanceof ItemStack stack ? stack : item;
    }

    public int getEnchantLevel(ItemStack item, String aeName) {
        if (!available || item == null) {
            return 0;
        }
        Object result = HookReflection.invokeStatic(plugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "getEnchantLevel", new Class<?>[]{String.class, ItemStack.class}, aeName, item);
        return result instanceof Integer level ? level : 0;
    }

    public boolean hasEnchant(ItemStack item, String aeName) {
        return getEnchantLevel(item, aeName) > 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> allEnchantmentsOnItem(ItemStack item) {
        if (!available || item == null) {
            return Map.of();
        }
        Object result = HookReflection.invokeStatic(plugin.getLogger(), "AdvancedEnchantments", API_CLASS,
                "getEnchantmentsOnItem", new Class<?>[]{ItemStack.class}, item);
        return result instanceof Map<?, ?> map ? (Map<String, Integer>) map : Map.of();
    }
}
