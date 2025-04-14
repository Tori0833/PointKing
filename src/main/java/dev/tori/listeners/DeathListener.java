package dev.tori.listeners;

import dev.tori.managers.*;
import dev.tori.utils.AuditLogger;
import dev.tori.utils.CooldownManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class DeathListener implements Listener {
    private final PointsManager points;
    private final CooldownManager cooldown;
    private final AuditLogger logger;

    public DeathListener(PointsManager points, CooldownManager cooldown, AuditLogger logger) {
        this.points = points;
        this.cooldown = cooldown;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKill(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        UUID killerId = killer.getUniqueId();

        // Cooldown checks
        if (cooldown.checkGlobal(killerId)) {
            killer.sendMessage(ChatColor.RED + "Thao tác quá nhanh! Chờ " + cooldown.getRemaining(killerId, "global") + "s");
            return;
        }
        if (cooldown.checkPvP(killerId)) {
            killer.sendMessage(ChatColor.RED + "Giết người quá nhanh! Chờ " + cooldown.getRemaining(killerId, "pvp") + "s");
            return;
        }

        // Point transfer
        int stolen = Math.min(points.getPoints(victim.getUniqueId()), points.getStolenPoints());
        points.addPoints(victim.getUniqueId(), -stolen, true, killer);
        points.addPoints(killerId, stolen, false, killer);
        logger.logPointChange(killerId, stolen, "pvp_kill", killer);


        // Set cooldowns
        cooldown.setGlobal(killerId);
        cooldown.setPvP(killerId);

        // Messages
        killer.sendMessage(ChatColor.GOLD + "+" + stolen + " điểm (Hạ " + victim.getName() + ")");
        victim.sendMessage(ChatColor.RED + "-" + stolen + " điểm (Bị hạ bởi " + killer.getName() + ")");
    }
}