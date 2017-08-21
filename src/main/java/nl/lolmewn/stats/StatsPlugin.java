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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatsPlugin extends JavaPlugin {

    private final Map<String, Statistic> statistics = new LinkedHashMap<>();
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
    public void onLoad() {
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
    }

	@Override
	public void onEnable() {
		getCommand("stats").setExecutor(new StatsMainCommand());
        this.statistics.values().forEach(Statistic::enable);
        this.enabled = true;
	}

	private void loadStats() {
        addStat(new PlayerStatistic());
        addStat(new BlockBreakStat());
        addStat(new PlaytimeStat());
    }

    private void addStat(Statistic statistic) {
        this.statistics.put(statistic.getName(), statistic);
    }

    public Collection<Statistic> getStatistics() {
        return statistics.values();
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
