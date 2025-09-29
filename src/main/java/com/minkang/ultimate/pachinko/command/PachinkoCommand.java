package com.minkang.ultimate.pachinko.command;

import com.minkang.ultimate.pachinko.PachinkoPlugin;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PachinkoCommand implements CommandExecutor, TabCompleter {

    private final PachinkoPlugin plugin;

    public PachinkoCommand(PachinkoPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 설치 <번호> - 기계 설치 (OP)");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 설정 구슬 <번호> - 손 아이템을 전용 구슬로");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 랭킹 - 버스트 기록 TOP");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 나가기 - 점유 해제");
            sender.sendMessage(ChatColor.YELLOW + "/파칭코 리로드 - 설정 리로드");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("설치")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한 없음"); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "번호 필요"); return true; }
            String id = args[1];
            boolean ok = plugin.machines().installTemplate(id, (Player)sender);
            sender.sendMessage(ok ? ChatColor.GREEN + "설치 완료: " + id : ChatColor.RED + "이미 존재하는 번호입니다.");
            return true;
        } else if (sub.equalsIgnoreCase("설정")) {
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한 없음"); return true; }
            if (args.length >= 3 && args[1].equalsIgnoreCase("구슬")) {
                if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
                String id = args[2];
                ItemStack inHand = ((Player)sender).getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType() == Material.AIR) {
                    sender.sendMessage(ChatColor.RED + "손에 아이템을 들어주세요.");
                    return true;
                }
                plugin.machines().setExclusiveBall(id, inHand.clone());
                sender.sendMessage(ChatColor.GREEN + "전용 구슬 등록 완료: " + id);
                return true;
            }
            sender.sendMessage(ChatColor.RED + "사용법: /파칭코 설정 구슬 <번호>");
            return true;
        } else if (sub.equalsIgnoreCase("랭킹")) {
            int n = plugin.getConfig().getInt("ranking.top-n", 10);
            List<Map<String, Object>> list = plugin.machines().topN(n);
            sender.sendMessage(ChatColor.GOLD + "=== 파칭코 버스트 랭킹 TOP " + n + " ===");
            int i=1;
            for (Map<String, Object> row : list) {
                String name = String.valueOf(row.get("name"));
                int drops = (int) row.get("drops");
                sender.sendMessage(ChatColor.YELLOW + "" + (i++) + ". " + name + " - " + drops);
            }
            return true;
        } else if (sub.equalsIgnoreCase("나가기")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            for (Machine m : plugin.machines().all().values()) {
                if (m.getOccupier() != null && m.getOccupier().equals(((Player) sender).getUniqueId())) {
                    m.setOccupier(null);
                    m.setStage(0);
                    m.setTokens(0);
                }
            }
            sender.sendMessage(ChatColor.GRAY + "점유를 해제했습니다.");
            return true;
        } else if (sub.equalsIgnoreCase("리로드")) {
            plugin.reloadConfig();
            plugin.machines().loadAll();
            sender.sendMessage(ChatColor.GREEN + "리로드 완료");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "알 수 없는 하위명령어");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("설치"); out.add("설정"); out.add("랭킹"); out.add("나가기"); out.add("리로드");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("설정")) {
            out.add("구슬");
        }
        return out;
    }
}
