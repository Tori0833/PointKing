package dev.tori.placeholders;

import dev.tori.PointKing;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import dev.tori.managers.PointsManager;

public class PointKingExpansion extends PlaceholderExpansion {

    private final PointKing plugin;

    public PointKingExpansion(PointKing plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pointking";
    }

    @Override
    public @NotNull String getAuthor() {
        return "tori.toriko"; // để tên t nè
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("points")) {
            return String.valueOf(plugin.getPointsManager().getPoints(player.getUniqueId()));
        }

        return null;
    }
}

