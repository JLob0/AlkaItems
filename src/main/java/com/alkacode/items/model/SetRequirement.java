package com.alkacode.items.model;

/** Requisito de "set bonus" de um {@link CustomEnchantment} - null em
 * {@code CustomEnchantment#setRequirement()} significa "nao e um encantamento de set".
 * {@code piecesMustMatch=true}: todas as pecas precisam ter O MESMO encantamento (agrupa
 * por id do encantamento). {@code piecesMustMatch=false}: pecas podem ter encantamentos
 * DIFERENTES desde que compartilhem o mesmo {@code setGroup} (agrupa por esse id). */
public record SetRequirement(int minPieces, boolean piecesMustMatch, String setGroup) {
}
