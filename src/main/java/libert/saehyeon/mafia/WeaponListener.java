package libert.saehyeon.mafia;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class WeaponListener implements Listener {

    @EventHandler
    public void onUseWeapon(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            ItemStack item = attacker.getInventory().getItemInMainHand();
            if (!ClueManager.isWeaponItem(item)) {
                return;
            }

            if (victim.getUniqueId().equals(attacker.getUniqueId())) {
                return;
            }

            if (victim.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            event.setDamage(0);
            victim.setGameMode(GameMode.SPECTATOR);
            attacker.sendMessage("§7"+victim.getName() + "(을)를 처치했습니다.");
            victim.sendTitle("","당신은 §7"+attacker.getName()+"§f에게 암살되었습니다.", 0,70,25);

            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.MASTER, 0.7f,1);
            victim.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.MASTER, 0.7f,1);

            consumeOne(attacker, item);

            if (!GameManager.checkCitizenWin()) {
                GameManager.checkMafiaWinNow();
            }
        }
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

