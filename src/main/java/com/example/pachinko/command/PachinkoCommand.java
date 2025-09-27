package com.example.pachinko.command;

import com.example.pachinko.PachinkoPlugin;
import com.example.pachinko.manager.MachineManager;
import com.example.pachinko.manager.RankingManager;
import com.example.pachinko.model.Machine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PachinkoCommand implements CommandExecutor {

    private final PachinkoPlugin plugin;
    private final MachineManager mm;

    public PachinkoCommand(PachinkoPlugin plugin) {
        this.plugin = plugin;
        this.mm = plugin.machines();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "/파칭코 설치 <번호> " + ChatColor.GRAY + "- 기계 설치");
            sender.sendMessage(ChatColor.AQUA + "/파칭코 설정 구슬 <번호> " + ChatColor.GRAY + "- 손에 든 아이템을 전용 구슬로 설정");
            sender.sendMessage(ChatColor.AQUA + "/파칭코 랭킹 " + ChatColor.GRAY + "- 실패 랭킹 보기");
            sender.sendMessage(ChatColor.AQUA + "/파칭코 리로드 " + ChatColor.GRAY + "- 설정 리로드");
            sender.sendMessage(ChatColor.AQUA + "/파칭코 나가기 " + ChatColor.GRAY + "- 현재 기계 점유 해제");
            return true;
        }

        if (args[0].equalsIgnoreCase("리로드")) {
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }
            plugin.reloadConfig();
            mm.reloadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "리로드 완료.");
            return true;
        }

        if (args[0].equalsIgnoreCase("설치")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "번호를 입력하세요."); return true; }
            int id;
            try { id = Integer.parseInt(args[1]); } catch (Exception ex) { sender.sendMessage("숫자를 입력하세요."); return true; }
            boolean ok = mm.installMachine((Player)sender, id);
            return true;
        }

        if (args[0].equalsIgnoreCase("설정")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }
            if (args.length < 3) { sender.sendMessage(ChatColor.RED + "/파칭코 설정 구슬 <번호>"); return true; }
            if (!args[1].equalsIgnoreCase("구슬")) { sender.sendMessage(ChatColor.RED + "/파칭코 설정 구슬 <번호>"); return true; }
            int id;
            try { id = Integer.parseInt(args[2]); } catch (Exception ex) { sender.sendMessage("숫자를 입력하세요."); return true; }
            Machine m = mm.getMachine(id);
            if (m == null) { sender.sendMessage(ChatColor.RED + "해당 번호의 기계가 없습니다."); return true; }
            Player p = (Player) sender;
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType() == Material.AIR) { sender.sendMessage(ChatColor.RED + "손에 아이템을 들고 사용하세요."); return true; }
            ItemStack copy = inHand.clone();
            copy.setAmount(1);
            m.setExclusiveBall(copy);
            mm.saveMachines();
            sender.sendMessage(ChatColor.GREEN + "기계 #" + id + " 전용 구슬 설정 완료: " + copy.getType().name());
            return true;
        }

        if (args[0].equalsIgnoreCase("랭킹")) {
            List<RankingManager.Entry> top = plugin.ranking().top(10);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.ranking-header", "&e=== 랭킹 ===")).replace("%n%", ""+top.size()));
            if (top.isEmpty()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.ranking-empty", "&7기록 없음")));
            }
            int i=1;
            for (RankingManager.Entry e : top) {
                String line = plugin.getConfig().getString("messages.ranking-line", "&6%idx%. &f%name% &7- &b%score%&7 회")
                        .replace("%idx%", ""+i)
                        .replace("%name%", e.name)
                        .replace("%score%", ""+e.score);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                i++;
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("나가기")) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            Player p = (Player) sender;
            // 본인을 점유자로 갖는 기계 찾기
            Machine mine = mm.findByOccupant(p.getUniqueId());
            if (mine != null) {
                mine.release();
                sender.sendMessage(ChatColor.GREEN + "기계 #" + mine.getId() + " 점유 해제 완료.");
            } else {
                sender.sendMessage(ChatColor.GRAY + "현재 점유 중인 기계가 없습니다.");
            }
            return true;
        }

            Player p = (Player) sender;
            // 점유 중인 기계 찾아 해제
            for (Machine m : plugin.machines().getClass().cast(plugin.machines()).getClass().getDeclaredMethods()) {
                // (간단화를 위해 명령 생략) 사용중인 기계를 저장하지 않아 일괄 안내만
            }
            p.sendMessage(ChatColor.GRAY + "기계에서 멀어지거나 일정 시간(2분) 무조작 시 자동 해제됩니다.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "알 수 없는 하위 명령입니다.");
        return true;
    }
}
