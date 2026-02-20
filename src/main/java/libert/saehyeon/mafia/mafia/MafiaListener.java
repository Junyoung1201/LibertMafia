package libert.saehyeon.mafia.mafia;

import libert.saehyeon.mafia.elimiator.Eliminator;
import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.RoleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import static libert.saehyeon.mafia.RoleManager.isJackTheRipper;

public class MafiaListener implements Listener {

    @EventHandler
    void onMafiaAttack(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player attacker && e.getEntity() instanceof Player victim) {
            if(Mafia.isMafia(attacker) && !Mafia.isMafia(victim)) {
                if(!Mafia.canKill(attacker)) {
                    e.setCancelled(true);
                    attacker.sendActionBar("§c흉기로만 공격할 수 있습니다.");
                } else {
                    e.setDamage(8);
                }
            }
        }
    }

    @EventHandler
    public void onMafiaKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if(Mafia.canKill(killer) && Mafia.isMafia(killer)) {

            // 밤에 죽인 사람으로 기록
            Mafia.recordNightKill(victim);

            // 탈락 처리
            Eliminator.eliminate(victim);

            // 칼 뻇기
            Mafia.removeWeaponItem(killer);

            killer.sendMessage("당신은 §7"+victim.getName()+"§f(을)를 §c살해했습니다.");

            if (!GameManager.checkCitizenWin()) {
                GameManager.checkMafiaWinNow();
            }
        }
    }
}
