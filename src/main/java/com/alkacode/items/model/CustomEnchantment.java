package com.alkacode.items.model;

import com.alkacode.items.enchant.EnchantTrigger;

import java.util.List;

/**
 * {@code setRequirement}/{@code setBonusEffects}/{@code setBonusEffectsOnRemove} sao
 * pro "Set Bonus" (Prompt_SetBonus_AlkaItems.md) - deliberadamente usam
 * {@link ItemEffect} (o sistema de efeito CONTINUO com apply/remove simetrico ja
 * usado por ON_EQUIP/ON_HOLD/ON_PASSIVE), NAO {@link EnchantEffect} (o sistema de
 * proc ONE-SHOT usado por {@code effects} acima) - o spec original reaproveitava o
 * mesmo campo `effects` pros dois casos, mas um bonus de set e um estado CONTINUO
 * enquanto o set fica completo (duracao infinita, precisa desligar quando quebra),
 * exatamente o que {@code service/EffectService} ja resolve corretamente (ref-count
 * de POTION, chave deterministica de ATTRIBUTE) - reusar esse sistema em vez de
 * reimplementar apply/remove simetrico dentro do EnchantService evita duplicar essa
 * logica e evita bugs de "esqueci de escrever o POTION_REMOVE espelhado".
 * {@code setBonusEffectsOnRemove} fica so pra efeitos ONE-SHOT cosmeticos adicionais
 * na desativacao (ex: um som diferente do de ativacao) - nao e um "desfazer" (isso
 * ja acontece automaticamente via remove() do EffectService), so extra.
 */
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
        String loreFormat,                  // "<dark_purple>%name% %roman%"
        SetRequirement setRequirement,      // null = nao e encantamento de set
        List<ItemEffect> setBonusEffects,           // aplicado (e removido simetricamente) quando o set liga/desliga
        List<ItemEffect> setBonusEffectsOnRemove    // extra cosmetico one-shot so na desativacao
) {

    public double chanceAt(int level) {
        return Math.min(100.0, baseChance + chancePerLevel * Math.max(0, level - 1));
    }

    public boolean isSetBonus() {
        return setRequirement != null;
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
