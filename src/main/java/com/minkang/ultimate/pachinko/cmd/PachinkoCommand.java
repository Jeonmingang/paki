package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.RankingService;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PachinkoCommand implements CommandExecutor {
    private final Main plugin;
    public PachinkoCommand(Main plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0){
            help(sender);
            return true;
        }
        String sub = args[0];

        if (sub.equalsIgnoreCase("리로드")){
            plugin.reloadAll();
            Text.msg(sender, "&a리로드 완료");
            return true;
        }
        if (sub.equalsIgnoreCase("목록")){
            for (Machine m : plugin.machines().all()){
                Text.msg(sender, "&7#"+m.getId()+" &f@ "+fmt(m.getBase()));
            }
            return true;
        }
        if (sub.equalsIgnoreCase("랭킹")){
            RankingService rs = plugin.ranks();
            List<RankingService.RankEntry> list = rs.topByClears(10);
            Text.msg(sender, "&6==== &e파칭코 랭킹 (클리어 기준) &6====");
            int i=1;
            for (RankingService.RankEntry r : list){
                Text.msg(sender, "&e"+(i++)+". &f"+r.name+" &7- clears: &b"+r.clears+" &7| bestStage: &a"+r.bestStage);
            }
            return true;
        }
        if (sub.equalsIgnoreCase("강제해제")){
            if (!sender.isOp()){
                Text.msg(sender, "&c권한이 필요합니다.");
                return true;
            }
            if (args.length < 2){ Text.msg(sender, "&7사용법: /파칭코 강제해제 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Machine m = plugin.machines().byId(id);
            if (m == null){ Text.msg(sender, "&c해당 기계를 찾을 수 없습니다."); return true; }
            m.setOperator(null);
            Text.msg(sender, "&a기계 #"+id+" 락 해제 완료");
            return true;
        }
        if (sub.equalsIgnoreCase("상태")){
            if (args.length < 2){ Text.msg(sender, "&7사용법: /파칭코 상태 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Machine m = plugin.machines().byId(id);
            if (m == null){ Text.msg(sender, "&c해당 기계를 찾을 수 없습니다."); return true; }
            String op = (m.getOperator()==null) ? "&7(없음)" : String.valueOf(m.getOperator());
            Text.msg(sender, "&f#"+id+" &7- stage: &e"+m.getStage()+" &7/ cup: &e"+m.getStagePayout()+"/"+m.getStageCup()+" &7/ operator: &e"+op);
            return true;
        }
        if (sub.equalsIgnoreCase("설정")){
            if (!sender.isOp()){
                Text.msg(sender, "&c권한이 필요합니다.");
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("구슬")){
                if (!(sender instanceof Player)){
                    Text.msg(sender, "&c플레이어만 사용할 수 있습니다.");
                    return true;
                }
                if (args.length < 3){ Text.msg(sender, "&7사용법: /파칭코 설정 구슬 <기계번호>"); return true; }
                int id = parseInt(args[2], -1);
                Machine m = plugin.machines().byId(id);
                if (m == null){ Text.msg(sender, "&c해당 기계를 찾을 수 없습니다."); return true; }
                Player p = (Player) sender;
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR){
                    Text.msg(sender, "&c손에 아이템을 들고 실행하세요.");
                    return true;
                }
                String type = hand.getType().name();
                ItemMeta im = hand.getItemMeta();
                String name = im != null && im.hasDisplayName() ? im.getDisplayName() : null;
                List<String> lore = im != null && im.hasLore() ? im.getLore() : new ArrayList<>();
                m.setBallSpec(type, name, lore);
                plugin.machines().save();
                Text.msg(sender, "&a기계 #"+id+" 전용 구슬을 설정했습니다. &7("+type+")");
                return true;
            }
            Text.msg(sender, "&7사용법: /파칭코 설정 구슬 <기계번호>");
            return true;
        }

        help(sender);
        return true;
    }

    private void help(CommandSender s){
        Text.msg(s, "&6/파칭코 &7리로드, 목록, 랭킹, 상태 <id>, 강제해제 <id>, 설정 구슬 <id>");
    }

    private String fmt(Location l){
        return l.getWorld().getName()+" "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    private int parseInt(String s, int def){
        try { return Integer.parseInt(s); }catch(Exception e){ return def; }
    }
}
