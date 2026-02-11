package libert.saehyeon.mafia;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PoliceManager {
    public static final String POLICE_GUI_TITLE = "경찰서";

    private static final int MAX_GUI_SIZE = 54;
    private static JavaPlugin plugin;
    private static NamespacedKey targetKey;
    private static Location stationLocation;
    private static File configFile;

    private static final Set<UUID> investigatedThisNight = new HashSet<>();

    public static void initialize(JavaPlugin plugin) {
        PoliceManager.plugin = plugin;
        targetKey = new NamespacedKey(plugin, "police_target");
        configFile = new File(plugin.getDataFolder(), "police.yml");
    }

    public static void loadFromConfig() {
        if (configFile == null) {
            return;
        }
        if (!configFile.exists()) {
            stationLocation = null;
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String worldName = config.getString("police.station.world");
        if (worldName == null || worldName.isBlank()) {
            stationLocation = null;
            return;
        }

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            stationLocation = null;
            return;
        }

        int x = config.getInt("police.station.x");
        int y = config.getInt("police.station.y");
        int z = config.getInt("police.station.z");
        stationLocation = new Location(world, x, y, z);
    }

    public static void saveToConfig() {
        if (configFile == null) {
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        if (stationLocation == null || stationLocation.getWorld() == null) {
            try {
                config.save(configFile);
            } catch (IOException ignored) {
                // No-op: best-effort save
            }
            return;
        }

        config.set("police.station.world", stationLocation.getWorld().getName());
        config.set("police.station.x", stationLocation.getBlockX());
        config.set("police.station.y", stationLocation.getBlockY());
        config.set("police.station.z", stationLocation.getBlockZ());

        try {
            config.save(configFile);
        } catch (IOException ignored) {
            // No-op: best-effort save
        }
    }

    public static void setStationLocation(Location location) {
        if (location == null) {
            stationLocation = null;
            return;
        }
        stationLocation = location.getBlock().getLocation();
    }

    public static Location getStationLocation() {
        return stationLocation;
    }

    public static boolean isStationBlock(Block block) {
        if (stationLocation == null || block == null || block.getWorld() == null) {
            return false;
        }

        Location loc = block.getLocation();

        if (!stationLocation.getWorld().equals(loc.getWorld())) {
            return false;
        }

        return stationLocation.getBlockX() == loc.getBlockX()
                && stationLocation.getBlockY() == loc.getBlockY()
                && stationLocation.getBlockZ() == loc.getBlockZ();
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
}
