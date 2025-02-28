package me.pacdev.zeta_hordes;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final ZetaHordes plugin;

    public ConfigManager(ZetaHordes plugin) {
        this.plugin = plugin;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void saveConfig() {
        plugin.saveConfig();
    }
}