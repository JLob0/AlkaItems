package com.alkacode.items.config;

import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.effect.EffectType;
import com.alkacode.items.model.ItemEffect;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.model.ParamMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * items.yml - templates de item. Diferente de {@link EnchantsConfig}, aqui EXISTE
 * escrita de volta pro YAML ({@link #save}) - a ItemEditorMenu (escopo pratico
 * combinado com o usuario: campos clicaveis, sem seletor de material/lore visual)
 * grava direto aqui, e /alkaitems create|save|delete tambem passam por este loader.
 */
public final class ItemsConfig {

    private final JavaPlugin plugin;
    private final Map<String, ItemTemplate> templates = new LinkedHashMap<>();
    private File file;
    private FileConfiguration yaml;

    public ItemsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        templates.clear();

        ConfigurationSection root = yaml.getConfigurationSection("items");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                templates.put(id.toLowerCase(Locale.ROOT), parse(id, section));
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao carregar template '" + id + "': " + e.getMessage());
            }
        }
    }

    private ItemTemplate parse(String id, ConfigurationSection section) {
        Map<String, Integer> vanillaEnchants = new LinkedHashMap<>();
        ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (String key : enchSection.getKeys(false)) {
                vanillaEnchants.put(key.toLowerCase(Locale.ROOT), enchSection.getInt(key));
            }
        }

        Map<String, Integer> customEnchants = new LinkedHashMap<>();
        ConfigurationSection customSection = section.getConfigurationSection("custom-enchantments");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                customEnchants.put(key.toLowerCase(Locale.ROOT), customSection.getInt(key));
            }
        }

        Map<String, Double> attributes = new LinkedHashMap<>();
        ConfigurationSection attrSection = section.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                attributes.put(key.toUpperCase(Locale.ROOT), attrSection.getDouble(key));
            }
        }

        return ItemTemplate.builder(id.toLowerCase(Locale.ROOT))
                .material(section.getString("material", "STONE"))
                .name(section.getString("name", id))
                .lore(section.getStringList("lore"))
                .vanillaEnchantments(vanillaEnchants)
                .customEnchantments(customEnchants)
                .effectsOnEquip(parseEffects(id, section, "effects-on-equip", EffectTrigger.ON_EQUIP))
                .effectsOnHold(parseEffects(id, section, "effects-on-hold", EffectTrigger.ON_HOLD))
                .effectsOnUse(parseEffects(id, section, "effects-on-use", EffectTrigger.ON_USE))
                .effectsPassive(parseEffects(id, section, "effects-passive", EffectTrigger.ON_PASSIVE))
                .soulbound(section.getBoolean("soulbound", false))
                .glow(section.getBoolean("glow", false))
                .unbreakable(section.getBoolean("unbreakable", false))
                .customModelData(section.getInt("custom-model-data", 0))
                .itemsAdderId(section.getString("itemsadder-id", ""))
                .attributes(attributes)
                .maxDurability(section.getInt("max-durability", 0))
                .hideEnchants(section.getBoolean("hide-enchants", false))
                .hideAttributes(section.getBoolean("hide-attributes", false))
                .vipRequired(section.getString("vip-required", ""))
                .rankRequired(section.getInt("rank-required", 0))
                .color(section.getString("color", ""))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ItemEffect> parseEffects(String templateId, ConfigurationSection section, String key, EffectTrigger trigger) {
        List<ItemEffect> result = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList(key)) {
            Object typeRaw = raw.get("type");
            if (!(typeRaw instanceof String typeStr)) {
                continue;
            }
            EffectType type;
            try {
                type = EffectType.valueOf(typeStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Item '" + templateId + "' (" + key + ") com efeito de tipo invalido: " + typeStr);
                continue;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (!"type".equals(entry.getKey())) {
                    params.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            result.add(new ItemEffect(type, trigger, new ParamMap(params)));
        }
        return result;
    }

    public ItemTemplate get(String id) {
        return id == null ? null : templates.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public List<ItemTemplate> all() {
        return List.copyOf(templates.values());
    }

    /** Grava o template (novo ou existente) de volta em items.yml e no cache em memoria. Preserva
     * effects-on-X/attributes ja existentes no YAML - o editor pratico so mexe em flags/material/nome/
     * encantamentos custom, entao os campos de efeito sao serializados de volta tal como estao no template
     * em memoria (que so muda via YAML+reload, nunca via GUI neste escopo). */
    public void save(ItemTemplate template) {
        templates.put(template.id(), template);

        ConfigurationSection section = yaml.getConfigurationSection("items." + template.id());
        if (section == null) {
            section = yaml.createSection("items." + template.id());
        }
        section.set("material", template.material());
        section.set("name", template.name());
        section.set("lore", template.lore());
        section.set("enchantments", template.vanillaEnchantments());
        section.set("custom-enchantments", template.customEnchantments());
        section.set("soulbound", template.soulbound());
        section.set("glow", template.glow());
        section.set("unbreakable", template.unbreakable());
        section.set("custom-model-data", template.customModelData());
        section.set("itemsadder-id", template.itemsAdderId());
        section.set("attributes", template.attributes());
        section.set("max-durability", template.maxDurability());
        section.set("hide-enchants", template.hideEnchants());
        section.set("hide-attributes", template.hideAttributes());
        section.set("vip-required", template.vipRequired());
        section.set("rank-required", template.rankRequired());
        section.set("color", template.color());

        persist();
    }

    public void delete(String id) {
        templates.remove(id.toLowerCase(Locale.ROOT));
        yaml.set("items." + id.toLowerCase(Locale.ROOT), null);
        persist();
    }

    private void persist() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao salvar items.yml: " + e.getMessage());
        }
    }
}
