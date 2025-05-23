package me.not_ryuzaki.mainScorePlugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.geysermc.floodgate.api.FloodgateApi;

public class ChatHoverStats implements Listener {

    private final MainScorePlugin plugin;

    public ChatHoverStats(MainScorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        // Detect Bedrock player and fall back to normal chat
        if (FloodgateApi.getInstance().isFloodgatePlayer(sender.getUniqueId())) {
            return; // Let default chat handle Bedrock players
        }

        String filteredMessage = event.getMessage(); // Already filtered

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Hover stats
            double money = plugin.getEconomy().getBalance(sender);
            int kills = plugin.getKills().getOrDefault(sender.getUniqueId(), 0);
            int deaths = plugin.getDeaths().getOrDefault(sender.getUniqueId(), 0);
            int shards = plugin.getShardCount(sender.getUniqueId());
            long playtimeTicks = sender.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            long playtimeHours = playtimeTicks / (20 * 60 * 60);
            long playtimeDays = playtimeHours / 24;
            long playtimeRemainingHours = playtimeHours % 24;

            String hoverText =
                    "§7" + sender.getName() + "\n" +
                            "§7----------\n" +
                            "§a§l$ §fMoney " + ChatColor.GREEN + plugin.formatShort(money) + "\n" +
                            "§d⭐ §fShards §d" + shards + "\n" +
                            "§c🗡 §fKills §c" + kills + "\n" +
                            "§6☠ §fDeaths §6" + deaths + "\n" +
                            "§e⌚ §fPlaytime §e" + playtimeDays + "d " + playtimeRemainingHours + "h";

            // Get group
            LuckPerms luckPerms = plugin.getServer().getServicesManager().load(LuckPerms.class);
            User user = luckPerms.getUserManager().getUser(sender.getUniqueId());
            String group = "default";
            if (user != null) {
                CachedMetaData meta = user.getCachedData().getMetaData();
                group = user.getPrimaryGroup();
            }

            // Define prefix + color
            String rawPrefix = "";
            String prefixColor = "#AAAAAA"; // default gray

            switch (group.toLowerCase()) {
                case "owner":
                    rawPrefix = "OWNER ";
                    prefixColor = "#0094FF";
                    break;
                case "media":
                    rawPrefix = "📹";
                    prefixColor = "#FF00AA";
                    break;
                case "admin":
                    rawPrefix = "ADMIN ";
                    prefixColor = ChatColor.RED.getName(); // §c
                    break;
                case "default":
                    rawPrefix = "";
                    prefixColor = "#AAAAAA";
                    break;
            }

            // Build name component
            TextComponent nameComponent = new TextComponent();

            // Add prefix
            if (!rawPrefix.isEmpty()) {
                TextComponent prefixComponent = new TextComponent(rawPrefix);
                prefixComponent.setColor(ChatColor.of(prefixColor));
                if (!group.equalsIgnoreCase("media")) {
                    prefixComponent.setBold(true);
                }
                nameComponent.addExtra(prefixComponent);
            }

            // Add username
            TextComponent playerNameComponent = new TextComponent(sender.getName());
            if (group.equalsIgnoreCase("owner") || group.equalsIgnoreCase("admin")) {
                playerNameComponent.setColor(ChatColor.of(prefixColor));
            } else {
                playerNameComponent.setColor(ChatColor.GRAY);
            }
            nameComponent.addExtra(playerNameComponent);

            // Add hover
            nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

            // Add message
            TextComponent messageComponent = new TextComponent(ChatColor.GRAY + ": " + filteredMessage);

            // Final message
            TextComponent fullMessage = new TextComponent();
            fullMessage.addExtra(nameComponent);
            fullMessage.addExtra(messageComponent);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(fullMessage);
            }
        });

        // Cancel original message
        event.setCancelled(true);
    }
}
