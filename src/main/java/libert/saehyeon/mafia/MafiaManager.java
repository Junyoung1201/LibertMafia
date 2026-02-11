package libert.saehyeon.mafia;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MafiaManager {
    public static final String MAFIA_GUI_TITLE = "마피아";

    private static final int MAX_GUI_SIZE = 54;
    private static JavaPlugin plugin;
    private static NamespacedKey targetKey;
    private static boolean killDone = false;
    private static final Set<UUID> mafiaPlayers = new HashSet<>();
    private static String lastNightKillName;

    public static void initialize(JavaPlugin plugin) {
        MafiaManager.plugin = plugin;
        targetKey = new NamespacedKey(plugin, "mafia_target");
    }

    public static NamespacedKey getTargetKey() {
        return targetKey;
    }

    public static void startNight() {
        killDone = false;
        lastNightKillName = null;
        mafiaPlayers.clear();
        for (Player player : GameManager.getPlayers()) {
            if ("마피아".equals(RoleManager.getRole(player))) {
                mafiaPlayers.add(player.getUniqueId());
                openKillGui(player);
            }
        }
    }

    public static boolean isKillDone() {
        return killDone;
    }

    public static void markKillDone() {
        killDone = true;
    }

    public static boolean isMafia(Player player) {
        return mafiaPlayers.contains(player.getUniqueId());
    }

    public static void closeMafiaGuiForAll() {
        for (UUID mafiaId : mafiaPlayers) {
            Player player = Bukkit.getPlayer(mafiaId);
            if (player != null && MAFIA_GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                player.closeInventory();
            }
        }
    }

    public static void reopenKillGui(Player mafia) {
        if (!GameManager.isNight() || killDone) {
            return;
        }
        if (!isMafia(mafia)) {
            return;
        }
        openKillGui(mafia);
    }

    public static void scheduleReopen(Player mafia) {
        if (!GameManager.isNight() || killDone) {
            return;
        }
        if (!isMafia(mafia)) {
            return;
        }
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskLater(plugin, () -> reopenKillGui(mafia), 2L);
    }

    public static void openKillGui(Player mafia) {
        List<Player> players = GameManager.getPlayers();
        int size = Math.min(MAX_GUI_SIZE, Math.max(9, ((players.size() - 1) / 9 + 1) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, MAFIA_GUI_TITLE);

        for (Player player : players) {
            if (player.getUniqueId().equals(mafia.getUniqueId())) {
                continue;
            }
            if ("마피아".equals(RoleManager.getRole(player))) {
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

        mafia.openInventory(inventory);
    }

    public static void recordNightKill(Player target) {
        if (target == null) {
            return;
        }
        lastNightKillName = target.getName();
    }

    public static String consumeNightKillMessage() {
        if (lastNightKillName == null) {
            return "밤 사이 아무도 사살되지 않았습니다.";
        }
        String message = "밤 사이 §7" + lastNightKillName + "§f(이)가 사살되었습니다.";
        lastNightKillName = null;
        return message;
    }
}
