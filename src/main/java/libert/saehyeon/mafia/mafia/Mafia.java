package libert.saehyeon.mafia.mafia;

import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.RoleManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Mafia {
    private static final String MAFIA_WEAPON_DISPLAY_NAME = "§c잭 더 리퍼의 흉기";
    private static final int MAFIA_WEAPON_CUSTOM_MODEL_DATA = 1001;
    private static final Set<String> nightKillNames = new LinkedHashSet<>();

    public static void startNight() {
        nightKillNames.clear();
    }

    public static ItemStack createWeaponItem() {
        ItemStack mafiaWeapon = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = mafiaWeapon.getItemMeta();
        meta.setDisplayName(MAFIA_WEAPON_DISPLAY_NAME);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setCustomModelData(MAFIA_WEAPON_CUSTOM_MODEL_DATA);
        meta.setUnbreakable(true);
        mafiaWeapon.setItemMeta(meta);
        return mafiaWeapon;
    }

    public static void removeWeaponItem(Player mafiaPlayer) {
        for(ItemStack item : mafiaPlayer.getInventory().getContents()) {
            if(isWeaponItem(item)) {
                item.setAmount(0);
            }
        }
    }

    public static void recordNightKill(Player target) {
        if (target == null) {
            return;
        }
        nightKillNames.add(target.getName());
    }

    public static String getNightKillMessage() {
        if (nightKillNames.isEmpty()) {
            return "밤 사이 아무도 사살되지 않았습니다.";
        }
        String joinedNames = nightKillNames.stream()
                .map(name -> "§7" + name + "§f")
                .collect(Collectors.joining(", "));
        nightKillNames.clear();
        return "밤 사이 " + joinedNames + "(이)가 사살되었습니다.";
    }

    public static boolean isMafia(Player player) {
        return RoleManager.isJackTheRipper(player);
    }

    public static boolean canKill(Player mafiaPlayer) {
        if(mafiaPlayer == null) return false;
        if(!GameManager.isNight()) return false;

        return isWeaponItem(mafiaPlayer.getInventory().getItemInMainHand());
    }

    public static boolean isWeaponItem(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == MAFIA_WEAPON_CUSTOM_MODEL_DATA;
    }

    public static void setMafia(Player player) {
        RoleManager.setRole(player, "잭 더 리퍼");
    }
}
