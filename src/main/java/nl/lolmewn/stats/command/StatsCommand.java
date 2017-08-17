package nl.lolmewn.stats.command;

import nl.lolmewn.stats.StatsPlugin;
import nl.lolmewn.stats.util.PluginAssociated;
import org.bukkit.plugin.Plugin;

public abstract class StatsCommand implements PluginAssociated {

    @Override
    public Plugin getPlugin() {
        return StatsPlugin.getInstance();
    }
}
