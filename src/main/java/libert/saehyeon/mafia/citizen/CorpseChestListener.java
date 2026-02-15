package libert.saehyeon.mafia.citizen;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class CorpseChestListener implements Listener {
    private static final String CORPSE_TEXT = "§c시체 상자";

    @EventHandler
    public void onCorpseChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }

        if (block.getState() instanceof org.bukkit.block.Chest chest) {
            String name = chest.getCustomName();
            if (name == null || !name.equals(CORPSE_TEXT)) {
                return;
            }
        } else {
            return;
        }

        Location center = block.getLocation().add(0.5, 1.2, 0.5);
        for (Entity entity : block.getWorld().getNearbyEntities(center, 1.0, 1.0, 1.0)) {
            if (entity instanceof TextDisplay display
                    && CORPSE_TEXT.equals(display.getText())
                    && display.getBillboard() == Display.Billboard.CENTER) {
                display.remove();
            }
        }
    }
}

