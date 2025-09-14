package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PCommand implements CommandExecutor {
    private final Main plugin;
    public PCommand(Main p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("파칭코")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0){
            help(p);
            return true;
        }

        // 설치
        if (args[0].equalsIgnoreCase("설치")) {
            if (args.length < 2){ p.sendMessage("§c/파칭코 설치 <슬롯개수>"); return true; }
            int cols;
            try{ cols = Integer.parseInt(args[1]); }catch(Exception e){ p.sendMessage("§c숫자를 입력하세요."); return true; }
            Location base = p.getLocation().getBlock().getLocation();
            int id = plugin.getRegistry().addWithCols(base, cols);
            plugin.getRegistry().saveToConfig();
            p.sendMessage("§a파칭코 설치 완료! 번호: §e"+id+" §7(슬롯: "+cols+")");
            return true;
        }

        // 삭제
        if (args[0].equalsIgnoreCase("삭제")){
            if (args.length < 2){ p.sendMessage("§c/파칭코 삭제 <번호>"); return true; }
            int id;
            try{ id = Integer.parseInt(args[1]); }catch(Exception e){ p.sendMessage("§c번호는 숫자여야 합니다."); return true; }
            if (plugin.getRegistry().remove(id)){
                plugin.getRegistry().saveToConfig();
                p.sendMessage("§a파칭코 §e"+id+"§a 번을 삭제했습니다.");
            }else{
                p.sendMessage("§c해당 번호의 기계를 찾을 수 없습니다.");
            }
            return true;
        }

        // 목록
        if (args[0].equalsIgnoreCase("목록")){
            if (plugin.getRegistry().size()==0){
                p.sendMessage("§7등록된 기계가 없습니다.");
            }else{
                for (Machine m : plugin.getRegistry().all()){
                    p.sendMessage("§7- §eID "+m.id+" §7@ §f"+m.base.getWorld().getName()+" "+m.base.getBlockX()+","+m.base.getBlockY()+","+m.base.getBlockZ()+" §7("+m.cols+"슬롯)");
                }
            }
            return true;
        }

        // 설정 구슬 [번호]
        if (args[0].equalsIgnoreCase("설정") && args.length>=2 && args[1].equalsIgnoreCase("구슬")){
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType()== Material.AIR){ p.sendMessage("§c손에 아이템을 들어주세요."); return true; }

            if (args.length >= 3){
                // per machine
                int id;
                try{ id = Integer.parseInt(args[2]); }catch(Exception e){ p.sendMessage("§c번호는 숫자여야 합니다."); return true; }
                Machine m = plugin.getRegistry().get(id);
                if (m==null){ p.sendMessage("§c해당 번호의 기계를 찾을 수 없습니다."); return true; }
                // set machine's ball from hand
                m.ballItem = hand.getType().name();
                org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
                m.ballName = (meta!=null && meta.hasDisplayName()) ? meta.getDisplayName() : null;
                java.util.List<String> lore = (meta!=null && meta.hasLore()) ? meta.getLore() : null;
                m.ballLore = (lore==null || lore.isEmpty()) ? null : new java.util.ArrayList<String>(lore);
                plugin.getRegistry().saveToConfig();
                plugin.getRegistry().refreshSigns();
                p.sendMessage("§a기계 §e"+id+" §a구슬을 손에 든 아이템으로 설정했습니다.");
            }else{
                // global
                plugin.setBallFromItem(hand);
                plugin.getRegistry().refreshSigns();
                p.sendMessage("§a전역 구슬을 손에 든 아이템으로 설정했습니다.");
            }
            return true;
        }

        // 구슬지급 <닉> <개수> [<슬롯>]
        if (args[0].equalsIgnoreCase("구슬지급")){
            if (args.length < 3){ p.sendMessage("§c/파칭코 구슬지급 <닉> <개수> [<슬롯ID>]"); return true; }
            Player tgt = Bukkit.getPlayerExact(args[1]);
            if (tgt == null){ p.sendMessage("§c해당 플레이어를 찾을 수 없습니다."); return true; }
            int amt;
            try{ amt = Integer.parseInt(args[2]); }catch(Exception e){ p.sendMessage("§c개수는 정수여야 합니다."); return true; }

            Machine m = null;
            if (args.length >= 4){
                try{ int id = Integer.parseInt(args[3]); m = plugin.getRegistry().get(id); }catch(Exception ignored){}
                if (args[3].equalsIgnoreCase("global") || args[3].equalsIgnoreCase("전역")) m = null;
            }
            ItemStack ball = plugin.createBallItemWith(m, amt);
            tgt.getInventory().addItem(ball);
            p.sendMessage("§a"+tgt.getName()+" §7에게 §e"+amt+"§7개 지급 ("+(m==null?"전역":("ID "+m.id))+").");
            return true;
        }

        // 리로드
        if (args[0].equalsIgnoreCase("리로드")){
            plugin.reloadConfig();
            plugin.getLucky().reload();
            plugin.getRegistry().refreshSigns();
            p.sendMessage("§aPachinko 설정을 리로드했습니다.");
            return true;
        }

        help(p);
        return true;
    }

    private void help(Player p){
        p.sendMessage("§7/파칭코 설치 <슬롯개수> | 삭제 <번호> | 목록 | 설정 구슬[ <번호>] | 구슬지급 <닉> <개수> [<슬롯ID>] | 리로드");
    }
}
