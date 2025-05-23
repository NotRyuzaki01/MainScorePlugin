package me.not_ryuzaki.mainScorePlugin;

import fr.mrmicky.fastboard.FastBoard;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Combat {
    private static final Map<UUID, Long> combatTimestamps = new HashMap<>();
    private static final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private static final Map<UUID, Set<UUID>> combatRelations = new HashMap<>(); // Tracks who is fighting whom
    private static final long COMBAT_DURATION = 20 * 1000; // 20 seconds in milliseconds
    static final Set<UUID> combatLoggedPlayers = new HashSet<>();
    private static final Map<UUID, UUID> lastOpponent = new HashMap<>();
    private static final Map<UUID, Runnable> teleportCancelCallbacks = new HashMap<>();

    public static void registerTeleportCancelCallback(UUID playerId, Runnable onCancel) {
        teleportCancelCallbacks.put(playerId, onCancel);
    }

    public static void unregisterTeleportCallback(UUID playerId) {
        teleportCancelCallbacks.remove(playerId);
    }

    public static void triggerCombat(Player player) {
        // Cancel teleport if active
        Runnable callback = teleportCancelCallbacks.remove(player.getUniqueId());
        if (callback != null) callback.run();
    }

    public static void startCombat(Player attacker, Player defender) {
        triggerCombat(attacker);
        triggerCombat(defender);
        enterCombat(attacker, defender);
        enterCombat(defender, attacker);
    }

    public static void enterCombat(Player player, Player opponent) {
        UUID playerId = player.getUniqueId();
        UUID opponentId = opponent.getUniqueId();

        combatTimestamps.put(playerId, System.currentTimeMillis());
        combatRelations.computeIfAbsent(playerId, k -> new HashSet<>()).add(opponentId);
        lastOpponent.put(playerId, opponentId); // Save last opponent

        // Cancel existing task if any
        if (tasks.containsKey(playerId)) {
            tasks.get(playerId).cancel();
        }

        // Start new task for action bar updates
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(MainScorePlugin.getInstance(), () -> {
            if (isInCombat(player)) {
                long remainingTime = combatTimestamps.get(playerId) + COMBAT_DURATION - System.currentTimeMillis();
                int seconds = (int) (remainingTime / 1000) + 1;

                String message = ChatColor.WHITE + "Combat: " + ChatColor.of("#0094FF") + seconds + "s";
                player.sendActionBar(message);
            }
        }, 0L, 20L); // Update every second

        tasks.put(playerId, task);
    }

    public static void endCombat(Player player) {
        UUID playerId = player.getUniqueId();

        if (tasks.containsKey(playerId)) {
            tasks.get(playerId).cancel();
            tasks.remove(playerId);
        }

        combatTimestamps.remove(playerId);

        // Remove from all combat relationships
        if (combatRelations.containsKey(playerId)) {
            // Notify opponents that this player is no longer in combat
            for (UUID opponentId : combatRelations.get(playerId)) {
                combatRelations.computeIfPresent(opponentId, (k, v) -> {
                    v.remove(playerId);
                    return v.isEmpty() ? null : v;
                });
            }
            combatRelations.remove(playerId);
        }

        player.sendActionBar(""); // Clear action bar
    }

    public static boolean isInCombat(Player player) {
        UUID playerId = player.getUniqueId();
        return combatTimestamps.containsKey(playerId) &&
                System.currentTimeMillis() < combatTimestamps.get(playerId) + COMBAT_DURATION;
    }

    public static void handlePlayerDeath(Player player) {
        UUID playerId = player.getUniqueId();

        // End combat for all opponents of the dead player
        if (combatRelations.containsKey(playerId)) {
            // Create a copy to avoid ConcurrentModificationException
            Set<UUID> opponents = new HashSet<>(combatRelations.get(playerId));
            for (UUID opponentId : opponents) {
                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null && opponent.isOnline()) {
                    endCombat(opponent);
                }
            }
        }

        endCombat(player);
    }


    public static void handlePlayerQuit(Player player) {
        if (isInCombat(player)) {
            UUID playerId = player.getUniqueId();

            // Drop all inventory items
            player.getInventory().forEach(item -> {
                if (item != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            });
            player.getInventory().clear();

            int expToDrop = (int) (Math.random() * 8); // Random between 0 and 7
            if (expToDrop > 0) {
                player.getWorld().spawn(player.getLocation(), org.bukkit.entity.ExperienceOrb.class)
                        .setExperience(expToDrop);
            }

            // Register as combat logged
            combatLoggedPlayers.add(playerId);

            // Get last opponent and credit them with the kill
            UUID opponentId = lastOpponent.get(playerId);
            if (opponentId != null) {
                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null && opponent.isOnline()) {
                    MainScorePlugin plugin = MainScorePlugin.getInstance();

                    plugin.getKills().put(opponentId, plugin.getKills().getOrDefault(opponentId, 0) + 1);
                    plugin.getDeaths().put(playerId, plugin.getDeaths().getOrDefault(playerId, 0) + 1);

                    if (plugin.canGetShardsFromPlayer(opponentId, playerId)) {
                        plugin.setShardCount(opponentId, plugin.getShardCount(opponentId) + 10);
                        opponent.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.DARK_PURPLE + "⭐ +10 Shards"));
                        plugin.recordDailyKill(opponentId, playerId);
                    }
                    FastBoard board = plugin.getBoard(opponentId);
                    if (board != null) plugin.updateBoard(board, opponent);
                }
            }

            // Kill the player
            player.setHealth(0);
        }

        endCombat(player);
    }

    public static boolean isCombatLogged(UUID playerId) {
        return combatLoggedPlayers.contains(playerId);
    }

    public static void handlePlayerJoin(Player player) {
        if (combatLoggedPlayers.remove(player.getUniqueId())) {
            // Respawn player if they combat logged
            player.spigot().respawn();

            // Send as chat message
            player.sendMessage(ChatColor.RED + "You were killed for logging out during combat!");

            // Send as title (main title and subtitle)
            player.sendTitle(
                    ChatColor.RED + "" + ChatColor.BOLD + "COMBAT LOGGED", // Main title
                    ChatColor.GRAY + "You were killed for logging out during combat", // Subtitle
                    10, // Fade in (ticks)
                    70, // Stay (ticks)
                    20  // Fade out (ticks)
            );
        }
    }
}