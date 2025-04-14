package dev.tori.managers;

import dev.tori.PointKing;
import dev.tori.utils.AuditLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PointsManager {
    private final JavaPlugin plugin;
    private final AuditLogger auditLogger;
    private FileConfiguration pointsConfig;
    private File pointsFile;

    // Cache optimization
    private final Map<UUID, Integer> pointsCache = new ConcurrentHashMap<>();
    private final Set<UUID> modifiedPoints = ConcurrentHashMap.newKeySet();

    // Config values
    private int defaultPoints;
    private int stolenPoints;
    private int crystalPoints;
    private String onZeroPointsCommand;
    private boolean allowCheckOthers;

    public PointsManager(JavaPlugin plugin, AuditLogger auditLogger) {
        this.plugin = plugin;
        this.auditLogger = auditLogger;
    }

    public void load() {
        loadPointsFile();
        loadSettings();
    }

    private void loadPointsFile() {
        pointsFile = new File(plugin.getDataFolder(), "points.yml");

        try {
            if (!pointsFile.exists()) {
                if (pointsFile.createNewFile()) {
                    // Secure file permissions
                    try {
                        Files.setPosixFilePermissions(
                                pointsFile.toPath(),
                                Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
                        );
                    } catch (UnsupportedOperationException e) {
                        // Windows system, ignore
                    }
                }
            }

            pointsConfig = YamlConfiguration.loadConfiguration(pointsFile);

            // Load all points into cache
            for (String key : pointsConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    pointsCache.put(uuid, pointsConfig.getInt(key));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in points.yml: " + key);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load points.yml", e);
        }
    }

    private void loadSettings() {
        defaultPoints = plugin.getConfig().getInt("starting-points", 100);
        stolenPoints = plugin.getConfig().getInt("points-stolen-on-kill", 20);
        onZeroPointsCommand = plugin.getConfig().getString("on-zero-points-command", "ban %player%");
        allowCheckOthers = plugin.getConfig().getBoolean("allow-check-others", false);
        crystalPoints = plugin.getConfig().getInt("crystal-break-points", 5);
    }

    public int getPoints(UUID uuid) {
        return pointsCache.getOrDefault(uuid, defaultPoints);
    }

    public void addPoints(UUID uuid, int amount, boolean silent, CommandSender executor) {
        int current = getPoints(uuid);
        int newAmount = current + amount;

        // Validate points range
        if (newAmount < 0) newAmount = 0;
        if (newAmount > 1000000) newAmount = 1000000;

        pointsCache.put(uuid, newAmount);
        modifiedPoints.add(uuid);

        // Log the transaction
        String reason = silent ? "system" : "command";
        auditLogger.logPointChange(uuid, amount, reason, executor);

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

    public void saveAllPoints() {
        try {
            // Only save modified points
            for (UUID uuid : modifiedPoints) {
                pointsConfig.set(uuid.toString(), pointsCache.get(uuid));
            }

            pointsConfig.save(pointsFile);
            modifiedPoints.clear();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Lỗi lưu points.yml", e);
        }
    }

    public void reload() {
        pointsCache.clear();
        modifiedPoints.clear();
        loadSettings();
        loadPointsFile();
    }

    public int getStolenPoints() {
        return stolenPoints;
    }
    public int getCrystalPoints() {
        return crystalPoints;
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

