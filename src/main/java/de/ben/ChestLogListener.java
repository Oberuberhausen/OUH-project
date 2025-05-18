package de.ben;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;

public class ChestLogListener implements Listener {

    private final oUH plugin;

    public ChestLogListener(oUH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (e.getView().getTopInventory().getType() != InventoryType.CHEST) return;

        InventoryView view = e.getView();
        Inventory clicked = e.getClickedInventory();
        ItemStack item = e.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Container)) return;

        String action = clicked.equals(view.getTopInventory()) ? "TAKE" : "PUT";
        logChestAction(player.getName(), targetBlock, item.getType().name(), item.getAmount(), action, player.getWorld().getName());
    }

    @EventHandler
    public void onInspectChest(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!plugin.getInspectMode().contains(e.getPlayer().getUniqueId())) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!e.hasBlock()) return;

        Block block = e.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) return;

        e.setCancelled(true); // Öffnen verhindern

        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getConnection().prepareStatement(
                    "SELECT * FROM chest_logs WHERE x=? AND y=? AND z=? AND world=? ORDER BY time DESC")) {
                ps.setInt(1, block.getX());
                ps.setInt(2, block.getY());
                ps.setInt(3, block.getZ());
                ps.setString(4, block.getWorld().getName());

                ResultSet rs = ps.executeQuery();

                player.sendMessage("§e§lChest-Historie §7@ (" +
                        block.getX() + ", " + block.getY() + ", " + block.getZ() + ")");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    String user = rs.getString("user");
                    String action = rs.getString("action");
                    String item = rs.getString("item");
                    int amount = rs.getInt("amount");
                    String time = rs.getTimestamp("time").toLocalDateTime()
                            .plusHours(2)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    player.sendMessage("§8➤ §7" + time + " - §a" + user +
                            " §7" + action + " §ex" + amount + " " + item);
                }

                if (!found) {
                    player.sendMessage("§cKeine Einträge für diese Chest gefunden.");
                }
            } catch (Exception ex) {
                player.sendMessage("§cFehler beim Laden der Chest-Historie.");
                ex.printStackTrace();
            }
        });
    }

    private void logChestAction(String user, Block block, String item, int amount, String action, String world) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = plugin.getConnection().prepareStatement(
                    "INSERT INTO chest_logs(user, x, y, z, world, action, item, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, user);
                ps.setInt(2, block.getX());
                ps.setInt(3, block.getY());
                ps.setInt(4, block.getZ());
                ps.setString(5, world);
                ps.setString(6, action);
                ps.setString(7, item);
                ps.setInt(8, amount);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
