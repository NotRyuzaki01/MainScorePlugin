package me.not_ryuzaki.mainScorePlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class JoinListener implements Listener {

    private final Plugin plugin;

    public JoinListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> messages = PayCommand.getPendingMessages(player.getUniqueId());

        if (messages != null && !messages.isEmpty()) {
            double totalOfflineAmount = 0.0;
            boolean hasOfflinePayments = false;

            for (String rawMsg : messages) {
                if (rawMsg.startsWith("OFFLINE_PAY $")) {
                    String amountStr = rawMsg.substring("OFFLINE_PAY $".length());
                    try {
                        totalOfflineAmount += parseFormattedAmount(amountStr);
                        hasOfflinePayments = true;
                    } catch (NumberFormatException ignored) {}
                } else {
                    String[] parts = rawMsg.split(" paid you \\$");
                    if (parts.length == 2) {
                        String sender = parts[0];
                        String amount = parts[1];

                        TextComponent msg = new TextComponent("You were paid by ");
                        msg.setColor(ChatColor.WHITE);

                        TextComponent senderComponent = new TextComponent(sender);
                        senderComponent.setColor(ChatColor.of("#0094FF"));

                        TextComponent space = new TextComponent(" ");
                        TextComponent amountComponent = new TextComponent("$" + amount);
                        amountComponent.setColor(ChatColor.GREEN);

                        msg.addExtra(senderComponent);
                        msg.addExtra(space);
                        msg.addExtra(amountComponent);
                        player.spigot().sendMessage(ChatMessageType.CHAT, msg);

                        // Action bar
                        TextComponent actionBar = new TextComponent("You were paid by ");
                        actionBar.setColor(ChatColor.WHITE);
                        actionBar.addExtra(senderComponent.duplicate());
                        actionBar.addExtra(space.duplicate());
                        actionBar.addExtra(amountComponent.duplicate());
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBar);

                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    } else {
                        player.sendMessage("§a[Payment] §f" + rawMsg);
                    }
                }
            }

            if (hasOfflinePayments && totalOfflineAmount > 0) {
                String formattedTotal = PayCommand.formatAmountStatic(totalOfflineAmount);

                TextComponent msg = new TextComponent("You were paid ");
                msg.setColor(ChatColor.WHITE);

                TextComponent amountComponent = new TextComponent("$" + formattedTotal);
                amountComponent.setColor(ChatColor.GREEN);

                TextComponent suffix = new TextComponent(" while you were away");
                suffix.setColor(ChatColor.WHITE);

                msg.addExtra(amountComponent);
                msg.addExtra(suffix);
                player.spigot().sendMessage(ChatMessageType.CHAT, msg);

                // Action bar
                TextComponent actionBar = new TextComponent("You were paid ");
                actionBar.setColor(ChatColor.WHITE);
                actionBar.addExtra(amountComponent.duplicate());
                actionBar.addExtra(suffix.duplicate());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBar);

                // ✅ Delayed sound to ensure it plays after login
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f),
                        20L
                );
            }
        }
    }

    private double parseFormattedAmount(String formatted) throws NumberFormatException {
        formatted = formatted.toLowerCase().replace(",", "").trim();
        if (formatted.endsWith("k")) return Double.parseDouble(formatted.replace("k", "")) * 1_000;
        if (formatted.endsWith("m")) return Double.parseDouble(formatted.replace("m", "")) * 1_000_000;
        if (formatted.endsWith("b")) return Double.parseDouble(formatted.replace("b", "")) * 1_000_000_000;
        return Double.parseDouble(formatted);
    }
}
