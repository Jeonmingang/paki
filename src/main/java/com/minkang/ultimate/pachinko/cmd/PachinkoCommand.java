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
        if (sub.equalsIgnoreCase("설치") && sender instanceof Player){
            Player p = (Player) sender;
            World w = p.getWorld();
            Location base = p.getLocation().getBlock().getLocation();
            int id = (int) (System.currentTimeMillis() % 100000);
            plugin.machines().all(); // ensure load
            Text.msg(sender, "&a임시 설치: &7ID="+id+" @ "+fmt(base)+" &8(machines.yml 직접 편집 권장)");
            return true;
        }
        help(sender);
        return true;
    }

    private void help(CommandSender s){
        Text.msg(s, "&6/파칭코 &7리로드, 목록, 랭킹");
    }

    private String fmt(Location l){
        return l.getWorld().getName()+" "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }
}
