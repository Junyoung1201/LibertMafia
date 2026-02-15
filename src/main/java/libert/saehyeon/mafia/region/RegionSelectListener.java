package libert.saehyeon.mafia.region;

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
    public void onSelect(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = e.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.FLINT) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            RegionManager.setPos1(block.getLocation());
            RegionManager.saveRegion();
            player.sendMessage("첫번째 지점이 설정되었습니다: "
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
        }

        else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            RegionManager.setPos2(block.getLocation());
            RegionManager.saveRegion();
            player.sendMessage("두번째 지점이 설정되었습니다: "
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
        }
    }
}

