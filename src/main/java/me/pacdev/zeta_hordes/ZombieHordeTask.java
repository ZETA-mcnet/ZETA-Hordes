package me.pacdev.zeta_hordes;

import org.bukkit.Bukkit;
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
        List<Player> players = (List<Player>) Bukkit.getOnlinePlayers();
        int targetPlayerCount = (int) Math.ceil(players.size() * plugin.getConfig().getDouble("player-percentage"));

        for (int i = 0; i < targetPlayerCount; i++) {
            Player targetPlayer = players.get(random.nextInt(players.size()));
            spawnZombieHorde(targetPlayer);
            sendRandomMessage(targetPlayer);
        }
    }

    private void spawnZombieHorde(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        int minZombies = plugin.getConfig().getInt("zombie-count.min");
        int maxZombies = plugin.getConfig().getInt("zombie-count.max");
        int zombieCount = minZombies + random.nextInt(maxZombies - minZombies + 1);

        for (int i = 0; i < zombieCount; i++) {
            Location spawnLocation = playerLocation.clone().add(
                    random.nextInt(20) - 10, // Random X offset (-10 to 10 blocks)
                    0, // Y offset (will be adjusted to the highest block)
                    random.nextInt(20) - 10  // Random Z offset (-10 to 10 blocks)
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