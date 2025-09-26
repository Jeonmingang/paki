
package com.example.pachinko.commands;

import com.example.pachinko.UltimatePachinko;
import com.example.pachinko.machine.Machine;
import com.example.pachinko.machine.MachineManager;
import com.example.pachinko.machine.StageConfig;
import com.example.pachinko.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PachinkoCommand implements CommandExecutor, TabCompleter {
    private final UltimatePachinko plugin;
    public PachinkoCommand(UltimatePachinko plugin){ this.plugin=plugin; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (args.length==0){ sender.sendMessage(Text.prefix()+"/파칭코 설치 | /파칭코 설정 구슬 <번호> | /파칭코 스테이지 추가|삭제 | /파칭코 랭킹 <번호> | /파칭코 리로드"); return true; }
        String sub=args[0]; MachineManager mm=plugin.machines();

        if (sub.equalsIgnoreCase("설치")){
            if (!(sender instanceof Player)){ sender.sendMessage("게임 내에서만 가능"); return true; }
            if (!sender.hasPermission("pachinko.admin")){ sender.sendMessage("권한 없음"); return true; }
            Player p=(Player)sender; Location base=p.getLocation().getBlock().getLocation();
            Machine m=mm.createMachine(base); sender.sendMessage(Text.prefix()+"기계 #"+m.getId()+" 설치됨.");
            mm.saveToConfig(); return true;
        }
        if (sub.equalsIgnoreCase("제거")){
            if (!sender.hasPermission("pachinko.admin")){ sender.sendMessage("권한 없음"); return true; }
            if (args.length<2){ sender.sendMessage(Text.prefix()+"/파칭코 제거 <번호>"); return true; }
            int id=Integer.parseInt(args[1]); boolean ok=mm.removeMachine(id);
            sender.sendMessage(Text.prefix()+(ok? "기계 #"+id+" 제거됨":"해당 번호의 기계를 찾을 수 없음")); mm.saveToConfig(); return true;
        }
        if (sub.equalsIgnoreCase("리로드")){ plugin.reloadAll(); sender.sendMessage(Text.prefix()+"config 리로드 완료"); return true; }

        if (sub.equalsIgnoreCase("설정")){
            if (args.length<2){ sender.sendMessage(Text.prefix()+"/파칭코 설정 구슬 <번호>"); return true; }
            if (args[1].equalsIgnoreCase("구슬")){
                if (!(sender instanceof Player)){ sender.sendMessage("게임 내에서만 가능"); return true; }
                if (!sender.hasPermission("pachinko.admin")){ sender.sendMessage("권한 없음"); return true; }
                if (args.length<3){ sender.sendMessage(Text.prefix()+"/파칭코 설정 구슬 <번호>"); return true; }
                int id=Integer.parseInt(args[2]); Machine m=mm.getById(id);
                if (m==null){ sender.sendMessage(Text.prefix()+"기계 #"+id+" 없음"); return true; }
                Player p=(Player)sender; ItemStack hand=p.getInventory().getItemInMainHand();
                if (hand==null || hand.getType()== Material.AIR){ sender.sendMessage(Text.prefix()+"손에 아이템을 들고 사용"); return true; }
                ItemStack ball=hand.clone(); ball.setAmount(1); m.setBallItem(ball); mm.saveToConfig();
                sender.sendMessage(Text.prefix()+"기계 #"+id+" 전용구슬 설정: "+ball.getType()); return true;
            }
        }

        if (sub.equalsIgnoreCase("스테이지")){
            if (!sender.hasPermission("pachinko.admin")){ sender.sendMessage("권한 없음"); return true; }
            if (args.length<2){ sender.sendMessage(Text.prefix()+"/파칭코 스테이지 추가 <번호> <cup> <업그레이드확률%> [burst]\n/파칭코 스테이지 삭제 <번호> <index>"); return true; }
            if (args[1].equalsIgnoreCase("추가")){
                if (args.length<5){ sender.sendMessage(Text.prefix()+"/파칭코 스테이지 추가 <번호> <cup> <확률%> [burst]"); return true; }
                int id=Integer.parseInt(args[2]); int cup=Integer.parseInt(args[3]); double up=Double.parseDouble(args[4]);
                int burst = (args.length>=6)? Integer.parseInt(args[5]) : plugin.getConfig().getInt("settings.payoutDefaultBurst",3);
                Machine m=mm.getById(id); if (m==null){ sender.sendMessage(Text.prefix()+"기계 #"+id+" 없음"); return true; }
                m.getStages().add(new StageConfig(cup, up, burst)); mm.saveToConfig(); sender.sendMessage(Text.prefix()+"추가됨: #"+id+" cup="+cup+" up="+up+"% burst="+burst); return true;
            }
            if (args[1].equalsIgnoreCase("삭제")){
                if (args.length<4){ sender.sendMessage(Text.prefix()+"/파칭코 스테이지 삭제 <번호> <index>"); return true; }
                int id=Integer.parseInt(args[2]); int idx=Integer.parseInt(args[3]); Machine m=mm.getById(id);
                if (m==null){ sender.sendMessage(Text.prefix()+"기계 #"+id+" 없음"); return true; }
                if (idx<=0 || idx>m.getStages().size()){ sender.sendMessage(Text.prefix()+"index: 1~"+m.getStages().size()); return true; }
                StageConfig rm=m.getStages().remove(idx-1); mm.saveToConfig(); sender.sendMessage(Text.prefix()+"삭제됨: "+rm.toString()); return true;
            }
        }

        if (sub.equalsIgnoreCase("랭킹")){
            if (args.length<2){ sender.sendMessage(Text.prefix()+"/파칭코 랭킹 <번호>"); return true; }
            int id=Integer.parseInt(args[1]); int limit=plugin.getConfig().getInt("settings.rankingSize",10);
            java.util.List<String> lines = getRanking(id, limit); if (lines.isEmpty()) sender.sendMessage(Text.prefix()+"랭킹 데이터가 없습니다.");
            else { sender.sendMessage(Text.prefix()+"기계 #"+id+" 랭킹 TOP"+limit); for (String s : lines) sender.sendMessage(Text.color(s)); }
            return true;
        }

        sender.sendMessage(Text.prefix()+"알 수 없는 하위 명령어"); return true;
    }

    private java.util.List<String> getRanking(int machineId, int limit){
        java.util.List<String> out=new java.util.ArrayList<>(); String base="ranking."+machineId;
        if (!plugin.getConfig().isConfigurationSection(base)) return out;
        java.util.Map<String,Object> map=plugin.getConfig().getConfigurationSection(base).getValues(false);
        java.util.List<java.util.Map.Entry<String,Object>> list=new java.util.ArrayList<>(map.entrySet());
        list.sort((a,b)->{ int av=plugin.getConfig().getInt(base+"."+a.getKey()+".bestStage",0); int bv=plugin.getConfig().getInt(base+"."+b.getKey()+".bestStage",0); return Integer.compare(bv,av); });
        int rank=1; for (java.util.Map.Entry<String,Object> e : list){ String path=base+"."+e.getKey();
            String name=plugin.getConfig().getString(path+".name", e.getKey().substring(0,8));
            int best=plugin.getConfig().getInt(path+".bestStage",0); out.add("&e"+rank+". &f"+name+" &7- 최고 스테이지 &a"+best);
            if (rank++>=limit) break; }
        return out;
    }

    @Override public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args){
        if (args.length==1) return java.util.Arrays.asList("설치","제거","설정","스테이지","랭킹","리로드");
        if (args.length==2 && args[0].equalsIgnoreCase("설정")) return java.util.Arrays.asList("구슬");
        if (args.length==2 && args[0].equalsIgnoreCase("스테이지")) return java.util.Arrays.asList("추가","삭제");
        return java.util.Collections.emptyList();
    }
}
