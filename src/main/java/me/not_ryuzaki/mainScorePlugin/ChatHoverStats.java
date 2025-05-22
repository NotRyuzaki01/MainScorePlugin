package me.not_ryuzaki.mainScorePlugin;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        String filteredMessage = event.getMessage(); // Already filtered by ChatFilter if needed

        Bukkit.getScheduler().runTask(plugin, () -> {
            // Hover data
            double money = plugin.getEconomy().getBalance(sender);
            int kills = plugin.getKills().getOrDefault(sender.getUniqueId(), 0);
            int deaths = plugin.getDeaths().getOrDefault(sender.getUniqueId(), 0);
            int shards = plugin.getShardCount(sender.getUniqueId());
            long playtimeTicks = sender.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            long playtimeHours = playtimeTicks / (20 * 60 * 60);
            long playtimeDays = playtimeHours / 24;
            long playtimeRemainingHours = playtimeHours % 24;

            String hoverText =
                    "§a§l$ §fMoney " + ChatColor.GREEN + plugin.formatShort(money) + "\n" +
                            "§d⭐ §fShards §d" + shards + "\n" +
                            "§c\uD83D\uDDE1 §fKills §c" + kills + "\n" +
                            "§6☠ §fDeaths §6" + deaths + "\n" +
                            "§e⌚ §fPlaytime §e" + playtimeDays + "d " + playtimeRemainingHours + "h";

            TextComponent nameComponent = new TextComponent(sender.getName());
            nameComponent.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));

            TextComponent messageComponent = new TextComponent(ChatColor.GRAY + ": " + ChatColor.WHITE + filteredMessage);

            TextComponent fullMessage = new TextComponent();
            fullMessage.addExtra(nameComponent);
            fullMessage.addExtra(messageComponent);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(fullMessage);
            }
        });

        // Cancel default broadcasting (but AFTER others already handled it)
        event.setCancelled(true);
    }

}
