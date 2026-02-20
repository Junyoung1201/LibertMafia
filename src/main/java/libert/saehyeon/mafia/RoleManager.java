package libert.saehyeon.mafia;

import libert.saehyeon.mafia.elimiator.Eliminator;
import libert.saehyeon.mafia.mafia.Mafia;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoleManager {
    private static final Map<UUID, String> roles = new HashMap<>();

    public static boolean assignRoles() {
        Eliminator.resetEliminatedTeam();
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
        roles.put(mafia.getUniqueId(), "잭 더 리퍼");
        roles.put(police.getUniqueId(), "경찰");

        // Ensure mafia starts clean and receives the weapon.
        mafia.getInventory().clear();
        mafia.getInventory().setArmorContents(null);
        mafia.getInventory().setItemInOffHand(null);
        ItemStack mafiaWeapon = Mafia.createWeaponItem();
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

    public static boolean isJackTheRipper(Player player) {
        return "잭 더 리퍼".equals(getRole(player));
    }

    public static int countAliveMafia() {
        int count = 0;
        for (Player player : GameManager.getPlayers()) {
            if (isJackTheRipper(player)) {
                count++;
            }
        }
        return count;
    }

    public static int countAliveCitizens() {
        int count = 0;
        for (Player player : GameManager.getPlayers()) {
            if (!isJackTheRipper(player)) {
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
        if ("잭 더 리퍼".equals(normalized)) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
            player.getInventory().addItem(Mafia.createWeaponItem());
            player.getInventory().addItem(new ItemStack(Material.LANTERN));
        }
        player.sendMessage("당신의 역할이 " + normalized + "로 설정되었습니다.");
        return true;
    }

    public static String normalizeRole(String roleName) {
        if (roleName == null) {
            return null;
        }
        String trimmed = roleName.trim();
        return switch (trimmed) {
            case "잭 더 리퍼", "잭더리퍼", "jack", "jack the ripper", "마피아", "mafia" -> "잭 더 리퍼";
            case "경찰", "police" -> "경찰";
            case "시민", "citizen" -> "시민";
            default -> null;
        };
    }

    public static void giveMafiaWeaponsForNight() {
        for (Player player : GameManager.getPlayers()) {
            if (isJackTheRipper(player)) {
                giveMafiaWeapon(player);
            }
        }
    }

    public static void giveMafiaWeapon(Player player) {
        removeMafiaWeapons(player);
        player.getInventory().addItem(Mafia.createWeaponItem());
    }

    public static void removeMafiaWeapons(Player player) {
        if (player == null) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (Mafia.isWeaponItem(contents[i])) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
        if (Mafia.isWeaponItem(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    public static boolean assignRolesWithForcedMafia(String playerName) {
        Eliminator.resetEliminatedTeam();
        List<Player> players = new ArrayList<>(GameManager.getPlayers());
        if (players.size() < 2) {
            return false;
        }
        if (playerName == null || playerName.isBlank()) {
            return false;
        }

        Player forcedMafia = null;
        for (Player player : players) {
            if (playerName.equals(player.getName())) {
                forcedMafia = player;
                break;
            }
        }
        if (forcedMafia == null) {
            return false;
        }

        for (Player player : players) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
        }

        players.remove(forcedMafia);
        Collections.shuffle(players);
        Player police = players.get(0);

        roles.clear();
        roles.put(forcedMafia.getUniqueId(), "잭 더 리퍼");
        roles.put(police.getUniqueId(), "경찰");

        // Ensure mafia starts clean and receives the weapon.
        forcedMafia.getInventory().clear();
        forcedMafia.getInventory().setArmorContents(null);
        forcedMafia.getInventory().setItemInOffHand(null);
        ItemStack mafiaWeapon = Mafia.createWeaponItem();
        forcedMafia.getInventory().addItem(mafiaWeapon);

        for (Player player : players) {
            if (player.getUniqueId().equals(police.getUniqueId())) {
                continue;
            }
            roles.put(player.getUniqueId(), "시민");
        }

        for (Player player : GameManager.getPlayers()) {
            String role = roles.getOrDefault(player.getUniqueId(), "시민");
            player.sendMessage("당신의 역할은 " + role + "입니다.");
        }

        return true;
    }
}
