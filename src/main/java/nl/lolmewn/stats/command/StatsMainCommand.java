package nl.lolmewn.stats.command;

import nl.lolmewn.stats.Statistic;
import nl.lolmewn.stats.StatisticsContainer;
import nl.lolmewn.stats.StatsPlugin;
import nl.lolmewn.stats.util.AsyncTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsMainCommand extends StatsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) {
            // Send own statistics
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Cannot send stats to a non-player!");
                sender.sendMessage(ChatColor.RED + "For help, see /stats help");
                return true;
            }
            sendStatistics(sender, (Player) sender);
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Not implemented yet");
        return false;
    }

    private void sendStatistics(CommandSender sender, Player player) {
        long start = System.currentTimeMillis();
        new AsyncTask<Map<Statistic, StatisticsContainer>>(getPlugin()) {
            @Override
            public Map<Statistic, StatisticsContainer> asyncTask() {
                Map<Statistic, StatisticsContainer> map = new HashMap<>();
                for (Statistic stat : StatsPlugin.getInstance().getStatistics()) {
                    map.put(stat, stat.getContainer(player.getUniqueId(), 0));
                }
                return map;
            }

            @Override
            public void syncTask(Map<Statistic, StatisticsContainer> asyncResult) {
                asyncResult.forEach((stat, container) -> {
                    if (container.getValues().size() == 1) {
                        sender.sendMessage(ChatColor.GREEN + stat.getName() + ": " + // Key
                                ChatColor.WHITE + container.getValues().values().stream().findFirst().orElse("None")); // Value
                    } else {
                        sender.sendMessage(ChatColor.GREEN + stat.getName() + ": ");
                        container.getValues().forEach((key, value) -> sender.sendMessage(" " + ChatColor.GOLD + key + ChatColor.WHITE + ": " + value));
                    }
                });
                System.out.println("Stats command: " + (System.currentTimeMillis() - start) + "ms");
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 1) {
            return Bukkit.getServer().getOnlinePlayers().stream().map(Player::getDisplayName).filter(name -> name.startsWith(args[0])).collect(Collectors.toList());
        }
        return null;
    }
}
