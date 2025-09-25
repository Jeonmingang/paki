
package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PachinkoCommand implements CommandExecutor {

    private final Main plugin;
    private final MachineManager manager;

    public PachinkoCommand(Main plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMachineManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Text.prefix(plugin) + "/파칭코 <설치|시작|구슬지급|목록|리로드|설정>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("리로드")) {
            plugin.reloadConfig();
            manager.saveAll(plugin.getMachinesConfig());
            try { plugin.getMachinesConfig().save(manager.getMachinesFile()); } catch (Exception ignored) {}
            sender.sendMessage(Text.prefix(plugin) + "리로드 완료");
            return true;
        }

        if (sub.equals("목록")) {
            Set<Integer> ids = manager.getMachines().keySet();
            String list = ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(", "));
            sender.sendMessage(Text.prefix(plugin) + "기계 ID: " + list);
            return true;
        }

        if (sub.equals("설치")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("플레이어만 사용 가능합니다.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(Text.prefix(plugin) + "/파칭코 설치 <기계번호>");
                return true;
            }
            int id;
            try { id = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                sender.sendMessage(Text.prefix(plugin) + "기계번호는 숫자여야 합니다.");
                return true;
            }
            Player p = (Player) sender;
            Machine m = manager.installMachine(p, id);
            if (m != null) sender.sendMessage(Text.prefix(plugin) + "기계 #" + id + " 설치 완료 (" + m.getFacing().name() + ")");
            else sender.sendMessage(Text.prefix(plugin) + "설치 실패: 동일 ID 존재");
            return true;
        }

        if (sub.equals("시작")) {
            if (args.length < 2) {
                sender.sendMessage(Text.prefix(plugin) + "/파칭코 시작 <기계번호>");
                return true;
            }
            int id;
            try { id = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                sender.sendMessage(Text.prefix(plugin) + "기계번호는 숫자여야 합니다.");
                return true;
            }
            Machine m = manager.getMachine(id);
            if (m == null) {
                sender.sendMessage(Text.prefix(plugin) + "해당 ID의 기계가 없습니다.");
                return true;
            }
            Player asPlayer = sender instanceof Player ? (Player) sender : null;
            m.beginStage(asPlayer);
            return true;
        }

        if (sub.equals("구슬지급")) {
            if (args.length < 3) {
                sender.sendMessage(Text.prefix(plugin) + "/파칭코 구슬지급 <닉> <개수> [<기계번호>]");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Text.prefix(plugin) + "해당 플레이어를 찾을 수 없습니다.");
                return true;
            }
            int amount;
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                sender.sendMessage(Text.prefix(plugin) + "개수는 숫자여야 합니다.");
                return true;
            }
            Integer machineId = null;
            if (args.length >= 4) {
                try { machineId = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
            }
            manager.giveBalls(target, amount, machineId);
            sender.sendMessage(Text.prefix(plugin) + target.getName() + " 에게 구슬 " + amount + "개 지급");
            return true;
        }

        if (sub.equals("설정")) {
            // /파칭코 설정 구슬 <기계번호> <손|기본|잠금 [켜|끄]>
            if (args.length < 2) {
                sender.sendMessage(Text.prefix(plugin) + "사용법: /파칭코 설정 구슬 <기계번호> <손|기본|잠금 [켜|끄]>");
                return true;
            }
            String what = args[1];
            if (!what.equals("구슬")) {
                sender.sendMessage(Text.prefix(plugin) + "설정 가능한 항목: 구슬");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Text.prefix(plugin) + "사용법: /파칭코 설정 구슬 <기계번호> <손|기본|잠금 [켜|끄]>");
                return true;
            }
            int id;
            try { id = Integer.parseInt(args[2]); } catch (Exception e) {
                sender.sendMessage(Text.prefix(plugin) + "기계번호는 숫자여야 합니다.");
                return true;
            }
            Machine m = manager.getMachine(id);
            if (m == null) {
                sender.sendMessage(Text.prefix(plugin) + "해당 ID의 기계를 찾을 수 없습니다.");
                return true;
            }
            if (args.length >= 4 && args[3].equalsIgnoreCase("손")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Text.prefix(plugin) + "플레이어만 사용할 수 있습니다.");
                    return true;
                }
                Player p = (Player) sender;
                boolean ok = manager.applyBallFromHand(m, p);
                if (ok) sender.sendMessage(Text.prefix(plugin) + "기계 #" + id + " 의 구슬 템플릿이 손 아이템으로 설정됨");
                else sender.sendMessage(Text.prefix(plugin) + "손에 든 아이템을 구슬로 설정할 수 없습니다.");
                return true;
            }
            if (args.length >= 4 && args[3].equalsIgnoreCase("기본")) {
                manager.resetBallTemplate(m);
                sender.sendMessage(Text.prefix(plugin) + "기계 #" + id + " 의 구슬 템플릿을 기본값으로 복구");
                return true;
            }
            if (args.length >= 4 && args[3].equalsIgnoreCase("잠금")) {
                if (args.length >= 5 && (args[4].equalsIgnoreCase("켜") || args[4].equalsIgnoreCase("on"))) {
                    m.setLockBallToMachine(true);
                } else if (args.length >= 5 && (args[4].equalsIgnoreCase("끄") || args[4].equalsIgnoreCase("off"))) {
                    m.setLockBallToMachine(false);
                } else {
                    sender.sendMessage(Text.prefix(plugin) + "사용법: /파칭코 설정 구슬 " + id + " 잠금 <켜|끄>");
                    return true;
                }
                manager.saveAll(plugin.getMachinesConfig());
                try { plugin.getMachinesConfig().save(manager.getMachinesFile()); } catch (Exception ignored) {}
                sender.sendMessage(Text.prefix(plugin) + "잠금: " + (m.isLockBallToMachine() ? "켜짐" : "꺼짐"));
                return true;
            }
            sender.sendMessage(Text.prefix(plugin) + "사용법: /파칭코 설정 구슬 <기계번호> <손|기본|잠금 [켜|끄]>");
            return true;
        }

        sender.sendMessage(Text.prefix(plugin) + "/파칭코 <설치|시작|구슬지급|목록|리로드|설정>");
        return true;
    }
}
