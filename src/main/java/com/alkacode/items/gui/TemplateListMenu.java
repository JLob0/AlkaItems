package com.alkacode.items.gui;

import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.gui.layout.GuiLayoutLoader;
import com.alkacode.items.model.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** /alkaitems list - grade paginada de templates, clique esquerdo abre o editor, clique
 * direito da 1x na mao (atalho pra testar rapido sem digitar /alkaitems load). */
public final class TemplateListMenu extends AlkaItemsGui {

    private final int page;

    public TemplateListMenu(Player viewer, AlkaItemsServices services, int page) {
        super(services, viewer, "template-list");
        this.page = page;
    }

    @Override
    public void render() {
        GuiLayoutLoader.GuiLayout layout = applyBorder();
        List<Integer> slots = layout.findSlots('0');

        List<ItemTemplate> templates = services.itemsConfig.all();
        if (templates.isEmpty()) {
            setItem(slots.get(slots.size() / 2), icon("vazio"));
        }
        int from = page * slots.size();
        for (int i = 0; i < slots.size(); i++) {
            int index = from + i;
            if (index >= templates.size()) break;
            ItemTemplate template = templates.get(index);
            ItemStack preview = services.itemService.build(template, 1);
            setItem(slots.get(i), preview, e -> {
                if (e.isRightClick()) {
                    services.itemService.giveItem(player, template.id(), 1);
                } else {
                    new ItemEditorMenu(player, services, template.id()).open();
                }
            });
        }

        setAt(layout, 'F', icon("fechar"), e -> player.closeInventory());
        if (page > 0) {
            setAt(layout, 'A', icon("anterior"), e -> new TemplateListMenu(player, services, page - 1).open());
        }
        if (from + slots.size() < templates.size()) {
            setAt(layout, 'N', icon("proximo"), e -> new TemplateListMenu(player, services, page + 1).open());
        }
    }
}
