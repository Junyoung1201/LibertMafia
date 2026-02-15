package libert.saehyeon.mafia.clue;

import libert.saehyeon.mafia.Main;
import libert.saehyeon.mafia.region.RegionBounds;
import libert.saehyeon.mafia.region.RegionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static libert.saehyeon.mafia.region.RegionManager.isRegionReady;

public class Clue {
    private static boolean cluesPlaced;
    private static NamespacedKey clueKey;

    public static void init() {
        clueKey = new NamespacedKey(Main.ins, "clue_item");
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

        RegionBounds bounds = RegionManager.createRegionBounds();

        World world = RegionManager.getPos1().getWorld();
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

        if (chests.size() < 3) {
            if (notifier != null) {
                notifier.sendMessage("범위 내 상자가 3개 이상 필요합니다.");
            }
            return false;
        }

        Collections.shuffle(chests, new Random());
        Block first = chests.get(0);
        Block second = chests.get(1);
        Block third = chests.get(2);

        placeClue((Chest) first.getState());
        placeClue((Chest) second.getState());
        placeClue((Chest) third.getState());

        cluesPlaced = true;
        if (notifier != null) {
            notifier.sendMessage("단서 3개가 숨겨졌습니다.");
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
        meta.setLore(Arrays.asList("§f3개의 단서 아이템을 인벤토리의 제작 슬롯 또는 제작대에 두어 "));
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

    public static List<Location> findClueChestLocations() {
        if (!isRegionReady()) {
            return null;
        }

        Location pos1 = RegionManager.getPos1();
        Location pos2 = RegionManager.getPos2();

        RegionBounds bounds = RegionBounds.from(
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()
        );

        World world = pos1.getWorld();
        List<Location> locations = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int x = bounds.getMinX(); x <= bounds.getMaxX(); x++) {
            for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
                for (int z = bounds.getMinZ(); z <= bounds.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.CHEST) {
                        continue;
                    }
                    Chest chest = (Chest) block.getState();
                    if (!hasClueItem(chest)) {
                        continue;
                    }
                    Location location = getReportLocation(chest);
                    if (location == null || location.getWorld() == null) {
                        continue;
                    }
                    String key = location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
                    if (seen.add(key)) {
                        locations.add(location);
                    }
                }
            }
        }

        return locations;
    }

    public static boolean hasClueItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isClueItem(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasClueItem(Chest chest) {
        for (ItemStack item : chest.getInventory().getContents()) {
            if (isClueItem(item)) {
                return true;
            }
        }
        return false;
    }

    private static Location getReportLocation(Chest chest) {
        InventoryHolder holder = chest.getInventory().getHolder();
        if (holder instanceof DoubleChest doubleChest && doubleChest.getLocation() != null) {
            return RegionManager.toBlockLocation(doubleChest.getLocation());
        }
        return RegionManager.toBlockLocation(chest.getLocation());
    }

    public static ItemStack createWeaponItem() {
        ItemStack weapon = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("§c나이프");
        meta.setLore(java.util.List.of("§f1회용 무기입니다.","§f암살할 사람을 공격하여 처치할 수 있습니다."));
        weapon.setItemMeta(meta);
        return weapon;
    }

    public static boolean isWeaponItem(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SWORD || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().equals("§c나이프");
    }

    public static Location findChestLocation(Location origin) {
        Location base = origin.getBlock().getLocation();
        for (int i = 0; i <= 3; i++) {
            Location candidate = base.clone().add(0, i, 0);
            if (candidate.getBlock().getType().isAir()) {
                return candidate;
            }
        }
        return null;
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
        return itemCount == 3 && clueCount == 3;
    }

    public static int countClueItems(Player player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isClueItem(item)) {
                count += item.getAmount();
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isClueItem(offhand)) {
            count += offhand.getAmount();
        }
        return count;
    }

    public static int removeClueItems(Player player) {
        if (player == null) {
            return 0;
        }
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (!isClueItem(item)) {
                continue;
            }
            removed += item.getAmount();
            contents[i] = null;
        }
        player.getInventory().setContents(contents);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isClueItem(offhand)) {
            removed += offhand.getAmount();
            player.getInventory().setItemInOffHand(null);
        }
        return removed;
    }

    public static boolean hideClues(int count, Player notifier) {
        if (count <= 0) {
            return true;
        }
        if (!isRegionReady()) {
            if (notifier != null) {
                notifier.sendMessage("게임 범위가 설정되지 않았습니다.");
            }
            return false;
        }

        RegionBounds bounds = RegionManager.createRegionBounds();

        World world = RegionManager.getWorld();
        List<Chest> chests = new ArrayList<>();

        for (int x = bounds.getMinX(); x <= bounds.getMaxX(); x++) {
            for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
                for (int z = bounds.getMinZ(); z <= bounds.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.CHEST) {
                        continue;
                    }
                    chests.add((Chest) block.getState());
                }
            }
        }

        if (chests.isEmpty()) {
            if (notifier != null) {
                notifier.sendMessage("범위 내 상자가 없습니다.");
            }
            return false;
        }

        Collections.shuffle(chests, new Random());
        for (int i = 0; i < count; i++) {
            Chest chest = chests.get(i % chests.size());
            var leftovers = chest.getInventory().addItem(createClueItem());
            for (ItemStack leftover : leftovers.values()) {
                chest.getLocation().getWorld().dropItemNaturally(chest.getLocation(), leftover);
            }
        }
        return true;
    }

    public static boolean containsClue(List<ItemStack> items) {
        for (ItemStack item : items) {
            if (Clue.isClueItem(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasClueItem(ItemStack[] contents) {
        if (contents == null) {
            return false;
        }
        for (ItemStack item : contents) {
            if (Clue.isClueItem(item)) {
                return true;
            }
        }
        return false;
    }
}
