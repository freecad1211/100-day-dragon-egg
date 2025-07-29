package com.jeonensu.dragoneggrace;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item; // Item 엔티티 import 추가
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent; // ItemMergeEvent import 추가
import org.bukkit.event.entity.ItemSpawnEvent; // ItemSpawnEvent import 추가
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Color; // org.bukkit.Color import 추가
import org.bukkit.Particle;
public class GameEventListener implements Listener {

    private final DragonEggRacePlugin plugin;

    public GameEventListener(DragonEggRacePlugin plugin) {
        this.plugin = plugin;
    }
    // 플레이어가 블록을 우클릭 또는 좌클릭했을 때
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isGameActive()) return; // 게임이 활성화 상태일 때만

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // 클릭한 블록이 없거나, 드래곤 알이 아니면 리턴
        if (clickedBlock == null || clickedBlock.getType() != Material.DRAGON_EGG) {
            return;
        }

        // 알이 클릭되었을 때만 이벤트를 취소하고 인벤토리로 넣기
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true); // 이벤트 취소 (알이 움직이거나 파괴되지 않도록)

            // 플레이어 인벤토리에 알 추가
            ItemStack dragonEgg = new ItemStack(Material.DRAGON_EGG);
            if (player.getInventory().addItem(dragonEgg).isEmpty()) { // 인벤토리에 공간이 있으면 추가
                clickedBlock.setType(Material.AIR); // 월드의 알 블록 제거
                player.sendMessage(ChatColor.GREEN + "드래곤 알을 인벤토리로 가져왔습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else { // 인벤토리에 공간이 없으면
                player.sendMessage(ChatColor.RED + "인벤토리가 가득 찼습니다. 드래곤 알을 가져올 수 없습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!plugin.isGameActive()) return;

        Item spawnedItem = event.getEntity();
        if (spawnedItem.getItemStack().getType() == Material.DRAGON_EGG) {
            // 발광 효과 적용 (glow color override는 1.17+에서만 사용 가능)
            spawnedItem.setGlowing(true);
            // If you are on Minecraft 1.16.5 or older, remove or comment out the line below:
            // spawnedItem.setGlowColorOverride(Color.PURPLE);

            // For older versions, you can add a particle effect to simulate a purple glow
            // This task will run repeatedly to keep the particles visible
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                if (!spawnedItem.isDead() && spawnedItem.isValid() && spawnedItem.getItemStack().getType() == Material.DRAGON_EGG) {
                    spawnedItem.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL, // Or another particle like END_ROD, SPELL_MOB, etc.
                            spawnedItem.getLocation().add(0, 0.5, 0), // Adjust height
                            5, // amount
                            0.2, 0.2, 0.2, // offset (spread)
                            0.0 // extra (speed)
                    );
                }
            }, 0L, 10L); // Every 0.5 seconds (10 ticks)
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!plugin.isGameActive()) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.DRAGON_EGG) {
            Player player = (Player) event.getEntity();


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

        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.isGameActive()) {
            Block placedBlock = event.getBlockPlaced();
            if (placedBlock != null && placedBlock.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "드래곤 알은 설치할수없습니다!");
            }
        }
    }
    // 플레이어가 아이템을 던질 때 발생하는 이벤트
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // 게임이 활성화 상태일 때만 적용
        if (plugin.isGameActive()) {
            // 던진 아이템이 드래곤 알인지 확인
            if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
                event.setCancelled(true); // 아이템 던지기 취소
                event.getPlayer().sendMessage(ChatColor.RED + "드래곤 알은 던질 수 없습니다!");
            }
        }
    }
    // 플레이어가 블록을 파괴할 때 발생하는 이벤트
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 게임이 활성화 상태일 때만 적용
        if (plugin.isGameActive()) {
            // 파괴하려는 블록이 신호기 추적 목록에 있는지 확인
            // DragonEggTracker 인스턴스에 접근해야 하므로, GameEventListener 생성자에 plugin을 넘겨줘야 합니다.
            // DragonEggRacePlugin에 eggTracker getter를 추가하는 것이 더 깔끔합니다. (아래 플러그인 코드에 추가 예정)
            if (plugin.getEggTracker().getBeaconLocations().contains(event.getBlock().getLocation())) {
                event.setCancelled(true); // 블록 파괴 취소
                event.getPlayer().sendMessage(ChatColor.RED + "신호기 관련 블록은 파괴할 수 없습니다!");
            }
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