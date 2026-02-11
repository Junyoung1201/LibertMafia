package libert.saehyeon.mafia;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MafiaManager {
    private static JavaPlugin plugin;
    private static final Set<String> nightKillNames = new LinkedHashSet<>();

    public static void initialize(JavaPlugin plugin) {
        MafiaManager.plugin = plugin;
    }

    public static void startNight() {
        nightKillNames.clear();
    }

    public static void recordNightKill(Player target) {
        if (target == null) {
            return;
        }
        nightKillNames.add(target.getName());
    }

    public static String consumeNightKillMessage() {
        if (nightKillNames.isEmpty()) {
            return "밤 사이 아무도 사살되지 않았습니다.";
        }
        String joinedNames = nightKillNames.stream()
                .map(name -> "§7" + name + "§f")
                .collect(Collectors.joining(", "));
        nightKillNames.clear();
        return "밤 사이 " + joinedNames + "(이)가 사살되었습니다.";
    }
}
