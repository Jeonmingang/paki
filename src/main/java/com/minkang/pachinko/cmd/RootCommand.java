package com.minkang.pachinko.cmd;

import com.minkang.pachinko.PachinkoPlugin;
import com.minkang.pachinko.game.Machine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class RootCommand implements CommandExecutor, TabCompleter {

    private final PachinkoPlugin plugin;

    public RootCommand(PachinkoPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 설치 <번호>");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 설정 구슬 <번호>");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 리로드");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 랭킹");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 나가기");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("리로드")) {
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + "파칭코/슬롯 설정 리로드 완료.");
            return true;
        }
        if (sub.equalsIgnoreCase("설치")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용"); return true; }
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED+"사용법: /파칭코 설치 <번호>"); return true; }
            int id;
            try { id = Integer.parseInt(args[1]); } catch (Exception e) { sender.sendMessage(ChatColor.RED+"번호는 정수"); return true; }
            Player p = (Player) sender;
            Machine m = plugin.getMachineManager().createTemplate(id, p);
            if (m == null) sender.sendMessage(ChatColor.RED+"이미 존재하는 번호입니다.");
            else sender.sendMessage(ChatColor.GREEN+"파칭코 #"+id+" 설치 완료.");
            return true;
        }
        if (sub.equalsIgnoreCase("설정")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용"); return true; }
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
            if (args.length < 3 || !args[1].equalsIgnoreCase("구슬")) {
                sender.sendMessage(ChatColor.RED+"사용법: /파칭코 설정 구슬 <번호>"); return true;
            }
            int id;
            try { id = Integer.parseInt(args[2]); } catch (Exception e) { sender.sendMessage(ChatColor.RED+"번호는 정수"); return true; }
            Player p = (Player) sender;
            org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == org.bukkit.Material.AIR) { sender.sendMessage(ChatColor.RED+"손에 아이템을 들고 실행하세요."); return true; }
            Machine m = plugin.getMachineManager().getMachine(id);
            if (m == null) { sender.sendMessage(ChatColor.RED+"해당 번호의 기계가 없습니다."); return true; }
            m.setExclusiveBall(hand);
            plugin.getMachineManager().saveAll();
            sender.sendMessage(ChatColor.GREEN+"기계 #"+id+" 전용 구슬이 설정되었습니다. (이름/로어 포함)");
            return true;
        }
        if (sub.equalsIgnoreCase("랭킹")) {
            java.util.List<com.minkang.pachinko.game.RankingManager.Entry> top = plugin.getRankingManager().topN(10);
            sender.sendMessage(ChatColor.GOLD+"[파칭코 랭킹]");
            int i=1;
            for (com.minkang.pachinko.game.RankingManager.Entry e : top) {
                String name = Bukkit.getOfflinePlayer(e.player).getName();
                sender.sendMessage(ChatColor.YELLOW+""+i+". "+ChatColor.AQUA+name+ChatColor.GRAY+" - "+ChatColor.LIGHT_PURPLE+e.best);
                i++;
            }
            if (top.isEmpty()) sender.sendMessage(ChatColor.GRAY+"기록이 없습니다.");
            return true;
        }
        if (sub.equalsIgnoreCase("나가기")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용"); return true; }
            Player p = (Player) sender;
            Machine m = plugin.getMachineManager().getByOccupant(p.getUniqueId());
            if (m != null) { m.release(); sender.sendMessage(ChatColor.GREEN+"점유 해제되었습니다."); }
            else sender.sendMessage(ChatColor.GRAY+"점유 중인 기계가 없습니다.");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], Arrays.asList("설치","설정","리로드","랭킹","나가기"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("설치")) {
            return java.util.Collections.singletonList("<번호>");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("설정")) {
            return filter(args[1], java.util.Collections.singletonList("구슬"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("설정") && args[1].equalsIgnoreCase("구슬")) {
            return java.util.Collections.singletonList("<번호>");
        }
        return java.util.Collections.emptyList();
    }

    private List<String> filter(String token, List<String> src) {
        token = token==null?"" : token;
        List<String> out = new ArrayList<>();
        for (String s : src) if (s.startsWith(token)) out.add(s);
        return out;
    }
}
