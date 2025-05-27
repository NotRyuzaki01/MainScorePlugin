package me.not_ryuzaki.mainScorePlugin;

import net.milkbowl.vault.economy.Economy;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ShopCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private Economy economy;

    private record ShopItem(int price, int amount) {}

    private final Map<String, ShopItem> endShopItems = new LinkedHashMap<>() {{
        put("ENDER_CHEST", new ShopItem(2500, 1));
        put("ENDER_PEARL", new ShopItem(75, 1));
        put("END_STONE", new ShopItem(128, 16));
        put("DRAGON_BREATH", new ShopItem(1000, 1));
        put("END_ROD", new ShopItem(100, 1));
        put("CHORUS_FRUIT", new ShopItem(15, 1));
        put("POPPED_CHORUS_FRUIT", new ShopItem(10, 1));
        put("SHULKER_SHELL", new ShopItem(350, 1));
        put("SHULKER_BOX", new ShopItem(800, 1));
    }};

    private final Map<String, ShopItem> netherShopItems = new LinkedHashMap<>() {{
        put("BLAZE_ROD", new ShopItem(150, 1));
        put("NETHER_WART", new ShopItem(15, 1));
        put("GLOWSTONE_DUST", new ShopItem(15, 1));
        put("MAGMA_CREAM", new ShopItem(15, 1));
        put("GHAST_TEAR", new ShopItem(350, 1));
        put("QUARTZ", new ShopItem(15, 1));
        put("SOUL_SAND", new ShopItem(50, 1));
        put("MAGMA_BLOCK", new ShopItem(35, 1));
        put("CRYING_OBSIDIAN", new ShopItem(150, 1));
    }};

    private final Map<String, ShopItem> gearShopItems = new LinkedHashMap<>() {{
        put("OBSIDIAN", new ShopItem(100, 1));
        put("END_CRYSTAL", new ShopItem(350, 1));
        put("RESPAWN_ANCHOR", new ShopItem(1000, 1));
        put("GLOWSTONE", new ShopItem(100, 1));
        put("TOTEM_OF_UNDYING", new ShopItem(1250, 1));
        put("ENDER_PEARL", new ShopItem(75, 1));
        put("GOLDEN_APPLE", new ShopItem(250, 1));
        put("EXPERIENCE_BOTTLE", new ShopItem(100, 1));
        put("ARROW", new ShopItem(10, 1));
    }};

    private final Map<String, ShopItem> foodShopItems = new LinkedHashMap<>() {{
        put("POTATO", new ShopItem(75, 1));
        put("SWEET_BERRIES", new ShopItem(50, 1));
        put("MELON_SLICE", new ShopItem(10, 1));
        put("CARROT", new ShopItem(65, 1));
        put("APPLE", new ShopItem(25, 1));
        put("COOKED_CHICKEN", new ShopItem(30, 1));
        put("COOKED_BEEF", new ShopItem(35, 1));
        put("GOLDEN_CARROT", new ShopItem(50, 1));
        put("ENCHANTED_GOLDEN_APPLE", new ShopItem(250, 1));
    }};

    public ShopCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        openMainShop(player);
        return true;
    }

    public void openMainShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Shop");
        inv.setItem(11, createIcon(Material.END_STONE, ChatColor.of("#37eb9a") + "End", ChatColor.WHITE + "Click to view the end shop"));
        inv.setItem(12, createIcon(Material.NETHERRACK, ChatColor.of("#37eb9a") + "Nether", ChatColor.WHITE + "Click to view the nether shop"));
        inv.setItem(13, createIcon(Material.TOTEM_OF_UNDYING, ChatColor.of("#37eb9a") + "Gear", ChatColor.WHITE + "Click to view the gear shop"));
        inv.setItem(14, createIcon(Material.COOKED_BEEF, ChatColor.of("#37eb9a") + "Food", ChatColor.WHITE + "Click to view the food shop"));
        inv.setItem(15, createIcon(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "Shard Shop", ChatColor.WHITE + "Click to view the shard shop"));
        player.openInventory(inv);
    }

    public void openCategoryShop(Player player, String name, Map<String, ShopItem> items) {
        Inventory inv = Bukkit.createInventory(null, 27, "Shop - " + capitalize(name));
        int slot = 9;
        for (Map.Entry<String, ShopItem> entry : items.entrySet()) {
            String key = entry.getKey();
            ShopItem item = entry.getValue();
            Material mat = Material.valueOf(key);
            ItemStack stack = new ItemStack(mat, item.amount());
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + capitalize(key));
            meta.setLore(List.of(ChatColor.GRAY + "Buy price: " + ChatColor.GREEN + "$" + formatPrice(item.price())));
            stack.setItemMeta(meta);
            inv.setItem(slot++, stack);
        }
        inv.setItem(18, createIcon(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Back", ChatColor.WHITE + "Click to return"));
        player.setMetadata("shop_category", new FixedMetadataValue(plugin, name));
        player.openInventory(inv);
    }

    public void openBuyScreen(Player player, Material item, int pricePerItem) {
        int maxStack = item.getMaxStackSize();
        int amount = player.hasMetadata("shop_amount") ? player.getMetadata("shop_amount").get(0).asInt() : 1;
        amount = Math.max(1, Math.min(amount, maxStack));

        player.setMetadata("shop_item", new FixedMetadataValue(plugin, item.name()));
        player.setMetadata("shop_price", new FixedMetadataValue(plugin, pricePerItem));
        player.setMetadata("shop_amount", new FixedMetadataValue(plugin, amount));

        Inventory inv = Bukkit.createInventory(null, 27, "Buying " + capitalize(item.name()));
        ItemStack mainItem = new ItemStack(item, amount);
        ItemMeta itemMeta = mainItem.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + capitalize(item.name()));
        int total = amount * pricePerItem;
        itemMeta.setLore(List.of(ChatColor.GRAY + "Price: " + ChatColor.GREEN + "$" + formatPrice(total)));
        mainItem.setItemMeta(itemMeta);
        inv.setItem(13, mainItem);

        if (amount + 1 <= maxStack) inv.setItem(15, createButton(Material.LIME_STAINED_GLASS_PANE, "+1", 1));
        if (amount + 10 <= maxStack) inv.setItem(16, createButton(Material.LIME_STAINED_GLASS_PANE, "+10", 10));
        if (amount < maxStack) inv.setItem(17, createButton(Material.LIME_STAINED_GLASS_PANE, "+" + maxStack, maxStack));

        if (amount - 1 >= 1) inv.setItem(11, createButton(Material.RED_STAINED_GLASS_PANE, "-1", 1));
        if (amount - 10 >= 1) inv.setItem(10, createButton(Material.RED_STAINED_GLASS_PANE, "-10", 10));
        if (amount > 1) inv.setItem(9, createButton(Material.RED_STAINED_GLASS_PANE, "-" + maxStack, maxStack));

        inv.setItem(23, createButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Confirm Purchase"));
        inv.setItem(21, createButton(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Cancel"));
        player.openInventory(inv);
    }

    private ItemStack createIcon(Material mat, String name, String... loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(loreText));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material mat, String label, int amount) {
        ItemStack item = new ItemStack(mat, Math.min(amount, mat.getMaxStackSize()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(label);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material mat, String label) {
        return createButton(mat, label, 1);
    }

    private String capitalize(String s) {
        s = s.toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatPrice(int price) {
        return String.format("%,d", price);
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInv = e.getClickedInventory();
        Inventory topInv = e.getView().getTopInventory();
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();

        // Always cancel all interactions while a shop GUI is open
        if ((title.startsWith("Shop") || title.startsWith("Buying ")) && clicked != null) {
            e.setCancelled(true); // Block placing, removing, or moving items

            // Allow interaction logic ONLY for top-inventory (shop GUI)
            if (clickedInv == null || !clickedInv.equals(topInv)) return;
        }

        if (clicked == null || clicked.getType().isAir()) return;

        // Main Shop GUI
        if (title.equals("Shop")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            switch (clicked.getType()) {
                case END_STONE -> openCategoryShop(player, "End", endShopItems);
                case NETHERRACK -> openCategoryShop(player, "Nether", netherShopItems);
                case TOTEM_OF_UNDYING -> openCategoryShop(player, "Gear", gearShopItems);
                case COOKED_BEEF -> openCategoryShop(player, "Food", foodShopItems);
                case AMETHYST_SHARD -> Bukkit.dispatchCommand(player, "shardshop");
            }
            return;
        }

        // Category Shop
        if (title.startsWith("Shop - ")) {
            Map<String, ShopItem> category = switch (title.substring(7).toUpperCase()) {
                case "END" -> endShopItems;
                case "NETHER" -> netherShopItems;
                case "GEAR" -> gearShopItems;
                case "FOOD" -> foodShopItems;
                default -> null;
            };
            if (category == null) return;

            if (clicked.getType() == Material.RED_STAINED_GLASS_PANE &&
                    ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).equalsIgnoreCase("BACK")) {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                openMainShop(player);
                return;
            }

            ShopItem itemData = category.get(clicked.getType().name());
            if (itemData == null) return;

            playSound(player, Sound.UI_BUTTON_CLICK);
            int pricePerOne = itemData.price() / itemData.amount();
            player.setMetadata("shop_amount", new FixedMetadataValue(plugin, itemData.amount()));
            player.setMetadata("shop_category", new FixedMetadataValue(plugin, title.substring(7)));
            openBuyScreen(player, clicked.getType(), pricePerOne);
            return;
        }

        // Buy Screen
        if (title.startsWith("Buying ")) {
            Material item = Material.valueOf(player.getMetadata("shop_item").get(0).asString());
            int price = player.getMetadata("shop_price").get(0).asInt();
            int amount = player.getMetadata("shop_amount").get(0).asInt();
            int max = item.getMaxStackSize();
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            // Cancel
            if (name.equalsIgnoreCase("Cancel")) {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                String category = player.hasMetadata("shop_category") ? player.getMetadata("shop_category").get(0).asString() : "";
                switch (category.toUpperCase()) {
                    case "END" -> openCategoryShop(player, "End", endShopItems);
                    case "NETHER" -> openCategoryShop(player, "Nether", netherShopItems);
                    case "GEAR" -> openCategoryShop(player, "Gear", gearShopItems);
                    case "FOOD" -> openCategoryShop(player, "Food", foodShopItems);
                    default -> openMainShop(player);
                }
                return;
            }

            // Confirm
            if (name.equals("Confirm Purchase")) {
                double totalCost = price * amount;
                if (economy != null && economy.has(player, totalCost)) {
                    economy.withdrawPlayer(player, totalCost);
                    player.getInventory().addItem(new ItemStack(item, amount));
                    player.sendMessage(ChatColor.GREEN + "You bought x" + amount + " for $" + formatPrice((int) totalCost));
                    playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have enough money!");
                    playSound(player, Sound.ENTITY_VILLAGER_NO);
                }
                openBuyScreen(player, item, price);
                return;
            }

            // Adjust amount
            playSound(player, Sound.UI_BUTTON_CLICK);
            if (name.equals("+1")) amount += 1;
            else if (name.equals("+10")) amount += 10;
            else if (name.equals("+" + max)) amount = max;
            else if (name.equals("-1")) amount -= 1;
            else if (name.equals("-10")) amount -= 10;
            else if (name.equals("-" + max)) amount = 1;

            amount = Math.max(1, Math.min(amount, max));
            player.setMetadata("shop_amount", new FixedMetadataValue(plugin, amount));
            openBuyScreen(player, item, price);
        }
    }
}
