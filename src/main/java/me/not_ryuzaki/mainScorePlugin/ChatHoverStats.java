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

public class ChatHoverStats implements Listener {

    private final MainScorePlugin plugin;

    public ChatHoverStats(MainScorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String filteredMessage = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {
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

            LuckPerms luckPerms = plugin.getServer().getServicesManager().load(LuckPerms.class);
            User user = luckPerms.getUserManager().getUser(sender.getUniqueId());
            String group = "default";
            if (user != null) {
                CachedMetaData meta = user.getCachedData().getMetaData();
                group = user.getPrimaryGroup();
            }

            String rawPrefix = "";
            String prefixColor = "#AAAAAA";

            switch (group.toLowerCase()) {
                case "owner":
                    rawPrefix = "OWNER ";
                    prefixColor = "#0094FF";
                    break;
                case "admin":
                    rawPrefix = "ADMIN ";
                    prefixColor = ChatColor.RED.getName();
                    break;
                case "media":
                    rawPrefix = "📹";
                    prefixColor = "#FF00AA";
                    break;
                case "media_muted":
                    rawPrefix = "📹";
                    prefixColor = "#FF00AA";
                    break;
                case "plus":
                    rawPrefix = "+";
                    prefixColor = "#0094FF";
                    break;
                case "plus_muted":
                    rawPrefix = "+";
                    prefixColor = "#0094FF";
                    break;
                case "default":
                    rawPrefix = "";
                    prefixColor = "#AAAAAA";
                    break;
                case "default_muted":
                    rawPrefix = "";
                    prefixColor = "#AAAAAA";
                    break;
            }

            TextComponent nameComponent = new TextComponent();

            if (!rawPrefix.isEmpty()) {
                TextComponent prefixComponent = new TextComponent(rawPrefix);
                prefixComponent.setColor(ChatColor.of(prefixColor));
                if (group.equalsIgnoreCase("owner") || group.equalsIgnoreCase("admin")) {
                    prefixComponent.setBold(true); // ONLY bold for owner/admin
                }
                nameComponent.addExtra(prefixComponent);
            }

            TextComponent playerNameComponent = new TextComponent(sender.getName());
            if (group.equalsIgnoreCase("owner") || group.equalsIgnoreCase("admin")) {
                playerNameComponent.setColor(ChatColor.of(prefixColor));
            } else {
                playerNameComponent.setColor(ChatColor.GRAY);
            }
            nameComponent.addExtra(playerNameComponent);

            nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

            TextComponent messageComponent = new TextComponent(": " + filteredMessage);
            if (group.equalsIgnoreCase("owner") || group.equalsIgnoreCase("admin")) {
                messageComponent.setColor(ChatColor.of(prefixColor));
            } else {
                messageComponent.setColor(ChatColor.of("#BBBBBB")); // brighter gray
            }

            TextComponent fullMessage = new TextComponent();
            fullMessage.addExtra(nameComponent);
            fullMessage.addExtra(messageComponent);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(fullMessage);
            }
        });

        event.setCancelled(true);
    }
}
