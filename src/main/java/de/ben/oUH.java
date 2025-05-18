package de.ben;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashSet;
import java.util.UUID;

public class oUH extends JavaPlugin {
    private Connection connection;
    private final HashSet<UUID> inspectMode = new HashSet<>();

    @Override
    public void onEnable() {
        // Event Listener registrieren
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestLogListener(this), this); // ← NEU
        getCommand("inspect").setExecutor(new InspectCommand(this));

        connectDatabase();
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception ignored) {}
    }

    private void connectDatabase() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://192.168.178.63:3306/blocklogs", "DEINUSER", "DEINPASSWORT");

            Statement stmt = connection.createStatement();

            // Tabelle für Blocklogs
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS block_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "user VARCHAR(100) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "action VARCHAR(10) NOT NULL," +
                    "block_type VARCHAR(50) NOT NULL)");

            // Tabelle für Chestlogs
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS chest_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "user VARCHAR(100) NOT NULL," +
                    "x INT NOT NULL," +
                    "y INT NOT NULL," +
                    "z INT NOT NULL," +
                    "world VARCHAR(100) NOT NULL," +
                    "action VARCHAR(10) NOT NULL," +
                    "item VARCHAR(100) NOT NULL," +
                    "amount INT NOT NULL)");

            getLogger().info("MySQL-Verbindung erfolgreich und Tabellen vorhanden.");
        } catch (Exception e) {
            getLogger().severe("Fehler bei der MySQL-Verbindung oder Tabellenerstellung:");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public HashSet<UUID> getInspectMode() {
        return inspectMode;
    }
}
