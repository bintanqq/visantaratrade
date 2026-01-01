package me.bintanq.visantaraTrade.managers;

import me.bintanq.visantaraTrade.VisantaraTrade;
import me.bintanq.visantaraTrade.session.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final VisantaraTrade plugin;
    private Connection connection;

    public DatabaseManager(VisantaraTrade plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/trades.db";
            connection = DriverManager.getConnection(url);

            String createTable = """
                CREATE TABLE IF NOT EXISTS trade_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player1_uuid TEXT NOT NULL,
                    player1_name TEXT NOT NULL,
                    player2_uuid TEXT NOT NULL,
                    player2_name TEXT NOT NULL,
                    player1_items BLOB,
                    player2_items BLOB,
                    player1_money REAL,
                    player2_money REAL,
                    timestamp TEXT NOT NULL
                )
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTable);
            }

            plugin.getLogger().info("Database initialized successfully!");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void logTrade(TradeSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = """
                    INSERT INTO trade_logs 
                    (player1_uuid, player1_name, player2_uuid, player2_name, 
                     player1_items, player2_items, player1_money, player2_money, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, session.getPlayer1().getUniqueId().toString());
                    stmt.setString(2, session.getPlayer1().getName());
                    stmt.setString(3, session.getPlayer2().getUniqueId().toString());
                    stmt.setString(4, session.getPlayer2().getName());

                    stmt.setBytes(5, serializeItems(new ArrayList<>(session.getPlayer1Items().values())));
                    stmt.setBytes(6, serializeItems(new ArrayList<>(session.getPlayer2Items().values())));

                    stmt.setDouble(7, session.getPlayer1Money());
                    stmt.setDouble(8, session.getPlayer2Money());
                    stmt.setString(9, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));

                    stmt.executeUpdate();
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to log trade: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void getTradeLogs(String playerName, TradeLogsCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<TradeLog> logs = new ArrayList<>();
            try {
                String sql = """
                SELECT * FROM trade_logs 
                WHERE player1_name = ? OR player2_name = ?
                ORDER BY timestamp DESC
                """;

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    stmt.setString(2, playerName);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        TradeLog log = new TradeLog(
                                rs.getInt("id"),
                                UUID.fromString(rs.getString("player1_uuid")),
                                rs.getString("player1_name"),
                                UUID.fromString(rs.getString("player2_uuid")),
                                rs.getString("player2_name"),
                                deserializeItems(rs.getBytes("player1_items")),
                                deserializeItems(rs.getBytes("player2_items")),
                                rs.getDouble("player1_money"),
                                rs.getDouble("player2_money"),
                                rs.getString("timestamp")
                        );
                        logs.add(log);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to retrieve trade logs: " + e.getMessage());
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.onLogsRetrieved(logs));
        });
    }

    public void logPay(org.bukkit.entity.Player sender, org.bukkit.entity.Player target, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = """
                INSERT INTO trade_logs 
                (player1_uuid, player1_name, player2_uuid, player2_name, 
                 player1_items, player2_items, player1_money, player2_money, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, sender.getUniqueId().toString());
                    stmt.setString(2, sender.getName());
                    stmt.setString(3, target.getUniqueId().toString());
                    stmt.setString(4, target.getName());

                    stmt.setBytes(5, serializeItems(new ArrayList<>()));
                    stmt.setBytes(6, serializeItems(new ArrayList<>()));

                    stmt.setDouble(7, amount);
                    stmt.setDouble(8, 0.0);
                    stmt.setString(9, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now()));

                    stmt.executeUpdate();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to log pay: " + e.getMessage());
            }
        });
    }

    private byte[] serializeItems(List<ItemStack> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.size());
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            return outputStream.toByteArray();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to serialize items: " + e.getMessage());
            return new byte[0];
        }
    }

    private List<ItemStack> deserializeItems(byte[] data) {
        List<ItemStack> items = new ArrayList<>();
        if (data == null || data.length == 0) {
            return items;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             org.bukkit.util.io.BukkitObjectInputStream dataInput =
                     new org.bukkit.util.io.BukkitObjectInputStream(inputStream)) {

            int size = dataInput.readInt();
            for (int i = 0; i < size; i++) {
                items.add((ItemStack) dataInput.readObject());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deserialize items: " + e.getMessage());
        }

        return items;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }

    public interface TradeLogsCallback {
        void onLogsRetrieved(List<TradeLog> logs);
    }

    public void getGlobalTradeLogsByTime(String durationStr, TradeLogsCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<TradeLog> logs = new ArrayList<>();
            String timeModifier;
            if (durationStr.endsWith("m")) timeModifier = "-" + durationStr.replace("m", "") + " minutes";
            else if (durationStr.endsWith("h")) timeModifier = "-" + durationStr.replace("h", "") + " hours";
            else if (durationStr.endsWith("d")) timeModifier = "-" + durationStr.replace("d", "") + " days";
            else timeModifier = "-1 hours";

            try {
                String sql = "SELECT * FROM trade_logs WHERE timestamp >= datetime('now', ?, 'localtime') ORDER BY timestamp DESC";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, timeModifier);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        logs.add(new TradeLog(
                                rs.getInt("id"),
                                UUID.fromString(rs.getString("player1_uuid")), rs.getString("player1_name"),
                                UUID.fromString(rs.getString("player2_uuid")), rs.getString("player2_name"),
                                deserializeItems(rs.getBytes("player1_items")), deserializeItems(rs.getBytes("player2_items")),
                                rs.getDouble("player1_money"), rs.getDouble("player2_money"), rs.getString("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to retrieve timed logs: " + e.getMessage());
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.onLogsRetrieved(logs));
        });
    }

    public static class TradeLog {
        private final int id;
        private final UUID player1Uuid;
        private final String player1Name;
        private final UUID player2Uuid;
        private final String player2Name;
        private final List<ItemStack> player1Items;
        private final List<ItemStack> player2Items;
        private final double player1Money;
        private final double player2Money;
        private final String timestamp;

        public TradeLog(int id, UUID player1Uuid, String player1Name,
                        UUID player2Uuid, String player2Name,
                        List<ItemStack> player1Items, List<ItemStack> player2Items,
                        double player1Money, double player2Money, String timestamp) {
            this.id = id;
            this.player1Uuid = player1Uuid;
            this.player1Name = player1Name;
            this.player2Uuid = player2Uuid;
            this.player2Name = player2Name;
            this.player1Items = player1Items;
            this.player2Items = player2Items;
            this.player1Money = player1Money;
            this.player2Money = player2Money;
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public UUID getPlayer1Uuid() { return player1Uuid; }
        public String getPlayer1Name() { return player1Name; }
        public UUID getPlayer2Uuid() { return player2Uuid; }
        public String getPlayer2Name() { return player2Name; }
        public List<ItemStack> getPlayer1Items() { return player1Items; }
        public List<ItemStack> getPlayer2Items() { return player2Items; }
        public double getPlayer1Money() { return player1Money; }
        public double getPlayer2Money() { return player2Money; }
        public String getTimestamp() { return timestamp; }
    }
}