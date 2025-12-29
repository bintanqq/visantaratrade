package me.bintanq.visantaraTrade.commands;

import me.bintanq.visantaraTrade.VisantaraTrade;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final VisantaraTrade plugin;

    public PayCommand(VisantaraTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (!player.hasPermission("visantara.pay")) {
            plugin.getMessageManager().send(player, "pay.no-permission");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§cUsage: /pay <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().send(player, "trade.player-not-found", Map.of("player", args[0]));
            return true;
        }

        if (target.equals(player)) {
            plugin.getMessageManager().send(player, "pay.self-pay");
            return true;
        }

        double amount = parseMoney(args[1]);

        if (amount <= 0) {
            plugin.getMessageManager().send(player, "pay.invalid-amount");
            return true;
        }

        double senderBalance = plugin.getEconomy().getBalance(player);
        if (senderBalance < amount) {
            plugin.getMessageManager().send(player, "pay.insufficient-funds", Map.of("amount", String.format("%.2f", amount)));
            return true;
        }

        plugin.getEconomy().withdrawPlayer(player, amount);
        plugin.getEconomy().depositPlayer(target, amount);

        plugin.getDatabaseManager().logPay(player, target, amount);

        String formattedAmount = formatMoney(amount);

        plugin.getMessageManager().send(player, "pay.sent", Map.of(
                "amount", formattedAmount,
                "player", target.getName()
        ));

        plugin.getMessageManager().send(target, "pay.received", Map.of(
                "amount", formattedAmount,
                "player", player.getName()
        ));
        plugin.getMessageManager().sendSound(player, "MONEY_CHANGE");
        plugin.getMessageManager().sendSound(target, "MONEY_CHANGE");

        return true;
    }

    private double parseMoney(String input) {
        try {
            input = input.toLowerCase();
            double multiplier = 1.0;

            if (input.endsWith("k")) {
                multiplier = 1_000.0;
                input = input.replace("k", "");
            } else if (input.endsWith("m")) {
                multiplier = 1_000_000.0;
                input = input.replace("m", "");
            } else if (input.endsWith("b")) {
                multiplier = 1_000_000_000.0;
                input = input.replace("b", "");
            }

            return Double.parseDouble(input) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatMoney(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000.0).replace(".0", "");
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0).replace(".0", "");
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0).replace(".0", "");
        }
        return String.format("%.2f", amount);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            if (input.isEmpty()) {
                return List.of("100", "1k", "10k", "1m");
            }

            if (input.matches("^[0-9.]+$")) {
                suggestions.add(input + "k");
                suggestions.add(input + "m");
                suggestions.add(input + "b");
            } else {
                List.of("100", "1k", "10k", "1m", "10m").forEach(sug -> {
                    if (sug.startsWith(input)) suggestions.add(sug);
                });
            }

            return suggestions;
        }

        return new ArrayList<>();
    }
}