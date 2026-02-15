package libert.saehyeon.mafia.citizen;

import libert.saehyeon.mafia.clue.Clue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CorpseChest {
    private static final String CORPSE_TEXT = "§c시체 상자";

    public static boolean create(Location origin, List<ItemStack> items) {
        if (origin == null || origin.getWorld() == null || items == null || items.isEmpty()) {
            return false;
        }
        if (!Clue.containsClue(items)) {
            return false;
        }

        Location chestLocation = Clue.findChestLocation(origin);

        if (chestLocation == null) {
            return false;
        }

        Block block = chestLocation.getBlock();
        block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        chest.setCustomName(CORPSE_TEXT);
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

    private static void spawnCorpseMarker(Location chestLocation) {
        Location markerLoc = chestLocation.clone().add(0.5, 1.2, 0.5);
        TextDisplay display = chestLocation.getWorld().spawn(markerLoc, TextDisplay.class);
        display.setText(CORPSE_TEXT);
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowed(true);
    }

    public static void removeAll() {
        for (var world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (!CORPSE_TEXT.equals(display.getText())) {
                    continue;
                }
                Location chestLocation = display.getLocation().clone().subtract(0.5, 1.2, 0.5);
                Block block = chestLocation.getBlock();
                if (block.getType() == Material.CHEST && block.getState() instanceof Chest chest) {
                    if (CORPSE_TEXT.equals(chest.getCustomName())) {
                        block.setType(Material.AIR);
                    }
                }
                display.remove();
            }
        }
    }
}
