package me.not_ryuzaki.mainScorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Spawner Shop");

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

            // Store mob type for identification
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mob);

            meta.setLore(lore);
            spawner.setItemMeta(meta);
            gui.setItem(slot++, spawner);
        }

        player.openInventory(gui);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Spawner Shop")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.SPAWNER) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        String mobType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        int price = spawnerPrices.getOrDefault(mobType, -1);
        if (price == -1) return;

        UUID uuid = player.getUniqueId();
        MainScorePlugin pluginInstance = (MainScorePlugin) plugin;
        int playerShards = pluginInstance.getShardCount(uuid);

        if (playerShards < price) {
            player.sendMessage(ChatColor.RED + "You don't have enough shards!");
            return;
        }

        pluginInstance.removeShards(uuid, price);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ss give " + player.getName() + " " + mobType + " 1");
        player.closeInventory();
    }
}
