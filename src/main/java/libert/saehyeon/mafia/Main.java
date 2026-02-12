package libert.saehyeon.mafia;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        RoleManager.initialize(this);
        GameManager.initialize(this);
        VoteManager.initialize(this);
        PoliceManager.initialize(this);
        MafiaManager.initialize(this);
        ClueManager.initialize(this);
        PoliceManager.loadFromConfig();
        ClueManager.loadRegion();
        GameManager.setDebugMode(getConfig().getBoolean("debug.enabled", false));
        loadDayTeleportLocation();
        getServer().getPluginManager().registerEvents(new VoteListener(), this);
        getServer().getPluginManager().registerEvents(new PoliceListener(), this);
        getServer().getPluginManager().registerEvents(new MafiaListener(), this);
        getServer().getPluginManager().registerEvents(new RegionSelectListener(), this);
        getServer().getPluginManager().registerEvents(new ClueCraftListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new CorpseChestListener(), this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.getWorlds().forEach(world -> {
                world.setGameRule(GameRules.SHOW_DEATH_MESSAGES,false);
                world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES,false);
                world.setGameRule(GameRules.ADVANCE_TIME,false);
                world.setGameRule(GameRules.ADVANCE_WEATHER,false);
            });
        },10);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        GameManager.stopLoop();
        getConfig().set("debug.enabled", GameManager.isDebugMode());
        PoliceManager.saveToConfig();
        saveConfig();
    }

    private void loadDayTeleportLocation() {
        String worldName = getConfig().getString("day.teleport.world");
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        int x = getConfig().getInt("day.teleport.x");
        int y = getConfig().getInt("day.teleport.y");
        int z = getConfig().getInt("day.teleport.z");
        GameManager.setDayTeleportLocation(new Location(world, x, y, z));
    }

    private void saveDayTeleportLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        getConfig().set("day.teleport.world", location.getWorld().getName());
        getConfig().set("day.teleport.x", location.getBlockX());
        getConfig().set("day.teleport.y", location.getBlockY());
        getConfig().set("day.teleport.z", location.getBlockZ());
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

            WeaponListener.removeAllCorpseChests();
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

        if (command.getName().equalsIgnoreCase("역할")) {
            if (args.length < 2) {
                sender.sendMessage("사용법: /역할 [플레이어] [역할]");
                sender.sendMessage("가능한 역할: 마피아, 경찰, 시민");
                return true;
            }

            String targetName = args[0];
            String roleName = args[1];
            var target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage("플레이어를 찾을 수 없습니다: " + targetName);
                return true;
            }

            boolean success = RoleManager.setRole(target, roleName);
            if (!success) {
                sender.sendMessage("알 수 없는 역할입니다. 가능한 역할: 마피아, 경찰, 시민");
                return true;
            }

            sender.sendMessage(target.getName() + "님의 역할을 " + RoleManager.getRole(target) + "로 설정했습니다.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("시체상자")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용할 수 있습니다.");
                return true;
            }

            boolean hasClue = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (ClueManager.isClueItem(item)) {
                    hasClue = true;
                    break;
                }
            }
            if (!hasClue) {
                sender.sendMessage("단서 아이템을 가진 경우에만 시체 상자를 생성할 수 있습니다.");
                return true;
            }

            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item);
                }
            }
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR) {
                    items.add(armor);
                }
            }
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && offhand.getType() != Material.AIR) {
                items.add(offhand);
            }

            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
            player.setGameMode(GameMode.SPECTATOR);

            boolean created = WeaponListener.createCorpseChest(player.getLocation(), items);
            if (!created) {
                sender.sendMessage("시체 상자를 생성할 수 없습니다.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("시체상자청소")) {
            WeaponListener.removeAllCorpseChests();
            sender.sendMessage("시체 상자 정리를 완료했습니다.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("단서숨기기")) {
            if (sender instanceof Player player) {
                boolean success = ClueManager.placeCluesOnce(player);
                if (!success) {
                    sender.sendMessage("단서 숨기기에 실패했습니다. 범위/상자 상태를 확인해주세요.");
                }
                return true;
            }

            boolean success = ClueManager.placeCluesOnce(null);
            if (!success) {
                sender.sendMessage("단서 숨기기에 실패했습니다. 범위/상자 상태를 확인해주세요.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("단서아이템")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용할 수 있습니다.");
                return true;
            }
            player.getInventory().addItem(ClueManager.createClueItem());
            player.sendMessage("단서 아이템 1개를 지급했습니다.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("낮시간위치")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("플레이어만 사용할 수 있습니다.");
                return true;
            }
            Location location = player.getLocation().getBlock().getLocation();
            GameManager.setDayTeleportLocation(location);
            saveDayTeleportLocation(location);
            sender.sendMessage("낮 시간 이동 위치를 저장했습니다: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            return true;
        }

        if (command.getName().equalsIgnoreCase("랜덤티피")) {
            boolean success = ClueManager.teleportPlayersInRegion(GameManager.getPlayers(), 65, 68);
            if (!success) {
                sender.sendMessage("랜덤 티피에 실패했습니다. 플레이 구역을 먼저 설정하세요.");
            }
            return true;
        }

        return false;
    }
}
