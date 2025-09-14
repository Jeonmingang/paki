
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PCommand implements CommandExecutor {
    private final Main plugin;
    public PCommand(Main p){ this.plugin = p; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) { s.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player)s;

        if (a.length>=1 && a[0].equalsIgnoreCase("설치")){
            if (a.length<2){ p.sendMessage("§7/파칭코 설치 <슬롯개수>"); return true; }
            int slots;
            try{ slots = Integer.parseInt(a[1]); }catch(Exception ex){ p.sendMessage("§c숫자를 입력하세요."); return true; }
            Location base = p.getLocation().getBlock().getLocation();
            base = base.add(0, 0, 0);
            Machine m = plugin.getRegistry().create(base, slots);
            plugin.getRegistry().saveAll();
            p.sendMessage("§a파칭코 설치 완료: §f#" + m.id + " §7슬롯=" + m.cols);
            return true;
        }

        if (a.length>=1 && a[0].equalsIgnoreCase("삭제")){
            if (a.length<2){ p.sendMessage("§7/파칭코 삭제 <번호>"); return true; }
            try{
                int id = Integer.parseInt(a[1]);
                boolean ok = plugin.getRegistry().remove(id);
                if (ok){ plugin.getRegistry().saveAll(); p.sendMessage("§c삭제됨: §f#" + id); }
                else p.sendMessage("§c해당 ID의 기계가 없습니다.");
            }catch(Exception ex){ p.sendMessage("§c숫자를 입력하세요."); }
            return true;
        }

        if (a.length>=1 && a[0].equalsIgnoreCase("목록")){
            if (plugin.getRegistry().all().isEmpty()){ p.sendMessage("§7설치된 파칭코가 없습니다."); return true; }
            for (Machine m : plugin.getRegistry().all()){
                p.sendMessage("§f#" + m.id + " §7@ " + m.base.getBlockX()+","+m.base.getBlockY()+","+m.base.getBlockZ()+" §7슬롯="+m.cols);
            }
            return true;
        }

        if (a.length>=1 && a[0].equalsIgnoreCase("리로드")){
            plugin.reloadConfig();
            p.sendMessage("§a구성 리로드 완료.");
            return true;
        }

        if (a.length>=1 && a[0].equalsIgnoreCase("슬롯")){
            // Run slot manually
            RunBall.runSlots(plugin, p,
                    () -> plugin.getLucky().onJackpotAndBall(p, true),
                    () -> plugin.getLucky().onJackpotAndBall(p, false));
            return true;
        }

        if (a.length>=1 && a[0].equalsIgnoreCase("설정")){ p.sendMessage("§7/파칭코 설정 구슬 [미구현]"); return true; }
if (a.length>=1 && a[0].equalsIgnoreCase("구슬지급")){
    if (a.length<3){ p.sendMessage("§7/파칭코 구슬지급 <닉> <개수>"); return true; }
    String nick = a[1]; int cnt; try{ cnt=Integer.parseInt(a[2]); }catch(Exception ex){ p.sendMessage("§c개수 숫자!"); return true; }
    p.sendMessage("§a(더미) " + nick + "에게 구슬 " + cnt + "개 지급 처리.");
    return true;
}
p.sendMessage("§7/파칭코 설치 <슬롯개수> | 삭제 <번호> | 목록 | 슬롯 | 설정 구슬 | 구슬지급 <닉> <개수> | 리로드");
        return true;
    }
}
