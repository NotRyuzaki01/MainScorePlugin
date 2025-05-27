package me.not_ryuzaki.mainScorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.UUID;

public class SettingsCommand implements CommandExecutor, Listener {
    private final MainScorePlugin plugin;

    public SettingsCommand(MainScorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        boolean scoreboard = plugin.getBoard(player.getUniqueId()) != null;

        Inventory gui = Bukkit.createInventory(null, 27, "Settings");

        // Scoreboard toggle
        ItemStack lectern = new ItemStack(Material.LECTERN);
        ItemMeta lecternMeta = lectern.getItemMeta();
        lecternMeta.setDisplayName("§x§3§7§e§b§9§aScoreboard");
        lecternMeta.setLore(Collections.singletonList("§fCurrently: " + (scoreboard ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
        lectern.setItemMeta(lecternMeta);
        gui.setItem(11, lectern);

        // Payments toggle
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName("§x§3§7§e§b§9§aPayments");
        boolean payEnabled = plugin.isPayEnabled(player.getUniqueId());
        emeraldMeta.setLore(Collections.singletonList("§fCurrently: " + (payEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
        emerald.setItemMeta(emeraldMeta);
        gui.setItem(13, emerald);

        // TPA toggle
        ItemStack enderPearl = new ItemStack(Material.ENDER_PEARL);
        ItemMeta pearlMeta = enderPearl.getItemMeta();
        pearlMeta.setDisplayName("§x§3§7§e§b§9§aTPA Requests");
        boolean tpaEnabled = plugin.isTpaEnabled(player.getUniqueId());
        pearlMeta.setLore(Collections.singletonList("§fCurrently: " + (tpaEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
        enderPearl.setItemMeta(pearlMeta);
        gui.setItem(15, enderPearl);

        player.openInventory(gui);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Settings")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        UUID uuid = player.getUniqueId();

        if (clicked.getType() == Material.LECTERN) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            boolean current = plugin.getBoard(uuid) != null;
            boolean newState = !current;
            plugin.setScoreboardEnabled(uuid, newState);
            plugin.getConfig().set("scoreboard-enabled." + uuid, newState);
            plugin.saveConfig();

            ItemMeta meta = clicked.getItemMeta();
            meta.setLore(Collections.singletonList("§fCurrently: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
            clicked.setItemMeta(meta);
            player.getOpenInventory().setItem(11, clicked);
        }

        if (clicked.getType() == Material.EMERALD) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            boolean current = plugin.isPayEnabled(uuid);
            boolean newState = !current;
            plugin.setPayEnabled(uuid, newState);
            plugin.getConfig().set("pay-enabled." + uuid, newState);
            plugin.saveConfig();

            ItemMeta meta = clicked.getItemMeta();
            meta.setLore(Collections.singletonList("§fCurrently: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
            clicked.setItemMeta(meta);
            player.getOpenInventory().setItem(13, clicked);
        }

        if (clicked.getType() == Material.ENDER_PEARL) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            boolean current = plugin.isTpaEnabled(uuid);
            boolean newState = !current;
            plugin.setTpaEnabled(uuid, newState);
            plugin.getConfig().set("tpa-enabled." + uuid, newState);
            plugin.saveConfig();

            ItemMeta meta = clicked.getItemMeta();
            meta.setLore(Collections.singletonList("§fCurrently: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
            clicked.setItemMeta(meta);
            player.getOpenInventory().setItem(15, clicked);
        }
    }
}
