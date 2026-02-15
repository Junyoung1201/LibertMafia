package libert.saehyeon.mafia;

import libert.saehyeon.mafia.elimiator.Eliminator;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameHideListener implements Listener {
    @EventHandler
    void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(Main.ins, () -> {

            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

            if(!Eliminator.isEliminated(e.getPlayer())) {

                Team team = scoreboard.getTeam("name-hide");

                if(team != null && team.hasEntry(e.getPlayer().getName())) {
                    team.addEntry(e.getPlayer().getName());
                }
            }
        },5);
    }
}
