package me.not_ryuzaki.mainScorePlugin;

import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    private final MainScorePlugin plugin;

    public PlayerJoinListener(MainScorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getHiddenPlayers().contains(event.getPlayer().getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Reuse the same HideCommand with LuckPerms instance from the plugin
                    HideCommand hideCommand = new HideCommand(plugin, plugin.getLuckPerms());
                    hideCommand.hidePlayer(event.getPlayer());
                }
            }.runTaskLater(plugin, 20L); // delay by 1 second (20 ticks)
        }
    }
}
