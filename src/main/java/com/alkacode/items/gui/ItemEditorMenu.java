package com.alkacode.items.gui;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.gui.layout.GuiLayoutLoader;
import com.alkacode.items.model.ItemTemplate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor pratico de um template (/alkaitems edit <id>) - escopo combinado com o usuario
 * (AskUserQuestion): campos escalares/flags clicaveis (cycle pra bool, chat-input pra
 * texto/numero), sem seletor de material paginado nem editor de lore linha-a-linha.
 * Efeitos (effects-on-*) e encantamentos vanilla continuam so editaveis via items.yml +
 * /alkaitems reload - so os encantamentos CUSTOMIZADOS (mapa pequeno, id:nivel) tem um
 * atalho de chat aqui, o resto do template e "rapido de mexer, lento de criar do zero"
 * por design.
 */
public final class ItemEditorMenu extends AlkaItemsGui {

    private final String templateId;

    public ItemEditorMenu(Player viewer, AlkaItemsServices services, String templateId) {
        super(services, viewer, "item-editor");
        this.templateId = templateId;
    }

    @Override
    public void render() {
        GuiLayoutLoader.GuiLayout layout = applyBorder();
        ItemTemplate template = services.itemsConfig.get(templateId);
        if (template == null) {
            setAt(layout, 'P', icon("template-nao-encontrado"));
            return;
        }

        setItem(layout.firstSlot('P'), services.itemService.build(template, 1));

        setAt(layout, 'M', icon("material", Map.of("valor", template.material())),
                e -> promptText("material", template.material(), (t, v) -> t.material(v)));

        setAt(layout, 'N', icon("nome", Map.of("valor", template.name())),
                e -> promptText("name", template.name(), (t, v) -> t.name(v)));

        setAt(layout, 'G', icon("glow", Map.of("valor", String.valueOf(template.glow()))),
                e -> save(template.toBuilder().glow(!template.glow())));

        setAt(layout, 'S', icon("soulbound", Map.of("valor", String.valueOf(template.soulbound()))),
                e -> save(template.toBuilder().soulbound(!template.soulbound())));

        setAt(layout, 'U', icon("unbreakable", Map.of("valor", String.valueOf(template.unbreakable()))),
                e -> save(template.toBuilder().unbreakable(!template.unbreakable())));

        setAt(layout, 'C', icon("custom-model-data", Map.of("valor", String.valueOf(template.customModelData()))),
                e -> promptInt("custom-model-data", template.customModelData(), (t, v) -> t.customModelData(v)));

        setAt(layout, 'D', icon("max-durability", Map.of("valor", String.valueOf(template.maxDurability()))),
                e -> promptInt("max-durability", template.maxDurability(), (t, v) -> t.maxDurability(v)));

        setAt(layout, 'H', icon("hide-enchants", Map.of("valor", String.valueOf(template.hideEnchants()))),
                e -> save(template.toBuilder().hideEnchants(!template.hideEnchants())));

        setAt(layout, 'A', icon("hide-attributes", Map.of("valor", String.valueOf(template.hideAttributes()))),
                e -> save(template.toBuilder().hideAttributes(!template.hideAttributes())));

        setAt(layout, 'J', icon("vip-required", Map.of("valor",
                        template.vipRequired().isBlank() ? "nenhum" : template.vipRequired())),
                e -> promptText("vip-required", template.vipRequired(), (t, v) -> t.vipRequired(v)));

        setAt(layout, 'R', icon("rank-required", Map.of("valor",
                        template.rankRequired() <= 0 ? "nenhum" : String.valueOf(template.rankRequired()))),
                e -> promptInt("rank-required", template.rankRequired(), (t, v) -> t.rankRequired(v)));

        setAt(layout, 'K', icon("color", Map.of("valor", template.color().isBlank() ? "nenhuma" : template.color())),
                e -> promptText("color", template.color(), (t, v) -> t.color(v)));

        setAt(layout, 'E', customEnchantIcon(template), e -> promptCustomEnchant(template));

        setAt(layout, 'V', icon("voltar"), e -> new TemplateListMenu(player, services, 0).open());
        setAt(layout, 'X', icon("deletar"), e -> {
            services.itemsConfig.delete(templateId);
            new TemplateListMenu(player, services, 0).open();
        });
        setAt(layout, 'Z', icon("dar"), e -> services.itemService.giveItem(player, templateId, 1));
    }

