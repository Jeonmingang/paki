package com.minkang.ultimate.pachinko;

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
        if (!p.isOp() && !p.hasPermission("pachinko.admin")){
            p.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (a.length==0){
            p.sendMessage("§7/파칭코 설치 <슬롯개수> | 삭제 <번호> | 목록 | 설정 구슬 | 리로드");
            return true;
        }

        if (a[0].equalsIgnoreCase("리로드")){
            plugin.reloadConfig();
            p.sendMessage("§aPachinko 설정을 리로드했습니다.");
            return true;
        }

        if (a[0].equalsIgnoreCase("설치") && a.length>=2){
            try{
                int cols = Integer.parseInt(a[1]);
                cols = Math.max(3, Math.min(13, cols));
                int id = plugin.getRegistry().addWithCols(p.getLocation().getBlock().getLocation(), cols);
                plugin.getRegistry().saveToConfig();
                p.sendMessage(plugin.getConfig().getString("messages.created").replace("{id}", String.valueOf(id)).replace("{cols}", String.valueOf(cols)).replace("&","§"));
            }catch(Exception e){ p.sendMessage("§c슬롯개수는 정수여야 합니다."); }
            return true;
        }

        if (a[0].equalsIgnoreCase("삭제") && a.length>=2){
            try{
                int id = Integer.parseInt(a[1]);
                boolean ok = plugin.getRegistry().remove(id);
                if (!ok){ p.sendMessage("§c해당 번호의 기계가 없습니다."); return true; }
                plugin.getRegistry().saveToConfig();
                p.sendMessage(plugin.getConfig().getString("messages.removed").replace("{id}", String.valueOf(id)).replace("&","§"));
            }catch(Exception e){ p.sendMessage("§c번호는 정수여야 합니다."); }
            return true;
        }

        if (a[0].equalsIgnoreCase("목록")){
            p.sendMessage(plugin.getConfig().getString("messages.list-title").replace("&","§"));
            for (Machine m : plugin.getRegistry().all()){
                Location b = m.base;
                p.sendMessage(String.format("  §f#%d §7@ %s (%d,%d,%d) slots=%d",
                        m.id, b.getWorld().getName(), b.getBlockX(), b.getBlockY(), b.getBlockZ(), m.cols));
            }
            return true;
        }

        if (a[0].equalsIgnoreCase("설정") && a.length>=2 && a[1].equalsIgnoreCase("구슬")){
            if (p.getInventory().getItemInMainHand()==null || p.getInventory().getItemInMainHand().getType()==Material.AIR){
                p.sendMessage("§c손에 아이템을 들고 실행하세요.");
                return true;
            }
            plugin.setBallFromItem(p.getInventory().getItemInMainHand());
            String name = plugin.getBallName();
            java.util.List<String> lore = plugin.getBallLore();
            p.sendMessage(plugin.getConfig().getString("messages.set-ball")
                    .replace("{mat}", plugin.getBallMaterial().name())
                    .replace("{name}", (name==null||name.isEmpty())?"(없음)":name.replace("&","§"))
                    .replace("{lore}", (lore==null||lore.isEmpty())?"(없음)":String.valueOf(lore).replace("&","§"))
                    .replace("&","§"));
            return true;
        }

        p.sendMessage("§7/파칭코 설치 <슬롯개수> | 삭제 <번호> | 목록 | 설정 구슬 | 리로드");
        return true;
    }
}
