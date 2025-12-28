package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final VisantaraTrade plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(VisantaraTrade plugin) {
        this.plugin = plugin;
    }

    public void setCooldown(Player player) {
        int cooldownSeconds = plugin.getConfigManager().getConfig()
                .getInt("settings.cooldown-seconds", 5);
        long cooldownTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.put(player.getUniqueId(), cooldownTime);
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("visantara.trade.admin")) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownTime = cooldowns.get(uuid);

        if (currentTime >= cooldownTime) {
            cooldowns.remove(uuid);
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownTime = cooldowns.get(uuid);

        return Math.max(0, (cooldownTime - currentTime) / 1000);
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}