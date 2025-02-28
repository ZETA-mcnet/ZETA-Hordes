package me.pacdev.zeta_hordes.zombies;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomZombieType {
    private final String id;
    private final String displayName;
    private final double health;
    private final double damage;
    private final double speed;
    private final double knockbackResistance;
    private final Map<String, ItemStack> armor;
    private final List<PotionEffect> effects;
    private final Map<String, Object> abilities;
    private final int spawnWeight;
    private final int minPlayers;
    private final List<ZombieDrop> drops;

    public CustomZombieType(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = ChatColor.translateAlternateColorCodes('&', config.getString("display-name", id));
        this.health = config.getDouble("health", 20.0);
        this.damage = config.getDouble("damage", 3.0);
        this.speed = config.getDouble("speed", 0.23);
        this.knockbackResistance = config.getDouble("knockback-resistance", 0.0);
        this.spawnWeight = config.getInt("spawn-weight", 10);
        this.minPlayers = config.getInt("min-players", 0);
        
        // Load armor
        this.armor = new HashMap<>();
        if (config.contains("armor")) {
            ConfigurationSection armorSection = config.getConfigurationSection("armor");
            if (armorSection != null) {
                for (String slot : armorSection.getKeys(false)) {
                    Material material = Material.valueOf(armorSection.getString(slot));
                    armor.put(slot, new ItemStack(material));
                }
            }
        }

        // Load effects
        this.effects = new ArrayList<>();
        if (config.contains("effects")) {
            for (String effectStr : config.getStringList("effects")) {
                String[] parts = effectStr.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                int amplifier = Integer.parseInt(parts[1]);
                int duration = parts[2].equals("infinite") ? Integer.MAX_VALUE : Integer.parseInt(parts[2]);
                effects.add(new PotionEffect(type, duration, amplifier));
            }
        }

        // Load abilities
        this.abilities = new HashMap<>();
        if (config.contains("abilities")) {
            ConfigurationSection abilitiesSection = config.getConfigurationSection("abilities");
            if (abilitiesSection != null) {
                for (String abilityName : abilitiesSection.getKeys(false)) {
                    ConfigurationSection abilitySection = abilitiesSection.getConfigurationSection(abilityName);
                    if (abilitySection != null) {
                        Map<String, Object> abilityData = new HashMap<>();
                        for (String key : abilitySection.getKeys(false)) {
                            abilityData.put(key, abilitySection.get(key));
                        }
                        abilities.put(abilityName, abilityData);
                    }
                }
            }
        }

        // Load drops
        this.drops = new ArrayList<>();
        if (config.contains("drops")) {
            for (Map<?, ?> dropMap : config.getMapList("drops")) {
                String item = (String) dropMap.get("item");
                double chance = ((Number) dropMap.get("chance")).doubleValue();
                String amount = (String) dropMap.get("amount");
                drops.add(new ZombieDrop(Material.valueOf(item), chance, amount));
            }
        }
    }

    public void apply(Zombie zombie) {
        // Set basic attributes
        zombie.setCustomName(displayName);
        zombie.setCustomNameVisible(true);
        
        if (zombie.getAttribute(Attribute.MAX_HEALTH) != null) {
            zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
            zombie.setHealth(health);
        }
        if (zombie.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
        }
        if (zombie.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(knockbackResistance);
        }

        // Apply armor
        if (armor.containsKey("helmet")) zombie.getEquipment().setHelmet(armor.get("helmet"));
        if (armor.containsKey("chestplate")) zombie.getEquipment().setChestplate(armor.get("chestplate"));
        if (armor.containsKey("leggings")) zombie.getEquipment().setLeggings(armor.get("leggings"));
        if (armor.containsKey("boots")) zombie.getEquipment().setBoots(armor.get("boots"));

        // Apply effects
        effects.forEach(effect -> zombie.addPotionEffect(effect));

        // Store custom type ID in metadata
        zombie.setMetadata("custom_zombie_type", new FixedMetadataValue(
            me.pacdev.zeta_hordes.Main.getPlugin(me.pacdev.zeta_hordes.Main.class), 
            id
        ));
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getHealth() { return health; }
    public double getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public int getSpawnWeight() { return spawnWeight; }
    public int getMinPlayers() { return minPlayers; }
    public Map<String, Object> getAbilities() { return abilities; }
    public List<ZombieDrop> getDrops() { return drops; }

    // Inner class for drops
    public static class ZombieDrop {
        private final Material material;
        private final double chance;
        private final int minAmount;
        private final int maxAmount;

        public ZombieDrop(Material material, double chance, String amount) {
            this.material = material;
            this.chance = chance;
            String[] parts = amount.split("-");
            this.minAmount = Integer.parseInt(parts[0]);
            this.maxAmount = parts.length > 1 ? Integer.parseInt(parts[1]) : minAmount;
        }

        public ItemStack generateDrop() {
            if (Math.random() > chance) return null;
            int amount = minAmount;
            if (maxAmount > minAmount) {
                amount += (int)(Math.random() * (maxAmount - minAmount + 1));
            }
            return new ItemStack(material, amount);
        }
    }
} 