package com.alkacode.items.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** config.yml/messages.yml/menus.yml - mesmo padrao ja usado em com.alkacode.vips.config.ConfigManager. */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration menus;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = loadResource("config.yml");
        messages = loadResource("messages.yml");
        menus = loadResource("menus.yml");
    }

    public void reload() {
        load();
    }

    private FileConfiguration loadResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration menus() { return menus; }

    public String prefix() { return messages.getString("prefix", ""); }

    public String message(String path) { return messages.getString(path, path); }

    public int effectTickInterval() { return config.getInt("effect-tick-interval", 1); }

    public double doubleJumpVelocity() { return config.getDouble("double-jump.velocity", 0.5); }

    public double dashVelocity() { return config.getDouble("dash.velocity", 1.2); }

    public int dashCooldownTicks() { return config.getInt("dash.cooldown-ticks", 40); }

    public int dashDoubleTapWindowTicks() { return config.getInt("dash.double-tap-window-ticks", 8); }

    public String rankIndexPlaceholder() { return config.getString("requirements.rank-index-placeholder", "%alkarankup_rank_index%"); }
}
