package me.not_ryuzaki.mainScorePlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class PayCommand implements CommandExecutor, TabCompleter {
    private final Economy econ;
    private final Plugin plugin;
    private static final Map<UUID, List<String>> pendingMessages = new HashMap<>();

    public PayCommand(Economy econ, Plugin plugin) {
        this.econ = econ;
        this.plugin = plugin;
    }

    public static void addPendingMessage(UUID uuid, String message) {
        pendingMessages.computeIfAbsent(uuid, k -> new ArrayList<>()).add(message);
    }

    public static List<String> getPendingMessages(UUID uuid) {
        return pendingMessages.remove(uuid); // remove after delivering
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player fromPlayer)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 2) {
            fromPlayer.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            return true;
        }

        OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(args[0]);
        if (!toPlayer.hasPlayedBefore() && !toPlayer.isOnline()) {
            fromPlayer.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        if (toPlayer.getUniqueId().equals(fromPlayer.getUniqueId())) {
            fromPlayer.sendMessage(ChatColor.RED + "You cannot pay yourself!");
            return true;
        }

        double amount = parseAmount(args[1]);
        if (amount <= 0) {
            fromPlayer.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }

        if (econ.getBalance(fromPlayer) < amount) {
            fromPlayer.sendMessage(ChatColor.RED + "You don't have enough money.");
            return true;
        }

        if (plugin instanceof MainScorePlugin mainPlugin) {
            if (!mainPlugin.isPayEnabled(toPlayer.getUniqueId())) {
                fromPlayer.sendMessage(ChatColor.RED + toPlayer.getName() + " is not accepting payments.");
                TextComponent denyMsg = new TextComponent(ChatColor.RED + toPlayer.getName() + " is not accepting payments.");
                fromPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, denyMsg);
                return true;
            }
        }

        econ.withdrawPlayer(fromPlayer, amount);
        econ.depositPlayer(toPlayer, amount);

        String formatted = formatAmount(amount);

        // Sender message
        TextComponent senderMsg = new TextComponent("You paid ");
        senderMsg.setColor(ChatColor.WHITE);

        TextComponent toName = new TextComponent(toPlayer.getName());
        toName.setColor(ChatColor.of("#0094FF"));

        TextComponent dollarAmount = new TextComponent(" $" + formatted);
        dollarAmount.setColor(ChatColor.GREEN);

        senderMsg.addExtra(toName);
        senderMsg.addExtra(dollarAmount);
        fromPlayer.spigot().sendMessage(ChatMessageType.CHAT, senderMsg);

        // Action bar for sender
        TextComponent actionBarFrom = new TextComponent("You paid ");
        actionBarFrom.setColor(ChatColor.WHITE);
        actionBarFrom.addExtra(toName.duplicate());
        actionBarFrom.addExtra(dollarAmount.duplicate());
        fromPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBarFrom);

        if (toPlayer.isOnline()) {
            Player onlineTo = (Player) toPlayer;

            // Receiver message in chat
            TextComponent receiverMsg = new TextComponent("");
            TextComponent fromName = new TextComponent(fromPlayer.getName());
            fromName.setColor(ChatColor.of("#0094FF")); // Blue username

            TextComponent paidYou = new TextComponent(" paid you ");
            paidYou.setColor(ChatColor.WHITE);

            TextComponent amountMsg = new TextComponent("$" + formatted);
            amountMsg.setColor(ChatColor.GREEN);

            receiverMsg.addExtra(fromName);
            receiverMsg.addExtra(paidYou);
            receiverMsg.addExtra(amountMsg);

            onlineTo.spigot().sendMessage(ChatMessageType.CHAT, receiverMsg);

            // Receiver message in action bar
            TextComponent actionBarTo = new TextComponent("");
            actionBarTo.addExtra(fromName.duplicate());
            actionBarTo.addExtra(paidYou.duplicate());
            actionBarTo.addExtra(amountMsg.duplicate());

            onlineTo.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBarTo);

            // ✅ Play sound for online recipient
            onlineTo.playSound(onlineTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        } else {
            // Store anonymous message for offline player
            String message = "OFFLINE_PAY $" + formatted;
            addPendingMessage(toPlayer.getUniqueId(), message);
        }

        return true;
    }

    public static String formatAmountStatic(double amount) {
        String suffix = "";
        double value = amount;

        if (amount >= 1_000_000_000) {
            suffix = "B";
            value = amount / 1_000_000_000;
        } else if (amount >= 1_000_000) {
            suffix = "M";
            value = amount / 1_000_000;
        } else if (amount >= 1_000) {
            suffix = "k";
            value = amount / 1_000;
        }

        String formatted = String.format("%.2f", value).replaceAll("\\.?0+$", "");
        return formatted + suffix;
    }

    private double parseAmount(String input) {
        input = input.toLowerCase().replace(",", "").trim();
        try {
            if (input.endsWith("k")) return Double.parseDouble(input.replace("k", "")) * 1_000;
            if (input.endsWith("m")) return Double.parseDouble(input.replace("m", "")) * 1_000_000;
            if (input.endsWith("b")) return Double.parseDouble(input.replace("b", "")) * 1_000_000_000;
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatAmount(double amount) {
        String suffix = "";
        double value = amount;

        if (amount >= 1_000_000_000) {
            suffix = "B";
            value = amount / 1_000_000_000;
        } else if (amount >= 1_000_000) {
            suffix = "M";
            value = amount / 1_000_000;
        } else if (amount >= 1_000) {
            suffix = "k";
            value = amount / 1_000;
        }

        String formatted = String.format("%.2f", value).replaceAll("\\.?0+$", "");
        return formatted + suffix;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("pay") && args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .toList();
        }
        return Collections.emptyList();
    }
}
