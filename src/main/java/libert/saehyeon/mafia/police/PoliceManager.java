package libert.saehyeon.mafia.police;

import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PoliceManager {
    public static final String POLICE_GUI_TITLE = "경찰서";

    private static final int MAX_GUI_SIZE = 54;
    private static NamespacedKey targetKey;
    private static final List<Location> stationCandidates = new ArrayList<>();
    private static Location activeStationLocation;
    private static final File configFile = new File(Main.ins.getDataFolder(), "police.yml");
    private static final YamlConfiguration config = new YamlConfiguration();

    private static final Set<UUID> investigatedThisNight = new HashSet<>();

    public static void init() {
        targetKey = new NamespacedKey(Main.ins, "police_target");
    }

    public static void loadFromConfig() {
        stationCandidates.clear();
        activeStationLocation = null;
        if (configFile == null) {
            return;
        }
        if (!configFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<Map<?, ?>> stations = config.getMapList("police.stations");
        for (Map<?, ?> entry : stations) {
            Object worldRaw = entry.get("world");
            if (!(worldRaw instanceof String worldName) || worldName.isBlank()) {
                continue;
            }
            World world = Main.ins.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            int x = getInt(entry.get("x"));
            int y = getInt(entry.get("y"));
            int z = getInt(entry.get("z"));
            stationCandidates.add(new Location(world, x, y, z));
        }

        if (stationCandidates.isEmpty()) {
            String worldName = config.getString("police.station.world");
            if (worldName == null || worldName.isBlank()) {
                return;
            }

            World world = Main.ins.getServer().getWorld(worldName);
            if (world == null) {
                return;
            }

            int x = config.getInt("police.station.x");
            int y = config.getInt("police.station.y");
            int z = config.getInt("police.station.z");
            stationCandidates.add(new Location(world, x, y, z));
        }
    }

    public static void saveToConfig() {
        List<Map<String, Object>> stations = new ArrayList<>();
        for (Location location : stationCandidates) {
            if (location == null || location.getWorld() == null) {
                continue;
            }
            stations.add(Map.of(
                    "world", location.getWorld().getName(),
                    "x", location.getBlockX(),
                    "y", location.getBlockY(),
                    "z", location.getBlockZ()
            ));
        }

        config.set("police.stations", stations);
        config.set("police.station", null);
        config.set("police.station.world", null);
        config.set("police.station.x", null);
        config.set("police.station.y", null);
        config.set("police.station.z", null);

        try {
            config.save(configFile);
        } catch (IOException ignored) {
            // No-op: best-effort save
        }
    }

    public static boolean addStationCandidate(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Location blockLocation = location.getBlock().getLocation();
        for (Location existing : stationCandidates) {
            if (isSameBlock(existing, blockLocation)) {
                return false;
            }
        }
        stationCandidates.add(blockLocation);
        return true;
    }

    public static boolean removeStationCandidate(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Location blockLocation = location.getBlock().getLocation();
        for (int i = 0; i < stationCandidates.size(); i++) {
            if (isSameBlock(stationCandidates.get(i), blockLocation)) {
                stationCandidates.remove(i);
                if (isSameBlock(activeStationLocation, blockLocation)) {
                    activeStationLocation = null;
                }
                return true;
            }
        }
        return false;
    }

    public static boolean activateRandomStation() {
        if (stationCandidates.isEmpty()) {
            activeStationLocation = null;
            return false;
        }
        int index = ThreadLocalRandom.current().nextInt(stationCandidates.size());
        activeStationLocation = stationCandidates.get(index).getBlock().getLocation();
        return true;
    }

    public static List<Location> getStationCandidates() {
        return new ArrayList<>(stationCandidates);
    }

    public static Location getStationLocation() {
        return activeStationLocation;
    }

    public static boolean isStationBlock(Block block) {
        if (activeStationLocation == null || block == null || block.getWorld() == null) {
            return false;
        }

        Location loc = block.getLocation();

        if (!activeStationLocation.getWorld().equals(loc.getWorld())) {
            return false;
        }

        return activeStationLocation.getBlockX() == loc.getBlockX()
                && activeStationLocation.getBlockY() == loc.getBlockY()
                && activeStationLocation.getBlockZ() == loc.getBlockZ();
    }

    public static boolean isStationCandidate(Block block) {
        if (block == null || block.getWorld() == null) {
            return false;
        }
        Location loc = block.getLocation();
        for (Location candidate : stationCandidates) {
            if (isSameBlock(candidate, loc)) {
                return true;
            }
        }
        return false;
    }

    public static void openInvestigationGui(Player police) {
        List<Player> players = GameManager.getPlayers();
        int size = Math.min(MAX_GUI_SIZE, Math.max(9, ((players.size() - 1) / 9 + 1) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, POLICE_GUI_TITLE);

        for (Player player : players) {
            if (player.getUniqueId().equals(police.getUniqueId())) {
                continue;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            meta.setDisplayName(player.getName());

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(targetKey, PersistentDataType.STRING, player.getUniqueId().toString());

            head.setItemMeta(meta);
            inventory.addItem(head);
        }

        police.openInventory(inventory);
    }

    public static NamespacedKey getTargetKey() {
        return targetKey;
    }

    public static void resetNightInvestigations() {
        investigatedThisNight.clear();
    }

    public static boolean canInvestigate(Player player) {
        return !investigatedThisNight.contains(player.getUniqueId());
    }

    public static void markInvestigated(Player player) {
        investigatedThisNight.add(player.getUniqueId());
    }

    private static int getInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static boolean isSameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
            return false;
        }
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
