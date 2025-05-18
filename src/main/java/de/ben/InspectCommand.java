package de.ben;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InspectCommand implements CommandExecutor {
    private final oUH plugin;

    public InspectCommand(oUH plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;

        if (plugin.getInspectMode().contains(p.getUniqueId())) {
            plugin.getInspectMode().remove(p.getUniqueId());
            p.sendMessage("§cInspect-Modus deaktiviert.");
        } else {
            plugin.getInspectMode().add(p.getUniqueId());
            p.sendMessage("§aInspect-Modus aktiviert. Rechts-/Linksklick zeigt Historie.");
        }

        return true;
    }
}
