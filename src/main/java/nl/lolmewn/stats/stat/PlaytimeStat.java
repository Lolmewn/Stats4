package nl.lolmewn.stats.stat;

import nl.lolmewn.stats.Statistic;
import nl.lolmewn.stats.StatisticsContainer;
import nl.lolmewn.stats.StatsPlugin;
import nl.lolmewn.stats.database.DatabaseQueryWorker;
import nl.lolmewn.stats.database.MySQLThreadPool;
import nl.lolmewn.stats.util.ValuedRunnable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlaytimeStat implements Statistic, Listener, Runnable {

    private static final int TICKS = 20;
    private boolean enabled;
    private Map<UUID, Location> hasMovedMap = new HashMap<>();
    private Map<Integer, ValuedRunnable<UUID, Map<String, String>>> dataGatherers = new HashMap<>();

    @Override
    public void enable() {
        Bukkit.getServer().getScheduler().runTaskTimer(StatsPlugin.getInstance(), this, TICKS, TICKS);
        DatabaseQueryWorker.getInstance().submit(PlaytimeDAO::init);
        dataGatherers.put(0, (uuid) -> Collections.singletonMap("Total", "" + PlaytimeDAO.getTotalPlaytime(uuid)));
        dataGatherers.put(100, (uuid) -> PlaytimeDAO.getPlaytimeEntries(uuid).entrySet().stream().collect(
                Collectors.toMap(entry -> this.getPrettyTime(entry.getKey()), value -> value.getValue().generateLocationString())));
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
    public StatisticsContainer getContainer(UUID uuid, int level) {
        if (!dataGatherers.containsKey(level)) {
            // return highest available
            return new StatisticsContainer(dataGatherers.get(dataGatherers.keySet().stream().mapToInt(i -> i).max().orElse(0)).run(uuid), null);
        }
        return new StatisticsContainer(dataGatherers.get(level).run(uuid), dataGatherers.get(level + 1));
    }

    @Override
    public String getName() {
        return "Playtime";
    }

    @Override
    public void run() {
        if(!enabled){
            return; // lel
        }
        long timestamp = System.currentTimeMillis();
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            boolean hasMoved = true;
            if (this.hasMovedMap.containsKey(player.getUniqueId())
                    && this.hasMovedMap.get(player.getUniqueId()).equals(player.getLocation())) {
                hasMoved = false;
            }
            hasMovedMap.put(player.getUniqueId(), player.getLocation());
            boolean finalHasMoved = hasMoved;
            DatabaseQueryWorker.getInstance().submit(() -> {
                try {
                    PlaytimeDAO.updateSimpleRecord(player.getUniqueId());
                    if (finalHasMoved) {
                        PlaytimeDAO.storeRecord(
                                player.getUniqueId(),
                                player.getLocation(),
                                timestamp
                        );
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private String getPrettyTime(long timestamp) {
        return String.format("%1$TD %1$TT", timestamp);
    }

    private static class PlaytimeDAO {

        private static final String BIG_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS playtime_stat (" +
                "  id bigint(20) unsigned NOT NULL AUTO_INCREMENT," +
                "  player binary(16) NOT NULL," +
                "  loc_world text NOT NULL," +
                "  loc_x int(11) NOT NULL," +
                "  loc_y int(11) NOT NULL," +
                "  loc_z int(11) NOT NULL," +
                "  loc_yaw float NOT NULL," +
                "  loc_pitch float NOT NULL," +
                "  timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (id)," +
                "  UNIQUE KEY id_UNIQUE (id)," +
                "  KEY ps_uuid_idx (player)," +
                "  CONSTRAINT ps_uuid FOREIGN KEY (player) REFERENCES player_statistics (uuid) ON DELETE CASCADE ON UPDATE CASCADE" +
                ") ENGINE=InnoDB AUTO_INCREMENT=2026 DEFAULT CHARSET=utf8;";

        private static final String SIMPLE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS playtime_stat_simple (" +
                "  player BINARY(16) NOT NULL," +
                "  seconds INT(10) UNSIGNED NOT NULL," +
                "  PRIMARY KEY (player)," +
                "  UNIQUE KEY player_UNIQUE (player)," +
                "  KEY pss_uuid_idx (player)," +
                "  CONSTRAINT pss_uuid FOREIGN KEY (player) REFERENCES player_statistics (uuid) ON DELETE CASCADE ON UPDATE CASCADE" +
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
            }
        }

        static void updateSimpleRecord(UUID uniqueId) throws SQLException {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement tinyQuery = con.prepareStatement("INSERT INTO playtime_stat_simple VALUE (UNHEX(?), ?) " +
                        "ON DUPLICATE KEY UPDATE seconds=seconds+VALUES(seconds)");
                tinyQuery.setString(1, uniqueId.toString().replace("-", ""));
                tinyQuery.setInt(2, TICKS / 20); // 1 second every time
                tinyQuery.execute();
            }
        }

        public static int getTotalPlaytime(UUID uuid) {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement st = con.prepareStatement("SELECT seconds FROM playtime_stat_simple WHERE player=UNHEX(?)");
                st.setString(1, uuid.toString().replace("-", ""));
                ResultSet set = st.executeQuery();
                if (set != null && set.next()) {
                    return set.getInt("seconds");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }

        public static Map<Long, PlaytimeEntry> getPlaytimeEntries(UUID uuid) {
            Map<Long, PlaytimeEntry> entries = new HashMap<>();
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement st = con.prepareStatement("SELECT * FROM playtime_stat WHERE player=UNHEX(?)");
                st.setString(1, uuid.toString().replace("-", ""));
                ResultSet set = st.executeQuery();
                while (set != null && set.next()) {
                    entries.put(set.getTimestamp("timestamp").getTime(), new PlaytimeEntry(uuid, set));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return entries;
        }
    }

    private static class PlaytimeEntry {

        private final UUID uuid;
        private final String locWorld;
        private final int locX, locY, locZ;
        private final float locYaw, locPitch;
        private final long timestamp;

        private PlaytimeEntry(UUID uuid, ResultSet set) throws SQLException {
            this.uuid = uuid;
            this.locWorld = set.getString("loc_world");
            this.locX = set.getInt("loc_x");
            this.locY = set.getInt("loc_y");
            this.locZ = set.getInt("loc_z");
            this.locYaw = set.getFloat("loc_yaw");
            this.locPitch = set.getFloat("loc_pitch");
            this.timestamp = set.getTimestamp("timestamp").getTime();
        }

        public Location generateLocation() {
            if (Bukkit.getWorld(locWorld) == null) {
                return null; // Sorry :c
            }
            return new Location(Bukkit.getWorld(locWorld), locX, locY, locZ, locYaw, locPitch);
        }

        public UUID getUuid() {
            return uuid;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String generateLocationString() {
            return StringUtils.join(new String[]{
                    this.locWorld, Integer.toString(this.locX), Integer.toString(this.locY), Integer.toString(this.locZ),
                    Float.toString(this.locYaw), Float.toString(this.locPitch)
            }, ',');
        }
    }
}
