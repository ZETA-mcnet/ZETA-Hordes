package me.pacdev.zeta_hordes;


import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Objects;

public class Main extends JavaPlugin {

    private BukkitTask roundTask;
    private int hordeQuota;
    private int hordesSpawned;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Start the round task
        startRoundTask();

        // // Register the listener for custom spawning logic
        // getServer().getPluginManager().registerEvents(new ZombieSpawnListener(), this);

        // Register commands
        Objects.requireNonNull(getCommand("zetahordes")).setExecutor(this);

        getLogger().info("ZombieHorde plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ZombieHorde plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§aZetaHordes commands:");
            sender.sendMessage("§a/zetahordes reload - Reload the config.");
            sender.sendMessage("§a/zetahordes force [player] - Force a horde on a player.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                startRoundTask(); // Restart the round task with the new config
                sender.sendMessage("§aConfig reloaded!");
                return true;

            case "force":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /zetahordes force <player>");
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }

                // Check if the player is blacklisted or in an invalid game mode
                List<String> blacklist = getConfig().getStringList("blacklist");
                if (blacklist.contains(targetPlayer.getName())) {
                    sender.sendMessage("§cThis player is blacklisted!");
                    return true;
                }

                if (targetPlayer.getGameMode() != GameMode.SURVIVAL && targetPlayer.getGameMode() != GameMode.ADVENTURE) {
                    sender.sendMessage("§cThis player is not in Survival or Adventure mode!");
                    return true;
                }

                new ZombieHordeTask(this).spawnZombieHorde(targetPlayer);
                sender.sendMessage("§aForced a horde on " + targetPlayer.getName() + "!");
                return true;

            default:
                sender.sendMessage("§cUnknown command. Use /zetahordes for help.");
                return true;
        }
    }

    private void startRoundTask() {
        // Cancel the existing task if it's running
        if (roundTask != null) {
            roundTask.cancel();
        }

        // Reset horde quota and count
        hordeQuota = calculateHordeQuota();
        hordesSpawned = 0;

        // Schedule the round task
        int roundDuration = getConfig().getInt("round-duration") * 20; // Convert seconds to ticks
        int hordeInterval = getConfig().getInt("horde-interval") * 20; // Convert seconds to ticks

        roundTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (hordesSpawned >= hordeQuota) {
                    // The 10-second delay is IMPORTANT to prevent stack overflow. Maybe find a better way?
                    getServer().getScheduler().runTaskLater(Main.this, () -> Main.this.startRoundTask(), 200);
                    return;
                }

                // Spawn a small number of hordes
                int hordesToSpawn = Math.min(3, hordeQuota - hordesSpawned); // Spawn up to 3 hordes at a time
                for (int i = 0; i < hordesToSpawn; i++) {
                    new ZombieHordeTask(Main.this).run();
                    hordesSpawned++;
                }
            }
        }.runTaskTimer(this, 0, hordeInterval);
    }

    private int calculateHordeQuota() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        double hordePercentage = getConfig().getDouble("horde-percentage") / 100.0;
        return (int) Math.ceil(onlinePlayers * hordePercentage);
    }
}