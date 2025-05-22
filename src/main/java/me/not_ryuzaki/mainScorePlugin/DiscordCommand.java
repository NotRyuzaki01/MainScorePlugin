package me.not_ryuzaki.mainScorePlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Only players can use this command.");
            return true;
        }

        String discordLink = "https://discord.gg/maPNXER9Xd";

        // First line with hex color
        player.sendMessage("§x§0§0§9§4§F§FJoin our Discord!");

        // Hex blue color
        ChatColor hexBlue = ChatColor.of("#0094FF");

        // Create blue dot
        TextComponent dot = new TextComponent("• ");
        dot.setColor(hexBlue);

        // Create white underlined clickable link
        TextComponent link = new TextComponent(discordLink);
        link.setColor(ChatColor.WHITE);
        link.setUnderlined(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordLink));

        // Combine and send
        dot.addExtra(link);
        player.spigot().sendMessage(dot);

        return true;
    }
}
