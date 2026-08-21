package com.alkacode.items.gui;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.items.AlkaItemsServices;
import com.alkacode.items.gui.layout.GuiLayoutLoader;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Base das GUIs do AlkaItems - icone/texto/layout de cada GUI concreta vem de
 * menus.yml/gui-layouts.yml (ver R8 no CLAUDE.md); essa classe so oferece os
 * helpers de wiring (applyBorder/setAt/icon), mesmo padrao de com.alkacode.clans.gui.ClanGui.
 */
abstract class AlkaItemsGui extends BaseGui {

    protected final AlkaItemsServices services;
    protected final String layoutId;

    protected AlkaItemsGui(AlkaItemsServices services, Player player, String layoutId) {
        super(services.plugin, player, services.menuConfig.title(layoutId + ".title", null),
                services.menuConfig.size(layoutId + ".size", 54) / 9, layoutId);
        this.services = services;
        this.layoutId = layoutId;
    }

    /** Preenche todo char '#' do layout (gui-layouts.yml) com o icone de menus.yml.common.border. */
    protected GuiLayoutLoader.GuiLayout applyBorder() {
        GuiLayoutLoader.GuiLayout layout = services.guiLayoutLoader.getLayout(layoutId);
        ItemStack border = services.menuConfig.item("common.border", null);
        layout(layout.layout(), Map.of('#', border), null);
        return layout;
    }

    protected void setAt(GuiLayoutLoader.GuiLayout layout, char c, ItemStack item, Consumer<InventoryClickEvent> action) {
        int slot = layout.firstSlot(c);
        if (slot >= 0) setItem(slot, item, action);
    }

    protected void setAt(GuiLayoutLoader.GuiLayout layout, char c, ItemStack item) {
        setAt(layout, c, item, null);
    }

    /** Icone de menus.yml.<layoutId>.<path> com placeholders. */
    protected ItemStack icon(String path, Map<String, String> placeholders) {
        return services.menuConfig.item(layoutId + "." + path, placeholders);
    }

    protected ItemStack icon(String path) {
        return icon(path, null);
    }
}
