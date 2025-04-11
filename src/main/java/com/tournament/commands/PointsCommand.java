package com.tournament.commands;

import com.tournament.managers.PointsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PointsCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PointsManager pointsManager;

    public PointsCommand(JavaPlugin plugin, PointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi mới dùng lệnh này được.");
                return true;
            }

            UUID uuid = player.getUniqueId();
            int points = pointsManager.getPoints(uuid);
            player.sendMessage(ChatColor.GREEN + "Bạn có " + points + " điểm.");
            return true;
        }

        if (args.length >= 1) {
            String sub = args[0].toLowerCase();

            if (sub.equals("set") || sub.equals("add")) {
                if (!sender.hasPermission("pointking.admin")) {
                    sender.sendMessage(ChatColor.RED + "Bạn không có quyền dùng lệnh này.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Cú pháp: /points " + sub + " <player> <amount>");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                int amount;

                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Số điểm phải là số nguyên.");
                    return true;
                }

                if (sub.equals("set")) {
                    int current = pointsManager.getPoints(target.getUniqueId());
                    pointsManager.addPoints(target.getUniqueId(), amount - current, true);
                    sender.sendMessage(ChatColor.YELLOW + "Đã set " + amount + " điểm cho " + target.getName());
                } else {
                    pointsManager.addPoints(target.getUniqueId(), amount, true);
                    sender.sendMessage(ChatColor.YELLOW + "Đã cộng " + amount + " điểm cho " + target.getName());
                }

                pointsManager.savePoints();
                return true;
            }

            if (sub.equals("reload")) {
                if (!sender.hasPermission("pointking.admin")) {
                    sender.sendMessage(ChatColor.RED + "Bạn không có quyền dùng lệnh này.");
                    return true;
                }

                plugin.reloadConfig();
                pointsManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Đã reload config và dữ liệu.");
                return true;
            }

            // /points top [amount]
            if (sub.equals("top")) {
                int amount = 5;
                if (args.length >= 2) {
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Số lượng phải là số nguyên.");
                        return true;
                    }
                }

                Map<UUID, Integer> allPoints = new HashMap<>();
                for (String uuidStr : pointsManager.getAllStoredUUIDs()) {
                    UUID uuid = UUID.fromString(uuidStr);
                    allPoints.put(uuid, pointsManager.getPoints(uuid));
                }

                List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(allPoints.entrySet());
                sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue())); // sort descending

                sender.sendMessage(ChatColor.GOLD + "Top " + amount + " người chơi có nhiều điểm nhất:");
                for (int i = 0; i < Math.min(amount, sorted.size()); i++) {
                    UUID uuid = sorted.get(i).getKey();
                    int point = sorted.get(i).getValue();
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    sender.sendMessage(ChatColor.YELLOW + "" + (i + 1) + ". " + name + ": " + point + " điểm");
                }

                return true;
            }

            // /points <player> (check điểm người khác)
            if (pointsManager.isAllowCheckOthers() || sender.hasPermission("pointking.admin")) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                if ((target.getName() == null) || (!target.hasPlayedBefore() && !target.isOnline())) {
                    sender.sendMessage(ChatColor.RED + "Người chơi này chưa từng vào server.");
                    return true;
                }
                int points = pointsManager.getPoints(target.getUniqueId());
                sender.sendMessage(ChatColor.AQUA + target.getName() + " hiện có " + points + " điểm.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Bạn không được phép xem điểm người khác.");
                return true;
            }

        }

        return false;
    }
}
