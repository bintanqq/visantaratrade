package me.bintanq.visantaraTrade.commands;

import me.bintanq.visantaraTrade.VisantaraTrade;
import me.bintanq.visantaraTrade.managers.DatabaseManager;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class TradeCommand implements CommandExecutor, TabCompleter {

    private final VisantaraTrade plugin;

    public TradeCommand(VisantaraTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            if (!player.hasPermission("visantara.trade.toggle")) return noPerm(player);
            plugin.getTradeManager().toggleTrades(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("visantara.trade.admin")) return noPerm(player);
            plugin.getConfigManager().reloadConfigs();
            plugin.getMessageManager().send(player, "trade.reload-success");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("accept")) {
            Player requester = findRequester(player);
            if (requester == null) {
                plugin.getMessageManager().send(player, "trade.no-request");
                return true;
            }
            plugin.getTradeManager().acceptTradeRequest(player, requester);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("deny")) {
            Player requester = findRequester(player);
            if (requester == null) {
                plugin.getMessageManager().send(player, "trade.no-request");
                return true;
            }
            plugin.getTradeManager().denyTradeRequest(player, requester);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("logs")) {
            if (!player.hasPermission("visantara.trade.admin")) return noPerm(player);

            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().colorize("&cUsage: /trade logs <player/time> [page]"));
                return true;
            }

            String input = args[1];
            int page = (args.length == 3) ? parsePage(args[2]) : 1;

            DatabaseManager.TradeLogsCallback displayLogs = logs -> {
                if (logs.isEmpty()) {
                    player.sendMessage(plugin.getMessageManager().colorize("&cNo logs found for: " + input));
                    return;
                }

                int maxPage = (int) Math.ceil(logs.size() / 10.0);
                if (page > maxPage) {
                    player.sendMessage(plugin.getMessageManager().colorize("&cMax page is " + maxPage));
                    return;
                }

                String header = plugin.getConfigManager().getMessages().getString("trade.logs-header")
                        .replace("{player}", input)
                        .replace("{page}", String.valueOf(page))
                        .replace("{max}", String.valueOf(maxPage));
                plugin.getMessageManager().sendWithoutPrefix(player, header);

                int start = (page - 1) * 10;
                int end = Math.min(start + 10, logs.size());

                for (int i = start; i < end; i++) {
                    DatabaseManager.TradeLog log = logs.get(i);
                    boolean isPay = log.getPlayer1Items().isEmpty() && log.getPlayer2Items().isEmpty();
                    String typePrefix = plugin.getConfigManager().getMessages().getString(isPay ? "trade.logs-prefix-pay" : "trade.logs-prefix-trade");

                    String timeStr = log.getTimestamp().length() > 16 ? log.getTimestamp().substring(5, 16) : log.getTimestamp();

                    String logLine = plugin.getConfigManager().getMessages().getString("trade.logs-format")
                            .replace("{prefix}", typePrefix)
                            .replace("{time}", timeStr)
                            .replace("{p1}", log.getPlayer1Name())
                            .replace("{p2}", log.getPlayer2Name());

                    TextComponent line = new TextComponent(plugin.getMessageManager().colorize(logLine));
                    line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(buildLogHover(log))));
                    player.spigot().sendMessage(line);
                }
                plugin.getMessageManager().sendWithoutPrefix(player, plugin.getConfigManager().getMessages().getString("trade.logs-footer"));
            };

            if (input.matches("\\d+[mhd]")) {
                plugin.getDatabaseManager().getGlobalTradeLogsByTime(input, displayLogs);
            } else {
                plugin.getDatabaseManager().getTradeLogs(input, displayLogs);
            }
            return true;
        }

        if (args.length == 1) {
            if (!player.hasPermission("visantara.trade.use")) return noPerm(player);

            if (plugin.getTradeManager().getToggledOffPlayers().contains(player.getUniqueId())) {
                plugin.getMessageManager().send(player, "trade.toggle-is-off");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageManager().send(player, "trade.player-not-found", Map.of("player", args[0]));
                return true;
            }

            if (target.equals(player)) {
                plugin.getMessageManager().send(player, "trade.cannot-trade-self");
                return true;
            }

            List<String> blacklistedWorlds = plugin.getConfigManager().getConfig().getStringList("settings.blacklisted-worlds");
            if (blacklistedWorlds.contains(player.getWorld().getName())) {
                plugin.getMessageManager().send(player, "trade.world-disabled");
                return true;
            }

            double maxDist = plugin.getConfigManager().getConfig().getDouble("settings.max-distance", 100.0);
            if (maxDist >= 0) {
                if (!player.getWorld().equals(target.getWorld()) || player.getLocation().distance(target.getLocation()) > maxDist) {
                    plugin.getMessageManager().send(player, "too-far");
                    return true;
                }
            }

            if (plugin.getTradeManager().isInTrade(target)) {
                plugin.getMessageManager().send(player, "trade.target-already-trading");
                return true;
            }

            plugin.getTradeManager().sendTradeRequest(player, target);
            return true;
        }

        sendHelp(player);
        return true;
    }

    private Player findRequester(Player target) {
        for (Map.Entry<UUID, UUID> entry : plugin.getTradeManager().getPendingRequests().entrySet()) {
            if (entry.getValue().equals(target.getUniqueId())) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }

    private String buildLogHover(DatabaseManager.TradeLog log) {
        var msgCfg = plugin.getConfigManager().getMessages();
        StringBuilder sb = new StringBuilder();

        // Header Hover
        sb.append(plugin.getMessageManager().colorize(msgCfg.getString("trade.hover-header"))).append("\n");

        // Player 1
        sb.append(plugin.getMessageManager().colorize(msgCfg.getString("trade.hover-player-gave").replace("{player}", log.getPlayer1Name()))).append("\n");
        appendItemsFromConfig(sb, log.getPlayer1Items(), log.getPlayer1Money());

        sb.append("\n"); // Spasi antar player

        // Player 2
        sb.append(plugin.getMessageManager().colorize(msgCfg.getString("trade.hover-player-gave").replace("{player}", log.getPlayer2Name()))).append("\n");
        appendItemsFromConfig(sb, log.getPlayer2Items(), log.getPlayer2Money());

        return sb.toString();
    }

    private void appendItemsFromConfig(StringBuilder sb, List<ItemStack> items, double money) {
        var msgCfg = plugin.getConfigManager().getMessages();

        boolean empty = true;

        if (money > 0) {
            String moneyMsg = msgCfg.getString("trade.hover-money-format").replace("{money}", String.format("%.2f", money));
            sb.append(plugin.getMessageManager().colorize(moneyMsg)).append("\n");
            empty = false;
        }

        if (!items.isEmpty()) {
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir()) continue;

                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : "&f" + item.getType().name().toLowerCase().replace("_", " ");

                String itemMsg = msgCfg.getString("trade.hover-item-format")
                        .replace("{amount}", String.valueOf(item.getAmount()))
                        .replace("{item}", itemName);

                sb.append(plugin.getMessageManager().colorize(itemMsg)).append("\n");
                empty = false;
            }
        }

        if (empty) {
            sb.append(plugin.getMessageManager().colorize(msgCfg.getString("trade.hover-nothing"))).append("\n");
        }
    }

    private boolean noPerm(Player p) { plugin.getMessageManager().send(p, "trade.no-permission"); return true; }
    private int parsePage(String s) { try { return Math.max(1, Integer.parseInt(s)); } catch (Exception e) { return 1; } }

    private void sendHelp(Player p) {
        List<String> helpMessages = plugin.getConfigManager().getMessages().getStringList("trade.help-message");

        for (String line : helpMessages) {
            if (line.contains("/trade logs") || line.contains("/trade reload")) {
                if (p.hasPermission("visantara.trade.admin")) {
                    p.sendMessage(plugin.getMessageManager().colorize(line));
                }
                continue;
            }
            p.sendMessage(plugin.getMessageManager().colorize(line));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("toggle", "accept", "deny", "reload"));
            if (s.hasPermission("visantara.trade.admin")) list.add("logs");
            list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return list.stream().filter(i -> i.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("logs")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}