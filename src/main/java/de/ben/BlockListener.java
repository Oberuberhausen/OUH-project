package de.ben;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;

public class BlockListener implements Listener {
    private final oUH plugin;

    public BlockListener(oUH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        logBlockEvent(e.getPlayer().getName(), e.getBlockPlaced(), "PLACE");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        logBlockEvent(e.getPlayer().getName(), e.getBlock(), "BREAK");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // Nur MAIN_HAND zulassen, um doppelte Events zu vermeiden
        if (e.getHand() == null || !e.getHand().name().equals("MAIN_HAND")) return;

        // Prüfen, ob der Spieler im Inspect-Modus ist
        if (!plugin.getInspectMode().contains(e.getPlayer().getUniqueId())) return;

        // Optional: Rechts- oder Linksklick auf Block erlauben
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            e.getPlayer().sendMessage("§7[Debug] Kein Klick auf Block erkannt.");
            return;
        }

        if (!e.hasBlock()) {
            e.getPlayer().sendMessage("§7[Debug] Kein Block vorhanden.");
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            e.getPlayer().sendMessage("§7[Debug] Block ist null.");
            return;
        }

        e.getPlayer().sendMessage("§7[Debug] Prüfe Logs für Block bei: " +
                block.getX() + ", " + block.getY() + ", " + block.getZ());

        try (PreparedStatement ps = plugin.getConnection().prepareStatement(
                "SELECT * FROM block_logs WHERE x=? AND y=? AND z=? ORDER BY time DESC")) {
            ps.setInt(1, block.getX());
            ps.setInt(2, block.getY());
            ps.setInt(3, block.getZ());
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                if (!found) {
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "§lHistory für Block bei §7" +
                            block.getX() + " " + block.getY() + " " + block.getZ() + ":");
                    found = true;
                }

                String user = rs.getString("user");
                String action = rs.getString("action");
                String type = rs.getString("block_type");
                String time = rs.getTimestamp("time")
                        .toLocalDateTime()
                        .plusHours(2)  // Zeitkorrektur (UTC+2)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                e.getPlayer().sendMessage("§8➤ §7" + time + " - §a" + user + " §7→ §e" + action + " §8(" + type + ")");
            }

            if (!found) {
                e.getPlayer().sendMessage("§cKeine Logs für diesen Block gefunden.");
            }
        } catch (Exception ex) {
            e.getPlayer().sendMessage("§cFehler beim Abrufen der Daten.");
            ex.printStackTrace();
        }
    }

    private void logBlockEvent(String user, Block block, String action) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement check = plugin.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM block_logs WHERE user=? AND x=? AND y=? AND z=? AND action=? AND block_type=?")) {
                check.setString(1, user);
                check.setInt(2, block.getX());
                check.setInt(3, block.getY());
                check.setInt(4, block.getZ());
                check.setString(5, action);
                check.setString(6, block.getType().name());
                ResultSet rs = check.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) return; // Duplikat? → nicht loggen
            } catch (Exception ignored) {}

            try (PreparedStatement ps = plugin.getConnection().prepareStatement(
                    "INSERT INTO block_logs(user, x, y, z, action, block_type) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, user);
                ps.setInt(2, block.getX());
                ps.setInt(3, block.getY());
                ps.setInt(4, block.getZ());
                ps.setString(5, action);
                ps.setString(6, block.getType().name());
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
