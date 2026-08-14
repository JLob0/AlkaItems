package com.alkacode.items.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.model.ItemTemplate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** /alkaitems list - grade paginada de templates, clique esquerdo abre o editor, clique
 * direito da 1x na mao (atalho pra testar rapido sem digitar /alkaitems load). */
public final class TemplateListMenu extends BaseGui {

    private static final int[] SLOTS = buildSlots();

    private final AlkaItemsServices services;
    private final int page;

    public TemplateListMenu(Player viewer, AlkaItemsServices services, int page) {
        super(services.plugin, viewer, services.configManager.menus().getString("template-list.title", "&8AlkaItems - Templates"),
                services.configManager.menus().getInt("template-list.size", 54) / 9, "alkaitems_template_list");
        this.services = services;
        this.page = page;
    }

    private static int[] buildSlots() {
        int[] slots = new int[45];
        for (int i = 0; i < 45; i++) slots[i] = i;
        return slots;
    }

    @Override
    public void render() {
        List<ItemTemplate> templates = services.itemsConfig.all();
        if (templates.isEmpty()) {
            setItem(22, createItem(Material.BARRIER, "<yellow>Nenhum template cadastrado",
                    "<gray>Crie um com /alkaitems create <id>"));
        }
        int from = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length; i++) {
            int index = from + i;
            if (index >= templates.size()) break;
            ItemTemplate template = templates.get(index);
            ItemStack preview = services.itemService.build(template, 1);
            setItem(SLOTS[i], preview, e -> {
                if (e.isRightClick()) {
                    services.itemService.giveItem(player, template.id(), 1);
                } else {
                    new ItemEditorMenu(player, services, template.id()).open();
                }
            });
        }

        setItem(49, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
        if (page > 0) {
            setItem(45, createItem(Material.ARROW, "<white>Anterior"), e -> new TemplateListMenu(player, services, page - 1).open());
        }
        if (from + SLOTS.length < templates.size()) {
            setItem(53, createItem(Material.ARROW, "<white>Proximo"), e -> new TemplateListMenu(player, services, page + 1).open());
        }
        fill(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
    }
}
