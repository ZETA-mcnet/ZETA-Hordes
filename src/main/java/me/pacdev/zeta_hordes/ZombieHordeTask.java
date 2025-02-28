package me.pacdev.zeta_hordes;

import me.pacdev.zeta_hordes.zombies.CustomZombieManager;
import org.bukkit.Bukkit;
import java.util.HashSet;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.attribute.Attribute;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ZombieHordeTask extends BukkitRunnable {

    private final ZetaHordes plugin; 
    private final Set<String> worldWhitelist;
    private final Random random = new Random();
    private final CustomZombieManager zombieManager;

    public ZombieHordeTask(ZetaHordes plugin) { 
        this.plugin = plugin;
        this.worldWhitelist = new HashSet<>(plugin.getConfig().getStringList("world-whitelist"));
        this.zombieManager = plugin.getZombieManager();
    }

    @Override
    public void run() {
        Player targetPlayer = getRandomTargetPlayer();
        if (targetPlayer != null) {
            spawnZombieHorde(targetPlayer);
            Bukkit.getLogger().info("HORDE spawned, target: " + targetPlayer);
            // sendRandomMessage(targetPlayer);
        }
    }

    private Player getRandomTargetPlayer() {
        List<Player> players = (List<Player>) Bukkit.getOnlinePlayers();
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");

        // Filter out blacklisted players and players in Creative/Spectator mode
        List<Player> validPlayers = players.stream()
                .filter(player -> worldWhitelist.contains(player.getWorld().getName())) // Only include players in whitelisted worlds
                .filter(player -> !blacklist.contains(player.getName())) // Exclude blacklisted players
                .filter(player -> player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) // Only Survival/Adventure
                .toList();

        if (validPlayers.isEmpty()) {
            return null; // No valid players to target
        }

        return validPlayers.get(random.nextInt(validPlayers.size()));
    }

    public void spawnZombieHorde(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        
        // Get horde size
        int minZombies = plugin.getConfig().getInt("zombie-count.min");
        int maxZombies = plugin.getConfig().getInt("zombie-count.max");
        int zombieCount = minZombies + random.nextInt(maxZombies - minZombies + 1);

        // Get spawn distance range
        int minDistance = plugin.getConfig().getInt("spread.min-distance");
        int maxDistance = plugin.getConfig().getInt("spread.max-distance");

        // Calculate special zombie quota
        boolean guaranteedSpecial = plugin.getConfig().getBoolean("horde-composition.guaranteed-special", true);
        double maxSpecialPercentage = plugin.getConfig().getDouble("horde-composition.max-special-percentage", 30.0) / 100.0;
        int maxSpecialCount = (int) Math.ceil(zombieCount * maxSpecialPercentage);
        final int specialCount = guaranteedSpecial ? 
            1 + random.nextInt(Math.max(1, maxSpecialCount)) : 
            random.nextInt(maxSpecialCount + 1);

        // Apply scaling if enabled
        final double scalingFactor = calculateScalingFactor();

        // Get follow range from config
        final double followRange = plugin.getConfig().getDouble("hordes.follow-range", 256.0);

        // Use AtomicInteger for the counter
        AtomicInteger counter = new AtomicInteger(0);

        // Spawn zombies
        for (int i = 0; i < zombieCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            double xOffset = Math.cos(angle) * distance;
            double zOffset = Math.sin(angle) * distance;

            Location spawnLocation = playerLocation.clone().add(xOffset, 0, zOffset);
            spawnLocation.setY(world.getHighestBlockYAt(spawnLocation));

            // Spawn the zombie
            world.spawn(spawnLocation, Zombie.class, zombie -> {
                zombie.setTarget(player);

                // Set follow range (use Double.MAX_VALUE for unlimited range)
                if (zombie.getAttribute(Attribute.FOLLOW_RANGE) != null) {
                    double range = followRange < 0 ? Double.MAX_VALUE : followRange;
                    zombie.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(range);
                }

                // Apply custom type if this should be a special zombie
                if (counter.getAndIncrement() < specialCount) {
                    zombieManager.applyRandomType(zombie);
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
