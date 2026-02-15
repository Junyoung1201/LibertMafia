package libert.saehyeon.mafia.vote;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

public class VoteListener implements Listener {

    @EventHandler
    public void onVoteClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!VoteManager.VOTE_TITLE.equals(title)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String targetId = container.get(VoteManager.getVoteKey(), PersistentDataType.STRING);
        if (targetId == null) {
            return;
        }

        Player voter = (Player) event.getWhoClicked();
        UUID targetUuid = UUID.fromString(targetId);
//        if (targetUuid.equals(voter.getUniqueId())) {
//            return;
//        }
        VoteManager.recordVote(voter, targetUuid);

        String targetName = Bukkit.getPlayer(targetUuid) != null
                ? Bukkit.getPlayer(targetUuid).getName()
                : targetUuid.toString();
        voter.sendMessage("§7"+targetName+"§f(을)를 지목했습니다.");
        voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.MASTER,0.7f,1);
        voter.closeInventory();
    }

    @EventHandler
    public void onVoteClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!VoteManager.VOTE_TITLE.equals(title)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (VoteManager.isVoteActive() && !VoteManager.hasVoted(player)) {
            VoteManager.scheduleReopen(player);
        }
    }
}