    private org.bukkit.inventory.ItemStack customEnchantIcon(ItemTemplate template) {
        List<String> lore = new ArrayList<>(enchantLore(template.customEnchantments()));
        lore.add("");
        lore.add(services.menuConfig.text("item-editor.custom-enchant.lore-instrucao", null));
        return createItem(services.menuConfig.material("item-editor.custom-enchant", org.bukkit.Material.ENCHANTED_BOOK),
                services.menuConfig.name("item-editor.custom-enchant", null), lore.toArray(new String[0]));
    }

    private List<String> enchantLore(Map<String, Integer> enchants) {
        if (enchants.isEmpty()) {
            return List.of(services.menuConfig.text("item-editor.custom-enchant.lore-nenhum", null));
        }
        return enchants.entrySet().stream()
                .map(en -> "<gray>- <white>" + en.getKey() + " <gray>nivel <white>" + en.getValue())
                .toList();
    }

    private void promptText(String label, String current, java.util.function.BiConsumer<ItemTemplate.Builder, String> setter) {
        player.closeInventory();
        player.sendMessage(MiniMessage.miniMessage().deserialize(services.configManager.prefix()
                + "<yellow>Digite o novo valor de <white>" + label + " <yellow>no chat (atual: <white>" + current + "<yellow>):"));
        services.chatInputManager.await(player.getUniqueId(), input -> {
            ItemTemplate current2 = services.itemsConfig.get(templateId);
            if (current2 != null) {
                ItemTemplate.Builder builder = current2.toBuilder();
                setter.accept(builder, input.trim());
                save(builder);
            }
            open();
        });
    }

    private void promptInt(String label, int current, java.util.function.BiConsumer<ItemTemplate.Builder, Integer> setter) {
        player.closeInventory();
        player.sendMessage(MiniMessage.miniMessage().deserialize(services.configManager.prefix()
                + "<yellow>Digite o novo valor de <white>" + label + " <yellow>no chat (numero inteiro, atual: <white>" + current + "<yellow>):"));
        services.chatInputManager.await(player.getUniqueId(), input -> {
            ItemTemplate current2 = services.itemsConfig.get(templateId);
            if (current2 != null) {
                try {
                    ItemTemplate.Builder builder = current2.toBuilder();
                    setter.accept(builder, Integer.parseInt(input.trim()));
                    save(builder);
                } catch (NumberFormatException ignored) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(services.configManager.prefix() + "<red>Valor invalido."));
                }
            }
            open();
        });
    }

    private void promptCustomEnchant(ItemTemplate template) {
        player.closeInventory();
        player.sendMessage(MiniMessage.miniMessage().deserialize(services.configManager.prefix()
                + "<yellow>Digite <white>id:nivel <yellow>para adicionar/atualizar, ou <white>remove:id <yellow>para remover:"));
        services.chatInputManager.await(player.getUniqueId(), input -> {
            ItemTemplate current = services.itemsConfig.get(templateId);
            if (current != null) {
                Map<String, Integer> enchants = new LinkedHashMap<>(current.customEnchantments());
                String trimmed = input.trim();
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("remove:")) {
                    enchants.remove(trimmed.substring("remove:".length()).toLowerCase(Locale.ROOT));
                } else {
                    String[] parts = trimmed.split(":");
                    if (parts.length == 2) {
                        try {
                            enchants.put(parts[0].toLowerCase(Locale.ROOT), Integer.parseInt(parts[1].trim()));
                        } catch (NumberFormatException ignored) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize(services.configManager.prefix() + "<red>Formato invalido."));
                        }
                    }
                }
                save(current.toBuilder().customEnchantments(enchants));
            }
            open();
        });
    }

    private void save(ItemTemplate.Builder builder) {
        services.itemsConfig.save(builder.build());
        refresh();
    }
}
