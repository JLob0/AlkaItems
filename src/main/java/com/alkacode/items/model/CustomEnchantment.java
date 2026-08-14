package com.alkacode.items.model;

import com.alkacode.items.enchant.EnchantTrigger;

import java.util.List;

public record CustomEnchantment(
        String id,
        String displayName,
        int maxLevel,
        List<String> applicableMaterials,   // suporta wildcard "*_SWORD" e "itemsadder:id"
        EnchantTrigger trigger,
        double baseChance,                  // % no nivel 1
        double chancePerLevel,              // % adicional por nivel acima de 1
        List<EnchantEffect> effects,
        String description,
        boolean showInLore,
        String loreFormat                   // "<dark_purple>%name% %roman%"
) {

    public double chanceAt(int level) {
        return Math.min(100.0, baseChance + chancePerLevel * Math.max(0, level - 1));
    }

    /** Suporta "*_SWORD"/"*_PICKAXE" (sufixo) e materiais/ids exatos (vanilla ou "itemsadder:x"). */
    public boolean appliesTo(String materialOrId) {
        if (applicableMaterials == null || applicableMaterials.isEmpty()) {
            return true;
        }
        String upper = materialOrId.toUpperCase(java.util.Locale.ROOT);
        for (String pattern : applicableMaterials) {
            String p = pattern.toUpperCase(java.util.Locale.ROOT);
            if (p.startsWith("*") && upper.endsWith(p.substring(1))) {
                return true;
            }
            if (p.equals(upper)) {
                return true;
            }
        }
        return false;
    }
}
