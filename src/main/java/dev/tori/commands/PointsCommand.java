package dev.tori.commands;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import dev.tori.managers.PointsManager;
import dev.tori.utils.AuditLogger;
import dev.tori.utils.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class PointsCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PointsManager pointsManager;
    private final AuditLogger auditLogger;
    private final CooldownManager cooldownManager;

    public PointsCommand(JavaPlugin plugin, PointsManager pointsManager,
                         AuditLogger auditLogger, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.auditLogger = auditLogger;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleSelfCheck(sender);
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "set":
            case "add":
            case "remove":
                return handlePointModification(sender, args, subCommand);
            case "reload":
                return handleReload(sender);
            case "top":
                return handleTopPlayers(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "topteams":
                return handleTopTeams(sender, args);
            default:
                return handlePlayerCheck(sender, args);
        }
    }

    // Xử lý lệnh /points (kiểm tra điểm của bản thân)
    private boolean handleSelfCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        int points = pointsManager.getPoints(player.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Bạn có " + points + " điểm.");
        return true;
    }

    // Xử lý các lệnh thay đổi điểm (set/add/remove)
    private boolean handlePointModification(CommandSender sender, String[] args, String operation) {
        if (!sender.hasPermission("pointking.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Cú pháp: /points " + operation + " <player> <amount>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Không tìm thấy người chơi này.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0 || amount > 1000000) {
                sender.sendMessage(ChatColor.RED + "Số điểm phải từ 0 đến 1,000,000.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Số điểm phải là số nguyên hợp lệ.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        switch (operation) {
            case "set":
                int currentPoints = pointsManager.getPoints(targetUUID);
                pointsManager.addPoints(targetUUID, amount - currentPoints, true, sender);
                sender.sendMessage(ChatColor.YELLOW + "Đã đặt " + amount + " điểm cho " + target.getName());
                break;
            case "add":
                pointsManager.addPoints(targetUUID, amount, false, sender);
                sender.sendMessage(ChatColor.YELLOW + "Đã thêm " + amount + " điểm cho " + target.getName());
                break;
            case "remove":
                pointsManager.addPoints(targetUUID, -amount, false, sender);
                sender.sendMessage(ChatColor.GREEN + "Đã trừ " + amount + " điểm từ " + target.getName());
                break;
        }

        return true;
    }

    // Xử lý lệnh /points reload
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("pointking.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        plugin.reloadConfig();
        pointsManager.reload();
        cooldownManager.reload();
        sender.sendMessage(ChatColor.GREEN + "Đã tải lại config!");
        return true;
    }

    // Xử lý lệnh /points top [số lượng]
    private boolean handleTopPlayers(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("allow-check-others", true) && !sender.hasPermission("pointking.admin")) {
            sender.sendMessage(ChatColor.RED + "Lệnh này đang bị tắt.");
            return true;
        }

        int limit = 5; // Mặc định hiển thị top 5
        if (args.length >= 2) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit < 1 || limit > 20) {
                    sender.sendMessage(ChatColor.RED + "Số lượng phải từ 1 đến 20.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Số lượng phải là số nguyên.");
                return true;
            }
        }

        // Lấy danh sách điểm và sắp xếp giảm dần
        Map<UUID, Integer> allPoints = pointsManager.getAllStoredUUIDs().stream()
                .map(UUID::fromString)  // Convert to UUID first
                .collect(Collectors.toMap(
                        uuid -> uuid,    // UUID → UUID (identity)
                        pointsManager::getPoints  // UUID → Integer
                ));

        List<Map.Entry<UUID, Integer>> sortedEntries = new ArrayList<>(allPoints.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Hiển thị kết quả
        sender.sendMessage(ChatColor.GOLD + "Top " + limit + " người chơi có nhiều điểm nhất:");
        for (int i = 0; i < Math.min(limit, sortedEntries.size()); i++) {
            Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            int points = entry.getValue();
            sender.sendMessage(ChatColor.YELLOW + "" + (i + 1) + ". " + playerName + ": " + points + " điểm");
        }

        return true;
    }

    private boolean handleTopTeams(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        // Reuse the logic from TopTeamCommand.java
        PartiesAPI api = Parties.getApi();
        if (api == null) {
            player.sendMessage(ChatColor.RED + "Parties plugin is not available.");
            return true;
        }

        Map<String, Integer> teamPoints = api.getPartiesListByName(1, 1).stream()
                .collect(Collectors.toMap(
                        Party::getName,
                        party -> party.getMembers().stream()
                                .mapToInt(pointsManager::getPoints)
                                .sum()
                ));

        if (teamPoints.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No teams found.");
            return true;
        }

        // Sort teams by points (descending)
        List<Map.Entry<String, Integer>> sortedTeams = new ArrayList<>(teamPoints.entrySet());
        sortedTeams.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Determine how many teams to show (default: 5)
        int limit = 5;
        if (args.length >= 2) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(limit, 20)); // Clamp between 1-20
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number format. Showing top 5 teams.");
            }
        }

        // Display results
        player.sendMessage(ChatColor.GOLD + "Top " + limit + " Teams:");
        for (int i = 0; i < Math.min(limit, sortedTeams.size()); i++) {
            Map.Entry<String, Integer> entry = sortedTeams.get(i);
            player.sendMessage(ChatColor.YELLOW + "" + (i + 1) + ". " +
                    ChatColor.AQUA + entry.getKey() +
                    ChatColor.WHITE + ": " +
                    ChatColor.GREEN + entry.getValue() + " points");
        }

        return true;
    }

    // Xử lý lệnh /points reset <player>
    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pointking.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Cú pháp: /points reset <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Không tìm thấy người chơi này.");
            return true;
        }

        int defaultPoints = plugin.getConfig().getInt("starting-points", 100);
        pointsManager.addPoints(target.getUniqueId(), defaultPoints - pointsManager.getPoints(target.getUniqueId()), true, sender);
        sender.sendMessage(ChatColor.GREEN + "Đã reset điểm của " + target.getName() + " về " + defaultPoints);

        return true;
    }

    // Xử lý lệnh /points <player> (kiểm tra điểm người khác)
    private boolean handlePlayerCheck(CommandSender sender, String[] args) {
        if (!pointsManager.isAllowCheckOthers() && !sender.hasPermission("pointking.admin")) {
            sender.sendMessage(ChatColor.RED + "Bạn không được phép xem điểm người khác.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(ChatColor.RED + "Người chơi này chưa từng tham gia server.");
            return true;
        }

        int points = pointsManager.getPoints(target.getUniqueId());
        sender.sendMessage(ChatColor.AQUA + target.getName() + " hiện có " + points + " điểm.");
        return true;
    }
}