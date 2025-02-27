package me.pacdev.zeta_hordes;

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

public class ZombieHordeTask extends BukkitRunnable {

    private final Main plugin; 
    private final Set<String> worldWhitelist;

    public ZombieHordeTask(Main plugin) { 
        this.plugin = plugin;
        this.worldWhitelist = new HashSet<>(plugin.getConfig().getStringList("world-whitelist"));
    }
    private final Random random = new Random();

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
                .filter(player -> worldWhitelist.contains(player.getWorld().getName())) // Only include players in whitelisted worlds
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
        int minZombies = plugin.getConfig().getInt("zombie-count.min");
        int maxZombies = plugin.getConfig().getInt("zombie-count.max");
        int zombieCount = minZombies + random.nextInt(maxZombies - minZombies + 1);

        int minDistance = plugin.getConfig().getInt("spread.min-distance");
        int maxDistance = plugin.getConfig().getInt("spread.max-distance");

        for (int i = 0; i < zombieCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI; // Random direction in radians
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            double xOffset = Math.cos(angle) * distance;
            double zOffset = Math.sin(angle) * distance;

            Location spawnLocation = playerLocation.clone().add(xOffset, 0, zOffset);
            spawnLocation.setY(world.getHighestBlockYAt(spawnLocation));

            // Spawn the zombie and set its target
            world.spawn(spawnLocation, Zombie.class, zombie -> {
                zombie.setTarget(player);
                setFollowRange(zombie, 80.0);
            });
        }
    }
    private void setFollowRange(Zombie zombie, double range) {
        if (zombie.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            zombie.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(range);
        }
    }
}
