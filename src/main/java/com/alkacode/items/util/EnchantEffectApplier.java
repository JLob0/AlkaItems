package com.alkacode.items.util;

import com.alkacode.items.enchant.EnchantContext;
import com.alkacode.items.model.EnchantEffect;
import com.alkacode.items.model.ParamMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Executa um {@link EnchantEffect} ja "aprovado" (a chance do CustomEnchantment ja foi
 * rolada em EnchantService antes de chegar aqui). Cada tipo que precisa mudar o
 * resultado do evento que disparou o gatilho (DAMAGE/CRITICAL/DODGE/LIFESTEAL/
 * DROP_MULTIPLIER/XP_BOOST) faz isso via {@link EnchantContext#event()} - se o evento
 * nao for do tipo esperado pro trigger que configurou esse efeito (ex: DROP_MULTIPLIER
 * num encantamento ON_HIT), o efeito e ignorado silenciosamente em vez de travar o
 * resto da cadeia de efeitos.
 */
public final class EnchantEffectApplier {

    private static final Random RANDOM = new Random();

    private EnchantEffectApplier() {
    }

    public static void apply(EnchantEffect effect, EnchantContext ctx, Logger logger) {
        ParamMap p = effect.params();
        try {
            switch (effect.type()) {
                case POTION_TARGET -> potion(ctx.target(), p);
                case POTION_SELF -> potion(ctx.player(), p);
                case POTION_AREA -> potionArea(ctx, p);
                case DAMAGE -> damage(ctx, p);
                case HEAL -> heal(ctx, p);
                case TELEPORT -> teleport(ctx, p);
                case LIGHTNING -> lightning(ctx, p);
                case EXPLOSION -> explosion(ctx, p);
                case PARTICLE -> particle(ctx, p);
                case SOUND -> sound(ctx, p);
                case COMMAND -> command(ctx, p);
                case DROP_MULTIPLIER -> dropMultiplier(ctx, p);
                case XP_BOOST -> xpBoost(ctx, p);
                case LIFESTEAL -> lifesteal(ctx, p);
                case DODGE -> dodge(ctx, p);
                case CRITICAL -> critical(ctx, p);
            }
        } catch (Exception e) {
            logger.fine("Falha ao aplicar efeito de encantamento " + effect.type() + ": " + e);
        }
    }

    private static void potion(LivingEntity entity, ParamMap p) {
        if (entity == null) return;
        PotionEffectType type = PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft(
                p.getString("potion", "").toLowerCase(Locale.ROOT)));
        if (type == null) return;
        entity.addPotionEffect(new PotionEffect(type, p.getInt("duration-ticks", 100), p.getInt("amplifier", 0)));
    }

    private static void potionArea(EnchantContext ctx, ParamMap p) {
        LivingEntity center = ctx.target() != null ? ctx.target() : ctx.player();
        if (center == null) return;
        double radius = p.getDouble("radius", 5.0);
        for (org.bukkit.entity.Entity nearby : center.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity living) {
                potion(living, p);
            }
        }
    }

    private static void damage(EnchantContext ctx, ParamMap p) {
        if (ctx.event() instanceof EntityDamageEvent damageEvent) {
            damageEvent.setDamage(damageEvent.getDamage() + p.getDouble("amount", 1.0));
        } else if (ctx.target() != null) {
            ctx.target().damage(p.getDouble("amount", 1.0), ctx.player());
        }
    }

    private static void heal(EnchantContext ctx, ParamMap p) {
        if (ctx.player() == null) return;
        double amount = p.getDouble("amount", 1.0);
        double max = ctx.player().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? ctx.player().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0;
        ctx.player().setHealth(Math.min(max, ctx.player().getHealth() + amount));
    }

    private static void teleport(EnchantContext ctx, ParamMap p) {
        if (ctx.target() == null || ctx.player() == null) return;
        double distance = p.getDouble("distance", 5.0);
        String direction = p.getString("direction", "BACKWARD").toUpperCase(Locale.ROOT);
        Location from = ctx.target().getLocation();
        org.bukkit.util.Vector vector;
        if (direction.equals("RANDOM")) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            vector = new org.bukkit.util.Vector(Math.cos(angle), 0, Math.sin(angle));
        } else {
            vector = from.toVector().subtract(ctx.player().getLocation().toVector());
            if (vector.lengthSquared() < 0.01) {
                vector = new org.bukkit.util.Vector(1, 0, 0);
            }
            vector.normalize();
        }
        Location to = from.clone().add(vector.multiply(distance));
        to.setY(from.getY());
        ctx.target().teleport(to);
    }

    private static void lightning(EnchantContext ctx, ParamMap p) {
        if (ctx.target() == null) return;
        Location loc = ctx.target().getLocation();
        if (p.getBoolean("fire", false)) {
            loc.getWorld().strikeLightning(loc);
        } else {
            loc.getWorld().strikeLightningEffect(loc);
        }
        double damage = p.getDouble("damage", 0.0);
        if (damage > 0) {
            ctx.target().damage(damage, ctx.player());
        }
    }

    private static void explosion(EnchantContext ctx, ParamMap p) {
        if (ctx.target() == null) return;
        Location loc = ctx.target().getLocation();
        loc.getWorld().createExplosion(loc, (float) p.getDouble("power", 1.0),
                p.getBoolean("fire", false), p.getBoolean("break-blocks", false), ctx.player());
    }

    private static void particle(EnchantContext ctx, ParamMap p) {
        Location loc = particleTarget(ctx, p.getString("target", "SELF"));
        if (loc == null) return;
        try {
            Particle particle = Particle.valueOf(p.getString("particle", "FLAME").toUpperCase(Locale.ROOT));
            loc.getWorld().spawnParticle(particle, loc, p.getInt("count", 10));
        } catch (IllegalArgumentException ignored) {
            // nome de particula invalido em enchants.yml/items.yml - ignora silenciosamente
        }
    }

    private static Location particleTarget(EnchantContext ctx, String target) {
        return switch (target.toUpperCase(Locale.ROOT)) {
            case "VICTIM" -> ctx.target() != null ? ctx.target().getLocation() : null;
            case "SELF" -> ctx.player() != null ? ctx.player().getLocation() : null;
            default -> ctx.player() != null ? ctx.player().getLocation() : null;
        };
    }

    private static void sound(EnchantContext ctx, ParamMap p) {
        Player player = ctx.player();
        if (player == null) return;
        Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(p.getString("sound", "").toLowerCase(Locale.ROOT)));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, (float) p.getDouble("volume", 1.0), (float) p.getDouble("pitch", 1.0));
        }
    }

    private static void command(EnchantContext ctx, ParamMap p) {
        if (ctx.player() == null) return;
        String cmd = p.getString("command", "").replace("{player}", ctx.player().getName());
        if (cmd.isBlank()) return;
        if ("PLAYER".equalsIgnoreCase(p.getString("executor", "CONSOLE"))) {
            ctx.player().performCommand(cmd);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private static void dropMultiplier(EnchantContext ctx, ParamMap p) {
        if (ctx.event() instanceof BlockBreakEvent breakEvent) {
            breakEvent.setExpToDrop((int) Math.round(breakEvent.getExpToDrop() * p.getDouble("multiplier", 2.0)));
            // Multiplicar itens dropados de verdade exigiria cancelar o break vanilla e
            // dropar manualmente (perderia ferramentas com Fortune/Silk Touch corretos) -
            // aplicado no listener via getDrops(), nao aqui (ver ItemBreakListener).
        }
    }

    private static void xpBoost(EnchantContext ctx, ParamMap p) {
        if (ctx.event() instanceof BlockBreakEvent breakEvent) {
            breakEvent.setExpToDrop((int) Math.round(breakEvent.getExpToDrop() * p.getDouble("multiplier", 1.5)));
        }
    }

    private static void lifesteal(EnchantContext ctx, ParamMap p) {
        if (ctx.player() == null || !(ctx.event() instanceof EntityDamageEvent damageEvent)) return;
        double percent = p.getDouble("percent-of-damage", 20.0) / 100.0;
        double heal = damageEvent.getDamage() * percent;
        double max = ctx.player().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? ctx.player().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0;
        ctx.player().setHealth(Math.min(max, ctx.player().getHealth() + heal));
    }

    private static void dodge(EnchantContext ctx, ParamMap p) {
        if (!(ctx.event() instanceof EntityDamageEvent damageEvent)) return;
        double chance = p.getDouble("chance", 100.0);
        if (RANDOM.nextDouble() * 100 < chance) {
            damageEvent.setCancelled(true);
        }
    }

    private static void critical(EnchantContext ctx, ParamMap p) {
        if (ctx.event() instanceof EntityDamageEvent damageEvent) {
            damageEvent.setDamage(damageEvent.getDamage() * p.getDouble("multiplier", 1.5));
        }
    }
}
