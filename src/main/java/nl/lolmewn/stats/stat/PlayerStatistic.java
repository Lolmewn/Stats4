package nl.lolmewn.stats.stat;

import nl.lolmewn.stats.Statistic;
import nl.lolmewn.stats.StatsPlugin;
import nl.lolmewn.stats.database.DatabaseQueryWorker;
import nl.lolmewn.stats.database.MySQLThreadPool;
import nl.lolmewn.stats.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PlayerStatistic implements Statistic, Runnable, Listener {

    @Override
    public void enable() {
        // Always enabled
        PlayerStatisticDAO.init();
        Bukkit.getServer().getPluginManager().registerEvents(this, StatsPlugin.getInstance());
        Bukkit.getServer().getScheduler().runTaskTimer(StatsPlugin.getInstance(), this, 20L, 20L);
        // TODO: Add more events to not need the scheduler
    }

    @Override
    public void disable() {
        // No.
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void run() {
        Bukkit.getServer().getOnlinePlayers().forEach(player ->
                DatabaseQueryWorker.getInstance().submit(() -> {
                    try {
                        PlayerStatisticDAO.storeRecord(
                                player
                        );
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }));
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        DatabaseQueryWorker.getInstance().submit(() -> {
            try {
                PlayerStatisticDAO.onPlayerJoin(
                        event.getPlayer()
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    private void onPlayerJoin(PlayerQuitEvent event) {
        DatabaseQueryWorker.getInstance().submit(() -> {
            try {
                PlayerStatisticDAO.onPlayerLeave(
                        event.getPlayer()
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private static class PlayerStatisticDAO {

        private static final String BIG_CREATE_QUERY = "CREATE TABLE IF NOT EXISTS player_statistics (" +
                "  uuid binary(16) NOT NULL," +
                "  player_name varchar(64) NOT NULL," +
                "  is_online tinyint(1) NOT NULL," +
                "  current_health double NOT NULL," +
                "  current_hunger INT UNSIGNED NOT NULL," +
                "  holding_item blob NOT NULL," +
                "  is_sprinting tinyint(1) NOT NULL," +
                "  is_crouching tinyint(1) NOT NULL," +
                "  is_blocking tinyint(1) NOT NULL," +
                "  is_flying tinyint(1) NOT NULL," +
                "  is_sleeping tinyint(1) NOT NULL," +
                "  first_join timestamp NOT NULL," +
                "  most_recent_join timestamp NOT NULL," +
                "  most_recent_leave timestamp NULL," +
                "  PRIMARY KEY (uuid)," +
                "  UNIQUE KEY uuid_UNIQUE (uuid)," +
                "  KEY name_index (player_name)," +
                "  KEY online_players (is_online,uuid,player_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        static void init() {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                con.createStatement().execute(BIG_CREATE_QUERY);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        static void storeRecord(Player player) throws SQLException {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement st = con.prepareStatement("UPDATE player_statistics " +
                        "SET current_hunger=?, current_health=?, holding_item=?, is_sprinting=?, is_blocking=?," +
                        "is_crouching=?, is_flying=?, is_sleeping=? WHERE uuid=UNHEX(?)");
                st.setInt(1, player.getFoodLevel());
                st.setDouble(2, player.getHealth());
                st.setString(3, Util.serialiseItemStack(player.getInventory().getItemInMainHand()));
                st.setBoolean(4, player.isSprinting());
                st.setBoolean(5, player.isBlocking());
                st.setBoolean(6, player.isSneaking());
                st.setBoolean(7, player.isFlying());
                st.setBoolean(8, player.isSleeping());
                st.setString(9, player.getUniqueId().toString().replace("-", ""));
                st.execute();
            }
        }

        static void onPlayerJoin(Player player) throws SQLException {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement st = con.prepareStatement("INSERT INTO player_statistics " +
                        "(uuid, player_name, is_online, current_health, current_hunger, holding_item, is_sprinting, " +
                        "is_crouching, is_blocking, is_flying, is_sleeping, first_join, most_recent_join) " +
                        "VALUE (UNHEX(?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE " +
                        "is_online=VALUES(is_online), current_health=VALUES(current_health)," +
                        "current_hunger=VALUES(current_hunger), holding_item=VALUES(holding_item)," +
                        "is_sprinting=VALUES(is_sprinting), is_crouching=VALUES(is_crouching)," +
                        "is_blocking=VALUES(is_blocking), is_flying=VALUES(is_flying)," +
                        "is_sleeping=VALUES(is_sleeping), most_recent_join=VALUES(most_recent_join)," +
                        "first_join=VALUES(first_join)");
                st.setString(1, player.getUniqueId().toString().replace("-", ""));
                st.setString(2, player.getName());
                st.setBoolean(3, true);
                st.setDouble(4, player.getHealth());
                st.setInt(5, player.getFoodLevel());
                st.setString(6, Util.serialiseItemStack(player.getInventory().getItemInMainHand()));
                st.setBoolean(7, player.isSprinting());
                st.setBoolean(9, player.isSneaking());
                st.setBoolean(8, player.isBlocking());
                st.setBoolean(10, player.isFlying());
                st.setBoolean(11, player.isSleeping());
                st.setTimestamp(12, new Timestamp(player.getFirstPlayed()));
                st.execute();
            }
        }

        static void onPlayerLeave(Player player) throws SQLException {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement st = con.prepareStatement("UPDATE player_statistics " +
                        "SET is_online=FALSE, most_recent_leave=CURRENT_TIMESTAMP WHERE uuid=UNHEX(?)");
                st.setString(1, player.getUniqueId().toString().replace("-", ""));
                st.execute();
            }
        }
    }
}
