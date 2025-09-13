package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            p.sendMessage("§7/파칭코 설치 <슬롯개수> | 삭제 <번호> | 목록 | 설정 구슬[ <번호>] | 구슬지급 <닉> <개수> | 리로드");
            return true;
        }

        if (a[0].equalsIgnoreCase("리로드")){
            plugin.reloadConfig();
            plugin.getLucky().reload();
            try{ plugin.updateAllSigns(); }catch(Exception ignored){}
            p.sendMessage("§aPachinko 설정을 리로드했습니다. (표지판 갱신 완료)");
            return true;
        }

        if (a[0].equalsIgnoreCase("구슬지급") && a.length>=3){
            Player tgt = Bukkit.getPlayerExact(a[1]);
            if (tgt==null){ p.sendMessage("§c해당 플레이어를 찾을 수 없습니다."); return true; }
            int amt;
            try{ amt = Integer.parseInt(a[2]); }catch(Exception e){ p.sendMessage("§c개수는 정수여야 합니다."); return true; }
            ItemStack ball = plugin.createBallItemWith(null, Math.max(1, amt));
            tgt.getInventory().addItem(ball);
            p.sendMessage("§a" + tgt.getName() + "에게 구슬 " + amt + "개 지급");
            return true;
        }

        if (a[0].equalsIgnoreCase("설치") && a.length>=2){
            try{
                int cols = Integer.parseInt(a[1]);
                cols = Math.max(3, Math.min(13, cols));
                Location base = p.getLocation().getBlock().getLocation();
                int id = plugin.getRegistry().addWithCols(base, cols);
                plugin.getRegistry().saveToConfig();
                p.sendMessage("§a파칭코 #" + id + " 설치 완료 (슬롯 " + cols + ")");
            }catch(Exception e){ p.sendMessage("§c슬롯개수는 정수여야 합니다."); }
            return true;
        }

        if (a[0].equalsIgnoreCase("삭제") && a.length>=2){
            try{
                int id = Integer.parseInt(a[1]);
                boolean ok = plugin.getRegistry().remove(id);
                if (!ok){ p.sendMessage("§c해당 번호의 기계가 없습니다."); return true; }
                plugin.getRegistry().saveToConfig();
                p.sendMessage("§c파칭코 #" + id + " 삭제 완료");
            }catch(Exception e){ p.sendMessage("§c번호는 정수여야 합니다."); }
            return true;
        }

        if (a[0].equalsIgnoreCase("목록")){
            p.sendMessage("§e[파칭코 목록]");
            for (Machine m : plugin.getRegistry().all()){
                Location b = m.base;
                p.sendMessage(String.format("  §f#%d §7@ %s (%d,%d,%d) slots=%d §8| 중앙:%d%% 잭팟:%d%%",
                        m.id, b.getWorld().getName(), b.getBlockX(), b.getBlockY(), b.getBlockZ(), m.cols,
                        plugin.getGlobalCenterHitPercent(), plugin.getGlobalJackpotPercent()));
            }
            return true;
        }

        if (a[0].equalsIgnoreCase("설정") && a.length>=2 && a[1].equalsIgnoreCase("구슬")){
            if (p.getInventory().getItemInMainHand()==null || p.getInventory().getItemInMainHand().getType()==Material.AIR){
                p.sendMessage("§c손에 아이템을 들고 실행하세요.");
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            String display = (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) ? hand.getItemMeta().getDisplayName().replace("§","&") : "";
            java.util.List<String> lore = (hand.hasItemMeta() && hand.getItemMeta().hasLore()) ? hand.getItemMeta().getLore() : new java.util.ArrayList<String>();

            if (a.length>=3){
                try{
                    int id = Integer.parseInt(a[2]);
                    Machine m = plugin.getRegistry().get(id);
                    if (m==null){ p.sendMessage("§c해당 번호의 기계가 없습니다."); return true; }
                    m.ballItem = hand.getType().name();
                    m.ballName = display;
                    m.ballLore = lore;
                    plugin.getRegistry().saveToConfig();
                    p.sendMessage(plugin.getConfig().getString("messages.set-ball-machine")
                            .replace("{id}", String.valueOf(id))
                            .replace("{mat}", m.ballItem)
                            .replace("{name}", m.ballName==null?"":m.ballName)
                            .replace("{lore}", String.valueOf(m.ballLore)).replace("&","§"));
                    return true;
                }catch(Exception e){ p.sendMessage("§c번호는 정수여야 합니다."); return true; }
            } else {
                plugin.setBallFromItem(hand);
                String name = plugin.getBallName();
                java.util.List<String> loreCfg = plugin.getBallLore();
                p.sendMessage(("§a구슬 설정 완료: §f" + plugin.getBallMaterial().name()
                        + " §7/ 이름: §f" + ((name==null||name.isEmpty())?"(없음)":name.replace("&","§"))
                        + " §7/ 로어: §f" + ((loreCfg==null||loreCfg.isEmpty())?"(없음)":String.valueOf(loreCfg).replace("&","§"))));
                return true;
            }
        }

        p.sendMessage("§7/파칭코 설치 <슬롯개수> | 삭제 <번호> | 목록 | 설정 구슬[ <번호>] | 구슬지급 <닉> <개수> | 리로드");
        return true;
    }
}
