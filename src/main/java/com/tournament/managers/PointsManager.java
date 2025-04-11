package com.tournament.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class PointsManager {
    private final JavaPlugin plugin;
    private FileConfiguration pointsConfig;
    private File pointsFile;
    private int defaultPoints;
    private int stolenPoints;
    private String onZeroPointsCommand;
    private boolean allowCheckOthers;

    public PointsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadPointsFile();
        loadSettings();
    }

    private void loadPointsFile() {
        pointsFile = new File(plugin.getDataFolder(), "points.yml");
        if (!pointsFile.exists()) {
            try {
                pointsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pointsConfig = YamlConfiguration.loadConfiguration(pointsFile);
    }

    private void loadSettings() {
        defaultPoints = plugin.getConfig().getInt("starting-points", 100);
        stolenPoints = plugin.getConfig().getInt("points-stolen-on-kill", 20);
        onZeroPointsCommand = plugin.getConfig().getString("on-zero-points-command", "ban %player%");
        allowCheckOthers = plugin.getConfig().getBoolean("allow-check-others", false);
    }

    public int getPoints(UUID uuid) {
        return pointsConfig.getInt(uuid.toString(), defaultPoints);
    }

    public void addPoints(UUID uuid, int amount) {
        addPoints(uuid, amount, false);
    }

    public void addPoints(UUID uuid, int amount, boolean silent) {
        int current = getPoints(uuid);
        pointsConfig.set(uuid.toString(), current + amount);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && !silent) {
            if (amount > 0) {
                player.sendMessage(ChatColor.AQUA + "Bạn nhận được " + amount + " điểm!");
            } else if (amount < 0) {
                player.sendMessage(ChatColor.RED + "Bạn bị trừ " + (-amount) + " điểm.");
            }

            player.playSound(player.getLocation(), "minecraft:block.note_block.bell", 1.0f, 1.0f);
        }
    }

    public void savePoints() {
        try {
            pointsConfig.save(pointsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        loadSettings();
        loadPointsFile();
    }

    public int getStolenPoints() {
        return stolenPoints;
    }

    public boolean isAllowCheckOthers() {
        return allowCheckOthers;
    }

    public String getZeroPointsCommand() {
        return onZeroPointsCommand;
    }

    public Set<String> getAllStoredUUIDs() {
        return pointsConfig.getKeys(false);
    }

}

