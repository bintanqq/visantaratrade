package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageManager {

    private final VisantaraTrade plugin;
    private String prefix;

    public MessageManager(VisantaraTrade plugin) {
        this.plugin = plugin;
        loadPrefix();
    }

    private void loadPrefix() {
        FileConfiguration messages = plugin.getConfigManager().getMessages();
        prefix = colorize(messages.getString("prefix", "&8[&6VT&8] &r"));
    }

    public void send(Player player, String path) {
        send(player, path, Map.of());
    }

    public void send(Player player, String path, Map<String, String> placeholders) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();

        if (!messages.contains(path)) {
            player.sendMessage(prefix + "§cMessage not found: " + path);
            return;
        }

        String message = messages.getString(path, "");

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        message = colorize(message);

        if (!message.isEmpty()) {
            player.sendMessage(prefix + message);
        }

        String titlePath = path + "-title";
        if (messages.contains(titlePath)) {
            String title = colorize(messages.getString(titlePath, ""));
            String subtitle = messages.contains(titlePath + "-subtitle") ?
                    colorize(messages.getString(titlePath + "-subtitle", "")) : "";

            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                title = title.replace("{" + entry.getKey() + "}", entry.getValue());
                subtitle = subtitle.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            player.sendTitle(title, subtitle, 10, 70, 20);
        }
    }

    public void sendWithoutPrefix(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public void sendSound(Player player, String soundKey) {
        ConfigurationSection section = plugin.getConfigManager().getConfig().getConfigurationSection("sounds." + soundKey);

        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }

        try {
            String soundName = section.getString("sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            float volume = (float) section.getDouble("volume", 1.0);
            float pitch = (float) section.getDouble("pitch", 1.0);

            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Sound Error] Invalid sound name for key: " + soundKey);
        }
    }

    public String colorize(String text) {
        return text.replace("&", "§");
    }

    public String getPrefix() {
        return prefix;
    }
}