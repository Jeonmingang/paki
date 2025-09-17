
package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PachinkoCommand implements CommandExecutor {

    private final MachineManager mm;

    public PachinkoCommand(MachineManager mm) { this.mm = mm; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("도움말")) {
            help(sender);
            return true;
        }
        if (sub.equalsIgnoreCase("설치")) {
            if (!(sender instanceof Player) || !sender.isOp()) { Text.msg(sender, "&c오피만 사용 가능합니다."); return true; }
            if (args.length < 2) { Text.msg(sender, "&c사용법: /파칭코 설치 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            if (id <= 0) { Text.msg(sender, "&c기계번호는 자연수로 입력하세요."); return true; }
            Player p = (Player)sender;
            Machine m = mm.installMachine(p, id);
            Text.msg(sender, "&a설치 완료: #" + m.getId());
            return true;
        }
        if (sub.equalsIgnoreCase("삭제")) {
            if (!(sender instanceof Player) || !sender.isOp()) { Text.msg(sender, "&c오피만 사용 가능합니다."); return true; }
            if (args.length < 2) { Text.msg(sender, "&c사용법: /파칭코 삭제 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            if (!mm.deleteMachine((Player)sender, id)) {
                Text.msg(sender, "&c해당 기계를 찾을 수 없습니다.");
            }
            return true;
        }
        if (sub.equalsIgnoreCase("목록")) {
            StringBuilder sb = new StringBuilder("&7생성된 기계: ");
            boolean first = true;
            for (Integer id : mm.getMachines().keySet()) {
                if (!first) sb.append(", ");
                sb.append("#").append(id);
                first = false;
            }
            Text.msg(sender, sb.toString());
            return true;
        }
        if (sub.equalsIgnoreCase("설정")) {
            if (!sender.isOp()) { Text.msg(sender, "&c오피만 사용 가능합니다."); return true; }
            if (args.length >= 3 && args[1].equalsIgnoreCase("구슬")) {
                int id = parseInt(args[2], -1);
                Machine m = mm.get(id);
                if (m == null) { Text.msg(sender, "&c기계를 찾을 수 없습니다."); return true; }
                if (!(sender instanceof Player)) { Text.msg(sender, "&c플레이어만 가능합니다."); return true; }
                Player p = (Player)sender;
                ItemStack it = p.getInventory().getItemInMainHand();
                if (it == null || it.getType().isAir()) { Text.msg(sender, "&c손에 설정할 아이템을 들어주세요."); return true; }
                m.setBallItem(it.clone());
                mm.saveMachines();
                Text.msg(sender, "&a기계 #" + id + " 구슬 아이템 설정 완료 (이름/로어 일치 검사).");
                return true;
            }
            Text.msg(sender, "&c사용법: /파칭코 설정 구슬 <기계번호> (손에 든 아이템 적용)");
            return true;
        }
        if (sub.equalsIgnoreCase("구슬지급")) {
            if (!sender.isOp()) { Text.msg(sender, "&c오피만 사용 가능합니다."); return true; }
            if (args.length < 4) { Text.msg(sender, "&c사용법: /파칭코 구슬지급 <플레이어> <개수> <기계번호>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount = parseInt(args[2], 0);
            int id = parseInt(args[3], -1);
            Machine m = mm.get(id);
            if (m == null) { Text.msg(sender, "&c기계를 찾을 수 없습니다."); return true; }
            mm.giveMarble((sender instanceof Player) ? (Player)sender : null, target, amount, m);
            Text.msg(sender, "&a지급 완료.");
            return true;
        }
        if (sub.equalsIgnoreCase("랭킹")) {
            java.util.List<java.util.Map.Entry<String,Integer>> top = mm.topWins(10);
            Text.msg(sender, "&6==== 파칭코 랭킹 TOP 10 ====");
            int i=1; for (java.util.Map.Entry<String,Integer> e : top) {
                Text.msg(sender, "&e"+(i++)+". &f"+e.getKey()+" &7- &b"+e.getValue()+"점");
            }
            if (top.isEmpty()) Text.msg(sender, "&7기록이 없습니다.");
            return true;
        }
        if (sub.equalsIgnoreCase("테스트")) {
            if (!(sender instanceof Player)) { Text.msg(sender, "&c플레이어만 가능합니다."); return true; }
            if (args.length < 2) { Text.msg(sender, "&c사용법: /파칭코 테스트 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Machine m = mm.get(id);
            if (m == null) { Text.msg(sender, "&c기계를 찾을 수 없습니다."); return true; }
            mm.launchBall((Player)sender, m);
            return true;
        }
        if (sub.equalsIgnoreCase("보상")) {
            if (!(sender instanceof Player)) { Text.msg(sender, "&c플레이어만 가능합니다."); return true; }
            if (args.length < 2) { Text.msg(sender, "&c사용법: /파칭코 보상 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Machine m = mm.get(id);
            if (m == null) { Text.msg(sender, "&c기계를 찾을 수 없습니다."); return true; }
            mm.payOut((Player)sender, m);
            return true;
        }
        help(sender);
        return true;
    }

    private void help(CommandSender s) {
        Text.msg(s, "&b/파칭코 설치 <번호> &7- 기계 생성 (오피)");
        Text.msg(s, "&b/파칭코 삭제 <번호> &7- 기계 제거 (오피)");
        Text.msg(s, "&b/파칭코 목록 &7- 모든 기계 번호");
        Text.msg(s, "&b/파칭코 설정 구슬 <번호> &7- 해당 기계 구슬 아이템 설정 (오피)");
        Text.msg(s, "&b/파칭코 구슬지급 <플레이어> <개수> <번호> &7- 기계의 구슬 지급 (오피)");
        Text.msg(s, "&b/파칭코 테스트 <번호> &7- 공 시뮬레이션 실행");
        Text.msg(s, "&b/파칭코 보상 <번호> &7- 적립된 보상 구슬 수령");
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
