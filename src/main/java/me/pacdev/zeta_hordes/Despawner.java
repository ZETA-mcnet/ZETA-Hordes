package me.pacdev.zeta_hordes;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Despawner extends BukkitRunnable {
    private final Main plugin;
    private final int maxPlayerDistance;

    public Despawner(Main plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.maxPlayerDistance = config.getInt("despawner.max-player-distance", 50);
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getLivingEntities()) {
                if (entity instanceof Zombie zombie) {
                    boolean hasNearbyPlayer = world.getPlayers().stream()
                            .anyMatch(player -> player.getLocation().distance(zombie.getLocation()) < maxPlayerDistance);

                    if (!hasNearbyPlayer) {
                        zombie.remove();
                    }
                }
            }
        }
    }
}