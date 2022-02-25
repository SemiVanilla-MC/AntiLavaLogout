package com.semivanilla.antilavalogout;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.swing.text.html.Option;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AntiLavaLogout extends JavaPlugin implements Listener {
    private static Map<UUID,Long> lavaLogouts = new HashMap<>();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        if (!new File(getDataFolder(), "config.yml").exists())
            this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        if (isInLava(event.getPlayer())) {
            lavaLogouts.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
            System.out.println(event.getPlayer().getName() + " logged out in lava!");
        }
    }

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        Optional<Map.Entry<UUID,Long>> entry = lavaLogouts.entrySet().stream().filter(e -> e.getKey().equals(event.getPlayer().getUniqueId())).findFirst();
        if (entry.orElse(null) == null)
             return;
        Location loc = event.getPlayer().getLocation();
        long logout = entry.get().getValue();
        long now = System.currentTimeMillis();
        long diff = now - logout;
        long seconds = diff / 1000;
        double damagePerSecond = getConfig().getDouble("damage-second"),
        baseDamage = getConfig().getDouble("base-damage");
        double damage = baseDamage + (seconds * damagePerSecond);
        damagePlayer(event.getPlayer(), damage);
    }

    public void damagePlayer(Player p, double damage) {
        double points = p.getAttribute(Attribute.GENERIC_ARMOR).getValue();
        double toughness = p.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
        PotionEffect effect = p.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        int resistance = effect == null ? 0 : effect.getAmplifier();
        int epf = getEPF(p.getInventory());
        int fire = getFireProt(p.getInventory());

        double end = p.getHealth() - calculateDamageApplied(damage, points, toughness, resistance, epf, fire);
        //clamp to 0.0
        if (end < 0.0) {
            end = 0.0;
        }
        p.setHealth(end);
    }

    public double calculateDamageApplied(double damage, double points, double toughness, int resistance, int epf, int fireProt) {
        double withArmorAndToughness = damage * (1 - Math.min(20, Math.max(points / 5, points - damage / (2 + toughness / 4))) / 25);
        double withResistance = withArmorAndToughness * (1 - (resistance * 0.2));
        double withEnchants = withResistance * (1 - (Math.min(20.0, epf) / 25));
        // (8 * level)% of damage is blocked by fire protection
        double withFireProt = withEnchants * (1 - (Math.min(20.0, fireProt) / 25));
        return withFireProt;
    }

    public static int getEPF(PlayerInventory inv) {
        ItemStack helm = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boot = inv.getBoots();

        return (helm != null ? helm.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) : 0) +
                (chest != null ? chest.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) : 0) +
                (legs != null ? legs.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) : 0) +
                (boot != null ? boot.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) : 0);
    }

    public static int getFireProt(PlayerInventory inv) {
        ItemStack helm = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boot = inv.getBoots();

        return (helm != null ? helm.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) : 0) +
                (chest != null ? chest.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) : 0) +
                (legs != null ? legs.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) : 0) +
                (boot != null ? boot.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) : 0);
    }

    public boolean isInLava(Player p) {
        if (p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE))
            return false;
        Location loc = p.getLocation();
        return loc.getBlock().getType() == Material.LAVA || loc.clone().add(0, 1, 0).getBlock().getType() == Material.LAVA;
    }
}
