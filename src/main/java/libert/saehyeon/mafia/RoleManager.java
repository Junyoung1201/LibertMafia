package libert.saehyeon.mafia;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoleManager {
    private static final Map<UUID, String> roles = new HashMap<>();

    public static boolean assignRoles() {
        List<Player> players = new ArrayList<>(GameManager.getPlayers());
        if (players.size() < 2) {
            return false;
        }

        Collections.shuffle(players);
        Player mafia = players.get(0);
        Player police = players.get(1);

        roles.clear();
        roles.put(mafia.getUniqueId(), "마피아");
        roles.put(police.getUniqueId(), "경찰");

        for (int i = 2; i < players.size(); i++) {
            roles.put(players.get(i).getUniqueId(), "시민");
        }

        for (Player player : players) {
            String role = roles.getOrDefault(player.getUniqueId(), "시민");
            player.sendMessage("당신의 역할은 " + role + "입니다.");
        }

        return true;
    }

    public static String getRole(Player player) {
        return roles.getOrDefault(player.getUniqueId(), "시민");
    }

    public static int countAliveMafia() {
        int count = 0;
        for (Player player : GameManager.getPlayers()) {
            if ("마피아".equals(getRole(player))) {
                count++;
            }
        }
        return count;
    }

    public static int countAliveCitizens() {
        int count = 0;
        for (Player player : GameManager.getPlayers()) {
            if (!"마피아".equals(getRole(player))) {
                count++;
            }
        }
        return count;
    }
}
