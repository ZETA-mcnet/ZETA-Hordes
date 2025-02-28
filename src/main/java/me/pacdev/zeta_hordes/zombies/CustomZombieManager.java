package me.pacdev.zeta_hordes.zombies;

import me.pacdev.zeta_hordes.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Zombie;

import java.util.*;

public class CustomZombieManager {
    private final Main plugin;
    private final Map<String, CustomZombieType> zombieTypes;
    private final NavigableMap<Integer, String> weightMap;
    private int totalWeight;

    public CustomZombieManager(Main plugin) {
        this.plugin = plugin;
        this.zombieTypes = new HashMap<>();
        this.weightMap = new TreeMap<>();
        loadZombieTypes();
    }

    private void loadZombieTypes() {
        zombieTypes.clear();
        weightMap.clear();
        totalWeight = 0;

        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("zombie-types");
        if (typesSection == null) return;

        for (String typeId : typesSection.getKeys(false)) {
            ConfigurationSection typeConfig = typesSection.getConfigurationSection(typeId);
            if (typeConfig == null) continue;

            CustomZombieType type = new CustomZombieType(typeId, typeConfig);
            zombieTypes.put(typeId, type);

            // Only add to weight map if minimum player requirement is met
            if (Bukkit.getOnlinePlayers().size() >= type.getMinPlayers()) {
                totalWeight += type.getSpawnWeight();
                weightMap.put(totalWeight, typeId);
            }
        }
    }

    public void refreshWeights() {
        weightMap.clear();
        totalWeight = 0;
        
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        for (CustomZombieType type : zombieTypes.values()) {
            if (onlinePlayers >= type.getMinPlayers()) {
                totalWeight += type.getSpawnWeight();
                weightMap.put(totalWeight, type.getId());
            }
        }
    }

    public CustomZombieType getRandomType() {
        if (weightMap.isEmpty()) return null;
        
        int roll = new Random().nextInt(totalWeight) + 1;
        String typeId = weightMap.ceilingEntry(roll).getValue();
        return zombieTypes.get(typeId);
    }

    public CustomZombieType getType(String id) {
        return zombieTypes.get(id);
    }

    public Collection<CustomZombieType> getAllTypes() {
        return zombieTypes.values();
    }

    public void applyToZombie(Zombie zombie, String typeId) {
        CustomZombieType type = zombieTypes.get(typeId);
        if (type != null) {
            type.apply(zombie);
        }
    }

    public void applyRandomType(Zombie zombie) {
        CustomZombieType type = getRandomType();
        if (type != null) {
            type.apply(zombie);
        }
    }
} 