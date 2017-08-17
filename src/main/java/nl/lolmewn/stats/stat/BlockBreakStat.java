package nl.lolmewn.stats.stat;

import nl.lolmewn.stats.Statistic;
import nl.lolmewn.stats.StatsPlugin;
import nl.lolmewn.stats.Util;
import nl.lolmewn.stats.database.DatabaseQueryWorker;
import nl.lolmewn.stats.database.MySQLThreadPool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class BlockBreakStat implements Listener, Statistic {

    private boolean enabled;

    @Override
    public void enable() {
        BlockBreakDAO.init();
        Bukkit.getPluginManager().registerEvents(this, StatsPlugin.getInstance());
        enabled = true;
    }

    @Override
    public void disable() {
        BlockBreakEvent.getHandlerList().unregister(this);
        this.enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler
    private void onBlockBreak(final BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        byte data = event.getBlock().getData();
        ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
        DatabaseQueryWorker.getInstance().submit(() -> {
            try {
                BlockBreakDAO.storeRecord(
                        event.getPlayer().getUniqueId(), event.getPlayer().getType().name(),
                        material, data,
                        event.getBlock().getLocation(), mainHandItem
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private static class BlockBreakDAO {

        private static final String BIG_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS block_break_stat (" +
                "  id INT(11) NOT NULL AUTO_INCREMENT," +
                "  entity BINARY(16) NOT NULL," +
                "  entity_type TEXT NOT NULL," +
                "  block_material TEXT NOT NULL," +
                "  block_data TINYINT(1) DEFAULT NULL," +
                "  loc_world TEXT NOT NULL," +
                "  loc_x INT(11) NOT NULL," +
                "  loc_y INT(11) NOT NULL," +
                "  loc_z INT(11) NOT NULL," +
                "  holding_item BLOB NOT NULL," +
                "  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  PRIMARY KEY (id)," +
                "  UNIQUE KEY block_break_stat_id_uindex (id)," +
                "  KEY block_break_stat_ent (entity)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        private static final String SIMPLE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS block_break_stat_simple (" +
                "  player binary(16) NOT NULL," +
                "  material varchar(64) NOT NULL," +
                "  block_data tinyint(4) NOT NULL," +
                "  amount int(10) unsigned NOT NULL," +
                "  PRIMARY KEY (player,material,block_data)," +
                "  KEY block_break_stat_simple_player_idx (player)," +
                "  CONSTRAINT bbss_uuid FOREIGN KEY (player) REFERENCES player_statistics (uuid) ON DELETE CASCADE ON UPDATE CASCADE" +
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

        static void storeRecord(UUID entity, String entityType, Material material, byte blockData,
                                Location location, ItemStack holding) throws SQLException {
            try (Connection con = MySQLThreadPool.getInstance().getConnection()) {
                PreparedStatement bigSt = con.prepareStatement("INSERT INTO block_break_stat " +
                        "(entity, entity_type, block_material, block_data, loc_world, loc_x, loc_y, loc_z, holding_item) VALUE " +
                        "(UNHEX(?), ?, ?, ?, ?, ?, ?, ?, ?)");
                bigSt.setString(1, entity.toString().replace("-", ""));
                bigSt.setString(2, entityType);
                bigSt.setString(3, material.name());
                bigSt.setByte(4, blockData);
                bigSt.setString(5, location.getWorld().getName());
                bigSt.setInt(6, location.getBlockX());
                bigSt.setInt(7, location.getBlockY());
                bigSt.setInt(8, location.getBlockZ());
                bigSt.setString(9, Util.serialiseItemStack(holding));
                bigSt.execute();

                PreparedStatement smallSt = con.prepareStatement("INSERT INTO block_break_stat_simple " +
                        "(player, material, block_data, amount) VALUE (UNHEX(?), ?, ?, ?) ON DUPLICATE KEY UPDATE amount=amount+VALUES(amount)");
                smallSt.setString(1, entity.toString().replace("-", ""));
                smallSt.setString(2, material.name());
                smallSt.setByte(3, blockData);
                smallSt.setInt(4, 1);
                smallSt.execute();
            }
        }
    }

}
