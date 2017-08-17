package nl.lolmewn.stats;

import nl.lolmewn.stats.command.StatsMainCommand;
import nl.lolmewn.stats.database.MySQLThreadPool;
import nl.lolmewn.stats.stat.BlockBreakStat;
import nl.lolmewn.stats.stat.PlayerStatistic;
import nl.lolmewn.stats.stat.PlaytimeStat;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public class StatsPlugin extends JavaPlugin {

	private static StatsPlugin instance;
	private boolean enabled = false;

	@Override
	public void onDisable() {
		if (!enabled) {
			return;
		}
		MySQLThreadPool.shutdown();
	}

	@Override
	public void onEnable() {
		instance = this;
		if (checkFirstRun() || !checkConfigured()) {
			getServer().getConsoleSender().sendMessage(ChatColor.RED + "Please configure Stats!");
			saveDefaultConfig();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		MySQLThreadPool.init(getConfig());
		try {
			MySQLThreadPool.getInstance().getConnection().close(); // Grab a connection and return it to the pool, see if it throws an exception
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Could not start Stats, a database error occurred!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		loadStats();
		getCommand("stats").setExecutor(new StatsMainCommand());
		this.enabled = true;
	}

	private void loadStats() {
		new PlayerStatistic().enable();
		new BlockBreakStat().enable();
		new PlaytimeStat().enable();
	}

	private boolean checkConfigured() {
		return !getConfig().getString("mysql.username", "CONFIGURE_ME").equals("CONFIGURE_ME");
	}

	private boolean checkFirstRun() {
		File file = getConfigFile();
		return !file.exists();
	}

	private File getConfigFile() {
		return new File(getDataFolder(), "config.yml");
	}

	public static StatsPlugin getInstance() {
		return instance;
	}
}
