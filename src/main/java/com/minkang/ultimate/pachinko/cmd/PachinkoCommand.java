package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.RankingService;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

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
        if (sub.equalsIgnoreCase("상태")){
            if (args.length < 2){ Text.msg(sender, "&7사용법: /파칭코 상태 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Machine m = plugin.machines().byId(id);
            if (m == null){ Text.msg(sender, "&c해당 기계를 찾을 수 없습니다."); return true; }
            String op = (m.getOperator()==null) ? "&7(없음)" : String.valueOf(m.getOperator());
            Text.msg(sender, "&f#"+id+" &7- stage: &e"+m.getStage()+" &7/ cup: &e"+m.getStagePayout()+"/"+m.getStageCup()+" &7/ operator: &e"+op);
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
        if (sub.equalsIgnoreCase("설치")){
            if (!sender.isOp()){ Text.msg(sender, "&c권한이 필요합니다."); return true; }
            if (!(sender instanceof Player)){ Text.msg(sender, "&c플레이어만 사용 가능합니다."); return true; }
            if (args.length < 2){ Text.msg(sender, "&7사용법: /파칭코 설치 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Player p = (Player) sender;
            Location base = p.getLocation().getBlock().getLocation().add(0,0,0);

            // 방향 계산(가장 가까운 수평 4방위로 스냅)
            float yaw = p.getLocation().getYaw();
            yaw = (yaw % 360 + 360) % 360;
            int dir = Math.round(yaw/90f) & 3; // 0:E,1:S,2:W,3:N (대략)
            Vector right, front;
            switch (dir){
                case 0: default: right = new Vector(0,0,1); front = new Vector(1,0,0); break; // East looking -> width along +Z
                case 1: right = new Vector(-1,0,0); front = new Vector(0,0,1); break;
                case 2: right = new Vector(0,0,-1); front = new Vector(-1,0,0); break;
                case 3: right = new Vector(1,0,0); front = new Vector(0,0,-1); break;
            }

            // 구조물 파라미터
            int width = 7;
            int rowsGlass = 4; // 유리 층 수
            int y0 = base.getBlockY()+1; // 바닥 한칸 위부터 시작
            Location origin = new Location(p.getWorld(), base.getBlockX(), y0, base.getBlockZ());

            // 바닥 특수 블럭 (좌:석탄, 중앙:금, 우:다이아)
            setBlock(origin.clone().add(right.clone().multiply(-3)), Material.COAL_BLOCK);
            setBlock(origin.clone().add(right.clone().multiply(0)), Material.GOLD_BLOCK);
            setBlock(origin.clone().add(right.clone().multiply(3)), Material.DIAMOND_BLOCK);

            // 하단 유리 라인
            for (int i=-3;i<=3;i++){
                setBlock(origin.clone().add(right.clone().multiply(i)).add(front.clone().multiply(0)), Material.GLASS);
            }
            // 유리/철창 교차 3단
            int y = 1;
            for (int layer=0; layer<3; layer++){
                // 철창
                for (int i=-3;i<=3;i++){
                    setBlock(origin.clone().add(right.clone().multiply(i)).add(0,y,0), Material.IRON_BARS);
                }
                y++;
                // 유리
                for (int i=-3;i<=3;i++){
                    setBlock(origin.clone().add(right.clone().multiply(i)).add(0,y,0), Material.GLASS);
                }
                y++;
            }
            // 상단 유리 + 그 위 호퍼
            for (int i=-3;i<=3;i++){
                setBlock(origin.clone().add(right.clone().multiply(i)).add(0,y,0), Material.GLASS);
                Block hb = origin.clone().add(right.clone().multiply(i)).add(0,y+1,0).getBlock();
                hb.setType(Material.HOPPER, false);
            }

            // 기계 베이스 좌표 = 하단 유리 중앙
            Location machineBase = origin.clone(); // 가운데가 base
            com.minkang.ultimate.pachinko.model.Machine m = new com.minkang.ultimate.pachinko.model.Machine(Main.get(), id, machineBase.getWorld().getName(), machineBase, 7);
            boolean ok = Main.get().machines().addMachine(m);
            Text.msg(sender, ok ? "&a설치 완료: &7ID="+id+" @ "+fmt(machineBase)+" &8(구조물 생성+등록)" : "&c설치 실패");
            return true;
        }
        if (sub.equalsIgnoreCase("삭제")){
            if (!sender.isOp()){ Text.msg(sender, "&c권한이 필요합니다."); return true; }
            if (args.length < 2){ Text.msg(sender, "&7사용법: /파칭코 삭제 <기계번호>"); return true; }
            int id = parseInt(args[1], -1);
            Machine m = plugin.machines().byId(id);
            if (m == null){ Text.msg(sender, "&c해당 기계를 찾을 수 없습니다."); return true; }
            // 목록에서 제거 후 저장
            // (구조물 파괴는 안전을 위해 자동 처리하지 않음)
            // 실제 제거 원하시면 좌표 박스 범위로 AIR 세팅 루틴 추가 가능합니다.
            boolean ok = Main.get().machines().removeMachine(id);
            Text.msg(sender, ok ? "&a기계 #"+id+" 삭제 완료(구조물은 남겨둠)" : "&c해당 기계를 찾을 수 없습니다.");
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

    private void setBlock(Location l, Material mat){
        try{
            l.getBlock().setType(mat, false);
        }catch(Throwable ignored){}
    }

    private void help(CommandSender s){
        Text.msg(s, "&6/파칭코 &7리로드, 목록, 랭킹, 상태 <id>, 강제해제 <id>, 설치 <id>, 삭제 <id>, 설정 구슬 <id>");
    }

    private String fmt(Location l){
        return l.getWorld().getName()+" "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    private int parseInt(String s, int def){
        try { return Integer.parseInt(s); }catch(Exception e){ return def; }
    }
}
