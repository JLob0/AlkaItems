package com.alkacode.items.enchant;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/** Contexto passado pra {@link com.alkacode.items.util.EnchantEffectApplier} - o {@code event} bruto
 * e opcional/nullable e so e mutado pelos tipos de efeito que fazem sentido pro trigger que o gerou
 * (DAMAGE/CRITICAL/DODGE esperam um EntityDamageEvent, DROP_MULTIPLIER/XP_BOOST esperam um
 * BlockBreakEvent - efeito aplicado num trigger "errado" so ignora o event silenciosamente). */
public record EnchantContext(Player player, LivingEntity target, Event event) {
}
