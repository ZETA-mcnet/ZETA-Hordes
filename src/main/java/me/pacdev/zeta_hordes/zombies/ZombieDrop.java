package me.pacdev.zeta_hordes.zombies;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class ZombieDrop {
    private final Material material;
    private final double chance;
    private final int minAmount;
    private final int maxAmount;
    private final Random random;

    public ZombieDrop(ConfigurationSection config) {
        this.material = Material.valueOf(config.getString("item").toUpperCase());
        this.chance = config.getDouble("chance", 1.0);
        
        String[] amountParts = config.getString("amount", "1").split("-");
        this.minAmount = Integer.parseInt(amountParts[0]);
        this.maxAmount = amountParts.length > 1 ? Integer.parseInt(amountParts[1]) : minAmount;
        
        this.random = new Random();
    }

    public boolean shouldDrop() {
        return random.nextDouble() < chance;
    }

    public ItemStack generateDrop() {
        int amount = minAmount;
        if (maxAmount > minAmount) {
            amount = minAmount + random.nextInt(maxAmount - minAmount + 1);
        }
        return new ItemStack(material, amount);
    }
} 