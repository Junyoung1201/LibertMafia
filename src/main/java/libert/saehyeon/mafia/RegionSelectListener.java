package libert.saehyeon.mafia;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class RegionSelectListener implements Listener {

    @EventHandler
    public void onSelect(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FLINT) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            ClueManager.setPos1(block.getLocation());
            ClueManager.saveRegion();
            player.sendMessage("첫번째 지점이 설정되었습니다: "
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            ClueManager.setPos2(block.getLocation());
            ClueManager.saveRegion();
            player.sendMessage("두번째 지점이 설정되었습니다: "
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
        }
    }
}

