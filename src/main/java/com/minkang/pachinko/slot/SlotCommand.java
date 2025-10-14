package com.minkang.pachinko.slot;

import com.minkang.pachinko.PachinkoPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SlotCommand implements CommandExecutor {

    private final PachinkoPlugin plugin;

    public SlotCommand(PachinkoPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/슬롯 설치 <번호>");
            sender.sendMessage(ChatColor.YELLOW + "/슬롯 아이템 <번호>");
            sender.sendMessage(ChatColor.YELLOW + "/슬롯 리로드");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("리로드")) {
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + "슬롯 설정 리로드 완료.");
            return true;
        }
        if (sub.equalsIgnoreCase("설치")) {
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용"); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "사용법: /슬롯 설치 <번호>"); return true; }
            int id; try { id = Integer.parseInt(args[1]); } catch (Exception e) { sender.sendMessage("번호는 정수"); return true; }
            Player p = (Player) sender;
            com.minkang.pachinko.slot.SlotMachine m = plugin.getSlotManager().createTemplate(id, p);
            if (m == null) sender.sendMessage(ChatColor.RED + "이미 존재하는 번호입니다.");
            else sender.sendMessage(ChatColor.GREEN + "슬롯 #" + id + " 설치 완료.");
            return true;
        }
        if (sub.equalsIgnoreCase("아이템")) {
            if (!sender.hasPermission("pachinko.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용"); return true; }
            if (args.length < 2) { sender.sendMessage(ChatColor.RED + "사용법: /슬롯 아이템 <번호>"); return true; }
            int id; try { id = Integer.parseInt(args[1]); } catch (Exception e) { sender.sendMessage("번호는 정수"); return true; }
            Player p = (Player) sender;
            org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == org.bukkit.Material.AIR) { sender.sendMessage(ChatColor.RED + "손에 아이템을 들고 실행하세요."); return true; }
            boolean ok = plugin.getSlotManager().setCoinForMachine(id, hand);
            if (!ok) sender.sendMessage(ChatColor.RED + "해당 번호의 슬롯이 없습니다.");
            else sender.sendMessage(ChatColor.GREEN + "슬롯 #" + id + " 전용 코인 아이템이 설정되었습니다. (이름/로어 포함)");
            return true;
        }
        return true;
    }
}
