package com.alkacode.items.effect;

/** Tipos de efeito aplicaveis por um {@link com.alkacode.items.model.ItemEffect}. */
public enum EffectType {
    POTION,
    ATTRIBUTE,
    COMMAND,
    PARTICLE,
    SOUND,
    SPEED,
    JUMP,
    FLY,
    NIGHT_VISION,
    WATER_BREATHING,
    FIRE_RESISTANCE,
    NO_FALL_DAMAGE,
    DOUBLE_JUMP,
    DASH,
    /** Cancela knockback recebido enquanto ativo (ver listener/KnockbackImmunityListener) - flag
     * booleana contada por referencia, mesmo padrao de NO_FALL_DAMAGE. */
    KNOCKBACK_IMMUNITY,
    /** Remove um PotionEffectType especifico do jogador no momento em que o trigger ativa -
     * acao one-shot, nao tem estado pra desfazer (sem contraparte no remove()). */
    POTION_REMOVE,
    /** Remove um Attribute inteiro (todos os modifiers dele) do jogador no momento em que o
     * trigger ativa - acao one-shot, mesma logica do POTION_REMOVE. */
    ATTRIBUTE_REMOVE
}
