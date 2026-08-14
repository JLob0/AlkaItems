package com.alkacode.items.effect;

/** Quando um {@link com.alkacode.items.model.ItemEffect} liga/desliga. */
public enum EffectTrigger {
    ON_EQUIP,
    ON_UNEQUIP,
    ON_HOLD,
    ON_UNHOLD,
    ON_USE,
    ON_PASSIVE,
    ON_TICK,
    /** Usado internamente pelo "Set Bonus" (service/EnchantService#updateArmorSetStatus) como
     * chave estavel de apply/remove - nao corresponde a uma secao items.yml (ver
     * model/CustomEnchantment#setBonusEffects). */
    ON_SET_COMPLETE
}
