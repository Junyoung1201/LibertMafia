package libert.saehyeon.mafia.police;

import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.RoleManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class PoliceListener implements Listener {

    @EventHandler
    public void onStationSelect(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block clicked = event.getClickedBlock();

        if (clicked == null) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE && item != null && item.getType() == Material.NAME_TAG) {
            PoliceManager.setStationLocation(clicked.getLocation());
            PoliceManager.saveToConfig();
            player.sendMessage("경찰서 위치가 지정되었습니다: "
                    + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ());
            return;
        }

        if (!PoliceManager.isStationBlock(clicked)) {
            return;
        }

        if (!"경찰".equals(RoleManager.getRole(player))) {
            player.sendMessage("경찰만 조사할 수 있습니다.");
            return;
        }

        if (!GameManager.isNight()) {
            player.sendMessage("밤 시간에만 조사할 수 있습니다.");
            return;
        }

        if (!PoliceManager.canInvestigate(player)) {
            player.sendMessage("이번 밤에는 이미 조사를 완료했습니다.");
            return;
        }

        PoliceManager.openInvestigationGui(player);
    }

    @EventHandler
    public void onInvestigateClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!PoliceManager.POLICE_GUI_TITLE.equals(title)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String targetId = container.get(PoliceManager.getTargetKey(), PersistentDataType.STRING);
        if (targetId == null) {
            return;
        }

        Player police = (Player) event.getWhoClicked();
        UUID targetUuid = UUID.fromString(targetId);
        if (targetUuid.equals(police.getUniqueId())) {
            return;
        }
        Player target = police.getServer().getPlayer(targetUuid);
        if (target == null) {
            police.sendMessage("대상이 오프라인입니다.");
            police.closeInventory();
            return;
        }

        String role = RoleManager.getRole(target);
        police.sendMessage(target.getName() + "님의 직업은 " + role + "입니다.");
        PoliceManager.markInvestigated(police);
        police.closeInventory();
    }
}
