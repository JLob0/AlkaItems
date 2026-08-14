package com.alkacode.items.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.model.ItemTemplate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
public final class ItemEditorMenu extends BaseGui {

    private final AlkaItemsServices services;
    private final String templateId;

    public ItemEditorMenu(Player viewer, AlkaItemsServices services, String templateId) {
        super(services.plugin, viewer, services.configManager.menus().getString("item-editor.title", "&8AlkaItems - Editor"),
                services.configManager.menus().getInt("item-editor.size", 54) / 9, "alkaitems_item_editor");
        this.services = services;
        this.templateId = templateId;
    }

    @Override
    public void render() {
        fill(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        ItemTemplate template = services.itemsConfig.get(templateId);
        if (template == null) {
            setItem(22, createItem(Material.BARRIER, "<red>Template nao encontrado"));
            return;
        }

        setItem(4, services.itemService.build(template, 1));

        setItem(10, createItem(Material.ITEM_FRAME, "<yellow>Material", "<gray>Atual: <white>" + template.material(),
                "", "<green>Clique para digitar"), e -> promptText("material", template.material(), (t, v) -> t.material(v)));

        setItem(11, createItem(Material.NAME_TAG, "<yellow>Nome", "<gray>Atual: <white>" + template.name(),
                "", "<green>Clique para digitar"), e -> promptText("name", template.name(), (t, v) -> t.name(v)));

        setItem(12, createItem(Material.GLOWSTONE_DUST, "<yellow>Glow", "<gray>Atual: <white>" + template.glow(),
                "", "<green>Clique para alternar"), e -> save(template.toBuilder().glow(!template.glow())));

        setItem(13, createItem(Material.TOTEM_OF_UNDYING, "<yellow>Soulbound", "<gray>Atual: <white>" + template.soulbound(),
                "", "<green>Clique para alternar"), e -> save(template.toBuilder().soulbound(!template.soulbound())));

        setItem(14, createItem(Material.ANVIL, "<yellow>Unbreakable", "<gray>Atual: <white>" + template.unbreakable(),
                "", "<green>Clique para alternar"), e -> save(template.toBuilder().unbreakable(!template.unbreakable())));

        setItem(15, createItem(Material.PAPER, "<yellow>Custom Model Data", "<gray>Atual: <white>" + template.customModelData(),
                "", "<green>Clique para digitar"), e -> promptInt("custom-model-data", template.customModelData(),
                (t, v) -> t.customModelData(v)));

        setItem(16, createItem(Material.EXPERIENCE_BOTTLE, "<yellow>Durabilidade Maxima", "<gray>Atual: <white>" + template.maxDurability(),
                "<gray>(0 = padrao vanilla)", "", "<green>Clique para digitar"), e -> promptInt("max-durability",
                template.maxDurability(), (t, v) -> t.maxDurability(v)));

        setItem(19, createItem(Material.BOOK, "<yellow>Esconder Encantamentos", "<gray>Atual: <white>" + template.hideEnchants(),
                "", "<green>Clique para alternar"), e -> save(template.toBuilder().hideEnchants(!template.hideEnchants())));

        setItem(20, createItem(Material.IRON_CHESTPLATE, "<yellow>Esconder Atributos", "<gray>Atual: <white>" + template.hideAttributes(),
                "", "<green>Clique para alternar"), e -> save(template.toBuilder().hideAttributes(!template.hideAttributes())));

        setItem(21, createItem(Material.NETHER_STAR, "<yellow>VIP Requerido",
                "<gray>Atual: <white>" + (template.vipRequired().isBlank() ? "nenhum" : template.vipRequired()),
                "<gray>(id do tier - ver vips.yml no AlkaVips)", "", "<green>Clique para digitar (vazio = remover)"),
                e -> promptText("vip-required", template.vipRequired(), (t, v) -> t.vipRequired(v)));

        setItem(22, createItem(Material.GOLD_INGOT, "<yellow>Rank Minimo Requerido",
                "<gray>Atual: <white>" + (template.rankRequired() <= 0 ? "nenhum" : template.rankRequired()),
                "<gray>(indice minimo - %alkarankup_rank_index%)", "", "<green>Clique para digitar (0 = remover)"),
                e -> promptInt("rank-required", template.rankRequired(), (t, v) -> t.rankRequired(v)));

        setItem(23, createItem(Material.LEATHER_CHESTPLATE, "<yellow>Cor (couro)",
                "<gray>Atual: <white>" + (template.color().isBlank() ? "nenhuma" : template.color()),
                "", "<green>Clique para digitar (#RRGGBB, vazio = remover)"),
                e -> promptText("color", template.color(), (t, v) -> t.color(v)));

        List<String> enchantLore = new java.util.ArrayList<>(enchantLore(template.customEnchantments()));
        enchantLore.add("");
        enchantLore.add("<green>Clique: 'id:nivel' adiciona, 'remove:id' remove");
        setItem(24, createItem(Material.ENCHANTED_BOOK, "<yellow>Encantamentos Customizados", enchantLore.toArray(new String[0])),
                e -> promptCustomEnchant(template));

        setItem(45, createItem(Material.ARROW, "<red>Voltar"), e -> new TemplateListMenu(player, services, 0).open());
        setItem(49, createItem(Material.TNT, "<red>Deletar Template",
                "<gray>Remove o template inteiro do items.yml", "", "<red><bold>Clique para deletar (sem confirmacao)"),
                e -> {
                    services.itemsConfig.delete(templateId);
                    new TemplateListMenu(player, services, 0).open();
                });
        setItem(53, createItem(Material.CHEST, "<green>Dar 1x na mao"), e -> services.itemService.giveItem(player, templateId, 1));
    }

    private List<String> enchantLore(Map<String, Integer> enchants) {
        if (enchants.isEmpty()) {
            return List.of("<gray>Nenhum");
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
