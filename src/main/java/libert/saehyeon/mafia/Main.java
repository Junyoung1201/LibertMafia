package libert.saehyeon.mafia;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        GameManager.initialize(this);
        VoteManager.initialize(this);
        PoliceManager.initialize(this);
        MafiaManager.initialize(this);
        ClueManager.initialize(this);
        PoliceManager.loadFromConfig();
        ClueManager.loadRegion();
        GameManager.setDebugMode(getConfig().getBoolean("debug.enabled", false));
        getServer().getPluginManager().registerEvents(new VoteListener(), this);
        getServer().getPluginManager().registerEvents(new PoliceListener(), this);
        getServer().getPluginManager().registerEvents(new MafiaListener(), this);
        getServer().getPluginManager().registerEvents(new RegionSelectListener(), this);
        getServer().getPluginManager().registerEvents(new ClueCraftListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        GameManager.stopLoop();
        getConfig().set("debug.enabled", GameManager.isDebugMode());
        PoliceManager.saveToConfig();
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("시작")) {
            boolean assigned = RoleManager.assignRoles();
            if (!assigned) {
                sender.sendMessage("플레이어가 2명 이상 필요합니다.");
                return true;
            }

            if(GameManager.isDebugMode()) {
                Bukkit.broadcastMessage("§e§l개발자 모드: §f실제 녹화할 떄는 꺼야함.");
            }

            ClueManager.placeCluesOnce(sender instanceof org.bukkit.entity.Player ? (org.bukkit.entity.Player) sender : null);
            GameManager.startLoop();
            return true;
        }

        if (command.getName().equalsIgnoreCase("디버그")) {
            boolean enabled = GameManager.toggleDebugMode();
            if (enabled) {
                sender.sendMessage("디버그 모드 활성화: 밤 10초, 낮 5초");
            } else {
                sender.sendMessage("디버그 모드 비활성화: 원래 시간으로 복구");
            }
            getConfig().set("debug.enabled", enabled);
            saveConfig();
            return true;
        }

        return false;
    }
}
