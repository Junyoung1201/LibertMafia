package libert.saehyeon.mafia.elimiator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;

public final class Eliminator {
    private static final String ELIMINATED_TEAM_NAME = "관전자";

    private static Scoreboard scoreboard;
    private static Team eliminatedTeam;

    private Eliminator() {
    }

    public static void eliminate(Player player) {
        if (player == null) {
            return;
        }

        if (!getTeam().hasEntry(player.getName())) {
            getTeam().addEntry(player.getName());
        }

        hide(player);
    }

    public static void show(Player player) {
        if(player.getGameMode() != GameMode.SPECTATOR) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    public static void hide(Player player) {
        player.setGameMode(GameMode.CREATIVE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 1,false,false,false));
    }

    public static boolean isEliminated(Player player) {
        if (player == null) {
            return false;
        }

        return getTeam() != null && getTeam().hasEntry(player.getName());
    }

    public static void resetEliminatedTeam() {

        Set<String> entries = new HashSet<>(getTeam().getEntries());
        for (String entry : entries) {
            Player target = Bukkit.getPlayerExact(entry);
            if(target != null) {
                target.removePotionEffect(PotionEffectType.INVISIBILITY);
                target.setGameMode(GameMode.ADVENTURE);
            }
            getTeam().removeEntry(entry);
        }
    }

    public static Team getTeam() {
        if(eliminatedTeam == null) {
            initTeam();
        }

        return eliminatedTeam;
    }

    public static void initTeam() {
        if (scoreboard == null) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            scoreboard = manager.getMainScoreboard();
        }

        eliminatedTeam = scoreboard.getTeam(ELIMINATED_TEAM_NAME);

        if(eliminatedTeam != null) {
            eliminatedTeam.unregister();
        }

        eliminatedTeam = scoreboard.registerNewTeam(ELIMINATED_TEAM_NAME);
        eliminatedTeam.setCanSeeFriendlyInvisibles(false);
        eliminatedTeam.setAllowFriendlyFire(false);
        eliminatedTeam.setNameTagVisibility(NameTagVisibility.NEVER);
    }
}
