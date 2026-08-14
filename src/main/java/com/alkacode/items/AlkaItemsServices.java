package com.alkacode.items;

import com.alkacode.items.config.ConfigManager;
import com.alkacode.items.config.EnchantsConfig;
import com.alkacode.items.config.ItemsConfig;
import com.alkacode.items.hook.AdvancedEnchantmentsHook;
import com.alkacode.items.hook.ItemsAdderHook;
import com.alkacode.items.hook.RequirementHook;
import com.alkacode.items.gui.ChatInputManager;
import com.alkacode.items.service.EffectService;
import com.alkacode.items.service.EnchantService;
import com.alkacode.items.service.ItemService;
import com.alkacode.items.util.ItemPdc;
import org.bukkit.command.CommandSender;

import java.util.Map;

/** Agrega tudo que os listeners/comandos/GUIs do AlkaItems precisam - mesmo padrao de
 * "services bag" ja usado em com.alkacode.vips.VipsServices. */
public final class AlkaItemsServices {

    public final AlkaItemsPlugin plugin;
    public final ConfigManager configManager;
    public final ItemsConfig itemsConfig;
    public final EnchantsConfig enchantsConfig;
    public final ItemPdc pdc;
    public final ItemService itemService;
    public final EnchantService enchantService;
    public final EffectService effectService;
    public final ItemsAdderHook itemsAdderHook;
    public final AdvancedEnchantmentsHook advancedEnchantmentsHook;
    public final RequirementHook requirementHook;
    public final ChatInputManager chatInputManager;

    public AlkaItemsServices(AlkaItemsPlugin plugin, ConfigManager configManager, ItemsConfig itemsConfig,
                              EnchantsConfig enchantsConfig, ItemPdc pdc, ItemService itemService,
                              EnchantService enchantService, EffectService effectService,
                              ItemsAdderHook itemsAdderHook, AdvancedEnchantmentsHook advancedEnchantmentsHook,
                              RequirementHook requirementHook, ChatInputManager chatInputManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemsConfig = itemsConfig;
        this.enchantsConfig = enchantsConfig;
        this.pdc = pdc;
        this.itemService = itemService;
        this.enchantService = enchantService;
        this.effectService = effectService;
        this.itemsAdderHook = itemsAdderHook;
        this.advancedEnchantmentsHook = advancedEnchantmentsHook;
        this.requirementHook = requirementHook;
        this.chatInputManager = chatInputManager;
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String raw = configManager.prefix() + configManager.message(path);
        sender.sendMessage(com.alkacode.items.util.TextUtil.parse(raw, placeholders));
    }
}
