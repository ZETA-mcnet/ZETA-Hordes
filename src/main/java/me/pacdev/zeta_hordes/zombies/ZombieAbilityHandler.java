package me.pacdev.zeta_hordes.zombies;

import me.pacdev.zeta_hordes.ZetaHordes;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ZombieAbilityHandler implements Listener {
    private final ZetaHordes plugin;
    private final CustomZombieManager zombieManager;
    private final Map<UUID, Map<String, Long>> cooldowns;

    public ZombieAbilityHandler(ZetaHordes plugin, CustomZombieManager zombieManager) {
        this.plugin = plugin;
        this.zombieManager = zombieManager;
        this.cooldowns = new HashMap<>();
    }

    @EventHandler
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Zombie zombie)) return;
        if (!zombie.hasMetadata("custom_zombie_type")) return;

        String typeId = getZombieTypeId(zombie);
        if (typeId == null) return;

        CustomZombieType type = zombieManager.getType(typeId);
        if (type == null) return;

        // Handle abilities that trigger on attack
        Map<String, Object> abilities = type.getAbilities();
        if (abilities.containsKey("ground-pound")) {
            handleGroundPound(zombie, abilities.get("ground-pound"));
        }
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!zombie.hasMetadata("custom_zombie_type")) return;

        String typeId = getZombieTypeId(zombie);
        if (typeId == null) return;

        CustomZombieType type = zombieManager.getType(typeId);
        if (type == null) return;

        // Handle death effects (like bomber explosion)
        Map<String, Object> abilities = type.getAbilities();
        if (abilities.containsKey("explosion")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> explosionData = (Map<String, Object>) abilities.get("explosion");
            Location loc = zombie.getLocation();
            float power = ((Number) explosionData.get("power")).floatValue();
            boolean fire = (boolean) explosionData.get("fire");
            boolean blockDamage = (boolean) explosionData.get("block-damage");
            
            zombie.getWorld().createExplosion(loc, power, fire, blockDamage);
        }

        // Handle drops
        event.getDrops().clear(); // Clear default drops
        type.getDrops().forEach(drop -> {
            ItemStack item = drop.generateDrop();
            if (item != null) {
                event.getDrops().add(item);
            }
        });
    }

    private String getZombieTypeId(Entity entity) {
        List<MetadataValue> metadata = entity.getMetadata("custom_zombie_type");
        if (metadata.isEmpty()) return null;
        return metadata.get(0).asString();
    }

    private boolean canUseAbility(UUID entityId, String ability, int cooldownTicks) {
        Map<String, Long> entityCooldowns = cooldowns.computeIfAbsent(entityId, k -> new HashMap<>());
        long lastUse = entityCooldowns.getOrDefault(ability, 0L);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastUse < cooldownTicks * 50L) { // Convert ticks to milliseconds
            return false;
        }
        
        entityCooldowns.put(ability, currentTime);
        return true;
    }

    private void handleGroundPound(Zombie zombie, Object abilityData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) abilityData;
        UUID zombieId = zombie.getUniqueId();
        
        // Check cooldown
        if (!canUseAbility(zombieId, "ground-pound", ((Number) data.get("cooldown")).intValue())) {
            return;
        }

        double radius = ((Number) data.get("radius")).doubleValue();
        double damage = ((Number) data.get("damage")).doubleValue();

        // Ground pound effect
        new BukkitRunnable() {
            @Override
            public void run() {
                Location center = zombie.getLocation();
                zombie.getWorld().getNearbyEntities(center, radius, radius, radius).stream()
                    .filter(e -> e instanceof LivingEntity)
                    .filter(e -> e instanceof Player || (e instanceof Zombie && !e.equals(zombie)))
                    .forEach(entity -> {
                        LivingEntity target = (LivingEntity) entity;
                        Vector knockback = target.getLocation().subtract(center).toVector().normalize().multiply(1.5);
                        knockback.setY(0.5);
                        target.setVelocity(knockback);
                        target.damage(damage);
                        
                        if (target instanceof Player) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        }
                    });

                // Visual and sound effects
                zombie.getWorld().strikeLightningEffect(center);
            }
        }.runTask(plugin);
    }
} 