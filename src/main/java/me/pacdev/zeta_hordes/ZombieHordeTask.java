package me.pacdev.zeta_hordes;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class ZombieHordeTask extends BukkitRunnable {

    private final Main plugin;
    private final Random random = new Random();

    public ZombieHordeTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Player targetPlayer = getRandomTargetPlayer();
        if (targetPlayer != null) {
            spawnZombieHorde(targetPlayer);
            // sendRandomMessage(targetPlayer);
        }
    }

    private Player getRandomTargetPlayer() {
        List<Player> players = (List<Player>) Bukkit.getOnlinePlayers();
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");

        // Filter out blacklisted players and players in Creative/Spectator mode
        List<Player> validPlayers = players.stream()
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
            Location spawnLocation = playerLocation.clone().add(
                    random.nextInt(maxDistance - minDistance + 1) + minDistance, // Random X offset
                    0, // Y offset (will be adjusted to the highest block)
                    random.nextInt(maxDistance - minDistance + 1) + minDistance  // Random Z offset
            );

            spawnLocation.setY(world.getHighestBlockYAt(spawnLocation));

            // Spawn the zombie using Paper's API
            world.spawn(spawnLocation, org.bukkit.entity.Zombie.class, zombie -> {
                zombie.setTarget(player); // Make the zombie target the player
            });
        }
    }

    private void sendRandomMessage(Player player) {
        List<String> messages = plugin.getConfig().getStringList("messages");
        String message = messages.get(random.nextInt(messages.size()));
        player.sendMessage(message);
    }
}