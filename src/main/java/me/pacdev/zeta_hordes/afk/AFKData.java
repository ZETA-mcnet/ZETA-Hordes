package me.pacdev.zeta_hordes.afk;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class AFKData {
    private final Player player;
    private long lastActivity;
    private Location lastLocation;
    private float lastYaw;
    private float lastPitch;
    private boolean isAFK;

    public AFKData(Player player) {
        this.player = player;
        this.lastActivity = System.currentTimeMillis();
        this.lastLocation = player.getLocation().clone();
        this.lastYaw = player.getLocation().getYaw();
        this.lastPitch = player.getLocation().getPitch();
        this.isAFK = false;
    }

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
        if (isAFK) {
            setAFK(false);
        }
    }

    public void updateLocation(Location location) {
        if (hasMovedSignificantly(location)) {
            updateActivity();
        }
        this.lastLocation = location.clone();
    }

    public void updateRotation(float yaw, float pitch) {
        if (hasRotatedSignificantly(yaw, pitch)) {
            updateActivity();
        }
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    private boolean hasMovedSignificantly(Location newLoc) {
        if (lastLocation.getWorld() != newLoc.getWorld()) return true;
        double threshold = player.getServer().getPluginManager()
            .getPlugin("ZETA-Hordes")
            .getConfig()
            .getDouble("afk.movement-threshold", 0.2);
        return lastLocation.distance(newLoc) >= threshold;
    }

    private boolean hasRotatedSignificantly(float newYaw, float newPitch) {
        double threshold = player.getServer().getPluginManager()
            .getPlugin("ZETA-Hordes")
            .getConfig()
            .getDouble("afk.rotation-threshold", 15.0);
        float yawDiff = Math.abs(newYaw - lastYaw);
        float pitchDiff = Math.abs(newPitch - lastPitch);
        return yawDiff >= threshold || pitchDiff >= threshold;
    }

    public void setAFK(boolean afk) {
        this.isAFK = afk;
    }

    public boolean isAFK() {
        return isAFK;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public Player getPlayer() {
        return player;
    }
} 