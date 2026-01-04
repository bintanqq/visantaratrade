package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import me.bintanq.visantaraTrade.session.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GUIManager {

    private final VisantaraTrade plugin;
    private ConfigurationSection guiConfig;

    private List<Integer> player1Slots, player2Slots, fillerSlots;
    private int player1ReadySlot, player2ReadySlot;
    private int player1MoneySlot, player2MoneySlot;
    private int player1AddMoneySlot;
    public int player1RemoveMoneySlot;
    private int player2AddMoneySlot;
    public int player2RemoveMoneySlot;
    private int tradeInfoSlot;
    private int guiSize;

    public GUIManager(VisantaraTrade plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        guiConfig = plugin.getConfigManager().getGUIConfig().getConfigurationSection("gui");
        guiSize = guiConfig.getInt("size", 54);

        player1Slots = guiConfig.getIntegerList("player1-slots");
        player2Slots = guiConfig.getIntegerList("player2-slots");
        fillerSlots = guiConfig.getIntegerList("filler-slots");

        player1ReadySlot = guiConfig.getInt("player1-ready-slot");
        player2ReadySlot = guiConfig.getInt("player2-ready-slot");
        player1MoneySlot = guiConfig.getInt("player1-money-slot");
        player2MoneySlot = guiConfig.getInt("player2-money-slot");
        player1AddMoneySlot = guiConfig.getInt("player1-add-money-slot");
        player1RemoveMoneySlot = guiConfig.getInt("player1-remove-money-slot");
        player2AddMoneySlot = guiConfig.getInt("player2-add-money-slot");
        player2RemoveMoneySlot = guiConfig.getInt("player2-remove-money-slot");

        tradeInfoSlot = guiConfig.getInt("trade-info-slot", 49);
    }

    public Inventory createTradeGUI(TradeSession session) {
        String title = colorize(guiConfig.getString("title", "Trade")
                .replace("{player1}", session.getPlayer1().getName())
                .replace("{player2}", session.getPlayer2().getName()));

        Inventory inv = Bukkit.createInventory(null, guiSize, title);

        ItemStack filler = createItem("filler");
        for (int slot : fillerSlots) {
            inv.setItem(slot, filler);
        }

        inv.setItem(player1ReadySlot, createItem("not-ready"));
        inv.setItem(player2ReadySlot, createItem("not-ready"));
        inv.setItem(player1MoneySlot, createMoneyItem(0.0));
        inv.setItem(player2MoneySlot, createMoneyItem(0.0));
        inv.setItem(player1AddMoneySlot, createItem("add-money"));
        inv.setItem(player1RemoveMoneySlot, createItem("remove-money"));
        inv.setItem(player2AddMoneySlot, createItem("add-money"));
        inv.setItem(player2RemoveMoneySlot, createItem("remove-money"));

        inv.setItem(tradeInfoSlot, createItem("trade-info"));

        return inv;
    }

    public void updateTradeInfo(Inventory inv, Player viewer, Player other) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(other);

            String name = guiConfig.getString("items.trade-info.name", "&6Trading with: &e{player}");
            meta.setDisplayName(colorize(name.replace("{player}", other.getName())));

            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList("items.trade-info.lore")) {
                lore.add(colorize(line));
            }
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        inv.setItem(tradeInfoSlot, head);
    }

    public void updateReadyButtons(Inventory inv, boolean p1Ready, boolean p2Ready) {
        inv.setItem(player1ReadySlot, createItem(p1Ready ? "ready" : "not-ready"));
        inv.setItem(player2ReadySlot, createItem(p2Ready ? "ready" : "not-ready"));
    }

    public void updateMoneyDisplay(Inventory inv, double p1Money, double p2Money) {
        inv.setItem(player1MoneySlot, createMoneyItem(p1Money));
        inv.setItem(player2MoneySlot, createMoneyItem(p2Money));
    }

    private ItemStack createItem(String key) {
        ConfigurationSection section = guiConfig.getConfigurationSection("items." + key);
        if (section == null) return new ItemStack(Material.AIR);

        String materialName = section.getString("material", "STONE").toUpperCase();
        Material mat;

        try {
            mat = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe(String.format("[VisantaraTrade] Config Error: Invalid Material '%s' in gui.yml at 'items.%s'. Falling back to STONE.", materialName, key));
            mat = Material.STONE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(section.getString("name", "")));
            List<String> lore = new ArrayList<>();
            section.getStringList("lore").forEach(l -> lore.add(colorize(l)));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMoneyItem(double amount) {
        ItemStack item = createItem("money-display");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String val = String.format("%.2f", amount);
            meta.setDisplayName(meta.getDisplayName().replace("{amount}", val));
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                meta.getLore().forEach(l -> lore.add(l.replace("{amount}", val)));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isPlayer1Slot(int s) { return player1Slots.contains(s); }
    public boolean isPlayer2Slot(int s) { return player2Slots.contains(s); }
    public boolean isFillerSlot(int s) { return fillerSlots.contains(s); }
    public boolean isReadySlot(int s) { return s == player1ReadySlot || s == player2ReadySlot; }
    public boolean isPlayer1ReadySlot(int s) { return s == player1ReadySlot; }
    public boolean isMoneyControlSlot(int s) {
        return s == player1AddMoneySlot || s == player1RemoveMoneySlot ||
                s == player2AddMoneySlot || s == player2RemoveMoneySlot ||
                s == player1MoneySlot || s == player2MoneySlot;
    }
    public boolean isAddMoneySlot(int s) { return s == player1AddMoneySlot || s == player2AddMoneySlot; }
    public boolean isPlayer1MoneyControl(int s) { return s == player1AddMoneySlot || s == player1RemoveMoneySlot; }
    public boolean isTradeInfoSlot(int s) { return s == tradeInfoSlot; }

    private String colorize(String t) { return t == null ? "" : t.replace("&", "§"); }
}