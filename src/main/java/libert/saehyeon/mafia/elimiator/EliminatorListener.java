package libert.saehyeon.mafia.elimiator;

import libert.saehyeon.mafia.Main;
import libert.saehyeon.mafia.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class EliminatorListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(Main.ins, () -> {
            if(Eliminator.isEliminated(e.getPlayer())) {
                Eliminator.hide(e.getPlayer());
            } else {
                Eliminator.show(e.getPlayer());
            }
        },3);
    }

    @EventHandler
    void onRespawn(PlayerRespawnEvent e) {
        Player killer = e.getPlayer().getKiller();

        Bukkit.getScheduler().runTaskLater(Main.ins, () -> {
            if(killer != null && RoleManager.isJackTheRipper(killer)) {
                Eliminator.eliminate(e.getPlayer());
            }
        },3);
    }
}

