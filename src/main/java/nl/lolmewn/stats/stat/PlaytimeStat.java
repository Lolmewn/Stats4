package nl.lolmewn.stats.stat;

import nl.lolmewn.stats.Statistic;
import nl.lolmewn.stats.StatsPlugin;
import nl.lolmewn.stats.database.DatabaseQueryWorker;
import nl.lolmewn.stats.database.MySQLThreadPool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class PlaytimeStat implements Statistic, Listener, Runnable {

    private static final int TICKS = 20;
    private boolean enabled;

    @Override
    public void enable() {
        Bukkit.getServer().getScheduler().runTaskTimer(StatsPlugin.getInstance(), this, TICKS, TICKS);
        DatabaseQueryWorker.getInstance().submit(PlaytimeDAO::init);
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void run() {
        if(!enabled){
            return; // lel
        }
        long timestamp = System.currentTimeMillis();
        Bukkit.getServer().getOnlinePlayers().forEach(player ->
                DatabaseQueryWorker.getInstance().submit(() -> {
                    try {
                        PlaytimeDAO.storeRecord(
                                player.getUniqueId(),
                                player.getLocation(),
                                timestamp
                        );
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }));
    }

    private static class PlaytimeDAO {

        private static final String BIG_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS playtime_stat (" +
                "  id BIGINT unsigned NOT NULL AUTO_INCREMENT," +
                "  player binary(16) NOT NULL," +
                "  loc_world text NOT NULL," +
                "  loc_x int(11) NOT NULL," +
                "  loc_y int(11) NOT NULL," +
                "  loc_z int(11) NOT NULL," +
                "  loc_yaw FLOAT NOT NULL," +
                "  loc_pitch FLOAT NOT NULL," +
                "  timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (id)," +
                "  UNIQUE KEY id_UNIQUE (id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        private static final String SIMPLE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS playtime_stat_simple ("+
                "  player BINARY(16) NOT NULL,"+
                "  seconds INT UNSIGNED NOT NULL,"+
                "  PRIMARY KEY (player),"+
                "  UNIQUE INDEX player_UNIQUE (player ASC)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        static void init() {
            // Create the tables
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                con.createStatement().execute(BIG_TABLE_QUERY);
                con.createStatement().execute(SIMPLE_TABLE_QUERY);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        static void storeRecord(UUID uniqueId, Location location, long timestamp) throws SQLException {
            try(Connection con = MySQLThreadPool.getInstance().getConnection()){
                PreparedStatement bigQuery = con.prepareStatement("INSERT INTO playtime_stat " +
                        "(player, loc_world, loc_x, loc_y, loc_z, loc_yaw, loc_pitch, timestamp) VALUE (UNHEX(?), ?, ?, ?, ?, ?, ?, ?)");
                bigQuery.setString(1, uniqueId.toString().replace("-", ""));
                bigQuery.setString(2, location.getWorld().getName());
                bigQuery.setInt(3, location.getBlockX());
                bigQuery.setInt(4, location.getBlockY());
                bigQuery.setInt(5, location.getBlockZ());
                bigQuery.setFloat(6, location.getYaw());
                bigQuery.setFloat(7, location.getPitch());
                bigQuery.setTimestamp(8, new Timestamp(timestamp));
                bigQuery.execute();

                PreparedStatement tinyQuery = con.prepareStatement("INSERT INTO playtime_stat_simple VALUE (UNHEX(?), ?) " +
                        "ON DUPLICATE KEY UPDATE seconds=seconds+VALUES(seconds)");
                tinyQuery.setString(1, uniqueId.toString().replace("-", ""));
                tinyQuery.setInt(2, TICKS/20); // 1 second every time
                tinyQuery.execute();
            }
        }
    }
}
