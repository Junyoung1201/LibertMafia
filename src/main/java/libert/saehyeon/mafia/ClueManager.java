package libert.saehyeon.mafia;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClueManager {
    private static JavaPlugin plugin;
    private static File configFile;
    private static Location pos1;
    private static Location pos2;
    private static boolean cluesPlaced;
    private static NamespacedKey clueKey;
    private static NamespacedKey weaponKey;

    public static void initialize(JavaPlugin plugin) {
        ClueManager.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "region.yml");
        clueKey = new NamespacedKey(plugin, "clue_item");
        weaponKey = new NamespacedKey(plugin, "clue_weapon");
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

        World world = plugin.getServer().getWorld(worldName);
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

    public static boolean placeCluesOnce(Player notifier) {
        if (cluesPlaced) {
            return false;
        }
        if (!isRegionReady()) {
            if (notifier != null) {
                notifier.sendMessage("게임 범위가 설정되지 않았습니다.");
            }
            return false;
        }

        RegionBounds bounds = RegionBounds.from(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());

        World world = pos1.getWorld();
        List<Block> chests = new ArrayList<>();

        for (int x = bounds.getMinX(); x <= bounds.getMaxX(); x++) {
            for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
                for (int z = bounds.getMinZ(); z <= bounds.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.CHEST) {
                        continue;
                    }
                    Chest chest = (Chest) block.getState();
                    chest.getInventory().clear();
                    chests.add(block);
                }
            }
        }

        if (chests.size() < 2) {
            if (notifier != null) {
                notifier.sendMessage("범위 내 상자가 2개 이상 필요합니다.");
            }
            return false;
        }

        Collections.shuffle(chests, new Random());
        Block first = chests.get(0);
        Block second = chests.get(1);

        placeClue((Chest) first.getState());
        placeClue((Chest) second.getState());

        cluesPlaced = true;
        if (notifier != null) {
            notifier.sendMessage("단서 2개가 숨겨졌습니다.");
        }
        return true;
    }

    private static void placeClue(Chest chest) {
        chest.getInventory().addItem(createClueItem());
    }

    public static ItemStack createClueItem() {
        ItemStack clue = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = clue.getItemMeta();
        meta.setDisplayName("§e단서");
        meta.setLore(Arrays.asList("§f2개의 단서 아이템을 인벤토리의 제작 슬롯 또는 제작대에 두어 "));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(clueKey, PersistentDataType.BYTE, (byte) 1);
        clue.setItemMeta(meta);
        return clue;
    }

    public static boolean isClueItem(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(clueKey, PersistentDataType.BYTE);
    }

    public static ItemStack createWeaponItem() {
        ItemStack weapon = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("§c나이프");
        meta.setLore(java.util.List.of("§f1회용 무기입니다.","§f암살할 사람을 공격하여 처치할 수 있습니다."));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(weaponKey, PersistentDataType.BYTE, (byte) 1);
        weapon.setItemMeta(meta);
        return weapon;
    }

    public static boolean isWeaponItem(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SWORD || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(weaponKey, PersistentDataType.BYTE);
    }

    public static boolean isValidClueRecipe(ItemStack[] matrix) {
        if (matrix == null) {
            return false;
        }
        int clueCount = 0;
        int itemCount = 0;
        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            itemCount++;
            if (isClueItem(item)) {
                clueCount++;
            }
        }
        return itemCount == 2 && clueCount == 2;
    }

    private static Location toBlockLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}

