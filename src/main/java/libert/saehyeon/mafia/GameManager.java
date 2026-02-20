package libert.saehyeon.mafia;

import libert.saehyeon.mafia.elimiator.Eliminator;
import libert.saehyeon.mafia.mafia.Mafia;
import libert.saehyeon.mafia.police.PoliceManager;
import libert.saehyeon.mafia.region.RegionManager;
import libert.saehyeon.mafia.vote.SkipVoteManager;
import libert.saehyeon.mafia.vote.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
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

    private static BossBar bossBar;
    private static BukkitTask loopTask;
    private static Phase currentPhase;
    private static int remainingSeconds;
    private static int totalSeconds;
    private static int nightSeconds = DEFAULT_NIGHT_SECONDS;
    private static int daySeconds = DEFAULT_DAY_SECONDS;
    private static boolean debugMode = false;
    private static Location dayTeleportLocation;
    private static int nightCount;

    private enum Phase {
        NIGHT,
        DAY,
        VOTE
    }

    public static void startLoop() {
        stopLoop();
        nightCount = 0;
        startPhase(Phase.NIGHT);
        loopTask = Bukkit.getScheduler().runTaskTimer(Main.ins, GameManager::tick, 0L, 20L);
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

    public static boolean isFirstNight() {
        return currentPhase == Phase.NIGHT && nightCount == 1;
    }

    public static boolean isDay() {
        return currentPhase == Phase.DAY;
    }

    public static boolean isSkippablePhase() {
        return currentPhase == Phase.DAY;
    }

    public static boolean skipCurrentPhase() {
        startPhase(nextPhase(currentPhase));
        return true;
    }

    public static void setDayTeleportLocation(Location location) {
        dayTeleportLocation = location;
    }

    public static Location getDayTeleportLocation() {
        return dayTeleportLocation;
    }

    private static void startPhase(Phase phase) {
        SkipVoteManager.cancelIfActive();
        currentPhase = phase;
        switch (phase) {
            case NIGHT -> {
                nightCount++;
                totalSeconds = nightSeconds;
                ensureBossBar(BarColor.PURPLE);
                updateBossBarTitle("밤 시간", true);
                PoliceManager.resetNightInvestigations();
                Mafia.startNight();
                RoleManager.giveMafiaWeaponsForNight();
                RegionManager.teleportPlayersInRegion(GameManager.getPlayers(), 65, 79);
                notifyNightRoleInstructions();
            }
            case DAY -> {
                totalSeconds = daySeconds;
                ensureBossBar(BarColor.BLUE);
                updateBossBarTitle("낮 시간", true);
                teleportPlayersToDayLocation();
                closePoliceGuiForAll();
                Bukkit.broadcastMessage(Mafia.getNightKillMessage());
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

    private static void teleportPlayersToDayLocation() {
        if (dayTeleportLocation == null || dayTeleportLocation.getWorld() == null) {
            return;
        }
        for (Player player : getPlayers()) {
            player.teleport(dayTeleportLocation);
        }
    }

    private static String formatTime(int seconds) {
        int minutes = Math.max(0, seconds) / 60;
        int remainder = Math.max(0, seconds) % 60;
        return String.format("%02d:%02d", minutes, remainder);
    }

    public static List<Player> getPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.getGameMode().equals(GameMode.SPECTATOR) && !Eliminator.isEliminated(player))
                .collect(Collectors.toList());
    }

    public static boolean checkCitizenWin() {
        int mafia = RoleManager.countAliveMafia();
        if (mafia == 0) {
            announceWin("§a§l시민 승리!", "잭 더 리퍼가 모두 처형되었습니다.");
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
            announceWin("§c§l잭 더 리퍼 승리!", "시민이 모두 처치되었습니다.");
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

    private static void notifyNightRoleInstructions() {
        Location station = PoliceManager.getStationLocation();
        String stationText = station == null
                ? "미설정"
                : station.getBlockX() + ", " + station.getBlockY() + ", " + station.getBlockZ();

        for (Player player : getPlayers()) {
            String role = RoleManager.getRole(player);
            switch (role) {
                case "잭 더 리퍼" -> player.sendMessage("당신은 무기로 §c밤 시간 동안 한 명을 사살§f할 수 있습니다.");
                case "경찰" -> player.sendMessage("밤 시간 동안 §b경찰서에서 한 명의 직업을 조사§f할 수 있습니다.");
                default -> player.sendMessage("§6§l단서를 찾으세요! §f단서 3개를 찾아 인벤토리 제작 슬롯 또는 작업대에서 조합하여 플레이어를 죽일 수 있는 무기를 얻을 수 있습니다.");
            }
        }
    }
}
