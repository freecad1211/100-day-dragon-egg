package com.jeonensu.dragoneggrace;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GameEventListener implements Listener {

    private final DragonEggRacePlugin plugin;

    public GameEventListener(DragonEggRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!plugin.isGameActive()) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.DRAGON_EGG) {
            Player player = (Player) event.getEntity();
            String playerName = player.getName();

            plugin.broadcastMessage("");
            plugin.broadcastMessage(ChatColor.LIGHT_PURPLE + "🥚 " + playerName + "이(가) 드래곤 알을 획득했습니다! 🥚");
            plugin.broadcastMessage(ChatColor.GOLD + "👑 " + playerName + "이(가) 현재 1위입니다! 👑");
            plugin.broadcastMessage("");

            // 획득 사운드
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // 획득 타이틀
            player.sendTitle(
                    ChatColor.GOLD + "🥚 드래곤 알 획득! 🥚",
                    ChatColor.YELLOW + "당신이 현재 1위입니다!",
                    10, 40, 10
            );
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isGameActive()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() == Material.DRAGON_EGG) {
            Player player = (Player) event.getWhoClicked();

            // 드래곤 알 이동 시에도 알림
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (hasPlayerDragonEgg(player)) {
                    plugin.broadcastMessage(ChatColor.YELLOW + "📦 " + player.getName() + "이(가) 여전히 드래곤 알을 보유 중입니다!");
                }
            }, 1L);
        }
    }

    private boolean hasPlayerDragonEgg(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        return false;
    }
}