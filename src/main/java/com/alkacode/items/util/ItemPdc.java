package com.alkacode.items.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PDC nativo do Paper (zero NBT-API de terceiro) - marca um ItemStack como instancia
 * de um {@link com.alkacode.items.model.ItemTemplate} e carrega o estado que PODE
 * divergir do template ao longo da vida do item (encantamentos customizados
 * aplicados/removidos depois da criacao via /alkaitems enchant, soulbound). O
 * template_id em si nunca muda depois de criado.
 */
public final class ItemPdc {

    private final NamespacedKey templateIdKey;
    private final NamespacedKey customEnchantsKey;
    private final NamespacedKey soulboundKey;

    public ItemPdc(Plugin plugin) {
        this.templateIdKey = new NamespacedKey(plugin, "template_id");
        this.customEnchantsKey = new NamespacedKey(plugin, "custom_enchants");
        this.soulboundKey = new NamespacedKey(plugin, "soulbound");
    }

    public String getTemplateId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(templateIdKey, PersistentDataType.STRING);
    }

    public boolean isAlkaItem(ItemStack item) {
        return getTemplateId(item) != null;
    }

    public void setTemplateId(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(templateIdKey, PersistentDataType.STRING, id);
    }

    public boolean isSoulbound(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte value = item.getItemMeta().getPersistentDataContainer().get(soulboundKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public void setSoulbound(ItemMeta meta, boolean soulbound) {
        meta.getPersistentDataContainer().set(soulboundKey, PersistentDataType.BYTE, (byte) (soulbound ? 1 : 0));
    }

    public Map<String, Integer> getCustomEnchants(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) {
            return result;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(customEnchantsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            try {
                result.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
                // entrada corrompida - ignora essa entrada, mantem o resto
            }
        }
        return result;
    }

    public void setCustomEnchants(ItemMeta meta, Map<String, Integer> enchants) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(entry.getKey()).append(':').append(entry.getValue());
        }
        meta.getPersistentDataContainer().set(customEnchantsKey, PersistentDataType.STRING, sb.toString());
    }

    public int getEnchantLevel(ItemStack item, String enchantId) {
        return getCustomEnchants(item).getOrDefault(enchantId.toLowerCase(java.util.Locale.ROOT), 0);
    }

    public boolean hasEnchant(ItemStack item, String enchantId) {
        return getEnchantLevel(item, enchantId) > 0;
    }

    /** Adiciona/atualiza um encantamento customizado na INSTANCIA (nao no template) - usado por
     * /alkaitems enchant e pelo AlkaAnvil (via API) quando aplica na bigorna. */
    public void addEnchant(ItemStack item, String enchantId, int level) {
        ItemMeta meta = item.getItemMeta();
        Map<String, Integer> enchants = new LinkedHashMap<>(getCustomEnchants(item));
        enchants.put(enchantId.toLowerCase(java.util.Locale.ROOT), level);
        setCustomEnchants(meta, enchants);
        item.setItemMeta(meta);
    }

    public void removeEnchant(ItemStack item, String enchantId) {
        ItemMeta meta = item.getItemMeta();
        Map<String, Integer> enchants = new LinkedHashMap<>(getCustomEnchants(item));
        enchants.remove(enchantId.toLowerCase(java.util.Locale.ROOT));
        setCustomEnchants(meta, enchants);
        item.setItemMeta(meta);
    }

    public PersistentDataContainer container(ItemMeta meta) {
        return meta.getPersistentDataContainer();
    }
}
