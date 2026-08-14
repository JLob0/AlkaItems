package com.alkacode.items.service;

import com.alkacode.items.config.ConfigManager;
import com.alkacode.items.config.EnchantsConfig;
import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.effect.EffectType;
import com.alkacode.items.enchant.EnchantContext;
import com.alkacode.items.enchant.EnchantTrigger;
import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.EnchantEffect;
import com.alkacode.items.model.ItemEffect;
import com.alkacode.items.util.EnchantEffectApplier;
import com.alkacode.items.util.ItemPdc;
import com.alkacode.items.util.TextUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Roda os encantamentos customizados de um item quando o trigger correspondente dispara -
 * chance = baseChance + chancePerLevel*(level-1), rolada uma vez POR ENCANTAMENTO no item
 * (um item pode ter varios encantamentos custom, todos sao checados no mesmo evento).
 *
 * <p>Tambem rastreia "Set Bonus" (Prompt_SetBonus_AlkaItems.md) -
 * {@link #updateArmorSetStatus(Player)} recomputa do zero, a cada chamada, quantas pecas
 * de cada "grupo de set" o jogador tem equipadas (sem guardar snapshot por-slot como o
 * spec original propunha - so o resultado agregado {@code playerActiveSets} precisa ser
 * estado, o resto e recalculado na hora). */
public final class EnchantService {

    private final JavaPlugin plugin;
    private final EnchantsConfig enchantsConfig;
    private final ItemPdc pdc;
    private final EffectService effectService;
    private final ConfigManager config;
    private final Random random = new Random();

    private final Map<UUID, Set<String>> playerActiveSets = new ConcurrentHashMap<>();

    public EnchantService(JavaPlugin plugin, EnchantsConfig enchantsConfig, ItemPdc pdc,
                           EffectService effectService, ConfigManager config) {
        this.plugin = plugin;
        this.enchantsConfig = enchantsConfig;
        this.pdc = pdc;
        this.effectService = effectService;
        this.config = config;
    }

    public void trigger(EnchantTrigger trigger, Player player, ItemStack item, LivingEntity target, Event event) {
        if (item == null) {
            return;
        }
        Map<String, Integer> customEnchants = pdc.getCustomEnchants(item);
        if (customEnchants.isEmpty()) {
            return;
        }
        EnchantContext ctx = new EnchantContext(player, target, event);
        for (Map.Entry<String, Integer> entry : customEnchants.entrySet()) {
            CustomEnchantment enchant = enchantsConfig.get(entry.getKey());
            if (enchant == null || enchant.trigger() != trigger) {
                continue;
            }
            int level = entry.getValue();
            double chance = enchant.chanceAt(level);
            if (random.nextDouble() * 100.0 >= chance) {
                continue;
            }
            for (EnchantEffect effect : enchant.effects()) {
                EnchantEffectApplier.apply(effect, ctx, plugin.getLogger());
            }
        }
    }

    public EnchantsConfig enchantsConfig() {
        return enchantsConfig;
    }

    // ---------------------------------------------------------------- Set Bonus

    /** Chamado ao equipar/desequipar armadura (ItemEquipListener) e periodicamente (EffectTickTask,
     * cobre login com set ja equipado e mudancas via /alkaitems enchant). Idempotente - recalcular
     * o mesmo estado nao dispara efeito de novo. */
    public void updateArmorSetStatus(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, CustomEnchantment> representative = new LinkedHashMap<>();
        Map<String, Integer> counts = countPiecesByGroup(player, representative);

        Set<String> previouslyActive = playerActiveSets.getOrDefault(uuid, Set.of());
        Set<String> nowActive = new HashSet<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String groupKey = entry.getKey();
            CustomEnchantment enchant = representative.get(groupKey);
            if (enchant.setRequirement().minPieces() > entry.getValue()) {
                continue;
            }
            nowActive.add(groupKey);
            if (!previouslyActive.contains(groupKey)) {
                onSetComplete(player, groupKey, enchant);
            }
        }

        for (String groupKey : previouslyActive) {
            if (!nowActive.contains(groupKey)) {
                CustomEnchantment enchant = representative.get(groupKey);
                if (enchant == null) {
                    // O jogador tirou TODAS as pecas do set (nao ha mais representante no scan atual) -
                    // ainda assim precisamos achar o encantamento pra saber que efeitos desfazer.
                    enchant = findEnchantForGroup(groupKey);
                }
                if (enchant != null) {
                    onSetIncomplete(player, groupKey, enchant);
                }
            }
        }

        if (nowActive.isEmpty()) {
            playerActiveSets.remove(uuid);
        } else {
            playerActiveSets.put(uuid, nowActive);
        }
    }

    private void onSetComplete(Player player, String groupKey, CustomEnchantment enchant) {
        effectService.applyEffects(player, "set_" + groupKey, EffectTrigger.ON_SET_COMPLETE, enchant.setBonusEffects());
        fireCosmeticOneShots(player, enchant.setBonusEffects());
        player.sendMessage(TextUtil.parse(config.prefix() + config.message("items.set.activated"),
                Map.of("set", TextUtil.plain(enchant.displayName()))));
    }

    private void onSetIncomplete(Player player, String groupKey, CustomEnchantment enchant) {
        effectService.removeEffects(player, "set_" + groupKey, EffectTrigger.ON_SET_COMPLETE, enchant.setBonusEffects());
        fireCosmeticOneShots(player, enchant.setBonusEffectsOnRemove());
        player.sendMessage(TextUtil.parse(config.prefix() + config.message("items.set.deactivated"),
                Map.of("set", TextUtil.plain(enchant.displayName()))));
    }

    private void fireCosmeticOneShots(Player player, List<ItemEffect> effects) {
        for (ItemEffect effect : effects) {
            if (effect.type() == EffectType.PARTICLE || effect.type() == EffectType.SOUND) {
                effectService.fireOneShot(player, effect);
            }
        }
    }

    private Map<String, Integer> countPiecesByGroup(Player player, Map<String, CustomEnchantment> representativeOut) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armor = {inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots()};
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) {
                continue;
            }
            for (String enchantId : pdc.getCustomEnchants(piece).keySet()) {
                CustomEnchantment enchant = enchantsConfig.get(enchantId);
                if (enchant == null || !enchant.isSetBonus()) {
                    continue;
                }
                String groupKey = groupKeyOf(enchant);
                counts.merge(groupKey, 1, Integer::sum);
                representativeOut.putIfAbsent(groupKey, enchant);
            }
        }
        return counts;
    }

    private String groupKeyOf(CustomEnchantment enchant) {
        return enchant.setRequirement().piecesMustMatch() ? enchant.id() : enchant.setRequirement().setGroup();
    }

    private CustomEnchantment findEnchantForGroup(String groupKey) {
        for (CustomEnchantment enchant : enchantsConfig.all()) {
            if (enchant.isSetBonus() && groupKeyOf(enchant).equals(groupKey)) {
                return enchant;
            }
        }
        return null;
    }

    public Set<String> activeSets(Player player) {
        return playerActiveSets.getOrDefault(player.getUniqueId(), Set.of());
    }

    /** %alkaitems_set_bonus_name% - nome do primeiro set ativo, ou "" se nenhum. */
    public String activeSetDisplayName(Player player) {
        Set<String> active = activeSets(player);
        if (active.isEmpty()) {
            return "";
        }
        CustomEnchantment enchant = findEnchantForGroup(active.iterator().next());
        return enchant != null ? TextUtil.plain(enchant.displayName()) : "";
    }

    /** %alkaitems_set_pieces_equipped_<enchant_id>% - conta as pecas do MESMO grupo que o
     * encantamento informado pertence, nao so pecas com esse id exato (relevante quando
     * pieces-must-match=false). */
    public int countSetPieces(Player player, String enchantId) {
        CustomEnchantment enchant = enchantsConfig.get(enchantId);
        if (enchant == null || !enchant.isSetBonus()) {
            return 0;
        }
        Map<String, CustomEnchantment> representative = new LinkedHashMap<>();
        Map<String, Integer> counts = countPiecesByGroup(player, representative);
        return counts.getOrDefault(groupKeyOf(enchant), 0);
    }

    /** Todos os efeitos com interval-ticks das secoes atualmente ativas do jogador - consumido
     * pela EffectTickTask pra reexecutar PARTICLE/SOUND periodicos enquanto o set continua completo. */
    public List<ItemEffect> activeSetIntervalEffects(Player player) {
        List<ItemEffect> result = new ArrayList<>();
        for (String groupKey : activeSets(player)) {
            CustomEnchantment enchant = findEnchantForGroup(groupKey);
            if (enchant != null) {
                result.addAll(enchant.setBonusEffects());
            }
        }
        return result;
    }

    /** %alkaitems_set_pieces_equipped% - "atual/necessario" do grupo de set com mais pecas
     * equipadas no momento (o mais "perto" de completar), ou "0/0" se nenhuma peca de set. */
    public String bestSetProgress(Player player) {
        Map<String, CustomEnchantment> representative = new LinkedHashMap<>();
        Map<String, Integer> counts = countPiecesByGroup(player, representative);
        if (counts.isEmpty()) {
            return "0/0";
        }
        String bestGroup = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestGroup = entry.getKey();
            }
        }
        CustomEnchantment enchant = representative.get(bestGroup);
        return bestCount + "/" + enchant.setRequirement().minPieces();
    }

    public void clearPlayer(Player player) {
        playerActiveSets.remove(player.getUniqueId());
    }
}
