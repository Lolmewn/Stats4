package nl.lolmewn.stats.database;

import nl.lolmewn.stats.StatsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class DatabaseQueryWorker {

    private static DatabaseQueryWorker instance;

    public static DatabaseQueryWorker getInstance() {
        return instance;
    }

    static {
        instance = new DatabaseQueryWorker();
    }

//    private Queue<Runnable> jobQueue = new ConcurrentLinkedQueue<>();

    private DatabaseQueryWorker() {
    }

    public BukkitTask submit(Runnable runnable) {
        return Bukkit.getServer().getScheduler().runTaskAsynchronously(StatsPlugin.getInstance(), runnable);
//        this.jobQueue.add(runnable);
    }
}
