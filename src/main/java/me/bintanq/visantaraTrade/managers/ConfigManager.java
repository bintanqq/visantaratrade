package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {

    private final VisantaraTrade plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration gui;

    public ConfigManager(VisantaraTrade plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
        config = plugin.getConfig();

        messages = loadAndSync("messages.yml");
        gui = loadAndSync("gui.yml");
    }

    private FileConfiguration loadAndSync(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));

            currentConfig.setDefaults(defaultConfig);
            currentConfig.options().copyDefaults(true);

            try {
                currentConfig.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Gagal mengupdate file " + fileName + ": " + e.getMessage());
            }
        }
        return currentConfig;
    }

    public void reloadConfigs() {
        loadConfigs();

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().loadConfig();
        }

        if (plugin.getMessageManager() != null) {
            plugin.getMessageManager().reload();
        }
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getGUIConfig() { return gui; }
}