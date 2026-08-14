package com.alkacode.items.config;

import com.alkacode.items.enchant.EnchantEffectType;
import com.alkacode.items.enchant.EnchantTrigger;
import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.EnchantEffect;
import com.alkacode.items.model.ParamMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** enchants.yml - so-leitura (criar/editar encantamento customizado novo = YAML + /alkaitems reload,
 * decisao de escopo igual a items.yml, so que aqui sem GUI de edicao nenhuma - o objeto e mais
 * complexo que um item e o editor visual completo do spec original foi deixado de fora). */
public final class EnchantsConfig {

    private final JavaPlugin plugin;
    private final Map<String, CustomEnchantment> enchantments = new LinkedHashMap<>();

    public EnchantsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "enchants.yml");
        if (!file.exists()) {
            plugin.saveResource("enchants.yml", false);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        enchantments.clear();

        ConfigurationSection root = yaml.getConfigurationSection("enchantments");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            enchantments.put(id.toLowerCase(Locale.ROOT), parse(id, section));
        }
    }

    private CustomEnchantment parse(String id, ConfigurationSection section) {
        EnchantTrigger trigger;
        try {
            trigger = EnchantTrigger.valueOf(section.getString("trigger", "ON_HIT").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Encantamento '" + id + "' com trigger invalido, usando ON_HIT.");
            trigger = EnchantTrigger.ON_HIT;
        }

        List<EnchantEffect> effects = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("effects")) {
            EnchantEffect effect = parseEffect(id, raw);
            if (effect != null) {
                effects.add(effect);
            }
        }

        return new CustomEnchantment(
                id.toLowerCase(Locale.ROOT),
                section.getString("display-name", id),
                Math.max(1, section.getInt("max-level", 1)),
                section.getStringList("applicable-materials"),
                trigger,
                section.getDouble("base-chance", 100.0),
                section.getDouble("chance-per-level", 0.0),
                effects,
                section.getString("description", ""),
                section.getBoolean("show-in-lore", true),
                section.getString("lore-format", "<gray><name> <level_roman>")
        );
    }

    @SuppressWarnings("unchecked")
    private EnchantEffect parseEffect(String enchantId, Map<?, ?> raw) {
        Object typeRaw = raw.get("type");
        if (!(typeRaw instanceof String typeStr)) {
            return null;
        }
        EnchantEffectType type;
        try {
            type = EnchantEffectType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Encantamento '" + enchantId + "' com efeito de tipo invalido: " + typeStr);
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!"type".equals(entry.getKey())) {
                params.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return new EnchantEffect(type, new ParamMap(params));
    }

    public CustomEnchantment get(String id) {
        return id == null ? null : enchantments.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public List<CustomEnchantment> all() {
        return List.copyOf(enchantments.values());
    }
}
