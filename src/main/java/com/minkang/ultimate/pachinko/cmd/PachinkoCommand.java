package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PachinkoCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final MachineManager manager;

    public PachinkoCommand(Main plugin, MachineManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6/파칭코 설치 §7- 바라보는 블럭에 기계 등록");
            sender.sendMessage("§6/파칭코 제거 <id> §7- 기계 제거");
            sender.sendMessage("§6/파칭코 목록 §7- 기계 목록");
            sender.sendMessage("§6/파칭코 리로드 §7- 설정 리로드");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("리로드")) {
            plugin.reloadConfig();
            sender.sendMessage("§a설정을 리로드했습니다.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        Player p = (Player) sender;
        if (sub.equalsIgnoreCase("설치")) {
            Block b = p.getTargetBlockExact(5);
            if (b == null) {
                p.sendMessage("§c바라보는 블럭이 없습니다.");
                return true;
            }
            Machine m = manager.register(b.getLocation());
            p.sendMessage("§a설치됨: §f" + m.getId());
            return true;
        } else if (sub.equalsIgnoreCase("제거")) {
            if (args.length < 2) { p.sendMessage("§c/파칭코 제거 <id>"); return true; }
            boolean ok = manager.unregister(args[1]);
            p.sendMessage(ok ? "§a제거 완료" : "§c해당 ID가 없습니다.");
            return true;
        } else if (sub.equalsIgnoreCase("목록")) {
            if (manager.getMachines().isEmpty()) {
                p.sendMessage("§7등록된 기계가 없습니다.");
                return true;
            }
            for (Machine m : manager.getMachines()) {
                Location l = m.getBase();
                p.sendMessage("§f- §e" + m.getId() + " §7@ " + l.getWorld().getName() + " " + l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());
            }
            return true;
        } else if (sub.equalsIgnoreCase("스테이지")) {
            Block b = p.getTargetBlockExact(5);
            Machine m = (b != null) ? manager.getByBase(b.getLocation()) : null;
            if (m == null) { p.sendMessage("§c기계를 찾을 수 없습니다."); return true; }
            manager.enterStage(m, p);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.add("설치"); out.add("제거"); out.add("목록"); out.add("리로드"); out.add("스테이지");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("제거")) {
            for (Machine m : manager.getMachines()) out.add(m.getId());
        }
        return out;
    }
}