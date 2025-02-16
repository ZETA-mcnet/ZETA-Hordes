package me.pacdev.zeta_hordes;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Schedule the zombie horde task
        int interval = 20 * 30; // in ticks
        new ZombieHordeTask(this).runTaskTimer(this, 0, interval);

        // Register the listener for custom spawning logic
        // getServer().getPluginManager().registerEvents(new ZombieSpawnListener(), this);

        getLogger().info("ZETA-Hordes enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ZETA-Hordes disabled!");
    }
}