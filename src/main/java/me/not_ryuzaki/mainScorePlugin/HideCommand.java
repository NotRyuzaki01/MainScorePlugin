package me.not_ryuzaki.mainScorePlugin;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public class HideCommand implements CommandExecutor {

    private final MainScorePlugin plugin;
    private final HashSet<UUID> hiddenPlayers;
    private final LuckPerms luckPerms;

    public HideCommand(MainScorePlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.hiddenPlayers = plugin.getHiddenPlayers();
        this.luckPerms = luckPerms;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        User user = luckPerms.getUserManager().getUser(playerId);
        if (user == null) {
            player.sendMessage("§cError checking permissions.");
            return true;
        }

        CachedMetaData meta = user.getCachedData().getMetaData(QueryOptions.defaultContextualOptions());

        String group = meta.getPrimaryGroup();
        if (!group.equalsIgnoreCase("owner") && !group.equalsIgnoreCase("media") && !group.equalsIgnoreCase("plus")) {
            player.sendMessage("§cPlus or Media rank required to use this command.");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cPlus or Media rank required to use this command."));
            return true;
        }

        if (hiddenPlayers.contains(playerId)) {
            showPlayer(player);
            hiddenPlayers.remove(playerId);
            player.sendMessage("§aYou are now visible to other players.");
        } else {
            hidePlayer(player);
            hiddenPlayers.add(playerId);
            player.sendMessage("§7You are now hidden from other players.");
        }

        return true;
    }

    public void hidePlayer(Player player) {
        TabAPI tabAPI = TabAPI.getInstance();
        NameTagManager nameTagManager = tabAPI.getNameTagManager();
        TabPlayer tabPlayer = tabAPI.getPlayer(player.getUniqueId());

        if (tabPlayer != null && nameTagManager != null) {
            nameTagManager.setPrefix(tabPlayer, "§k");
            nameTagManager.hideNameTag(tabPlayer);
        }
    }

    public void showPlayer(Player player) {
        TabAPI tabAPI = TabAPI.getInstance();
        NameTagManager nameTagManager = tabAPI.getNameTagManager();
        TabPlayer tabPlayer = tabAPI.getPlayer(player.getUniqueId());

        if (tabPlayer != null && nameTagManager != null) {
            // Setting prefix to null makes TAB revert to default placeholder (like %luckperms-prefix%)
            nameTagManager.setPrefix(tabPlayer, null);
            nameTagManager.showNameTag(tabPlayer);
        }
    }



}
