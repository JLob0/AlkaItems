package com.alkacode.items.service;

import com.alkacode.items.config.EnchantsConfig;
import com.alkacode.items.enchant.EnchantContext;
import com.alkacode.items.enchant.EnchantTrigger;
import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.EnchantEffect;
import com.alkacode.items.util.EnchantEffectApplier;
import com.alkacode.items.util.ItemPdc;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Random;

/** Roda os encantamentos customizados de um item quando o trigger correspondente dispara -
 * chance = baseChance + chancePerLevel*(level-1), rolada uma vez POR ENCANTAMENTO no item
 * (um item pode ter varios encantamentos custom, todos sao checados no mesmo evento). */
public final class EnchantService {

    private final JavaPlugin plugin;
    private final EnchantsConfig enchantsConfig;
    private final ItemPdc pdc;
    private final Random random = new Random();

    public EnchantService(JavaPlugin plugin, EnchantsConfig enchantsConfig, ItemPdc pdc) {
        this.plugin = plugin;
        this.enchantsConfig = enchantsConfig;
        this.pdc = pdc;
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
}
