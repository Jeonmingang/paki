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
            p.sendMessage("§7/파칭코 설치 <번호> | 삭제 <번호> | 목록 | 설정 구슬 | 리로드");
            return true;
        }

        if (a[0].equalsIgnoreCase("리로드")){
            plugin.reloadConfig();
            p.sendMessage("§aPachinko 설정을 리로드했습니다.");
            return true;
        }

        if (a[0].equalsIgnoreCase("설치") && a.length>=2){
            try{
                int id = Integer.parseInt(a[1]);
                boolean ok = plugin.getRegistry().add(id, p.getLocation().getBlock().getLocation());
                if (!ok){ p.sendMessage("§c이미 존재하는 번호입니다."); return true; }
                plugin.getRegistry().saveToConfig();
                p.sendMessage(plugin.getConfig().getString("messages.created").replace("{id}", String.valueOf(id)).replace("&","§"));
            }catch(Exception e){ p.sendMessage("§c번호는 정수여야 합니다."); }
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
                p.sendMessage(String.format("  §f#%d §7@ %s (%d,%d,%d)",
                        m.id, b.getWorld().getName(), b.getBlockX(), b.getBlockY(), b.getBlockZ()));
            }
            return true;
        }

        if (a[0].equalsIgnoreCase("설정") && a.length>=2 && a[1].equalsIgnoreCase("구슬")){
            Material mat = p.getInventory().getItemInMainHand()!=null
                    ? p.getInventory().getItemInMainHand().getType() : Material.AIR;
            if (mat==Material.AIR){ p.sendMessage("§c손에 아이템을 들고 실행하세요."); return true; }
            plugin.setBallMaterial(mat);
            p.sendMessage(plugin.getConfig().getString("messages.set-ball").replace("{mat}", mat.name()).replace("&","§"));
            return true;
        }

        p.sendMessage("§7/파칭코 설치 <번호> | 삭제 <번호> | 목록 | 설정 구슬 | 리로드");
        return true;
    }
}
