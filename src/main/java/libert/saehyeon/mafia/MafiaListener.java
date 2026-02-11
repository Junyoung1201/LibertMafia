package libert.saehyeon.mafia;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MafiaListener implements Listener {

    @EventHandler
    public void onKillClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!MafiaManager.MAFIA_GUI_TITLE.equals(title)) {
            return;
        }

        event.setCancelled(true);

        Player mafia = (Player) event.getWhoClicked();
        if (!MafiaManager.isMafia(mafia)) {
            mafia.closeInventory();
            return;
        }

        if (!GameManager.isNight()) {
            mafia.sendMessage("밤 시간에만 사살할 수 있습니다.");
            mafia.closeInventory();
            return;
        }

        if (MafiaManager.isKillDone()) {
            mafia.sendMessage("이번 밤에는 이미 사살이 완료되었습니다.");
            mafia.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String targetId = container.get(MafiaManager.getTargetKey(), PersistentDataType.STRING);
        if (targetId == null) {
            return;
        }

        UUID targetUuid = UUID.fromString(targetId);
        if (targetUuid.equals(mafia.getUniqueId())) {
            return;
        }

        Player target = mafia.getServer().getPlayer(targetUuid);
        if (target == null) {
            mafia.sendMessage("대상이 오프라인입니다.");
            mafia.closeInventory();
            return;
        }

        if ("마피아".equals(RoleManager.getRole(target))) {
            mafia.sendMessage("마피아는 사살할 수 없습니다.");
            mafia.closeInventory();
            return;
        }

        target.setGameMode(org.bukkit.GameMode.SPECTATOR);
        mafia.sendMessage(target.getName() + "님을 사살했습니다.");
        MafiaManager.recordNightKill(target);
        MafiaManager.markKillDone();
        MafiaManager.closeMafiaGuiForAll();
        GameManager.checkCitizenWin();
    }

    @EventHandler
    public void onKillClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!MafiaManager.MAFIA_GUI_TITLE.equals(title)) {
            return;
        }

        Player mafia = (Player) event.getPlayer();
        if (!MafiaManager.isMafia(mafia)) {
            return;
        }

        if (!GameManager.isNight()) {
            return;
        }

        if (MafiaManager.isKillDone()) {
            return;
        }

        MafiaManager.scheduleReopen(mafia);
    }
}
