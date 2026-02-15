package libert.saehyeon.mafia.vote;

import libert.saehyeon.mafia.GameManager;
import libert.saehyeon.mafia.Main;
import libert.saehyeon.mafia.clue.Clue;
import libert.saehyeon.mafia.elimiator.Eliminator;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VoteManager {
    public static final String VOTE_TITLE = "투표";

    private static final int MAX_GUI_SIZE = 54;
    private static NamespacedKey voteKey;
    private static final Map<UUID, UUID> votes = new HashMap<>();
    private static final Set<UUID> eligibleVoters = new HashSet<>();
    private static boolean voteActive = false;

    public static void init() {
        voteKey = new NamespacedKey(Main.ins, "vote_target");
    }

    public static NamespacedKey getVoteKey() {
        return voteKey;
    }

    public static void startVote() {
        votes.clear();
        eligibleVoters.clear();
        List<Player> players = GameManager.getPlayers();
        for (Player player : players) {
            eligibleVoters.add(player.getUniqueId());
        }
        voteActive = true;
        openVoteGuiForAll();
    }

    public static boolean isVoteActive() {
        return voteActive;
    }

    public static boolean hasVoted(Player player) {
        return votes.containsKey(player.getUniqueId());
    }

    public static boolean isVoteComplete() {
        return voteActive && !eligibleVoters.isEmpty() && votes.keySet().containsAll(eligibleVoters);
    }

    public static void recordVote(Player voter, UUID target) {
        if(target == null) {
            votes.put(voter.getUniqueId(), null);
        }

        if (!eligibleVoters.contains(voter.getUniqueId())) {
            return;
        }

        votes.put(voter.getUniqueId(), target);
    }

    public static void finishVote() {
        voteActive = false;
        closeVoteGuiForAll();
        List<Player> candidates = GameManager.getPlayers();
        List<UUID> candidateIds = candidates.stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toList());

        Optional<UUID> result = resolveTopVoted(votes, candidateIds);

        if (result.isEmpty()) {
            Bukkit.broadcastMessage("투표 결과가 없습니다.");
            return;
        }

        Player target = Bukkit.getPlayer(result.get());
        if (target == null) {
            Bukkit.broadcastMessage("투표 대상이 오프라인입니다.");
            return;
        }

        int clueCount = Clue.countClueItems(target);
        if (clueCount > 0) {
            boolean hidden = Clue.hideClues(clueCount, target);
            if (hidden) {
                Clue.removeClueItems(target);
            } else {
                target.sendMessage("단서를 숨기지 못했습니다. 게임 범위와 상자 상태를 확인해주세요.");
            }
        }

        Eliminator.eliminate(target);
        Bukkit.broadcastMessage("§7"+target.getName() + "§f(이)가 투표로 처형되었습니다.");

        GameManager.checkCitizenWin();
    }

    public static Optional<UUID> resolveTopVoted(Map<UUID, UUID> rawVotes, List<UUID> candidateIds) {
        Set<UUID> candidateSet = new HashSet<>(candidateIds);
        Map<UUID, Integer> counts = new HashMap<>();

        for (UUID target : rawVotes.values()) {
            if (!candidateSet.contains(target)) {
                continue;
            }
            counts.put(target, counts.getOrDefault(target, 0) + 1);
        }

        int maxVotes = 0;
        UUID top = null;
        boolean tie = false;

        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            int value = entry.getValue();
            if (value > maxVotes) {
                maxVotes = value;
                top = entry.getKey();
                tie = false;
            } else if (value == maxVotes && value > 0) {
                tie = true;
            }
        }

        if (maxVotes == 0 || tie || top == null) {
            return Optional.empty();
        }

        return Optional.of(top);
    }

    public static void reopenVoteGui(Player player) {
        if (!voteActive || hasVoted(player)) {
            return;
        }

        if (Eliminator.isEliminated(player)) {
            return;
        }

        player.openInventory(buildVoteInventory());
    }

    public static void scheduleReopen(Player player) {
        if (!voteActive || hasVoted(player)) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER,0.7f,1);
        player.sendMessage("§c한 명을 지목해야 합니다!");
        Bukkit.getScheduler().runTaskLater(Main.ins, () -> reopenVoteGui(player), 2L);
    }

    private static void openVoteGuiForAll() {
        List<Player> players = GameManager.getPlayers();

        for (Player player : players) {
            player.openInventory(buildVoteInventory());
        }
    }

    private static void closeVoteGuiForAll() {
        for (UUID voterId : eligibleVoters) {
            Player player = Bukkit.getPlayer(voterId);
            if (player != null) {
                player.closeInventory();
            }
        }
    }

    private static Inventory buildVoteInventory() {
        List<Player> players = GameManager.getPlayers();
        int size = Math.min(MAX_GUI_SIZE, Math.max(9, ((players.size() - 1) / 9 + 1) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, VOTE_TITLE);

        for (Player player : players) {
//            if (player.getUniqueId().equals(viewer.getUniqueId())) {
//                continue;
//            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            meta.setDisplayName(player.getName());

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(voteKey, PersistentDataType.STRING, player.getUniqueId().toString());

            head.setItemMeta(meta);
            inventory.addItem(head);
        }

        // 투표 건너뛰기 만들기
        ItemStack skipVote = new ItemStack(Material.BARRIER, 1);
        ItemMeta skipVoteMeta = skipVote.getItemMeta();
        skipVoteMeta.setDisplayName("§c투표 건너뛰기");
        skipVoteMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        skipVote.setItemMeta(skipVoteMeta);

        inventory.setItem(size-1,skipVote);

        return inventory;
    }
}
