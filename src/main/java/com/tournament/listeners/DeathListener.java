package com.tournament.listeners;

import com.tournament.managers.PointsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DeathListener implements Listener {
    private final JavaPlugin plugin;
    private final PointsManager pointsManager;

    public DeathListener(JavaPlugin plugin, PointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer != victim) {
            UUID victimUUID = victim.getUniqueId();
            UUID killerUUID = killer.getUniqueId();
            int taken = Math.min(pointsManager.getPoints(victimUUID), pointsManager.getStolenPoints());

            pointsManager.addPoints(victimUUID, -taken);
            pointsManager.addPoints(killerUUID, taken, true);
            pointsManager.savePoints();

            killer.sendMessage(ChatColor.GOLD + "Bạn đã nhận " + taken + " điểm vì hạ " + victim.getName());
            victim.sendMessage(ChatColor.RED + "Bạn đã mất " + taken + " điểm vì bị hạ bởi " + killer.getName());

            if (pointsManager.getPoints(victimUUID) <= 0) {
                String command = pointsManager.getZeroPointsCommand().replace("%player%", victim.getName());
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                );
            }
        }
    }
}

