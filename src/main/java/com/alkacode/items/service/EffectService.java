package com.alkacode.items.service;

import com.alkacode.items.config.ConfigManager;
import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.model.ItemEffect;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.model.ParamMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Aplica/remove {@link ItemEffect}s enquanto ON_EQUIP/ON_HOLD/ON_PASSIVE estao ativos.
 * Efeitos "de estado" (POTION continuo/NIGHT_VISION/WATER_BREATHING/FIRE_RESISTANCE)
 * sao contados por referencia (2 fontes concedendo o mesmo potion effect nao removem
 * um ao outro cedo demais); ATTRIBUTE/SPEED/JUMP usam uma NamespacedKey deterministica
 * por (templateId, trigger, indice do efeito), entao aplicar/remover sempre acerta o
 * MESMO modificador sem precisar guardar estado extra - stackam naturalmente como
 * qualquer AttributeModifier vanilla. FLY/NO_FALL_DAMAGE/DOUBLE_JUMP/DASH sao flags
 * booleanas tambem contadas por referencia (ver getters usados por
 * {@link com.alkacode.items.listener.ItemEffectMovementListener}).
 */
public final class EffectService {

    private final JavaPlugin plugin;
    private final ConfigManager config;

    private final Map<UUID, Map<PotionEffectType, Integer>> potionRefCount = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> flyRefCount = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> noFallDamageRefCount = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> doubleJumpRefCount = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> dashRefCount = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> activePassiveTemplates = new ConcurrentHashMap<>();

    public EffectService(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void applyAll(Player player, ItemTemplate template, EffectTrigger trigger) {
        List<ItemEffect> effects = effectsFor(template, trigger);
        for (int i = 0; i < effects.size(); i++) {
            apply(player, template.id(), trigger, i, effects.get(i));
        }
    }

    public void removeAll(Player player, ItemTemplate template, EffectTrigger trigger) {
        List<ItemEffect> effects = effectsFor(template, trigger);
        for (int i = 0; i < effects.size(); i++) {
            remove(player, template.id(), trigger, i, effects.get(i));
        }
    }

    private List<ItemEffect> effectsFor(ItemTemplate template, EffectTrigger trigger) {
        return switch (trigger) {
            case ON_EQUIP, ON_UNEQUIP -> template.effectsOnEquip();
            case ON_HOLD, ON_UNHOLD -> template.effectsOnHold();
            case ON_PASSIVE -> template.effectsPassive();
            default -> List.of();
        };
    }

    private void apply(Player player, String sourceId, EffectTrigger trigger, int index, ItemEffect effect) {
        ParamMap p = effect.params();
        UUID uuid = player.getUniqueId();
        switch (effect.type()) {
            case POTION -> addPotionRef(player, potionType(p), p.getInt("duration-ticks", 100), p.getInt("amplifier", 0));
            case NIGHT_VISION -> addPotionRef(player, PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0);
            case WATER_BREATHING -> addPotionRef(player, PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0);
            case FIRE_RESISTANCE -> addPotionRef(player, PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0);
            case ATTRIBUTE -> addAttribute(player, attributeKey(sourceId, trigger, index),
                    attributeOf(p.getString("attribute", "GENERIC_ARMOR")), p.getDouble("modifier", 0),
                    operationOf(p.getString("operation", "ADD_NUMBER")));
            case SPEED -> addAttribute(player, attributeKey(sourceId, trigger, index), Attribute.MOVEMENT_SPEED,
                    p.getDouble("multiplier", 1.0) - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            case JUMP -> addAttribute(player, attributeKey(sourceId, trigger, index), Attribute.JUMP_STRENGTH,
                    p.getDouble("modifier", 0.1), AttributeModifier.Operation.ADD_NUMBER);
            case FLY -> { flyRefCount.merge(uuid, 1, Integer::sum); player.setAllowFlight(true); }
            case NO_FALL_DAMAGE -> noFallDamageRefCount.merge(uuid, 1, Integer::sum);
            case DOUBLE_JUMP -> doubleJumpRefCount.merge(uuid, 1, Integer::sum);
            case DASH -> dashRefCount.merge(uuid, 1, Integer::sum);
            case COMMAND -> {
                if (trigger == EffectTrigger.ON_EQUIP || trigger == EffectTrigger.ON_HOLD) {
                    runCommand(player, p);
                }
            }
            case PARTICLE, SOUND -> {
                // Disparo unico na ativacao - repeticao periodica (interval-ticks) e feita
                // pela task de tick, nao aqui (ver EffectTickTask).
            }
        }
    }

    private void remove(Player player, String sourceId, EffectTrigger trigger, int index, ItemEffect effect) {
        UUID uuid = player.getUniqueId();
        switch (effect.type()) {
            case POTION -> removePotionRef(player, potionType(effect.params()));
            case NIGHT_VISION -> removePotionRef(player, PotionEffectType.NIGHT_VISION);
            case WATER_BREATHING -> removePotionRef(player, PotionEffectType.WATER_BREATHING);
            case FIRE_RESISTANCE -> removePotionRef(player, PotionEffectType.FIRE_RESISTANCE);
            case ATTRIBUTE -> removeAttribute(player, attributeKey(sourceId, trigger, index),
                    attributeOf(effect.params().getString("attribute", "GENERIC_ARMOR")));
            case SPEED -> removeAttribute(player, attributeKey(sourceId, trigger, index), Attribute.MOVEMENT_SPEED);
            case JUMP -> removeAttribute(player, attributeKey(sourceId, trigger, index), Attribute.JUMP_STRENGTH);
            case FLY -> {
                int left = flyRefCount.merge(uuid, -1, Integer::sum);
                if (left <= 0) {
                    flyRefCount.remove(uuid);
                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                }
            }
            case NO_FALL_DAMAGE -> decRef(noFallDamageRefCount, uuid);
            case DOUBLE_JUMP -> decRef(doubleJumpRefCount, uuid);
            case DASH -> decRef(dashRefCount, uuid);
            case COMMAND, PARTICLE, SOUND -> { }
        }
    }

    private void decRef(Map<UUID, Integer> map, UUID uuid) {
        int left = map.merge(uuid, -1, Integer::sum);
        if (left <= 0) {
            map.remove(uuid);
        }
    }

    private void addPotionRef(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        if (type == null) return;
        potionRefCount.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).merge(type, 1, Integer::sum);
        player.addPotionEffect(new PotionEffect(type, durationTicks < 0 ? Integer.MAX_VALUE : durationTicks, amplifier, true, false));
    }

    private void removePotionRef(Player player, PotionEffectType type) {
        if (type == null) return;
        Map<PotionEffectType, Integer> perPlayer = potionRefCount.get(player.getUniqueId());
        if (perPlayer == null) return;
        int left = perPlayer.merge(type, -1, Integer::sum);
        if (left <= 0) {
            perPlayer.remove(type);
            player.removePotionEffect(type);
        }
    }

    private void addAttribute(Player player, NamespacedKey key, Attribute attribute, double amount, AttributeModifier.Operation op) {
        if (attribute == null || player.getAttribute(attribute) == null) return;
        var instance = player.getAttribute(attribute);
        instance.getModifiers().stream().filter(m -> m.getKey().equals(key)).findFirst()
                .ifPresent(instance::removeModifier);
        instance.addModifier(new AttributeModifier(key, amount, op));
    }

    private void removeAttribute(Player player, NamespacedKey key, Attribute attribute) {
        if (attribute == null || player.getAttribute(attribute) == null) return;
        var instance = player.getAttribute(attribute);
        instance.getModifiers().stream().filter(m -> m.getKey().equals(key)).findFirst()
                .ifPresent(instance::removeModifier);
    }

    private NamespacedKey attributeKey(String sourceId, EffectTrigger trigger, int index) {
        return new NamespacedKey(plugin, "effect_" + sourceId + "_" + trigger.name().toLowerCase(Locale.ROOT) + "_" + index);
    }

    private PotionEffectType potionType(ParamMap p) {
        return PotionEffectType.getByKey(NamespacedKey.minecraft(p.getString("potion", "").toLowerCase(Locale.ROOT)));
    }

    private Attribute attributeOf(String name) {
        return org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
    }

    private AttributeModifier.Operation operationOf(String name) {
        try {
            return AttributeModifier.Operation.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
    }

    private void runCommand(Player player, ParamMap p) {
        String cmd = p.getString("command", "").replace("{player}", player.getName());
        if (cmd.isBlank()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    public void runUseEffects(Player player, ItemTemplate template) {
        for (ItemEffect effect : template.effectsOnUse()) {
            if (effect.type() == com.alkacode.items.effect.EffectType.PARTICLE
                    || effect.type() == com.alkacode.items.effect.EffectType.SOUND) {
                fireOneShot(player, effect);
            } else if (effect.type() == com.alkacode.items.effect.EffectType.COMMAND) {
                runCommand(player, effect.params());
            }
        }
    }

    public void fireOneShot(Player player, ItemEffect effect) {
        ParamMap p = effect.params();
        if (effect.type() == com.alkacode.items.effect.EffectType.PARTICLE) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(p.getString("particle", "FLAME").toUpperCase(Locale.ROOT));
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), p.getInt("count", 10));
            } catch (IllegalArgumentException ignored) { }
        } else if (effect.type() == com.alkacode.items.effect.EffectType.SOUND) {
            org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(NamespacedKey.minecraft(p.getString("sound", "").toLowerCase(Locale.ROOT)));
            if (sound != null) {
                player.playSound(player.getLocation(), sound, (float) p.getDouble("volume", 1.0), (float) p.getDouble("pitch", 1.0));
            }
        }
    }

