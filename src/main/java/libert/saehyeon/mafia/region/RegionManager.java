package libert.saehyeon.mafia.region;

import libert.saehyeon.mafia.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class RegionManager {
    private static final File configFile = new File(Main.ins.getDataFolder(), "region.yml");
    private static final double NEARBY_PLAYER_RADIUS = 5.0;

    private static Location pos1;
    private static Location pos2;

    public static Location getPos1() {
        return pos1.clone();
    }

    public static Location getPos2() {
        return pos2.clone();
    }

    public static void loadRegion() {
        if (configFile == null || !configFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String worldName = config.getString("region.world");
        if (worldName == null || worldName.isBlank()) {
            return;
        }

        World world = Main.ins.getServer().getWorld(worldName);
        if (world == null) {
            return;
        }

        int x1 = config.getInt("region.pos1.x");
        int y1 = config.getInt("region.pos1.y");
        int z1 = config.getInt("region.pos1.z");
        int x2 = config.getInt("region.pos2.x");
        int y2 = config.getInt("region.pos2.y");
        int z2 = config.getInt("region.pos2.z");

        pos1 = new Location(world, x1, y1, z1);
        pos2 = new Location(world, x2, y2, z2);
    }

    public static void saveRegion() {
        if (configFile == null) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            try {
                config.save(configFile);
            } catch (IOException ignored) {
                // No-op: best-effort save
            }
            return;
        }

        config.set("region.world", pos1.getWorld().getName());
        config.set("region.pos1.x", pos1.getBlockX());
        config.set("region.pos1.y", pos1.getBlockY());
        config.set("region.pos1.z", pos1.getBlockZ());
        config.set("region.pos2.x", pos2.getBlockX());
        config.set("region.pos2.y", pos2.getBlockY());
        config.set("region.pos2.z", pos2.getBlockZ());

        try {
            config.save(configFile);
        } catch (IOException ignored) {
            // No-op: best-effort save
        }
    }

    public static World getWorld() {
        return pos1.getWorld();
    }

    public static RegionBounds createRegionBounds() {
        Location pos1 = RegionManager.getPos1();
        Location pos2 = RegionManager.getPos2();

        return RegionBounds.from(
            pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
            pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()
        );
    }

    public static void setPos1(Location location) {
        pos1 = toBlockLocation(location);
    }

    public static void setPos2(Location location) {
        pos2 = toBlockLocation(location);
    }

    public static boolean isRegionReady() {
        return pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public static Location toBlockLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static boolean teleportPlayersInRegion(List<Player> players, int minY, int maxY) {
        if (players == null || players.isEmpty()) {
            return false;
        }
        if (!isRegionReady()) {
            return false;
        }

        RegionBounds bounds = RegionManager.createRegionBounds();

        int clampedMinY = Math.max(minY, bounds.getMinY());
        int clampedMaxY = Math.min(maxY, bounds.getMaxY());
        if (clampedMinY > clampedMaxY) {
            return false;
        }

        World world = pos1.getWorld();
        Random random = new Random();

        for (Player player : players) {
            Location location = findRandomLocation(world, bounds, clampedMinY, clampedMaxY, random, NEARBY_PLAYER_RADIUS);
            player.teleport(location);
        }

        return true;
    }

    private static Location findRandomLocation(World world, RegionBounds bounds, int minY, int maxY, Random random, double nearbyRadius) {
        Location fallback = new Location(world,
                bounds.getMinX() + 0.5,
                minY,
                bounds.getMinZ() + 0.5);

        for (int i = 0; i < 40; i++) {
            int x = random.nextInt(bounds.getMaxX() - bounds.getMinX() + 1) + bounds.getMinX();
            int y = random.nextInt(maxY - minY + 1) + minY;
            int z = random.nextInt(bounds.getMaxZ() - bounds.getMinZ() + 1) + bounds.getMinZ();

            if (isSafeSpawn(world, x, y, z)) {
                Location candidate = new Location(world, x + 0.5, y, z + 0.5);
                if (!isPlayerNearby(world, candidate, nearbyRadius)) {
                    return candidate;
                }
            }
        }

        return fallback;
    }

    private static boolean isPlayerNearby(World world, Location location, double radius) {
        if (world == null || location == null) {
            return false;
        }
        return !world.getNearbyPlayers(location, radius).isEmpty();
    }

    private static boolean isSafeSpawn(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();
        Material ground = world.getBlockAt(x, y - 1, z).getType();

        if (!feet.isAir() || !head.isAir()) {
            return false;
        }

        if (ground == Material.WATER || ground == Material.BUBBLE_COLUMN) {
            return false;
        }

        return ground.isSolid();
    }
}
