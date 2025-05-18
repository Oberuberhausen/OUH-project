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
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getCommand("inspect").setExecutor(new InspectCommand(this));
        connectDatabase();
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (Exception ignored) {}
    }

    private void connectDatabase() {
        try {
            // Verbindung zur Datenbank herstellen
            connection = DriverManager.getConnection(
                    "jdbc:mysql://192.168.178.63:3306/blocklogs", "monty", "!7JTQHm?i4Rr?7bq");

            // Tabelle erstellen, falls sie noch nicht existiert
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS block_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                user VARCHAR(100) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                action VARCHAR(10) NOT NULL
            );
        """);

            getLogger().info("MySQL-Verbindung erfolgreich und Tabelle geprüft.");
        } catch (Exception e) {
            getLogger().severe("Fehler bei der MySQL-Verbindung oder beim Tabellenerstellen:");
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
