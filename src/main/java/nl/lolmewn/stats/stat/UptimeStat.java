package nl.lolmewn.stats.stat;

import nl.lolmewn.stats.Statistic;
import nl.lolmewn.stats.database.GenericStorage;
import org.bukkit.plugin.Plugin;

@Statistic(table = "uptime", variables = {"ms"})
public class UptimeStat implements Runnable {

    public UptimeStat(Plugin plugin){
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, 1L, 1L); // TODO maybe not run every tick
    }

    @Override
    public void run() {
        GenericStorage.save(this, 50L);
    }
}
