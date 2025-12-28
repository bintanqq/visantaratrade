package me.bintanq.visantaraTrade;

import me.bintanq.visantaraTrade.commands.PayCommand;
import me.bintanq.visantaraTrade.commands.TradeCommand;
import me.bintanq.visantaraTrade.listeners.TradeListener;
import me.bintanq.visantaraTrade.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VisantaraTrade extends JavaPlugin {

    private static VisantaraTrade instance;
    private Economy economy;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private GUIManager guiManager;
    private TradeManager tradeManager;
    private DatabaseManager databaseManager;
    private CooldownManager cooldownManager;
    private TradeListener tradeListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Plugin disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        guiManager = new GUIManager(this);
        databaseManager = new DatabaseManager(this);
        cooldownManager = new CooldownManager(this);
        tradeManager = new TradeManager(this);
        this.tradeListener = new TradeListener(this);

        TradeCommand tradeCommand = new TradeCommand(this);
        getCommand("visantaratrade").setExecutor(tradeCommand);
        getCommand("trade").setExecutor(tradeCommand);
        getCommand("visantaratrade").setTabCompleter(tradeCommand);
        getCommand("trade").setTabCompleter(tradeCommand);

        PayCommand payCommand = new PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        getServer().getPluginManager().registerEvents(this.tradeListener, this);

        getLogger().info("VisantaraTrade has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.cancelAllTrades();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("VisantaraTrade has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static VisantaraTrade getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public TradeListener getTradeListener() {
        return tradeListener;
    }
}