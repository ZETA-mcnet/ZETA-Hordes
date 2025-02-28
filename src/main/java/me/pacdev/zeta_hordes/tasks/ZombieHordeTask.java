package me.pacdev.zeta_hordes.tasks;

import me.pacdev.zeta_hordes.ZetaHordes;
import me.pacdev.zeta_hordes.afk.AFKManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.attribute.Attribute;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class ZombieHordeTask extends BukkitRunnable {
    private final ZetaHordes plugin;
    private final Random random;
    private final AFKManager afkManager;

    public ZombieHordeTask(ZetaHordes plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.afkManager = plugin.getAfkManager();
    }

    @Override
    public void run() {
        // Get all online players for scaling
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) return;

        // Filter out AFK players for targeting
        List<Player> activeTargets = onlinePlayers.stream()
            .filter(player -> !afkManager.isPlayerAFK(player))
            .collect(Collectors.toList());

        if (activeTargets.isEmpty()) return;

        // Select a random active player as target
        Player target = activeTargets.get(random.nextInt(activeTargets.size()));
        World world = target.getWorld();

        // Check if the world is whitelisted
        if (!plugin.getConfig().getStringList("worlds").contains(world.getName())) return;

        // Get spawn location
        Location spawnLocation = getSpawnLocation(target);
        if (spawnLocation == null) return;

        // Calculate horde size based on ALL online players (including AFK)
        int baseSize = plugin.getConfig().getInt("horde.base-size", 5);
        double scalingFactor = plugin.getConfig().getDouble("horde.scaling-factor", 0.5);
        int hordeSize = (int) (baseSize + (onlinePlayers.size() - 1) * scalingFactor);

        // Spawn the horde
        spawnZombieHorde(spawnLocation, hordeSize);
    }

    private Location getSpawnLocation(Player target) {
        World world = target.getWorld();
        Location playerLocation = target.getLocation();
        
        // Get spawn distance range
        int minDistance = plugin.getConfig().getInt("spread.min-distance", 8);
        int maxDistance = plugin.getConfig().getInt("spread.max-distance", 30);
        int minY = plugin.getConfig().getInt("hordes.min-y", 64);
        int maxY = plugin.getConfig().getInt("hordes.max-y", 74);

        // Try to find a valid spawn location
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            double xOffset = Math.cos(angle) * distance;
            double zOffset = Math.sin(angle) * distance;

            Location spawnLocation = playerLocation.clone().add(xOffset, 0, zOffset);
            spawnLocation.setY(world.getHighestBlockYAt(spawnLocation));

            // Validate Y position
            if (spawnLocation.getY() >= minY && spawnLocation.getY() <= maxY) {
                return spawnLocation;
            }
        }
        return null;
    }

    private void spawnZombieHorde(Location location, int size) {
        World world = location.getWorld();
        if (world == null) return;

        // Get follow range from config
        double followRange = plugin.getConfig().getDouble("hordes.follow-range", 256.0);

        // Calculate special zombie quota
        boolean guaranteedSpecial = plugin.getConfig().getBoolean("horde-composition.guaranteed-special", true);
        double maxSpecialPercentage = plugin.getConfig().getDouble("horde-composition.max-special-percentage", 30.0) / 100.0;
        int maxSpecialCount = (int) Math.ceil(size * maxSpecialPercentage);
        int specialCount = guaranteedSpecial ? 
            1 + random.nextInt(Math.max(1, maxSpecialCount)) : 
            random.nextInt(maxSpecialCount + 1);

        // Calculate scaling factor
        double scalingFactor = calculateScalingFactor();

        // Use AtomicInteger for the counter
        AtomicInteger counter = new AtomicInteger(0);

        // Spawn zombies
        for (int i = 0; i < size; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * 5; // Spread within 5 blocks
            double xOffset = Math.cos(angle) * distance;
            double zOffset = Math.sin(angle) * distance;

            Location spawnLoc = location.clone().add(xOffset, 0, zOffset);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc));

            world.spawn(spawnLoc, Zombie.class, zombie -> {
                // Set follow range
                if (zombie.getAttribute(Attribute.FOLLOW_RANGE) != null) {
                    double range = followRange < 0 ? Double.MAX_VALUE : followRange;
                    zombie.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(range);
                }

                // Apply custom type if this is a special zombie
                if (counter.getAndIncrement() < specialCount) {
                    plugin.getZombieManager().applyRandomType(zombie);
                }

                // Apply scaling if enabled
                if (scalingFactor > 1.0) {
                    if (zombie.getAttribute(Attribute.MAX_HEALTH) != null) {
                        double baseHealth = zombie.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                        zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth * scalingFactor);
                        zombie.setHealth(baseHealth * scalingFactor);
                    }
                    if (zombie.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                        double baseDamage = zombie.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue();
                        zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(baseDamage * scalingFactor);
                    }
                }
            });
        }
    }

    private double calculateScalingFactor() {
        if (!plugin.getConfig().getBoolean("horde-composition.scaling.enabled", true)) {
            return 1.0;
        }
        double baseScaling = plugin.getConfig().getDouble("horde-composition.scaling.factor", 0.1);
        double maxMultiplier = plugin.getConfig().getDouble("horde-composition.scaling.max-multiplier", 2.0);
        return Math.min(maxMultiplier, 1 + (Bukkit.getOnlinePlayers().size() - 1) * baseScaling);
    }
} 