package me.pacdev.zeta_hordes.afk;

import me.pacdev.zeta_hordes.ZetaHordes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager implements Listener {
    private final ZetaHordes plugin;
    private final Map<UUID, AFKData> playerData;
    private final long timeoutSeconds;

    public AFKManager(ZetaHordes plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.timeoutSeconds = plugin.getConfig().getLong("afk.timeout", 300);
        
        // Start AFK checker task
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAFKPlayers();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }

    private void checkAFKPlayers() {
        long currentTime = System.currentTimeMillis();
        for (AFKData data : playerData.values()) {
            if (!data.isAFK() && (currentTime - data.getLastActivity()) / 1000 >= timeoutSeconds) {
                data.setAFK(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerData.put(event.getPlayer().getUniqueId(), new AFKData(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("afk.track.movement", true)) return;
        AFKData data = playerData.get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.updateLocation(event.getTo());
        }
    }

    @EventHandler
    public void onPlayerRotate(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("afk.track.rotation", true)) return;
        if (event.getTo().getYaw() == event.getFrom().getYaw() && 
            event.getTo().getPitch() == event.getFrom().getPitch()) return;
        
        AFKData data = playerData.get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.updateRotation(event.getTo().getYaw(), event.getTo().getPitch());
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("afk.track.chat", true)) return;
        AFKData data = playerData.get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.updateActivity();
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("afk.track.actions", true)) return;
        if (event.getMessage().toLowerCase().startsWith("/afk")) return;
        AFKData data = playerData.get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.updateActivity();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("afk.track.inventory", true)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        AFKData data = playerData.get(event.getWhoClicked().getUniqueId());
        if (data != null) {
            data.updateActivity();
        }
    }

    public boolean isPlayerAFK(Player player) {
        AFKData data = playerData.get(player.getUniqueId());
        return data != null && data.isAFK();
    }

    public void setPlayerAFK(Player player, boolean afk) {
        AFKData data = playerData.get(player.getUniqueId());
        if (data != null) {
            data.setAFK(afk);
        }
    }

    public Map<UUID, AFKData> getPlayerData() {
        return playerData;
    }
} 