package libert.saehyeon.mafia;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WeaponListener implements Listener {

    @EventHandler
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!GameManager.isNight()) {
            event.setCancelled(true);
            return;
        }

        if ("마피아".equals(RoleManager.getRole(attacker))
                && !RoleManager.isMafiaWeapon(attacker.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            return;
        }

        if ("시민".equals(RoleManager.getRole(attacker))
                && !ClueManager.isWeaponItem(attacker.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeathByWeapon(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = event.getPlayer().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack item = killer.getInventory().getItemInMainHand();
        if (!ClueManager.isWeaponItem(item)) {
            return;
        }

        if (victim.getUniqueId().equals(killer.getUniqueId())) {
            return;
        }

        if (victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        victim.setGameMode(GameMode.SPECTATOR);
        killer.sendMessage("§7" + victim.getName() + "(을)를 처치했습니다.");
        victim.sendTitle("", "당신은 §7" + killer.getName() + "§f에게 암살되었습니다.", 0, 70, 25);

        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 0.7f, 1);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.MASTER, 0.7f, 1);

        consumeOne(killer, item);

        if (!GameManager.checkCitizenWin()) {
            GameManager.checkMafiaWinNow();
        }
    }

    @EventHandler
    public void onDeathWithClues(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        if (!hasClueItem(victim.getInventory().getContents())) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        if (createCorpseChest(victim.getLocation(), drops)) {
            event.getDrops().clear();
        }
    }

    public static boolean createCorpseChest(Location origin, List<ItemStack> items) {
        if (origin == null || origin.getWorld() == null || items == null || items.isEmpty()) {
            return false;
        }
        if (!containsClue(items)) {
            return false;
        }

        Location chestLocation = findChestLocation(origin);
        if (chestLocation == null) {
            return false;
        }

        Block block = chestLocation.getBlock();
        block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        chest.setCustomName("§c시체 상자");
        chest.update();

        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            var remaining = chest.getInventory().addItem(item);
            leftovers.addAll(remaining.values());
        }

        for (ItemStack leftover : leftovers) {
            chestLocation.getWorld().dropItemNaturally(chestLocation, leftover);
        }

        spawnCorpseMarker(chestLocation);
        return true;
    }

    private static boolean containsClue(List<ItemStack> items) {
        for (ItemStack item : items) {
            if (ClueManager.isClueItem(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasClueItem(ItemStack[] contents) {
        if (contents == null) {
            return false;
        }
        for (ItemStack item : contents) {
            if (ClueManager.isClueItem(item)) {
                return true;
            }
        }
        return false;
    }

    private static Location findChestLocation(Location origin) {
        Location base = origin.getBlock().getLocation();
        for (int i = 0; i <= 3; i++) {
            Location candidate = base.clone().add(0, i, 0);
            if (candidate.getBlock().getType().isAir()) {
                return candidate;
            }
        }
        return null;
    }

    private static void spawnCorpseMarker(Location chestLocation) {
        Location markerLoc = chestLocation.clone().add(0.5, 1.2, 0.5);
        TextDisplay display = chestLocation.getWorld().spawn(markerLoc, TextDisplay.class);
        display.setText("§c시체 상자");
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
    }

    private void consumeOne(Player attacker, ItemStack item) {
        if (item.getAmount() <= 1) {
            attacker.getInventory().setItemInMainHand(null);
            return;
        }
        item.setAmount(item.getAmount() - 1);
        attacker.getInventory().setItemInMainHand(item);
    }
}
