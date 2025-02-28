package me.pacdev.zeta_hordes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Utils {
    private static ZetaHordes plugin;

    // Set the plugin instance
    public static void init(ZetaHordes main) {
        plugin = main;
    }

    public static boolean canZombieSpawn(Location location) {
        if (plugin == null) return false; // Prevent NullPointerException

        World world = location.getWorld();
        if (world == null) return false;

        int minY = plugin.getConfig().getInt("adaptive-spawner.min-y", 64);
        int maxY = plugin.getConfig().getInt("adaptive-spawner.max-y", 80);

        if (location.getY() < minY || location.getY() > maxY) return false;

        return world.getBlockAt(location).isEmpty()
                && world.getBlockAt(location.clone().add(0, 1, 0)).isEmpty()
                && !world.getBlockAt(location.clone().subtract(0, 1, 0)).isEmpty();
    }
}