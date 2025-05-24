package me.not_ryuzaki.mainScorePlugin;

import fr.mrmicky.fastboard.FastBoard;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.booksaw.betterTeams.Team;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class MainScorePlugin extends JavaPlugin implements Listener {
    private static Economy econ = null;
    private static MainScorePlugin instance;
    private Map<UUID, Integer> kills = new HashMap<>();
    private Map<UUID, Integer> deaths = new HashMap<>();
    private Map<UUID, Integer> shards = new HashMap<>();
    private Map<UUID, FastBoard> boards = new HashMap<>();
    private Set<UUID> payDisabled = new HashSet<>();
    private Map<UUID, Map<UUID, Long>> dailyKills = new HashMap<>(); // Killer UUID -> (Victim UUID -> Timestamp)
    private final HashSet<UUID> hiddenPlayers = new HashSet<>();
    private Map<UUID, Boolean> scoreboardEnabled = new HashMap<>();
    private LuckPerms luckPerms;

    public void setScoreboardEnabled(UUID uuid, boolean enabled) {
        scoreboardEnabled.put(uuid, enabled);

        FastBoard board = boards.get(uuid);
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            if (enabled) {
                if (board == null) {
                    FastBoard newBoard = new FastBoard(player);
                    boards.put(uuid, newBoard);
                    updateBoard(newBoard, player);
                }
            } else {
                if (board != null) {
                    board.delete();
                    boards.remove(uuid);
                }
            }
        }
    }

    public boolean isPayEnabled(UUID uuid) {
        return !payDisabled.contains(uuid);
    }

    public void setPayEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            payDisabled.remove(uuid);
        } else {
            payDisabled.add(uuid);
        }
    }

    @Override
    public void onEnable() {

        if (!setupEconomy()) {
            getLogger().severe("Vault with an economy plugin (EssentialsX) is required!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // LuckPerms check (optional but recommended for early validation)
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().severe("LuckPerms is required for chat formatting.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        kills = new HashMap<>();
        deaths = new HashMap<>();
        shards = new HashMap<>();
        boards = new HashMap<>();
        payDisabled = new HashSet<>();
        dailyKills = new HashMap<>();
        scoreboardEnabled = new HashMap<>();

        this.luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getCommand("hide").setExecutor(new HideCommand(this, luckPerms));

        // Register events and commands
        getServer().getPluginManager().registerEvents(new ChatHoverStats(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getCommand("media").setExecutor(new MediaCommand());
        getServer().getPluginManager().registerEvents(new MediaCommand(), this);

        PayCommand payCommand = new PayCommand(econ, this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        getCommand("shardshop").setExecutor(new SpawnerShopCommand(this));
        getCommand("giveshards").setExecutor(new ShardAdminCommand(this));
        getCommand("removeshards").setExecutor(new ShardAdminCommand(this));
        getCommand("shards").setExecutor(new ShardAdminCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));
        getCommand("discord").setExecutor(new DiscordCommand());

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        SettingsCommand settingsCommand = new SettingsCommand(this);
        getCommand("settings").setExecutor(settingsCommand);
        getServer().getPluginManager().registerEvents(settingsCommand, this);

        saveDefaultConfig();
        instance = this;

        // Load disabled pay settings
        ConfigurationSection section = getConfig().getConfigurationSection("pay-enabled");
        if (section != null) {
            for (String uuidStr : section.getKeys(false)) {
                boolean enabled = section.getBoolean(uuidStr);
                if (!enabled) {
                    payDisabled.add(UUID.fromString(uuidStr));
                }
            }
        }

        loadStats();
        getServer().getPluginManager().registerEvents(this, this);

        // Scoreboard auto update task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                FastBoard board = boards.get(player.getUniqueId());
                if (board != null) {
                    updateBoard(board, player);
                }
            }
        }, 20L, 20L);

        // Shard giving task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                int current = shards.getOrDefault(uuid, 0);
                shards.put(uuid, current + 1);

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.DARK_PURPLE + "⭐ +1 Shard"));

                FastBoard board = boards.get(uuid);
                if (board != null) updateBoard(board, player);
            }
        }, 0L, 12000L);

        getLogger().info("\u2705 MainUtilPluginScore loaded successfully!");
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void loadStats() {
        FileConfiguration config = this.getConfig();

        // Create stats section if it doesn't exist
        if (!config.contains("stats")) {
            config.createSection("stats");
        }

        // Load existing stats
        for (String uuidStr : config.getConfigurationSection("stats").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                kills.put(uuid, config.getInt("stats." + uuidStr + ".kills", 0));
                deaths.put(uuid, config.getInt("stats." + uuidStr + ".deaths", 0));
                shards.put(uuid, config.getInt("stats." + uuidStr + ".shards", 0));

                // Load daily kills if exists
                if (config.contains("stats." + uuidStr + ".dailyKills")) {
                    Map<UUID, Long> victimMap = new HashMap<>();
                    ConfigurationSection dailySection = config.getConfigurationSection("stats." + uuidStr + ".dailyKills");
                    for (String victimStr : dailySection.getKeys(false)) {
                        UUID victimId = UUID.fromString(victimStr);
                        long timestamp = dailySection.getLong(victimStr);
                        victimMap.put(victimId, timestamp);
                    }
                    dailyKills.put(uuid, victimMap);
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid UUID in config: " + uuidStr);
            }
        }

        // Initialize stats for online players who might not have stats yet
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!kills.containsKey(uuid)) {
                kills.put(uuid, 0);
                deaths.put(uuid, 0);
                shards.put(uuid, 0);

                // Save to config
                config.set("stats." + uuid + ".kills", 0);
                config.set("stats." + uuid + ".deaths", 0);
                config.set("stats." + uuid + ".shards", 0);
            }
        }
        saveConfig();
    }

    public void saveStats() {
        FileConfiguration config = this.getConfig();
        for (UUID uuid : kills.keySet()) {
            config.set("stats." + uuid + ".kills", kills.get(uuid));
        }
        for (UUID uuid : deaths.keySet()) {
            config.set("stats." + uuid + ".deaths", deaths.get(uuid));
        }
        for (UUID uuid : shards.keySet()) {
            config.set("stats." + uuid + ".shards", shards.get(uuid));

            // Save daily kills if exists
            if (dailyKills.containsKey(uuid)) {
                Map<UUID, Long> victimMap = dailyKills.get(uuid);
                for (UUID victimId : victimMap.keySet()) {
                    config.set("stats." + uuid + ".dailyKills." + victimId.toString(), victimMap.get(victimId));
                }
            }
        }
        saveConfig();
    }

    public void updateBoard(FastBoard board, Player player) {
        try {
            int playerKills = kills.getOrDefault(player.getUniqueId(), 0);
            int playerDeaths = deaths.getOrDefault(player.getUniqueId(), 0);
            double balance = econ != null ? econ.getBalance(player) : 0;
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int playerShards = shards.getOrDefault(player.getUniqueId(), 0);

            long ticks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            long hours = ticks / (20 * 60 * 60);
            long days = hours / 24;
            long remainingHours = hours % 24;

            List<String> lines = new ArrayList<>();
            lines.add("");
            lines.add("§a§l$ §fMoney §a" + formatShort(balance));
            lines.add("§d⭐ §fShards §d" + playerShards);
            lines.add("§c🗡 §fKills §c" + playerKills);
            lines.add("§6☠ §fDeaths §6" + playerDeaths);
            lines.add("§b👥 §fOnline §b" + onlinePlayers);
            lines.add("§e⌚ §fPlaytime §e" + days + "d " + remainingHours + "h");

            // Add team info if player is in a team
            Team team = Team.getTeam(player);
            if (team != null) {
                lines.add("§l§9\uD83E\uDE93 §fTeam §9" + team.getName());
            }

            board.updateTitle("    §x§5§5§a§a§f§f§lRyuzakiSMP   ");
            board.updateLines(lines);
        } catch (Exception e) {
            getLogger().warning("Error updating scoreboard for " + player.getName() + ": " + e.getMessage());
        }
    }



    public String formatShort(double value) {
        // Handle zero case
        if (value == 0) {
            return "0";
        }

        // Determine the appropriate suffix and divisor
        String suffix;
        double divisor;

        if (Math.abs(value) >= 1_000_000_000) {
            suffix = "B";
            divisor = 1_000_000_000;
        } else if (Math.abs(value) >= 1_000_000) {
            suffix = "M";
            divisor = 1_000_000;
        } else if (Math.abs(value) >= 1_000) {
            suffix = "K";
            divisor = 1_000;
        } else {
            // For numbers less than 1000, just show the number with up to 2 decimal places
            return String.format(value % 1 == 0 ? "%.0f" : "%.2f", value).replace(".00", "");
        }

        // Calculate the divided value
        double divided = value / divisor;

        // Format based on whether the number is whole or has decimals
        if (divided % 1 == 0) {
            return String.format("%.0f%s", divided, suffix);
        } else {
            // Remove trailing .00 if they exist
            return String.format("%.2f%s", divided, suffix).replace(".00", "");
        }
    }

    boolean canGetShardsFromPlayer(UUID killerId, UUID victimId) {
        // If killer hasn't killed anyone today, they can get shards
        if (!dailyKills.containsKey(killerId)) {
            return true;
        }

        Map<UUID, Long> victimMap = dailyKills.get(killerId);

        // If killer hasn't killed this specific victim today, they can get shards
        if (!victimMap.containsKey(victimId)) {
            return true;
        }

        // Check if 24 hours have passed since last shard reward from this victim
        long lastKillTime = victimMap.get(victimId);
        return System.currentTimeMillis() - lastKillTime > TimeUnit.DAYS.toMillis(1);
    }

    void recordDailyKill(UUID killerId, UUID victimId) {
        dailyKills.computeIfAbsent(killerId, k -> new HashMap<>())
                .put(victimId, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        UUID deceasedId = deceased.getUniqueId();

        // Prevent duplicate counting if already counted (e.g., from combat log)
        if (Combat.isCombatLogged(deceasedId)) {
            return; // Already handled in handlePlayerQuit
        }

        deaths.put(deceasedId, deaths.getOrDefault(deceasedId, 0) + 1);

        Player killer = deceased.getKiller();
        if (killer != null) {
            UUID killerId = killer.getUniqueId();

            kills.put(killerId, kills.getOrDefault(killerId, 0) + 1);

            boolean canGetShards = canGetShardsFromPlayer(killerId, deceasedId);

            if (canGetShards) {
                shards.put(killerId, shards.getOrDefault(killerId, 0) + 10);
                killer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.DARK_PURPLE + "⭐ +10 Shards"));
                recordDailyKill(killerId, deceasedId);
            }

            FastBoard killerBoard = boards.get(killerId);
            if (killerBoard != null) updateBoard(killerBoard, killer);
        }

        saveStats();
        updateBoard(boards.get(deceasedId), deceased);
        if (killer != null) updateBoard(boards.get(killer.getUniqueId()), killer);
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        // Initialize all maps if they're null (defensive programming)
        if (kills == null) kills = new HashMap<>();
        if (deaths == null) deaths = new HashMap<>();
        if (shards == null) shards = new HashMap<>();
        if (boards == null) boards = new HashMap<>();

        // Initialize player stats if they don't exist
        kills.putIfAbsent(uuid, 0);
        deaths.putIfAbsent(uuid, 0);
        shards.putIfAbsent(uuid, 0);

        // Save to config
        getConfig().set("stats." + uuid + ".kills", kills.get(uuid));
        getConfig().set("stats." + uuid + ".deaths", deaths.get(uuid));
        getConfig().set("stats." + uuid + ".shards", shards.get(uuid));
        saveConfig();

        // Create and update scoreboard
        FastBoard board = new FastBoard(player);
        board.updateTitle("   §x§5§5§a§a§f§f§lRyuzakiSMP   ");
        boards.put(uuid, board);
        updateBoard(board, player);

        // Handle scoreboard settings
        boolean enabled = getConfig().getBoolean("scoreboard-enabled." + uuid, true);
        setScoreboardEnabled(uuid, enabled);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        FastBoard board = boards.remove(uuid);
        if (board != null) board.delete();
    }

    public int getShardCount(UUID uuid) {
        return shards.getOrDefault(uuid, 0);
    }

    public void removeShards(UUID uuid, int amount) {
        shards.put(uuid, Math.max(0, getShardCount(uuid) - amount));
    }

    public void setShardCount(UUID uuid, int amount) {
        shards.put(uuid, amount);
    }

    public FastBoard getBoard(UUID uuid) {
        return boards.get(uuid);
    }

    public Map<UUID, Integer> getKills() {
        return kills;
    }

    public Map<UUID, Integer> getDeaths() {
        return deaths;
    }

    public Economy getEconomy() {
        return econ;
    }

    public static MainScorePlugin getInstance() {
        return instance;
    }

    public HashSet<UUID> getHiddenPlayers() {
        return hiddenPlayers;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
