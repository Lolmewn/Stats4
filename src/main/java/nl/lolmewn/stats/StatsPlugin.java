package nl.lolmewn.stats;

import nl.lolmewn.stats.database.MySQLThreadPool;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class StatsPlugin extends JavaPlugin {

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
		if (checkFirstRun() || !checkConfigured()) {
			getServer().getConsoleSender().sendMessage(ChatColor.RED + "Please configure Stats!");
			saveDefaultConfig();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		MySQLThreadPool.init(getConfig());
		this.enabled = true;
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
}