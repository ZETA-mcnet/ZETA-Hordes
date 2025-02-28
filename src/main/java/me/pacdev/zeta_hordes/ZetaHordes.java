package me.pacdev.zeta_hordes;

import me.pacdev.zeta_hordes.afk.AFKManager;
import me.pacdev.zeta_hordes.zombies.CustomZombieManager;
import me.pacdev.zeta_hordes.zombies.ZombieAbilityHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class ZetaHordes extends JavaPlugin {
    private AFKManager afkManager;
    private CustomZombieManager zombieManager;
    private ZombieAbilityHandler abilityHandler;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize managers
        zombieManager = new CustomZombieManager(this);
        afkManager = new AFKManager(this);
        abilityHandler = new ZombieAbilityHandler(this, zombieManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(afkManager, this);
        getServer().getPluginManager().registerEvents(abilityHandler, this);
    }

    public AFKManager getAfkManager() {
        return afkManager;
    }

    public CustomZombieManager getZombieManager() {
        return zombieManager;
    }
} 