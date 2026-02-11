package libert.saehyeon.mafia;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.Sound;

import java.util.List;
import java.util.stream.Collectors;

public class GameManager {
    private static final int DEFAULT_NIGHT_SECONDS = 10 * 60;
    private static final int DEFAULT_DAY_SECONDS = 5 * 60;
    private static final int DEBUG_NIGHT_SECONDS = 30;
    private static final int DEBUG_DAY_SECONDS = 15;
    private static final int VOTE_SECONDS = 60;

    private static JavaPlugin plugin;
    private static BossBar bossBar;
    private static BukkitTask loopTask;
    private static Phase currentPhase;
    private static int remainingSeconds;
    private static int totalSeconds;
    private static int nightSeconds = DEFAULT_NIGHT_SECONDS;
    private static int daySeconds = DEFAULT_DAY_SECONDS;
    private static boolean debugMode = false;

    private enum Phase {
        NIGHT,
        DAY,
        VOTE
    }

    public static void initialize(JavaPlugin plugin) {
        GameManager.plugin = plugin;
    }

    public static void startLoop() {
        stopLoop();
        startPhase(Phase.NIGHT);
        loopTask = Bukkit.getScheduler().runTaskTimer(plugin, GameManager::tick, 0L, 20L);
    }

    public static void stopLoop() {
        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    public static boolean toggleDebugMode() {
        debugMode = !debugMode;
        applyDebugMode();
        return debugMode;
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        applyDebugMode();
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    private static void applyDebugMode() {
        if (debugMode) {
            nightSeconds = DEBUG_NIGHT_SECONDS;
            daySeconds = DEBUG_DAY_SECONDS;
        } else {
            nightSeconds = DEFAULT_NIGHT_SECONDS;
            daySeconds = DEFAULT_DAY_SECONDS;
        }
    }

    public static boolean isNight() {
        return currentPhase == Phase.NIGHT;
    }

    private static void startPhase(Phase phase) {
        currentPhase = phase;
        switch (phase) {
            case NIGHT -> {
                totalSeconds = nightSeconds;
                ensureBossBar(BarColor.PURPLE);
                updateBossBarTitle("밤 시간", true);
                PoliceManager.resetNightInvestigations();
                MafiaManager.startNight();
                RoleManager.giveMafiaWeaponsForNight();
                ClueManager.teleportPlayersInRegion(GameManager.getPlayers(), 65, 79);
            }
            case DAY -> {
                totalSeconds = daySeconds;
                ensureBossBar(BarColor.BLUE);
                updateBossBarTitle("낮 시간", true);
                closePoliceGuiForAll();
                Bukkit.broadcastMessage(MafiaManager.consumeNightKillMessage());
            }
            case VOTE -> {
                totalSeconds = 0;
                ensureBossBar(BarColor.RED);
                updateBossBarTitle("투표시간", false);
                VoteManager.startVote();
            }
        }
        remainingSeconds = totalSeconds;
        updateBossBarProgress();

        if (phase == Phase.DAY && checkMafiaWinOnDayStart()) {
            return;
        }
    }

    private static void tick() {
        if (currentPhase == Phase.VOTE) {
            updateBossBarTitle("투표시간", false);
            updateBossBarProgress();
            if (VoteManager.isVoteComplete()) {
                VoteManager.finishVote();
                startPhase(nextPhase(currentPhase));
            }
            return;
        }

        if (remainingSeconds <= 0) {
            startPhase(nextPhase(currentPhase));
            return;
        }

        updateBossBarTitle(switch (currentPhase) {
            case NIGHT -> "밤 시간";
            case DAY -> "낮 시간";
            case VOTE -> "투표시간";
        }, true);

        updateBossBarProgress();
        remainingSeconds--;
    }

    private static Phase nextPhase(Phase phase) {
        return switch (phase) {
            case NIGHT -> Phase.DAY;
            case DAY -> Phase.VOTE;
            case VOTE -> Phase.NIGHT;
        };
    }

    private static void ensureBossBar(BarColor color) {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("", color, BarStyle.SOLID);
            bossBar.setVisible(true);
        } else {
            bossBar.setColor(color);
        }
    }

    private static void updateBossBarTitle(String prefix, boolean showTime) {
        if (!showTime) {
            bossBar.setTitle(prefix);
            return;
        }
        String time = formatTime(remainingSeconds);
        bossBar.setTitle(prefix + " " + time);
    }

    private static void updateBossBarProgress() {
        if (currentPhase == Phase.VOTE) {
            bossBar.setProgress(1.0);
        } else {
            double progress = totalSeconds == 0 ? 0.0 : (double) remainingSeconds / totalSeconds;
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
        bossBar.removeAll();
        for (Player player : getPlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private static String formatTime(int seconds) {
        int minutes = Math.max(0, seconds) / 60;
        int remainder = Math.max(0, seconds) % 60;
        return String.format("%02d:%02d", minutes, remainder);
    }

    public static List<Player> getPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .collect(Collectors.toList());
    }

    public static boolean checkCitizenWin() {
        int mafia = RoleManager.countAliveMafia();
        if (mafia == 0) {
            announceWin("§a§l시민 승리!", "마피아가 모두 처형되었습니다.");
            stopLoop();
            return true;
        }
        return false;
    }

    public static boolean checkMafiaWinNow() {
        return checkMafiaWin();
    }

    private static boolean checkMafiaWinOnDayStart() {
        return checkMafiaWin();
    }

    private static boolean checkMafiaWin() {
        int mafia = RoleManager.countAliveMafia();
        int citizens = RoleManager.countAliveCitizens();
        if (mafia > 0 && mafia >= citizens) {
            announceWin("§c§l마피아 승리!", "시민이 모두 처치되었습니다.");
            stopLoop();
            return true;
        }
        return false;
    }

    private static void announceWin(String title, String subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 10, 70, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private static void closePoliceGuiForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PoliceManager.POLICE_GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                player.closeInventory();
            }
        }
    }
}
