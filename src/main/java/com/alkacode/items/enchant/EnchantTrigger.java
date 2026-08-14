package com.alkacode.items.enchant;

/** Quando um {@link com.alkacode.items.model.CustomEnchantment} rola sua chance de ativar. */
public enum EnchantTrigger {
    ON_HIT,
    ON_KILL,
    ON_BLOCK_BREAK,
    ON_DAMAGE_TAKEN,
    ON_SHOOT,
    ON_FISH,
    ON_DEATH,
    ON_SNEAK,
    ON_SPRINT,
    ON_PROJECTILE_HIT,
    /** Marcador de "encantamento de set" - nunca disparado via EnchantService#trigger (loop de
     * proc normal), so usado pra identificar que CustomEnchantment#setRequirement() e relevante.
     * Ver model/CustomEnchantment e service/EnchantService#updateArmorSetStatus. */
    ON_SET_COMPLETE,
    ON_SET_INCOMPLETE
}
