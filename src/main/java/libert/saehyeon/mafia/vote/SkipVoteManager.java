package libert.saehyeon.mafia.vote;

import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SkipVoteManager {
    private static final int DEFAULT_SKIP_VOTE_SECONDS = 30;

    private static final Map<UUID, Boolean> votes = new HashMap<>();
    private static final Set<UUID> eligibleVoters = new HashSet<>();
    private static boolean voteActive = false;
    private static BukkitTask timeoutTask;

    public static boolean isVoteActive() {
        return voteActive;
    }

    public static void startSkipVote(CommandSender sender, boolean force) {
        if (voteActive) {
            sender.sendMessage("이미 스킵 투표가 진행 중입니다.");
            return;
        }
        if (!GameManager.isSkippablePhase() && !force) {
            sender.sendMessage("§c현재는 스킵 투표를 진행할 수 없습니다.");
            return;
        }

        votes.clear();
        eligibleVoters.clear();
        for (Player player : GameManager.getPlayers()) {
            eligibleVoters.add(player.getUniqueId());
        }

        if (eligibleVoters.isEmpty()) {
            sender.sendMessage("투표할 플레이어가 없습니다.");
            return;
        }

        voteActive = true;
        broadcastSkipVote();

        timeoutTask = Bukkit.getScheduler().runTaskLater(Main.ins, SkipVoteManager::finishVote, DEFAULT_SKIP_VOTE_SECONDS * 20L);
    }

    public static void recordVote(Player voter, boolean skip) {
        if (!voteActive) {
            voter.sendMessage("진행 중인 스킵 투표가 없습니다.");
            return;
        }
        if (!eligibleVoters.contains(voter.getUniqueId())) {
            return;
        }
        if (votes.containsKey(voter.getUniqueId())) {
            voter.sendMessage("이미 투표했습니다.");
            return;
        }

        votes.put(voter.getUniqueId(), skip);
        voter.sendMessage(skip ? "§a스킵에 투표했습니다." : "§c유지에 투표했습니다.");

        if (votes.keySet().containsAll(eligibleVoters)) {
            finishVote();
        }
    }

    public static void cancelIfActive() {
        if (!voteActive) {
            return;
        }
        finishVoteInternal(false, true);
    }

    private static void finishVote() {
        finishVoteInternal(true, false);
    }

    private static void finishVoteInternal(boolean announce, boolean canceled) {
        if (!voteActive) {
            return;
        }
        voteActive = false;

        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }

        int skipVotes = 0;
        int keepVotes = 0;
        for (boolean skip : votes.values()) {
            if (skip) {
                skipVotes++;
            } else {
                keepVotes++;
            }
        }

        if (announce) {
            Bukkit.broadcastMessage("스킵 투표 결과: §a스킵 " + skipVotes + "표§f, §c유지 " + keepVotes + "표§f");
        }

        if (!canceled && skipVotes > keepVotes) {
            Bukkit.broadcastMessage("시간을 스킵했습니다.");
            GameManager.skipCurrentPhase();
        } else if (!canceled) {
            Bukkit.broadcastMessage("스킵이 부결되었습니다.");
        }
    }

    private static void broadcastSkipVote() {
        TextComponent header = new TextComponent("[스킵 투표] ");
        header.setColor(ChatColor.GOLD);
        TextComponent question = new TextComponent("현재 시간을 스킵하시겠습니까? ");
        question.setColor(ChatColor.WHITE);

        TextComponent skip = new TextComponent("[스킵]");
        skip.setColor(ChatColor.GREEN);
        skip.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skip 찬성"));

        TextComponent keep = new TextComponent("[유지]");
        keep.setColor(ChatColor.RED);
        keep.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skip 반대"));

        for (Player player : GameManager.getPlayers()) {
            player.spigot().sendMessage(header, question, skip, new TextComponent(" "), keep);
        }
    }
}
