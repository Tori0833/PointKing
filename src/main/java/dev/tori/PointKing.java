package dev.tori;

import dev.tori.commands.PointsCommand;
import dev.tori.listeners.DeathListener;
import dev.tori.managers.PointsManager;
import dev.tori.utils.PointsTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class PointKing extends JavaPlugin {
    private PointsManager pointsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        pointsManager = new PointsManager(this);
        pointsManager.load();

        getServer().getPluginManager().registerEvents(new DeathListener(this, pointsManager), this);
        getCommand("points").setExecutor(new PointsCommand(this, pointsManager));
        getCommand("points").setTabCompleter(new PointsTabCompleter());

        getLogger().info("PointKing đã được bật!");
    }

    public PointsManager getPointsManager() {
        return pointsManager;
    }
}