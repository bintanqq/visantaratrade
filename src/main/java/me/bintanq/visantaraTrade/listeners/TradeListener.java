package me.bintanq.visantaraTrade.listeners;

import me.bintanq.visantaraTrade.VisantaraTrade;
import me.bintanq.visantaraTrade.session.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TradeListener implements Listener {

    private final VisantaraTrade plugin;

    public TradeListener(VisantaraTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session == null) return;

        Inventory tradeGUI = session.getGui();
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null) return;

        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);
            return;
        }

        if (clickedInv.equals(tradeGUI)) {
            int slot = e.getSlot();

            if (handleButtons(player, session, slot, e)) {
                e.setCancelled(true);
                return;
            }

            boolean isP1 = player.equals(session.getPlayer1());
            boolean isP2 = player.equals(session.getPlayer2());
            boolean isMySlot = (isP1 && plugin.getGuiManager().isPlayer1Slot(slot)) ||
                    (isP2 && plugin.getGuiManager().isPlayer2Slot(slot));

            if (!isMySlot || session.isLocked() || session.isCompleted()) {
                e.setCancelled(true);
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                ItemStack itemInSlot = tradeGUI.getItem(slot);
                if (itemInSlot != null && itemInSlot.getType() != Material.AIR) {
                    session.addItem(player, slot, itemInSlot);
                } else {
                    session.removeItem(player, slot);
                }
                session.resetReady();
            });
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session == null) return;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < e.getInventory().getSize()) {
                boolean isP1 = player.equals(session.getPlayer1());
                boolean isP2 = player.equals(session.getPlayer2());
                boolean isMySlot = (isP1 && plugin.getGuiManager().isPlayer1Slot(rawSlot)) ||
                        (isP2 && plugin.getGuiManager().isPlayer2Slot(rawSlot));

                if (!isMySlot) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int slot : e.getInventorySlots()) {
                ItemStack item = e.getInventory().getItem(slot);
                if (item != null) session.addItem(player, slot, item);
                else session.removeItem(player, slot);
            }
            session.resetReady();
        });
    }

    private boolean handleButtons(Player player, TradeSession session, int slot, InventoryClickEvent e) {
        if (plugin.getGuiManager().isReadySlot(slot)) {
            boolean isP1ReadySlot = plugin.getGuiManager().isPlayer1ReadySlot(slot);
            if ((isP1ReadySlot && player.equals(session.getPlayer1())) ||
                    (!isP1ReadySlot && player.equals(session.getPlayer2()))) {
                session.setReady(player, !session.isReady(player));
            }
            return true;
        }

        if (plugin.getGuiManager().isMoneyControlSlot(slot)) {
            boolean isP1Control = plugin.getGuiManager().isPlayer1MoneyControl(slot);
            if ((isP1Control && player.equals(session.getPlayer1())) ||
                    (!isP1Control && player.equals(session.getPlayer2()))) {

                double amount = plugin.getConfigManager().getConfig().getDouble("settings.money-increment", 100.0);
                if (e.isShiftClick()) amount *= 10;

                if (plugin.getGuiManager().isAddMoneySlot(slot)) session.addMoney(player, amount);
                else session.addMoney(player, -amount);
            }
            return true;
        }

        return plugin.getGuiManager().isFillerSlot(slot) || plugin.getGuiManager().isTradeInfoSlot(slot);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session != null && e.getInventory().equals(session.getGui())) {
            if (!session.isCompleted()) session.cancel();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        TradeSession session = plugin.getTradeManager().getSession(e.getPlayer());
        if (session != null) session.cancel();
    }
}