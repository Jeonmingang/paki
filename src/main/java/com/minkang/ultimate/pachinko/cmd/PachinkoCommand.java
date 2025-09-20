
package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PachinkoCommand implements CommandExecutor {
    private final MachineManager mm;
    public PachinkoCommand(MachineManager mm){ this.mm = mm; }

    private int toInt(String s, int def){ try { return Integer.parseInt(s); } catch (Exception e){ return def; } }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (args.length == 0 || args[0].equalsIgnoreCase("도움말")){
            Text.msg(sender, "&e/파칭코 설치 <번호> &7- 현재 위치에 설치");
            Text.msg(sender, "&e/파칭코 삭제 <번호> &7- 제거");
            Text.msg(sender, "&e/파칭코 목록 &7- 설치된 기계 목록");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("설치")){
            if (!(sender instanceof Player) || !sender.isOp()){ Text.msg(sender, "&c오피만 사용 가능합니다."); return true; }
            if (args.length < 2){ Text.msg(sender, "&c사용법: /파칭코 설치 <번호>"); return true; }
            int id = toInt(args[1], -1);
            if (id < 0){ Text.msg(sender, "&c번호는 0 이상의 정수여야 합니다."); return true; }
            Machine m = mm.installMachine((Player)sender, id);
            Text.msg(sender, "&a설치 완료: #" + m.getId());
            return true;
        }
        if (sub.equalsIgnoreCase("삭제")){
            if (!(sender instanceof Player) || !sender.isOp()){ Text.msg(sender, "&c오피만 사용 가능합니다."); return true; }
            if (args.length < 2){ Text.msg(sender, "&c사용법: /파칭코 삭제 <번호>"); return true; }
            int id = toInt(args[1], -1);
            if (!mm.deleteMachine((Player)sender, id)){ Text.msg(sender, "&c해당 기계를 찾을 수 없습니다."); }
            else Text.msg(sender, "&c삭제 완료: #" + id);
            return true;
        }
        if (sub.equalsIgnoreCase("목록")){
            StringBuilder sb = new StringBuilder("&7생성된 기계: ");
            boolean first = true;
            for (Integer i : mm.getMachines().keySet()){
                if(!first) sb.append(", ");
                first=false;
                sb.append(i);
            }
            Text.msg(sender, sb.toString());
            return true;
        }
        Text.msg(sender, "&7알 수 없는 하위명령입니다. /파칭코 도움말");
        return true;
    }
}
