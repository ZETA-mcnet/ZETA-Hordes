package me.pacdev.zeta_hordes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;

import java.util.Random;

public class AdaptiveSpawner extends BukkitRunnable {
    private final Main plugin;
    private final int gridSize;
    private final int zombiesPerPlayer;
    private final int minSpawnDistance;
    private final int maxSpawnDistance;
    private final int minY;
    private final int maxY;
    private final Random random = new Random();

    public AdaptiveSpawner(Main plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        
        this.gridSize = config.getInt("adaptive-spawner.grid-size", 25);
        this.zombiesPerPlayer = config.getInt("adaptive-spawner.zombies-per-player", 8);
        this.minSpawnDistance = config.getInt("adaptive-spawner.min-distance", 16);
        this.maxSpawnDistance = config.getInt("adaptive-spawner.max-distance", 48);
        this.minY = config.getInt("adaptive-spawner.min-y", 60);
        this.maxY = config.getInt("adaptive-spawner.max-y", 70);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            Location playerLocation = player.getLocation();

            // Count nearby zombies
            long nearbyZombies = player.getNearbyEntities(gridSize * 2, gridSize * 2, gridSize * 2)
                    .stream().filter(entity -> entity instanceof Zombie).count();

            // Spawn zombies if needed
            if (nearbyZombies < zombiesPerPlayer) {
                Location spawnLocation = getValidSpawnLocation(world, playerLocation);
                if (spawnLocation != null) {
                    Zombie zombie = world.spawn(spawnLocation, Zombie.class);
                    zombie.setTarget(player); // Set target to the player
                    setFollowRange(zombie, 80.0); // Set follow range to 80 blocks
                }
            }
        }
    }

    private Location getValidSpawnLocation(World world, Location playerLocation) {
        for (int attempts = 0; attempts < 10; attempts++) {
            double xOffset = random.nextInt(maxSpawnDistance - minSpawnDistance) + minSpawnDistance;
            double zOffset = random.nextInt(maxSpawnDistance - minSpawnDistance) + minSpawnDistance;
            if (random.nextBoolean()) xOffset *= -1;
            if (random.nextBoolean()) zOffset *= -1;

            double spawnY = playerLocation.getY() + (random.nextInt(7) + 3); // ±3 Y variation from player
            spawnY = Math.max(minY, Math.min(spawnY, maxY)); // Constrain to minY/maxY limits

            Location spawnLocation = new Location(world, playerLocation.getX() + xOffset, spawnY, playerLocation.getZ() + zOffset);
            if (Utils.canZombieSpawn(spawnLocation)) {
                return spawnLocation;
            }
        }
        return null;
    }

    private void setFollowRange(Zombie zombie, double range) {
        if (zombie.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            zombie.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(range);
        }
    }
}
