package com.alkacode.items;

import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.items.api.AlkaItemsAPI;
import com.alkacode.items.api.AlkaItemsAPIImpl;
import com.alkacode.items.command.ItemsCommand;
import com.alkacode.items.config.ConfigManager;
import com.alkacode.items.config.EnchantsConfig;
import com.alkacode.items.config.ItemsConfig;
import com.alkacode.items.gui.ChatInputManager;
import com.alkacode.items.hook.AdvancedEnchantmentsHook;
import com.alkacode.items.hook.ItemsAdderHook;
import com.alkacode.items.hook.PlaceholderAPIHook;
import com.alkacode.items.hook.RequirementHook;
import com.alkacode.items.listener.ChatInputListener;
import com.alkacode.items.listener.ItemBreakListener;
import com.alkacode.items.listener.ItemDeathListener;
import com.alkacode.items.listener.ItemDropListener;
import com.alkacode.items.listener.ItemEffectMovementListener;
import com.alkacode.items.listener.ItemEquipListener;
import com.alkacode.items.listener.ItemFishListener;
import com.alkacode.items.listener.ItemHitListener;
import com.alkacode.items.listener.ItemHoldListener;
import com.alkacode.items.listener.ItemShootListener;
import com.alkacode.items.listener.ItemUseListener;
import com.alkacode.items.listener.PlayerCleanupListener;
import com.alkacode.items.service.EffectService;
import com.alkacode.items.service.EffectTickTask;
import com.alkacode.items.service.EnchantService;
import com.alkacode.items.service.ItemService;
import com.alkacode.items.util.ItemPdc;

/**
 * Motor de itens customizados, efeitos e encantamentos - ver ALKANETWORKING.md/memoria
 * project-alkaitems pro racional das decisoes de design. ItemsAdder e
 * AdvancedEnchantments sao integrados 100% via reflection (hook/), NUNCA compileOnly -
 * ambos ficam em softdepend, coexistindo em vez de serem substituidos.
 */
public final class AlkaItemsPlugin extends AlkaPlugin {

    private AlkaItemsServices services;
    private AlkaItemsAPI api;
    private EffectTickTask tickTask;

    @Override
    protected void onPluginEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.load();
        ItemsConfig itemsConfig = new ItemsConfig(this);
        EnchantsConfig enchantsConfig = new EnchantsConfig(this);
        ItemPdc pdc = new ItemPdc(this);

        ItemsAdderHook itemsAdderHook = new ItemsAdderHook(this);
        AdvancedEnchantmentsHook advancedEnchantmentsHook = new AdvancedEnchantmentsHook(this);
        RequirementHook requirementHook = new RequirementHook(this, configManager);

        ItemService itemService = new ItemService(this, itemsConfig, itemsAdderHook, pdc);
        EnchantService enchantService = new EnchantService(this, enchantsConfig, pdc);
        EffectService effectService = new EffectService(this, configManager);
        ChatInputManager chatInputManager = new ChatInputManager();

        services = new AlkaItemsServices(this, configManager, itemsConfig, enchantsConfig, pdc, itemService,
                enchantService, effectService, itemsAdderHook, advancedEnchantmentsHook, requirementHook, chatInputManager);
        api = new AlkaItemsAPIImpl(services);
        // Registrado via ServicesManager (mesmo padrao de AlkaVipsBoostAPI/AlkaFlairAPI
        // no resto do ecossistema) - getAPI() abaixo continua existindo pra quem prefere
        // reflection direta no plugin em vez do ServicesManager.
        getServer().getServicesManager().register(AlkaItemsAPI.class, api, this, org.bukkit.plugin.ServicePriority.Normal);

        registerListeners();
        registerCommand();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this, itemsConfig, pdc).register();
        }

        int interval = configManager.effectTickInterval();
        ItemEffectMovementListener movementListener = new ItemEffectMovementListener(services);
        getServer().getPluginManager().registerEvents(movementListener, this);
        tickTask = new EffectTickTask(services, movementListener);
        tickTask.runTaskTimer(this, interval, interval);

        getLogger().info("AlkaItems habilitado (" + itemsConfig.all().size() + " templates, "
                + enchantsConfig.all().size() + " encantamentos customizados).");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ItemEquipListener(services), this);
        pm.registerEvents(new ItemHoldListener(services), this);
        pm.registerEvents(new ItemUseListener(services), this);
        pm.registerEvents(new ItemHitListener(services), this);
        pm.registerEvents(new ItemBreakListener(services), this);
        pm.registerEvents(new ItemShootListener(services), this);
        pm.registerEvents(new ItemFishListener(services), this);
        pm.registerEvents(new ItemDeathListener(services), this);
        pm.registerEvents(new ItemDropListener(services), this);
        pm.registerEvents(new PlayerCleanupListener(services), this);
        pm.registerEvents(new ChatInputListener(this, services.chatInputManager), this);
    }

    private void registerCommand() {
        ItemsCommand command = new ItemsCommand(services);
        var pluginCommand = getCommand("alkaitems");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    @Override
    protected void onPluginDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
    }

    /** Ponto de entrada pra outros plugins via reflection - ver com.alkacode.items.api.AlkaItemsAPI. */
    public AlkaItemsAPI getAPI() {
        return api;
    }

    public void reloadAll() {
        services.configManager.reload();
        services.itemsConfig.load();
        services.enchantsConfig.load();
    }
}
