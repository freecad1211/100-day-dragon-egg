package com.jeonensu.dragoneggrace;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class DragonEggTracker {

    private final DragonEggRacePlugin plugin;
    // Stores each player's footprints (BlockDisplay entities)
    private final Map<UUID, List<BlockDisplay>> playerFootprints = new HashMap<>();
    // Stores the locations of beacons and their base blocks in the game
    private final Set<Location> beaconLocations = new HashSet<>();

    // Scheduled task for egg tracking and footprint generation
    private BukkitRunnable trackingTask;
    // The player currently holding the Dragon Egg
    private Player currentEggHolder = null;
    // Tracks if it was night in the previous tick for day/night transition
    private boolean wasNightLastTick = false;

    public DragonEggTracker(DragonEggRacePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts tracking the Dragon Egg.
     * Schedules a task to check the current egg holder and create footprints every second.
     */
    public void startTracking() {
        if (trackingTask != null) {
            trackingTask.cancel(); // Cancel any existing task
        }

        trackingTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateTracking(); // Execute tracking update logic
            }
        };
        trackingTask.runTaskTimer(plugin, 0L, 20L); // Run immediately, then every 20 ticks (1 second)
    }

    /**
     * Stops Dragon Egg tracking and cleans up all footprints and beacons.
     */
    public void stopTracking() {
        if (trackingTask != null) {
            trackingTask.cancel();
        }
        clearAllFootprints(); // Remove all players' footprints
        clearAllBeacons();    // Remove all beacons
    }

    /**
     * Called periodically to update the Dragon Egg holder and manage footprints.
     */
    private void updateTracking() {
        Player newEggHolder = findDragonEggHolder(); // Find the current egg holder

        // If the egg holder has changed
        if (newEggHolder != currentEggHolder) {
            if (currentEggHolder != null && newEggHolder == null) {
                // If the previous holder lost the egg (death, dropping it, etc.)
                onEggLost(currentEggHolder);
            }

            if (newEggHolder != null && currentEggHolder == null) {
                // If a new player picked up the egg
                onEggPickedUp(newEggHolder);
            }

            currentEggHolder = newEggHolder; // Update the current egg holder
        }

        // Check if it's currently night in the main world (usually Overworld)
        World mainWorld = Bukkit.getWorlds().get(0);
        boolean isCurrentNight = isNight(mainWorld);

        // If it transitions from day to night, clear all footprints
        if (!wasNightLastTick && isCurrentNight) {
            clearAllFootprints();
            plugin.broadcastMessage(ChatColor.DARK_GRAY + "🌙 밤이 되어 모든 발자국이 사라졌습니다.");
        }

        // Create footprints only if the current egg holder is online AND it's not night
        if (currentEggHolder != null && currentEggHolder.isOnline() && !isCurrentNight) {
            createFootprint(currentEggHolder);
        }

        cleanupOldFootprints(); // Periodically remove old footprints for performance
        wasNightLastTick = isCurrentNight; // Save current night status for the next tick
    }

    /**
     * Finds and returns the online player who currently holds the Dragon Egg.
     *
     * @return The player holding the Dragon Egg, or null if no one has it.
     */
    private Player findDragonEggHolder() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasDragonEgg(player)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Checks if a specific player has the Dragon Egg in their inventory.
     *
     * @param player The player to check.
     * @return True if the player has the egg, false otherwise.
     */
    public boolean hasDragonEgg(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a footprint if conditions are met (no recent footprint nearby)
     */
    private void createFootprint(Player player) {
        Location loc = player.getLocation();

        // 5블록 내에 발자국이 있으면 생성하지 않음
        if (hasRecentFootprintNearby(player, loc)) {
            return;
        }

        // Call the method to actually create the arrow-shaped footprint
        createArrowFootprint(player, loc);
    }

    /**
     * Checks if the current world time is considered night.
     *
     * @param world The world to check.
     * @return True if it's night, false otherwise.
     */
    private boolean isNight(World world) {
        long time = world.getTime() % 24000; // 24000틱으로 나머지 계산
        return time >= 13000 && time <= 23000; // Night time (13000 to 23000 ticks)
    }

    /**
     * Checks if the player's last footprint is too close to the current location.
     *
     * @param player The player to check.
     * @param loc The current location.
     * @return True if a recent footprint is nearby, false otherwise.
     */
    private boolean hasRecentFootprintNearby(Player player, Location loc) {
        List<BlockDisplay> footprints = playerFootprints.get(player.getUniqueId());
        if (footprints == null || footprints.isEmpty()) {
            return false;
        }

        // 마지막 발자국과의 거리 확인 (5블록 이내면 생성하지 않음)
        BlockDisplay lastFootprint = footprints.get(footprints.size() - 1);
        return lastFootprint.getLocation().distance(loc) < 5.0;
    }

    /**
     * Creates an arrow-shaped BlockDisplay entity indicating the player's movement direction.
     *
     * @param player The player leaving the footprint.
     * @param loc The location where the footprint will be created.
     */
    private void createArrowFootprint(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // Calculate the arrow's direction based on player's movement velocity
        Vector velocity = player.getVelocity();
        float directionYaw;

        // 플레이어가 멈춰있을 때는 바라보는 방향 사용
        if (velocity.lengthSquared() < 0.01) {
            directionYaw = player.getLocation().getYaw();
        } else {
            // 속도 벡터를 기반으로 방향 계산
            directionYaw = (float) Math.toDegrees(Math.atan2(-velocity.getX(), velocity.getZ()));
        }

        // Normalize the angle to be within -180 to 180 degrees
        while (directionYaw < -180) directionYaw += 360;
        while (directionYaw > 180) directionYaw -= 360;

        // Create a BlockDisplay entity (arrow shape)
        BlockDisplay arrow = (BlockDisplay) world.spawnEntity(
                loc.clone().add(0, 0.1, 0), // Slightly above the player's feet to prevent clipping
                EntityType.BLOCK_DISPLAY
        );

        // Set the block data for the arrow (purple concrete)
        BlockData arrowBlock = Material.PURPLE_CONCRETE.createBlockData();
        arrow.setBlock(arrowBlock);

        // Configure the transformation (position, rotation, scale) for the arrow
        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0), // translation: Entity's pivot point (fixed at 0,0,0)
                new AxisAngle4f(0, (float) Math.toRadians(directionYaw), 0, 1), // rotation: Rotate around Y-axis based on movement direction
                new Vector3f(0.3f, 0.05f, 0.8f), // scale: X, Y, Z axis scale (creates a thin, long arrow shape)
                new AxisAngle4f(0, 0, 0, 1) // left rotation: Not used here
        );
        arrow.setTransformation(transformation);

        // Apply glowing effect and color
        arrow.setGlowing(true);
        // If your server is older than 1.17, comment out the line below:
        arrow.setGlowColorOverride(Color.RED);

        // Display settings: brightness and view range (10블록 내에서만 보이도록)
        arrow.setBrightness(new Display.Brightness(15, 15)); // Maximum brightness
        arrow.setViewRange(10.0f); // 10블록 내에서만 보임

        // Add the footprint to the player's list of footprints
        playerFootprints.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(arrow);

        // Remove the footprint after 5 days (120000 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!arrow.isDead()) { // Only remove if it hasn't been removed already
                arrow.remove();
                // 리스트에서도 제거
                List<BlockDisplay> footprints = playerFootprints.get(player.getUniqueId());
                if (footprints != null) {
                    footprints.remove(arrow);
                }
            }
        }, 120000L); // 5일 후 자동 제거
    }

    /**
     * Cleans up old (dead or invalid) footprint entities from the list.
     */
    private void cleanupOldFootprints() {
        for (UUID playerId : new ArrayList<>(playerFootprints.keySet())) {
            List<BlockDisplay> footprints = playerFootprints.get(playerId);
            if (footprints != null) {
                // Remove entities that are dead or no longer valid
                footprints.removeIf(display -> !display.isValid() || display.isDead());

                // 빈 리스트는 맵에서 제거
                if (footprints.isEmpty()) {
                    playerFootprints.remove(playerId);
                }
            }
        }
    }

    /**
     * Logic executed when a player loses the Dragon Egg.
     * Creates a purple beacon at their location and clears their footprints.
     *
     * @param player The player who lost the egg.
     */
    private void onEggLost(Player player) {
        createBeacon(player.getLocation()); // Create a purple beacon at the location
        clearPlayerFootprints(player);      // Clear the player's footprints

        plugin.broadcastMessage("💀 " + player.getName() + "이(가) 드래곤 알을 잃었습니다!");
        plugin.broadcastMessage("🔮 보라색 신호기가 설치되었습니다!");
    }

    /**
     * Logic executed when a player picks up the Dragon Egg.
     * Removes all active beacons in the world.
     *
     * @param player The player who picked up the egg.
     */
    private void onEggPickedUp(Player player) {
        clearAllBeacons(); // Remove all previously generated beacons
        plugin.broadcastMessage("✨ " + player.getName() + "이(가) 드래곤 알을 획득했습니다!");
        plugin.broadcastMessage("✨ 신호기가 사라졌습니다!");
    }

    /**
     * Creates a purple beacon structure (beacon + 3x3 diamond base) at a specified location.
     *
     * @param loc The location where the beacon will be created.
     */
    private void createBeacon(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // Center base location for the beacon (1 block below player's feet)
        Location beaconBaseCenter = loc.clone().subtract(0, 1, 0);

        // Place 3x3 diamond block base for the beacon
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block baseBlock = beaconBaseCenter.clone().add(x, 0, z).getBlock();
                baseBlock.setType(Material.DIAMOND_BLOCK);
                beaconLocations.add(baseBlock.getLocation()); // Store base block location for cleanup
            }
        }

        // Place the beacon block at the player's feet location
        Location beaconLoc = loc.clone();
        beaconLoc.setY(Math.floor(beaconLoc.getY())); // 정확한 블록 위치로 조정
        Block beaconBlock = beaconLoc.getBlock();
        beaconBlock.setType(Material.BEACON);
        beaconLocations.add(beaconLoc); // Store beacon block location

        // Purple particle effect around the beacon
        world.spawnParticle(
                Particle.WITCH, // Witch particles (purple-ish)
                beaconLoc.clone().add(0.5, 1, 0.5), // Center of the beacon, slightly above
                50, 1, 1, 1, 0.1 // Amount, spread, speed
        );

        // Beacon activation sound
        world.playSound(beaconLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    /**
     * Removes all beacons and their base blocks created by the plugin in the world.
     */
    private void clearAllBeacons() {
        for (Location beaconLoc : new HashSet<>(beaconLocations)) {
            Block block = beaconLoc.getBlock();

            // Only remove if it's a beacon or a diamond block
            if (block.getType() == Material.BEACON || block.getType() == Material.DIAMOND_BLOCK) {
                block.setType(Material.AIR); // Set block to air

                // Smoke particle effect when removed
                beaconLoc.getWorld().spawnParticle(
                        Particle.LARGE_SMOKE,
                        beaconLoc.clone().add(0.5, 0.5, 0.5), // Center of the block
                        20, 0.5, 0.5, 0.5, 0.1
                );
            }
        }
        beaconLocations.clear(); // Clear the list of beacon locations
    }

    /**
     * Removes all footprint entities left by a specific player.
     *
     * @param player The player whose footprints to clear.
     */
    private void clearPlayerFootprints(Player player) {
        List<BlockDisplay> footprints = playerFootprints.get(player.getUniqueId());
        if (footprints != null) {
            for (BlockDisplay footprint : new ArrayList<>(footprints)) {
                if (!footprint.isDead()) { // Only remove if it hasn't been removed already
                    footprint.remove(); // Remove the entity
                }
            }
            footprints.clear(); // Clear the list
        }
    }

    /**
     * Removes all footprint entities left by all players.
     */
    private void clearAllFootprints() {
        for (List<BlockDisplay> footprints : playerFootprints.values()) {
            for (BlockDisplay footprint : footprints) {
                if (!footprint.isDead()) {
                    footprint.remove();
                }
            }
        }
        playerFootprints.clear(); // Clear the entire map
    }

    /**
     * Cleans up all tracking-related elements when the plugin is disabled.
     */
    public void cleanup() {
        stopTracking();
        clearAllFootprints();
        clearAllBeacons();
    }

    /**
     * Returns an unmodifiable set of beacon and base block locations created in the game.
     * This list is used by `GameEventListener` to prevent breaking these blocks.
     *
     * @return An unmodifiable set of beacon and base block locations.
     */
    public Set<Location> getBeaconLocations() {
        return Collections.unmodifiableSet(beaconLocations); // Return an unmodifiable set to prevent external modification
    }
}