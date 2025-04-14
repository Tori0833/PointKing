package dev.tori;

import dev.tori.commands.PointsCommand;
import dev.tori.listeners.CrystalsListener;
import dev.tori.listeners.DeathListener;
import dev.tori.managers.PointsManager;
import dev.tori.placeholders.PointKingExpansion;
import dev.tori.utils.AuditLogger;
import dev.tori.utils.CooldownManager;
import dev.tori.utils.PointsTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PointKing extends JavaPlugin {
    private PointsManager pointsManager;
    private AuditLogger auditLogger;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize cooldown manager with plugin reference
        this.cooldownManager = new CooldownManager(this);

        // Initialize managers
        this.auditLogger = new AuditLogger(this);
        this.pointsManager = new PointsManager(this, auditLogger);
        pointsManager.load();

        // Register PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PointKingExpansion(this).register();
        }

        // Register events
        getServer().getPluginManager().registerEvents(
                new DeathListener(pointsManager, cooldownManager, auditLogger),
                this
        );
        getServer().getPluginManager().registerEvents(
                new CrystalsListener(pointsManager, cooldownManager, auditLogger),
                this
        );

        // Register commands
        getCommand("points").setExecutor(
                new PointsCommand(this, pointsManager, auditLogger, cooldownManager)
        );
        getCommand("points").setTabCompleter(new PointsTabCompleter());

        getLogger().info("PointKing Enabled! Version " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        pointsManager.saveAllPoints();
        getLogger().info("PointKing Disabled. All points saved.");
    }

    public PointsManager getPointsManager() {
        return pointsManager;
    }
}