    // ---- flags consultadas pelo ItemEffectMovementListener ----
    public boolean hasNoFallDamage(Player player) { return noFallDamageRefCount.getOrDefault(player.getUniqueId(), 0) > 0; }
    public boolean hasDoubleJump(Player player) { return doubleJumpRefCount.getOrDefault(player.getUniqueId(), 0) > 0; }
    public boolean hasDash(Player player) { return dashRefCount.getOrDefault(player.getUniqueId(), 0) > 0; }

    // ---- ON_PASSIVE: chamado periodicamente com o snapshot atual de templates passivos no inventario ----
    public void syncPassive(Player player, Set<String> currentTemplateIds, java.util.function.Function<String, ItemTemplate> resolver) {
        Set<String> previous = activePassiveTemplates.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArraySet<>());
        for (String id : Set.copyOf(previous)) {
            if (!currentTemplateIds.contains(id)) {
                ItemTemplate template = resolver.apply(id);
                if (template != null) {
                    removeAll(player, template, EffectTrigger.ON_PASSIVE);
                }
                previous.remove(id);
            }
        }
        for (String id : currentTemplateIds) {
            if (previous.add(id)) {
                ItemTemplate template = resolver.apply(id);
                if (template != null) {
                    applyAll(player, template, EffectTrigger.ON_PASSIVE);
                }
            }
        }
    }

    public void clearPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        potionRefCount.remove(uuid);
        flyRefCount.remove(uuid);
        noFallDamageRefCount.remove(uuid);
        doubleJumpRefCount.remove(uuid);
        dashRefCount.remove(uuid);
        activePassiveTemplates.remove(uuid);
    }
}
