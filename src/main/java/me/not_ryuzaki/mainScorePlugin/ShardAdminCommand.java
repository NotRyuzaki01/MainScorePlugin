package me.not_ryuzaki.mainScorePlugin;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ShardAdminCommand implements CommandExecutor {
    private final MainScorePlugin plugin;

    public ShardAdminCommand(MainScorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = label.toLowerCase();

        if (cmd.equals("shards")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            int balance = plugin.getShardCount(player.getUniqueId());
            player.sendMessage(ChatColor.AQUA + "⛁ You have " + balance + " shards.");
            return true;
        }

        // /giveshards <player> <amount> or /removeshards <player> <amount>
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + cmd + " <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (cmd.equals("giveshards")) {
            int current = plugin.getShardCount(uuid);
            plugin.setShardCount(uuid, current + amount);
            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " shards to " + target.getName() + ".");
        } else if (cmd.equals("removeshards")) {
            int current = plugin.getShardCount(uuid);
            plugin.setShardCount(uuid, Math.max(0, current - amount));
            sender.sendMessage(ChatColor.YELLOW + "Removed " + amount + " shards from " + target.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
            return true;
        }

        FastBoard board = plugin.getBoard(uuid);
        if (board != null) {
            plugin.updateBoard(board, target);
        }

        return true;
    }
}
