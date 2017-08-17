package nl.lolmewn.stats.util;

import org.bukkit.plugin.Plugin;

public abstract class AsyncTask<T> {

    public AsyncTask(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            T result = asyncTask();
            plugin.getServer().getScheduler().runTask(plugin, () -> syncTask(result));
        });
    }

    public abstract T asyncTask();

    public abstract void syncTask(T asyncResult);
}
