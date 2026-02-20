package libert.saehyeon.mafia.citizen;

import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.RoleManager;
import libert.saehyeon.mafia.clue.Clue;
import libert.saehyeon.mafia.elimiator.Eliminator;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static libert.saehyeon.mafia.citizen.CorpseChest.create;
import static libert.saehyeon.mafia.clue.Clue.hasClueItem;

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

        if ("시민".equals(RoleManager.getRole(attacker)) || "경찰".equals(RoleManager.getRole(attacker))) {
            if (!Clue.isWeaponItem(attacker.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
            } else {
                event.setDamage(8);
            }
        }
    }

    @EventHandler
    void onDeathNoDropItem(PlayerDeathEvent e) {
        e.getDrops().clear();
    }

    @EventHandler
    public void onDeathByWeapon(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = event.getPlayer().getKiller();

        if (killer == null) {
            return;
        }

        ItemStack item = killer.getInventory().getItemInMainHand();

        if (!Clue.isWeaponItem(item)) {
            return;
        }

        if (Eliminator.isEliminated(victim)) {
            return;
        }

        if(RoleManager.getRole(killer).equals("시민") && RoleManager.getRole(victim).equals("시민")) {
            Bukkit.broadcastMessage("§c뭐야 시민이 시민을 죽였네");
            return;
        }

        Eliminator.eliminate(victim);
        killer.sendMessage("§7" + victim.getName() + "§f(을)를 처치했습니다.");

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

        if (!Clue.hasClueItem(victim)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());

        if (create(victim.getLocation(), drops)) {
            event.getDrops().clear();
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
