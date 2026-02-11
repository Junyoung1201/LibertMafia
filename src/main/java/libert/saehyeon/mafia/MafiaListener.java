package libert.saehyeon.mafia;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MafiaListener implements Listener {

    @EventHandler
    public void onMafiaKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        if (!GameManager.isNight()) {
            return;
        }

        if (!"마피아".equals(RoleManager.getRole(killer))) {
            return;
        }

        if (!RoleManager.isMafiaWeapon(killer.getInventory().getItemInMainHand())) {
            return;
        }

        if ("마피아".equals(RoleManager.getRole(victim))) {
            return;
        }

        MafiaManager.recordNightKill(victim);
        RoleManager.removeMafiaWeapons(killer);
        killer.sendMessage("마피아는 밤에 한 명 씩만 사살할 수 있습니다.");

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(MafiaListener.class);
        Bukkit.getScheduler().runTask(plugin, () -> victim.setGameMode(GameMode.SPECTATOR));

        if (!GameManager.checkCitizenWin()) {
            GameManager.checkMafiaWinNow();
        }
    }
}
