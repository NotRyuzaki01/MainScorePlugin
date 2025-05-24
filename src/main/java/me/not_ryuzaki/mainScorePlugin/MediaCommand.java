package me.not_ryuzaki.mainScorePlugin;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

public class MediaCommand implements CommandExecutor, Listener {

    private final String GUI_TITLE = "ᴍᴇᴅɪᴀ ʀᴀɴᴋ";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        Player player = (Player) sender;
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Create pink leather helmet
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FF00AA") + "📽ᴍᴇᴅɪᴀ ʀᴀɴᴋ");
            meta.setColor(Color.fromRGB(255, 0, 170)); // #FF00AA

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.of("#FF00AA") + "Requirements: " + ChatColor.GRAY + "(only one needed)");
            lore.add(ChatColor.WHITE + "- 25 average viewers on a Stream");
            lore.add(ChatColor.WHITE + "- 5k Views on a YouTube Video");
            lore.add(ChatColor.WHITE + "- 50k views on a TikTok");
            lore.add(ChatColor.WHITE + "- 50k views on a YouTube Short");
            lore.add(ChatColor.WHITE + "- 50k views on a Instagram Reels");
            lore.add("");
            lore.add(ChatColor.of("#FF00AA") + "Reminders:");
            lore.add(ChatColor.WHITE + "- Must have the IP on screen");
            lore.add(ChatColor.WHITE + "- Create ticket in discord for the rank");
            lore.add(ChatColor.WHITE + "- It lasts 90 days and gives media perks");

            meta.setLore(lore);

            // Hide all extra text
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_DYE,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ENCHANTS);

            helmet.setItemMeta(meta);
        }

        gui.setItem(13, helmet);
        player.openInventory(gui);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);

            // Prevent moving items out of the inventory
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            }
        }
    }
}