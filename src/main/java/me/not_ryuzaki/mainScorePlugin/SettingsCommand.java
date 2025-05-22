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
        ItemStack lectern = new ItemStack(Material.LECTERN);
        ItemMeta lecternMeta = lectern.getItemMeta();

        lecternMeta.setDisplayName(ChatColor.GREEN + "SCOREBOARD");

        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        emeraldMeta.setDisplayName(ChatColor.GREEN + "PAYMENTS");

        boolean payEnabled = plugin.isPayEnabled(player.getUniqueId());
        emeraldMeta.setLore(Collections.singletonList("§fCurrently: " + (payEnabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
        emerald.setItemMeta(emeraldMeta);

        gui.setItem(14, emerald); // Slot 11 for emerald


        if (scoreboard)
            lecternMeta.setLore(Collections.singletonList("§fCurrently: " + ChatColor.GREEN + "ON"));
        else
            lecternMeta.setLore(Collections.singletonList("§fCurrently: " + ChatColor.RED + "OFF"));

        lectern.setItemMeta(lecternMeta);
        gui.setItem(12, lectern);

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

        if (clicked.getType() == Material.LECTERN) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            UUID uuid = player.getUniqueId();
            boolean current = plugin.getBoard(uuid) != null;
            boolean newState = !current;

            // Update plugin scoreboard toggle
            plugin.setScoreboardEnabled(uuid, newState);
            plugin.getConfig().set("scoreboard-enabled." + uuid, newState);
            plugin.saveConfig();

            // Update item
            ItemMeta meta = clicked.getItemMeta();
            meta.setLore(Collections.singletonList("§fCurrently: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
            clicked.setItemMeta(meta);

            // Update inventory view
            player.getOpenInventory().setItem(12, clicked);
        }
        if (clicked.getType() == Material.EMERALD) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            UUID uuid = player.getUniqueId();
            boolean current = plugin.isPayEnabled(uuid);
            boolean newState = !current;

            plugin.setPayEnabled(uuid, newState);
            plugin.getConfig().set("pay-enabled." + uuid, newState);
            plugin.saveConfig();

            // Update emerald button
            ItemMeta meta = clicked.getItemMeta();
            meta.setLore(Collections.singletonList("§fCurrently: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
            clicked.setItemMeta(meta);
            player.getOpenInventory().setItem(14, clicked);
        }

    }
}