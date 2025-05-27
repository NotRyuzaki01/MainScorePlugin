package me.not_ryuzaki.mainScorePlugin;

import com.booksaw.betterTeams.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player defender = (Player) event.getEntity();

        Player attacker = null;

        // Direct attack from player
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        // Indirect attack via projectile (e.g., arrow shot by player)
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        // 🛑 Prevent self-inflicted damage from triggering combat
        if (attacker != null && attacker.equals(defender)) return;

        if (attacker != null && shouldStartCombat(attacker, defender)) {
            Combat.startCombat(attacker, defender);
        }
    }



    private boolean shouldStartCombat(Player p1, Player p2) {
        Team team1 = Team.getTeam(p1);
        Team team2 = Team.getTeam(p2);

        if (team1 == null || team2 == null) {
            // At least one player is not in a team; allow combat
            return true;
        }

        if (!team1.equals(team2)) {
            // Players are in different teams; allow combat
            return true;
        }

        // Players are in the same team; check if PvP is enabled
        return team1.isPvp();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Combat.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // If this is a combat log death, suppress death messages and screen
        if (Combat.isCombatLogged(player.getUniqueId())) {
            event.setDeathMessage(null);
            event.setKeepInventory(false); // Or true if you want to keep inventory
            event.getDrops().clear();     // Clear drops if keeping inventory
        }

        Combat.handlePlayerDeath(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Combat.handlePlayerQuit(event.getPlayer());
    }
}
