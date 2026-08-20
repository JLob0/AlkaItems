package com.alkacode.items.shiftlore;

import com.alkacode.core.shiftlore.ShiftLoreProvider;
import com.alkacode.core.shiftlore.model.ShiftLoreEntry;
import com.alkacode.items.config.EnchantsConfig;
import com.alkacode.items.config.ItemsConfig;
import com.alkacode.items.model.CustomEnchantment;
import com.alkacode.items.model.ItemTemplate;
import com.alkacode.items.util.ItemPdc;
import com.alkacode.items.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item de teste do Shift-Lore System: qualquer item ja criado pelo AlkaItems
 * (`/alkaitems`) ganha lore detalhado de graca ao segurar Shift, sem precisar
 * editar nada no items.yml - os dados vem do proprio {@link ItemTemplate} +
 * PDC da instancia (encantamentos aplicados depois de criado, ver {@link ItemPdc}).
 *
 * Tambem e o primeiro consumidor real de {@code loreFormat}/{@code showInLore}
 * do {@link CustomEnchantment} - existiam no config desde sempre mas nada
 * renderizava, ver historico.
 */
public final class AlkaItemsShiftLoreProvider implements ShiftLoreProvider {

    private final ItemsConfig itemsConfig;
    private final EnchantsConfig enchantsConfig;
    private final ItemPdc pdc;

    public AlkaItemsShiftLoreProvider(ItemsConfig itemsConfig, EnchantsConfig enchantsConfig, ItemPdc pdc) {
        this.itemsConfig = itemsConfig;
        this.enchantsConfig = enchantsConfig;
        this.pdc = pdc;
    }

    @Override
    public int getPriority() {
        return 200; // vence o vanilla loader do Core (100)
    }

    @Override
    @Nullable
    public ShiftLoreEntry getLore(Player viewer, ItemStack item) {
        String templateId = pdc.getTemplateId(item);
        if (templateId == null) {
            return null;
        }
        ItemTemplate template = itemsConfig.get(templateId);
        if (template == null) {
            return null;
        }

        List<Component> detailed = new ArrayList<>();
        for (String line : template.lore()) {
            detailed.add(TextUtil.parse(line));
        }

        List<Component> vanillaBlock = vanillaEnchantLines(template);
        List<Component> customBlock = customEnchantLines(item);
        List<Component> attrBlock = attributeLines(template);

        if (!vanillaBlock.isEmpty() || !customBlock.isEmpty()) {
            detailed.add(Component.empty());
            detailed.add(TextUtil.parse("<dark_gray><bold>Encantamentos"));
            detailed.addAll(vanillaBlock);
            detailed.addAll(customBlock);
        }
        if (!attrBlock.isEmpty()) {
            detailed.add(Component.empty());
            detailed.add(TextUtil.parse("<dark_gray><bold>Atributos"));
            detailed.addAll(attrBlock);
        }
        if (pdc.isSoulbound(item)) {
            detailed.add(Component.empty());
            detailed.add(TextUtil.parse("<light_purple>✦ Vinculado a alma"));
        }
        if (template.hasVipRequirement()) {
            detailed.add(TextUtil.parse("<gray>Requer VIP: <yellow>" + template.vipRequired()));
        }
        if (template.hasRankRequirement()) {
            detailed.add(TextUtil.parse("<gray>Requer Rank: <yellow>#" + template.rankRequired()));
        }

        detailed.add(Component.empty());
        detailed.add(TextUtil.parse("<dark_gray>Mova o item para fechar esta lore"));

        return new ShiftLoreEntry(List.of(), detailed);
    }

    private List<Component> vanillaEnchantLines(ItemTemplate template) {
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : template.vanillaEnchantments().entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT).replace('_', ' ');
            lines.add(TextUtil.parse("<gray>  " + capitalize(name) + " " + toRoman(entry.getValue())));
        }
        return lines;
    }

    private List<Component> customEnchantLines(ItemStack item) {
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pdc.getCustomEnchants(item).entrySet()) {
            CustomEnchantment enchant = enchantsConfig.get(entry.getKey());
            if (enchant == null || !enchant.showInLore()) {
                continue;
            }
            String rendered = enchant.loreFormat()
                    .replace("<name>", enchant.displayName())
                    .replace("<level>", String.valueOf(entry.getValue()))
                    .replace("<level_roman>", toRoman(entry.getValue()));
            lines.add(TextUtil.parse("  " + rendered));
        }
        return lines;
    }

    private List<Component> attributeLines(ItemTemplate template) {
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<String, Double> entry : template.attributes().entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT).replace('_', ' ');
            double value = entry.getValue();
            String sign = value >= 0 ? "+" : "";
            lines.add(TextUtil.parse("<gray>  " + capitalize(name) + ": <green>" + sign
                    + trimZeros(value)));
        }
        return lines;
    }

    private String capitalize(String s) {
        if (s.isBlank()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String trimZeros(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private String toRoman(int number) {
        if (number <= 0) {
            return String.valueOf(number);
        }
        StringBuilder sb = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < ROMAN_VALUES.length && remaining > 0; i++) {
            while (remaining >= ROMAN_VALUES[i]) {
                remaining -= ROMAN_VALUES[i];
                sb.append(ROMAN_SYMBOLS[i]);
            }
        }
        return sb.toString();
    }
}
