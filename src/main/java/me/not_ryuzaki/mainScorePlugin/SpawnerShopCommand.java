package me.not_ryuzaki.mainScorePlugin;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SpawnerShopCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey key;

    private final Map<String, Integer> spawnerPrices = new LinkedHashMap<>() {{
        put("PIG", 250);
        put("COW", 350);
        put("ZOMBIE", 400);
        put("SPIDER", 750);
        put("SKELETON", 500);
        put("CREEPER", 625);
        put("ZOMBIFIED_PIGLIN", 750);
        put("BLAZE", 1000);
        put("IRON_GOLEM", 1500);
    }};

    public SpawnerShopCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "mobType");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        openSpawnerShop(player);
        return true;
    }

    private void openSpawnerShop(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SHOP - Spawner Shop");

        int slot = 9;
        for (Map.Entry<String, Integer> entry : spawnerPrices.entrySet()) {
            String mob = entry.getKey();
            int price = entry.getValue();

            ItemStack spawner = new ItemStack(Material.SPAWNER);
            ItemMeta meta = spawner.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Spawner");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + mob);
            lore.add(ChatColor.WHITE + "Buy Price: " + ChatColor.LIGHT_PURPLE + price + "x §lShards");

            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mob);
            meta.setLore(lore);
            spawner.setItemMeta(meta);
            gui.setItem(slot++, spawner);
        }

        // Back button
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "BACK");
        backMeta.setLore(Collections.singletonList(ChatColor.WHITE + "Click to return"));
        back.setItemMeta(backMeta);
        gui.setItem(18, back);

        player.openInventory(gui);
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        Inventory clickedInv = e.getClickedInventory();
        Inventory topInv = e.getView().getTopInventory();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Cancel all clicks during GUI open
        if (title.equals("SHOP - Spawner Shop") || title.equals("CONFIRM PURCHASE")) {
            e.setCancelled(true);
            if (clickedInv == null || !clickedInv.equals(topInv)) return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Spawner Shop GUI
        if (title.equals("SHOP - Spawner Shop")) {
            // Back button
            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE &&
                    ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("BACK")) {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                player.performCommand("shop");
                return;
            }

            if (clicked.getType() != Material.SPAWNER) return;
            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

            String mobType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            int price = spawnerPrices.getOrDefault(mobType, -1);
            if (price == -1) return;

            playSound(player, Sound.UI_BUTTON_CLICK);

            // Open confirm window
            Inventory confirm = Bukkit.createInventory(null, 27, "CONFIRM PURCHASE");

            ItemStack displaySpawner = new ItemStack(Material.SPAWNER);
            ItemMeta displayMeta = displaySpawner.getItemMeta();
            displayMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Spawner");
            displayMeta.setLore(List.of(ChatColor.YELLOW + mobType, ChatColor.GRAY + "Click Confirm to buy"));
            displayMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mobType);
            displaySpawner.setItemMeta(displayMeta);

            ItemStack confirmButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta confirmMeta = confirmButton.getItemMeta();
            confirmMeta.setDisplayName(ChatColor.GREEN + "CONFIRM");
            confirmMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mobType);
            confirmButton.setItemMeta(confirmMeta);

            ItemStack cancelButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta cancelMeta = cancelButton.getItemMeta();
            cancelMeta.setDisplayName(ChatColor.RED + "CANCEL");
            cancelButton.setItemMeta(cancelMeta);

            confirm.setItem(13, displaySpawner);
            confirm.setItem(23, confirmButton);
            confirm.setItem(21, cancelButton);

            player.openInventory(confirm);
        }

        // Confirm Purchase GUI
        else if (title.equals("CONFIRM PURCHASE")) {
            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE &&
                    ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("CANCEL")) {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                player.performCommand("shardshop");
                return;
            }

            if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE &&
                    ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("CONFIRM")) {
                if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

                String mobType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                int price = spawnerPrices.getOrDefault(mobType, -1);
                if (price == -1) return;

                UUID uuid = player.getUniqueId();
                MainScorePlugin pluginInstance = (MainScorePlugin) plugin;
                int playerShards = pluginInstance.getShardCount(uuid);

                if (playerShards < price) {
                    player.sendMessage(ChatColor.RED + "You don't have enough shards!");
                    playSound(player, Sound.ENTITY_VILLAGER_NO);
                    return;
                }

                pluginInstance.removeShards(uuid, price);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ss give " + player.getName() + " " + mobType + " 1");
                player.closeInventory();
                playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
            }
        }
    }
}
