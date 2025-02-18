package me.pacdev.zeta_hordes;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BlockNaturalSpawn implements Listener {

    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            event.setCancelled(true);
        }
    }
}