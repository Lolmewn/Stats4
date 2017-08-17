package nl.lolmewn.stats.command;

import nl.lolmewn.stats.stat.BlockBreakStat;
import nl.lolmewn.stats.util.AsyncTask;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
        new AsyncTask<Map<Material, Integer>>(getPlugin()) {
            @Override
            public Map<Material, Integer> asyncTask() {
                return BlockBreakStat.BlockBreakDAO.getSimpleStats(player.getUniqueId());
            }

            @Override
            public void syncTask(Map<Material, Integer> asyncResult) {
                sender.sendMessage(ChatColor.GREEN + "Blocks broken:");
                asyncResult.forEach((mat, amount) ->
                        sender.sendMessage(
                                ChatColor.GOLD + " " + StringUtils.capitalize(mat.name().toLowerCase()).replace("_", " ")
                                        + ChatColor.WHITE + ": " + amount)
                );
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
