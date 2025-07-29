package com.jeonensu.dragoneggrace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class DragonEggRacePlugin extends JavaPlugin {

    private boolean gameActive = false;
    private int gameTicks = 0;
    private final int GAME_DURATION = 24000 * 100; // 100일 (틱 단위)
    private BukkitTask gameTask;

    @Override
    public void onEnable() {
        getLogger().info("드래곤 알 경주 플러그인이 활성화되었습니다!");

        // 명령어 등록
        this.getCommand("dragongame").setExecutor(new GameCommand(this));

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new GameEventListener(this), this);

        // 자동 게임 시작 (원하지 않으면 주석 처리)
        startGame();
    }

    @Override
    public void onDisable() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        getLogger().info("드래곤 알 경주 플러그인이 비활성화되었습니다!");
    }

    public void startGame() {
        if (gameActive) {
            return;
        }

        gameActive = true;
        gameTicks = 0;

        // 모든 플레이어에게 게임 시작 알림
        broadcastMessage(ChatColor.GOLD + "=== 🐉 드래곤 알 경주 시작! 🐉 ===");
        broadcastMessage(ChatColor.YELLOW + "100일 후 드래곤 알을 가진 자가 승리!");
        broadcastMessage(ChatColor.RED + "남은 시간: 100일");

        // 게임 타이머 시작
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameTicks++;

                // 매일마다 (24000틱) 남은 시간 알림
                if (gameTicks % 24000 == 0) {
                    int daysLeft = 100 - (gameTicks / 24000);
                    if (daysLeft > 0) {
                        broadcastMessage(ChatColor.YELLOW + "남은 시간: " + daysLeft + "일");

                        // 마지막 10일일 때 경고
                        if (daysLeft <= 10) {
                            broadcastMessage(ChatColor.RED + "⚠ 게임 종료까지 " + daysLeft + "일 남았습니다!");
                            playSound(Sound.BLOCK_NOTE_BLOCK_BELL);
                        }

                        // 마지막 1일
                        if (daysLeft == 1) {
                            broadcastMessage(ChatColor.DARK_RED + "🚨 내일 게임이 종료됩니다! 🚨");
                            playSound(Sound.ENTITY_WITHER_SPAWN);
                        }
                    }
                }

                // 100일 경과 시 게임 종료
                if (gameTicks >= GAME_DURATION) {
                    endGame();
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 1L); // 매 틱마다 실행
    }

    public void stopGame() {
        if (!gameActive) {
            return;
        }

        gameActive = false;
        if (gameTask != null) {
            gameTask.cancel();
        }

        broadcastMessage(ChatColor.RED + "게임이 강제로 중단되었습니다!");
    }

    private void endGame() {
        gameActive = false;

        // 드래곤 알을 가진 플레이어 찾기
        Player winner = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasPlayerDragonEgg(player)) {
                winner = player;
                break;
            }
        }

        if (winner != null) {
            String winnerName = winner.getName();

            // 승리자 발표
            broadcastMessage("");
            broadcastMessage(ChatColor.GOLD + "================================");
            broadcastMessage(ChatColor.GOLD + "=== 🐉 게임 종료! 🐉 ===");
            broadcastMessage(ChatColor.GREEN + "🏆 " + winnerName + " 승리! 🏆");
            broadcastMessage(ChatColor.YELLOW + "🐲 드래곤이 소환됩니다! 🐲");
            broadcastMessage(ChatColor.GOLD + "================================");
            broadcastMessage("");

            // 승리 사운드
            playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);

            // 승리자 위치에 엔더 드래곤 소환
            spawnDragonAtPlayer(winner);

            // 승리자에게 특별한 효과
            winner.sendTitle(
                    ChatColor.GOLD + "🏆 승리! 🏆",
                    ChatColor.YELLOW + "드래곤 알 경주 우승자!",
                    10, 70, 20
            );

            // 승리자에게 보상 효과
            final Player finalPlayer = winner; // final로 복사
            Bukkit.getScheduler().runTaskLater(this, () -> {
                finalPlayer.getWorld().strikeLightningEffect(finalPlayer.getLocation());
            }, 20L);
        } else {
            broadcastMessage("");
            broadcastMessage(ChatColor.RED + "================================");
            broadcastMessage(ChatColor.RED + "=== 게임 종료 ===");
            broadcastMessage(ChatColor.RED + "❌ 아무도 드래곤 알을 가지고 있지 않습니다!");
            broadcastMessage(ChatColor.RED + "💔 게임이 무승부로 종료되었습니다.");
            broadcastMessage(ChatColor.RED + "================================");
            broadcastMessage("");
        }
    }

    private boolean hasPlayerDragonEgg(Player player) {
        // 인벤토리에서 드래곤 알 검색
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        return false;
    }

    private void spawnDragonAtPlayer(Player player) {
        World world = player.getWorld();

        // 플레이어 위 15블록에 드래곤 소환
        EnderDragon dragon = (EnderDragon) world.spawnEntity(
                player.getLocation().add(0, 15, 0),
                EntityType.ENDER_DRAGON
        );

        // 드래곤 이름 설정
        dragon.setCustomName(ChatColor.GOLD + player.getName() + "의 승리 드래곤");
        dragon.setCustomNameVisible(true);

        // 드래곤 소환 효과
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        // 파티클 효과
        world.spawnParticle(
                Particle.DRAGON_BREATH,
                player.getLocation().add(0, 15, 0),
                100, 3, 3, 3, 0.1
        );

        world.spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0, 10, 0),
                50, 2, 2, 2, 0.1
        );

        // 번개 효과
        world.strikeLightningEffect(player.getLocation());
    }

    public void broadcastMessage(String message) {
        if (message.isEmpty()) {
            Bukkit.broadcastMessage("");
        } else {
            Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[드래곤게임] " + message);
        }
    }

    private void playSound(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    // Getter 메소드들
    public boolean isGameActive() {
        return gameActive;
    }

    public int getRemainingDays() {
        if (!gameActive) return 0;
        return Math.max(0, 100 - (gameTicks / 24000));
    }

    public int getRemainingTicks() {
        if (!gameActive) return 0;
        return Math.max(0, GAME_DURATION - gameTicks);
    }

    public String getGameStatus() {
        if (!gameActive) {
            return ChatColor.RED + "게임이 진행 중이 아닙니다.";
        }

        int days = getRemainingDays();
        int hours = (getRemainingTicks() % 24000) / 1000;

        return ChatColor.YELLOW + "게임 진행 중 - 남은 시간: " + days + "일 " + hours + "시간";
    }
}