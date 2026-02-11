package libert.saehyeon.mafia;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoleManager {
    private static final Map<UUID, String> roles = new HashMap<>();
    private static NamespacedKey mafiaWeaponKey;

    public static void initialize(JavaPlugin plugin) {
        mafiaWeaponKey = new NamespacedKey(plugin, "mafia_weapon");
    }

    public static boolean assignRoles() {
        List<Player> players = new ArrayList<>(GameManager.getPlayers());
        if (players.size() < 2) {
            return false;
        }

        for (Player player : players) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
        }

        Collections.shuffle(players);
        Player mafia = players.get(0);
        Player police = players.get(1);

        roles.clear();
        roles.put(mafia.getUniqueId(), "마피아");
        roles.put(police.getUniqueId(), "경찰");

        ItemStack mafiaWeapon = createMafiaWeapon();
        mafia.getInventory().addItem(mafiaWeapon);

        for (int i = 2; i < players.size(); i++) {
            roles.put(players.get(i).getUniqueId(), "시민");
        }

        for (Player player : players) {
            String role = roles.getOrDefault(player.getUniqueId(), "시민");
            player.sendMessage("당신의 역할은 " + role + "입니다.");
        }

        return true;
    }

    public static String getRole(Player player) {
        return roles.getOrDefault(player.getUniqueId(), "시민");
    }

    public static int countAliveMafia() {
        int count = 0;
        for (Player player : GameManager.getPlayers()) {
            if ("마피아".equals(getRole(player))) {
                count++;
            }
        }
        return count;
    }

    public static int countAliveCitizens() {
        int count = 0;
        for (Player player : GameManager.getPlayers()) {
            if (!"마피아".equals(getRole(player))) {
                count++;
            }
        }
        return count;
    }

    public static boolean setRole(Player player, String roleName) {
        String normalized = normalizeRole(roleName);
        if (normalized == null) {
            return false;
        }
        roles.put(player.getUniqueId(), normalized);
        player.sendMessage("당신의 역할이 " + normalized + "로 설정되었습니다.");
        return true;
    }

    public static String normalizeRole(String roleName) {
        if (roleName == null) {
            return null;
        }
        String trimmed = roleName.trim();
        return switch (trimmed) {
            case "마피아", "mafia" -> "마피아";
            case "경찰", "police" -> "경찰";
            case "시민", "citizen" -> "시민";
            default -> null;
        };
    }

    public static void giveMafiaWeaponsForNight() {
        for (Player player : GameManager.getPlayers()) {
            if ("마피아".equals(getRole(player))) {
                giveMafiaWeapon(player);
            }
        }
    }

    public static void giveMafiaWeapon(Player player) {
        removeMafiaWeapons(player);
        player.getInventory().addItem(createMafiaWeapon());
    }

    public static void removeMafiaWeapons(Player player) {
        if (player == null) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isMafiaWeapon(contents[i])) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
        if (isMafiaWeapon(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    public static ItemStack createMafiaWeapon() {
        ItemStack mafiaWeapon = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = mafiaWeapon.getItemMeta();
        meta.setDisplayName("§c마피아의 흉기");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(mafiaWeaponKey, PersistentDataType.BYTE, (byte) 1);
        mafiaWeapon.setItemMeta(meta);
        return mafiaWeapon;
    }

    public static boolean isMafiaWeapon(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_SWORD || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(mafiaWeaponKey, PersistentDataType.BYTE);
    }
}
