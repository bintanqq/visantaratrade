package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

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
        config = plugin.getConfig();

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        gui = YamlConfiguration.loadConfiguration(guiFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        gui = YamlConfiguration.loadConfiguration(guiFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getGUIConfig() {
        return gui;
    }
}