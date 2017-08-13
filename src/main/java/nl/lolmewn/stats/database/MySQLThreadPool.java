package nl.lolmewn.stats.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLThreadPool {

	private static MySQLThreadPool instance;

	public static void init(FileConfiguration conf) {
		if (instance != null) {
			throw new IllegalStateException("Singleton already initialized");
		}
		instance = new MySQLThreadPool(conf);
	}

	public static void shutdown() {
		if (instance != null) {
			instance.dataSource.close();
		}
	}

	public static MySQLThreadPool getInstance() {
		return instance;
	}

	private final HikariDataSource dataSource;

	private MySQLThreadPool(FileConfiguration config) {
		HikariConfig poolConf = new HikariConfig();
		poolConf.setJdbcUrl("jdbc:mysql://"
				+ config.getString("mysql.host", "localhost")
				+ ":"
				+ config.getInt("mysql.port", 3306)
				+ "/"
				+ config.getString("mysql.database", "Stats4"));
		poolConf.setUsername(config.getString("mysql.username"));
		poolConf.setPassword(config.getString("mysql.password"));
		poolConf.addDataSourceProperty("cachePrepStmts", true);
		poolConf.addDataSourceProperty("prepStmtCacheSize", "256");
		poolConf.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		poolConf.addDataSourceProperty("useServerPrepStmts", true);
		poolConf.addDataSourceProperty("useLocalSessionState", true);
		poolConf.addDataSourceProperty("useLocalTransactionState", true);
		poolConf.addDataSourceProperty("rewriteBatchedStatements", true);
		poolConf.addDataSourceProperty("cacheResultSetMetadata", true);
		poolConf.addDataSourceProperty("cacheServerConfiguration", true);
		poolConf.addDataSourceProperty("elideSetAutoCommits", true);
		//		poolConf.addDataSourceProperty("maintainTimeStats", false);

		this.dataSource = new HikariDataSource(poolConf);
	}

	public synchronized Connection getConnection() throws SQLException {
		return this.dataSource.getConnection();
	}
}
