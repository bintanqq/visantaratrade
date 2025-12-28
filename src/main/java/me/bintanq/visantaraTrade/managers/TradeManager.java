package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import me.bintanq.visantaraTrade.session.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final VisantaraTrade plugin;
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerSessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    private final Set<UUID> toggledOff = ConcurrentHashMap.newKeySet();

    public TradeManager(VisantaraTrade plugin) {
        this.plugin = plugin;
        startExpiryCheck();
    }

    public boolean sendTradeRequest(Player sender, Player target) {
        if (plugin.getCooldownManager().isOnCooldown(sender)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(sender);
            plugin.getMessageManager().send(sender, "trade.cooldown",
                    Map.of("time", String.valueOf(remaining)));
            return false;
        }

        if (toggledOff.contains(target.getUniqueId())) {
            plugin.getMessageManager().send(sender, "trade.target-disabled");
            return false;
        }

        if (isInTrade(sender) || isInTrade(target)) {
            plugin.getMessageManager().send(sender, "trade.already-trading");
            return false;
        }

        if (hasPendingRequestSent(sender)) {
            plugin.getMessageManager().send(sender, "trade.request-already-sent");
            return false;
        }

        double maxDistance = plugin.getConfigManager().getConfig().getDouble("settings.max-distance", 100.0);
        if (maxDistance >= 0) {
            if (!sender.getWorld().equals(target.getWorld())) {
                plugin.getMessageManager().send(sender, "trade.too-far");
                return false;
            }
            if (sender.getLocation().distance(target.getLocation()) > maxDistance) {
                plugin.getMessageManager().send(sender, "trade.too-far");
                return false;
            }
        }

        List<String> blacklistedWorlds = plugin.getConfigManager().getConfig()
                .getStringList("settings.blacklisted-worlds");
        if (blacklistedWorlds.contains(sender.getWorld().getName())) {
            plugin.getMessageManager().send(sender, "trade.world-disabled");
            return false;
        }

        pendingRequests.put(sender.getUniqueId(), target.getUniqueId());
        plugin.getCooldownManager().setCooldown(sender);

        plugin.getMessageManager().send(sender, "trade.request-sent",
                Map.of("player", target.getName()));

        String acceptCommand = "/trade accept " + sender.getName();
        String denyCommand = "/trade deny " + sender.getName();

        plugin.getMessageManager().send(target, "trade.request-received",
                Map.of("player", sender.getName()));
        plugin.getMessageManager().sendSound(target, "REQUEST_RECEIVED");

        int expiryTime = plugin.getConfigManager().getConfig().getInt("settings.request-expiry-seconds", 30);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(sender.getUniqueId())) {
                pendingRequests.remove(sender.getUniqueId());
                plugin.getMessageManager().send(sender, "trade.request-expired");
                plugin.getMessageManager().send(target, "trade.request-expired-target");
            }
        }, expiryTime * 20L);

        return true;
    }

    public boolean acceptTradeRequest(Player accepter, Player requester) {
        UUID requesterId = requester.getUniqueId();
        UUID accepterId = accepter.getUniqueId();

        if (!pendingRequests.containsKey(requesterId) ||
                !pendingRequests.get(requesterId).equals(accepterId)) {
            plugin.getMessageManager().send(accepter, "trade.no-request");
            return false;
        }

        pendingRequests.remove(requesterId);

        double maxDistance = plugin.getConfigManager().getConfig().getDouble("settings.max-distance", 100.0);
        if (maxDistance >= 0) {
            if (!accepter.getWorld().equals(requester.getWorld())) {
                plugin.getMessageManager().send(accepter, "trade.too-far");
                return false;
            }

            if (accepter.getLocation().distance(requester.getLocation()) > maxDistance) {
                plugin.getMessageManager().send(accepter, "trade.too-far");
                return false;
            }
        }

        TradeSession session = new TradeSession(plugin, requester, accepter);
        activeSessions.put(session.getSessionId(), session);
        playerSessions.put(requesterId, session.getSessionId());
        playerSessions.put(accepterId, session.getSessionId());

        session.open();

        return true;
    }

    public boolean denyTradeRequest(Player denier, Player requester) {
        UUID requesterId = requester.getUniqueId();

        if (!pendingRequests.containsKey(requesterId)) {
            plugin.getMessageManager().send(denier, "trade.no-request");
            return false;
        }

        pendingRequests.remove(requesterId);
        plugin.getCooldownManager().clearCooldown(requester);
        plugin.getMessageManager().send(denier, "trade.request-denied-self");
        plugin.getMessageManager().send(requester, "trade.request-denied",
                Map.of("player", denier.getName()));

        return true;
    }

    public boolean hasPendingRequestSent(Player player) {
        return pendingRequests.containsKey(player.getUniqueId());
    }

    public void toggleTrades(Player player) {
        UUID uuid = player.getUniqueId();
        if (toggledOff.contains(uuid)) {
            toggledOff.remove(uuid);
            plugin.getMessageManager().send(player, "trade.toggle-enabled");
        } else {
            toggledOff.add(uuid);
            plugin.getMessageManager().send(player, "trade.toggle-disabled");
        }
    }

    public TradeSession getSession(Player player) {
        UUID sessionId = playerSessions.get(player.getUniqueId());
        return sessionId != null ? activeSessions.get(sessionId) : null;
    }

    public boolean isInTrade(Player player) {
        return playerSessions.containsKey(player.getUniqueId());
    }

    public void removeSession(TradeSession session) {
        activeSessions.remove(session.getSessionId());
        playerSessions.remove(session.getPlayer1().getUniqueId());
        playerSessions.remove(session.getPlayer2().getUniqueId());
    }

    public void cancelAllTrades() {
        for (TradeSession session : new ArrayList<>(activeSessions.values())) {
            session.cancel();
        }
    }

    private void startExpiryCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TradeSession session : new ArrayList<>(activeSessions.values())) {
                if (!session.getPlayer1().isOnline() || !session.getPlayer2().isOnline()) {
                    session.cancel();
                }
            }
        }, 20L, 20L);
    }



    public Map<UUID, UUID> getPendingRequests() {
        return pendingRequests;
    }

    public Set<UUID> getToggledOffPlayers() {
        return toggledOff;
    }
}