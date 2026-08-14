package com.alkacode.items.service;

import com.alkacode.items.config.ItemsConfig;
import com.alkacode.items.hook.ItemsAdderHook;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.util.ItemPdc;
import com.alkacode.items.util.TextUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;

/** CRUD de templates (delega em {@link ItemsConfig}) + monta o ItemStack real a partir de um {@link ItemTemplate}. */
public final class ItemService {

    private final JavaPlugin plugin;
    private final ItemsConfig itemsConfig;
    private final ItemsAdderHook itemsAdderHook;
    private final ItemPdc pdc;

    public ItemService(JavaPlugin plugin, ItemsConfig itemsConfig, ItemsAdderHook itemsAdderHook, ItemPdc pdc) {
        this.plugin = plugin;
        this.itemsConfig = itemsConfig;
        this.itemsAdderHook = itemsAdderHook;
        this.pdc = pdc;
    }

    public ItemStack build(ItemTemplate template, int amount) {
        ItemStack item = baseItem(template);
        item.setAmount(Math.max(1, amount));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (!template.name().isBlank()) {
            meta.displayName(TextUtil.parse(template.name()));
        }
        if (!template.lore().isEmpty()) {
            meta.lore(template.lore().stream().map(TextUtil::parse).toList());
        }

        for (Map.Entry<String, Integer> entry : template.vanillaEnchantments().entrySet()) {
            Enchantment enchantment = resolveEnchantment(entry.getKey());
            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
        if (template.glow() && template.vanillaEnchantments().isEmpty()) {
            // Truque padrao pra dar o glint de encantado sem ter um encantamento real -
            // some da lore porque HIDE_ENCHANTS so esconde ENCHANTS (nao afeta outra lore).
            Enchantment marker = resolveEnchantment("unbreaking");
            if (marker != null) {
                meta.addEnchant(marker, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        if (template.hideEnchants()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (template.hideAttributes()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        meta.setUnbreakable(template.unbreakable());
        if (template.customModelData() > 0) {
            meta.setCustomModelData(template.customModelData());
        }

        for (Map.Entry<String, Double> entry : template.attributes().entrySet()) {
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(entry.getKey().toLowerCase(Locale.ROOT)));
            if (attribute == null) {
                plugin.getLogger().warning("Template '" + template.id() + "' com atributo invalido: " + entry.getKey());
                continue;
            }
            meta.addAttributeModifier(attribute, new AttributeModifier(
                    new NamespacedKey(plugin, "alkaitems_" + entry.getKey().toLowerCase(Locale.ROOT)),
                    entry.getValue(), AttributeModifier.Operation.ADD_NUMBER));
        }

        if (!template.color().isBlank() && meta instanceof LeatherArmorMeta leatherMeta) {
            try {
                String hex = template.color().replace("#", "");
                leatherMeta.setColor(Color.fromRGB(Integer.parseInt(hex, 16)));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Template '" + template.id() + "' com cor invalida: " + template.color());
            }
        }

        if (template.maxDurability() > 0 && meta instanceof Damageable damageable) {
            damageable.setMaxDamage(template.maxDurability());
        }

        pdc.setTemplateId(meta, template.id());
        pdc.setSoulbound(meta, template.soulbound());
        pdc.setCustomEnchants(meta, template.customEnchantments());

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack baseItem(ItemTemplate template) {
        if (ItemsAdderHook.isCustomId(template.material())) {
            ItemStack fromIa = itemsAdderHook.getItemStack(ItemsAdderHook.stripPrefix(template.material()));
            if (fromIa != null) {
                return fromIa;
            }
            plugin.getLogger().warning("Template '" + template.id() + "' referencia item do ItemsAdder ausente ("
                    + template.material() + ") - usando STONE como fallback.");
            return new ItemStack(Material.STONE);
        }
        Material material = Material.matchMaterial(template.material());
        return new ItemStack(material != null ? material : Material.STONE);
    }

    private Enchantment resolveEnchantment(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
    }

    public enum GiveResult { SUCCESS, UNKNOWN_TEMPLATE }

    public GiveResult giveItem(Player player, String templateId, int amount) {
        ItemTemplate template = itemsConfig.get(templateId);
        if (template == null) {
            return GiveResult.UNKNOWN_TEMPLATE;
        }
        ItemStack item = build(template, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        return GiveResult.SUCCESS;
    }

    public String readTemplateId(ItemStack item) {
        return pdc.getTemplateId(item);
    }

    public ItemsConfig templates() {
        return itemsConfig;
    }
}
