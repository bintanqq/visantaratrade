package me.bintanq.visantaraTrade.session;

import me.bintanq.visantaraTrade.VisantaraTrade;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TradeSession {

    private final VisantaraTrade plugin;
    private final UUID sessionId;
    private final Player player1, player2;
    private final Inventory gui;
    private final Map<Integer, ItemStack> player1Items = new HashMap<>();
    private final Map<Integer, ItemStack> player2Items = new HashMap<>();
    private double player1Money = 0.0, player2Money = 0.0;
    private boolean player1Ready = false, player2Ready = false;
    private boolean locked = false, completed = false, isEnding = false;
    private BukkitTask countdownTask;

    public TradeSession(VisantaraTrade plugin, Player player1, Player player2) {
        this.plugin = plugin;
        this.sessionId = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.gui = plugin.getGuiManager().createTradeGUI(this);
    }

    public void open() {
        player1.openInventory(gui);
        player2.openInventory(gui);
        plugin.getGuiManager().updateTradeInfo(gui, player1, player2);
        plugin.getGuiManager().updateTradeInfo(gui, player2, player1);
        plugin.getMessageManager().sendSound(player1, "TRADE_OPEN");
        plugin.getMessageManager().sendSound(player2, "TRADE_OPEN");
    }

    public void addItem(Player player, int slot, ItemStack item) {
        if (locked || completed) return;
        if (player.equals(player1)) player1Items.put(slot, item.clone());
        else if (player.equals(player2)) player2Items.put(slot, item.clone());
    }

    public void removeItem(Player player, int slot) {
        if (locked || completed) return;
        if (player.equals(player1)) player1Items.remove(slot);
        else if (player.equals(player2)) player2Items.remove(slot);
    }

    public void resetReady() {
        if (locked || completed) return;
        player1Ready = false;
        player2Ready = false;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            player1.sendMessage("§cTrade countdown cancelled.");
            player2.sendMessage("§cTrade countdown cancelled.");
        }

        updateReadyButtons();
    }

    public void addMoney(Player player, double amount) {
        if (locked || completed) return;
        double currentBal = plugin.getEconomy().getBalance(player);
        if (player.equals(player1)) player1Money = Math.max(0, Math.min(player1Money + amount, currentBal));
        else player2Money = Math.max(0, Math.min(player2Money + amount, currentBal));
        resetReady();
        updateMoneyDisplay();
        plugin.getMessageManager().sendSound(player, "MONEY_CHANGE");
    }

    public void setReady(Player player, boolean ready) {
        if (locked || completed) return;
        if (player.equals(player1)) player1Ready = ready;
        else player2Ready = ready;

        updateReadyButtons();

        if (ready) {
            plugin.getMessageManager().sendSound(player, "READY");
        } else {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
                player1.sendMessage("§cTrade countdown cancelled.");
                player2.sendMessage("§cTrade countdown cancelled.");
            }
        }

        if (player1Ready && player2Ready) {
            startCompletionCountdown();
        }
    }

    public void startCompletionCountdown() {
        int seconds = plugin.getConfigManager().getConfig().getInt("settings.completion-countdown", 3);
        if (seconds <= 0) {
            completeTrade();
            return;
        }

        locked = true;
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    completeTrade();
                    countdownTask.cancel();
                    return;
                }

                plugin.getMessageManager().sendSound(player1, "READY");
                plugin.getMessageManager().sendSound(player2, "READY");

                String msgKey = "trade.countdown";
                Map<String, String> placeholders = Map.of("time", String.valueOf(timeLeft));

                plugin.getMessageManager().send(player1, msgKey, placeholders);
                plugin.getMessageManager().send(player2, msgKey, placeholders);

                timeLeft--;
            }
        }, 0L, 20L);
    }

    private void completeTrade() {
        if (completed || isEnding) return;
        if (!player1.isOnline() || !player2.isOnline()) { cancel(); return; }

        boolean dropEnabled = plugin.getConfigManager().getConfig().getBoolean("settings.drop-items-on-full-inventory", false);

        if (!dropEnabled) {
            if (!hasEnoughSpace(player1, player2Items.values()) || !hasEnoughSpace(player2, player1Items.values())) {
                locked = false;
                resetReady();
                plugin.getMessageManager().send(player1, "trade.inventory-full");
                plugin.getMessageManager().send(player2, "trade.inventory-full");
                return;
            }
        }

        completed = true;
        isEnding = true;

        returnCursorItem(player1);
        returnCursorItem(player2);

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getDatabaseManager().logTrade(this);

            if (player1Money > 0 && !plugin.getEconomy().has(player1, player1Money)) {
                cancel();
                return;
            }
            if (player2Money > 0 && !plugin.getEconomy().has(player2, player2Money)) {
                cancel();
                return;
            }

            if (player1Money > 0) { plugin.getEconomy().withdrawPlayer(player1, player1Money); plugin.getEconomy().depositPlayer(player2, player1Money); }
            if (player2Money > 0) { plugin.getEconomy().withdrawPlayer(player2, player2Money); plugin.getEconomy().depositPlayer(player1, player2Money); }

            transferItems(player1, player2Items.values(), dropEnabled);
            transferItems(player2, player1Items.values(), dropEnabled);

            sendSummary(player1, player1Items, player1Money, player2Items, player2Money);
            sendSummary(player2, player2Items, player2Money, player1Items, player1Money);

            player1.closeInventory();
            player2.closeInventory();
            plugin.getMessageManager().sendSound(player1, "TRADE_SUCCESS");
            plugin.getMessageManager().sendSound(player2, "TRADE_SUCCESS");
            plugin.getTradeManager().removeSession(this);
        });
    }

    private void returnCursorItem(Player player) {
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(cursorItem);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
            player.setItemOnCursor(null);
        }
    }

    public void cancel() {
        if (completed || isEnding) return;
        isEnding = true;
        locked = true;

        if (countdownTask != null) countdownTask.cancel();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player1.isOnline()) {
                transferItems(player1, player1Items.values(), true);
                plugin.getMessageManager().send(player1, "trade.cancelled");
                plugin.getMessageManager().sendSound(player1, "TRADE_CANCEL");
                player1.closeInventory();
            }

            if (player2.isOnline()) {
                transferItems(player2, player2Items.values(), true);
                plugin.getMessageManager().send(player2, "trade.cancelled");
                plugin.getMessageManager().sendSound(player2, "TRADE_CANCEL");
                player2.closeInventory();
            }

            plugin.getTradeManager().removeSession(this);
        });
    }


    private void sendSummary(Player player, Map<Integer, ItemStack> gaveItems, double gaveMoney, Map<Integer, ItemStack> gotItems, double gotMoney) {
        var msgCfg = plugin.getConfigManager().getMessages();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        plugin.getMessageManager().sendWithoutPrefix(player, msgCfg.getString("trade.completed-details-header"));
        plugin.getMessageManager().sendWithoutPrefix(player, "&7Time: &e" + time);

        String gaveText = msgCfg.getString("trade.completed-details-you-gave").replace("{count}", String.valueOf(gaveItems.size())).replace("{money}", String.format("%.2f", gaveMoney));
        TextComponent gaveComp = new TextComponent(plugin.getMessageManager().colorize(gaveText));
        gaveComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(formatHover(gaveItems, gaveMoney))));
        player.spigot().sendMessage(gaveComp);

        String gotText = msgCfg.getString("trade.completed-details-you-received").replace("{count}", String.valueOf(gotItems.size())).replace("{money}", String.format("%.2f", gotMoney));
        TextComponent gotComp = new TextComponent(plugin.getMessageManager().colorize(gotText));
        gotComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(formatHover(gotItems, gotMoney))));
        player.spigot().sendMessage(gotComp);

        plugin.getMessageManager().sendWithoutPrefix(player, msgCfg.getString("trade.completed-details-footer"));
    }

    private String formatHover(Map<Integer, ItemStack> items, double money) {
        StringBuilder sb = new StringBuilder("§6§lContent List:\n");
        if (money > 0) sb.append("§8• §e$").append(String.format("%.2f", money)).append("\n");
        if (items.isEmpty() && money <= 0) sb.append("§cNothing");
        else {
            items.values().forEach(i -> {
                String name = i.hasItemMeta() && i.getItemMeta().hasDisplayName() ? i.getItemMeta().getDisplayName() : "§f" + i.getType().name().toLowerCase().replace("_", " ");
                sb.append("§8• §7").append(i.getAmount()).append("x ").append(name).append("\n");
            });
        }
        return sb.toString();
    }

    private boolean hasEnoughSpace(Player player, Collection<ItemStack> items) {
        int emptySlots = 0;
        for (ItemStack is : player.getInventory().getStorageContents()) {
            if (is == null || is.getType() == Material.AIR) emptySlots++;
        }
        return emptySlots >= items.size();
    }

    private void transferItems(Player player, Collection<ItemStack> items, boolean dropIfFull) {
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty() && dropIfFull) {
                leftover.values().forEach(remain -> player.getWorld().dropItemNaturally(player.getLocation(), remain));
            }
        }
    }

    public UUID getSessionId() { return sessionId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Inventory getGui() { return gui; }
    public double getPlayer1Money() { return player1Money; }
    public double getPlayer2Money() { return player2Money; }
    public Map<Integer, ItemStack> getPlayer1Items() { return player1Items; }
    public Map<Integer, ItemStack> getPlayer2Items() { return player2Items; }
    public boolean isLocked() { return locked; }
    public boolean isCompleted() { return completed; }
    public boolean isReady(Player p) { return p.equals(player1) ? player1Ready : player2Ready; }
    private void updateReadyButtons() { plugin.getGuiManager().updateReadyButtons(gui, player1Ready, player2Ready); }
    private void updateMoneyDisplay() { plugin.getGuiManager().updateMoneyDisplay(gui, player1Money, player2Money); }
}