package com.jeonensu.dragoneggrace;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GameCommand implements CommandExecutor {

    private final DragonEggRacePlugin plugin;

    public GameCommand(DragonEggRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("dragongame.admin")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "사용법:");
            sender.sendMessage(ChatColor.YELLOW + "/dragongame start - 게임 시작");
            sender.sendMessage(ChatColor.YELLOW + "/dragongame stop - 게임 중단");
            sender.sendMessage(ChatColor.YELLOW + "/dragongame status - 게임 상태 확인");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (plugin.isGameActive()) {
                    sender.sendMessage(ChatColor.RED + "게임이 이미 진행 중입니다!");
                } else {
                    plugin.startGame();
                    sender.sendMessage(ChatColor.GREEN + "드래곤 알 게임을 시작했습니다!");
                }
                break;

            case "stop":
                if (!plugin.isGameActive()) {
                    sender.sendMessage(ChatColor.RED + "게임이 진행 중이 아닙니다!");
                } else {
                    plugin.stopGame();
                    sender.sendMessage(ChatColor.GREEN + "드래곤 알 게임을 중단했습니다!");
                }
                break;

            case "status":
                sender.sendMessage(plugin.getGameStatus());
                break;

            default:
                sender.sendMessage(ChatColor.YELLOW + "사용법: /dragongame <start|stop|status>");
                break;
        }

        return true;
    }
}