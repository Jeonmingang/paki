
package com.minkang.ultimate.pachinko.cmd;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PachinkoCommand implements CommandExecutor {
    private final MachineManager mm;
    public PachinkoCommand(MachineManager mm){ this.mm=mm; }

    private int toInt(String s, int def){ try { return Integer.parseInt(s);}catch(Exception e){return def;}}
    private double toD(String s, double def){ try { return Double.parseDouble(s);}catch(Exception e){return def;}}

    // 사진처럼 구조 자동 시공 (하얀색 유리 패널 + 철창 + 상단 호퍼, 좌석탄/중금/우다이아)
    private void buildStructureLikeScreenshot(Location base){
        org.bukkit.World w = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        Material GLASS = Material.WHITE_STAINED_GLASS;
        Material BARS = Material.IRON_BARS;
        Material HOP = Material.HOPPER;
        Material COAL = Material.COAL_BLOCK;
        Material GOLD = Material.GOLD_BLOCK;
        Material DIA  = Material.DIAMOND_BLOCK;

        // 컨트롤 블럭 배치
        w.getBlockAt(bx-3, by, bz).setType(COAL);
        w.getBlockAt(bx,   by, bz).setType(GOLD);
        w.getBlockAt(bx+3, by, bz).setType(DIA);

        // 뒤쪽 패널(z+1), 폭 7칸, 아래→위 GLASS/BARS 교차 + 상단 HOPPER
        int zPanel = bz + 1;
        for (int x=-3; x<=3; x++){
            w.getBlockAt(bx+x, by+1, zPanel).setType(GLASS);
            w.getBlockAt(bx+x, by+2, zPanel).setType(BARS);
            w.getBlockAt(bx+x, by+3, zPanel).setType(GLASS);
            w.getBlockAt(bx+x, by+4, zPanel).setType(BARS);
            w.getBlockAt(bx+x, by+5, zPanel).setType(GLASS);
            w.getBlockAt(bx+x, by+6, zPanel).setType(BARS);
            w.getBlockAt(bx+x, by+7, zPanel).setType(GLASS);
            w.getBlockAt(bx+x, by+8, zPanel).setType(HOP);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player)){ Text.msg(sender,"&c플레이어만 사용 가능합니다."); return true; }
        Player p = (Player)sender;

        if (a.length==0 || a[0].equalsIgnoreCase("도움말")){
            Text.msg(p,"&e/파칭코 설치 <번호> &7- 사진 형태로 자동 시공");
            Text.msg(p,"&e/파칭코 삭제 <번호>");
            Text.msg(p,"&e/파칭코 목록");
            Text.msg(p,"&e/파칭코 리로드");
            Text.msg(p,"&e/파칭코 설정 ... &7(구슬저장/전역가중치/가중치/삼중확률/보급수/중앙모드/스테이지테이블/기계구슬/저장)");
            return true;
        }

        if (a[0].equalsIgnoreCase("설치") && a.length>=2){
            int id = toInt(a[1], -1);
            if (id<0){ Text.msg(p,"&c번호 오류"); return true; }
            Location base = p.getLocation().getBlock().getLocation();

            // 구조 자동 시공
            buildStructureLikeScreenshot(base);

            Machine m = mm.ensure(id, base);
            // 컨트롤 포인트 저장
            m.setCoalButton(base.clone().add(-3,0,0));
            m.setGoldButton(base.clone());
            m.setDiamondBlock(base.clone().add(3,0,0));

            // 전역 가중치 초기화
            List<Integer> gw = Main.get().getConfig().getIntegerList("globalWeights");
            if (gw!=null && gw.size()>=7){
                int[] w = new int[7]; for (int i=0;i<7;i++) w[i] = gw.get(i);
                m.setWeights(w);
            }
            Text.msg(p,"&a#"+id+" 설치 완료. (좌:석탄, 중:금, 우:다이아) + 뒤 패널 자동 생성");
            return true;
        }

        if (a[0].equalsIgnoreCase("삭제") && a.length>=2){
            int id = toInt(a[1], -1);
            mm.getAll().remove(id);
            Text.msg(p,"&c#"+id+" 삭제.");
            return true;
        }

        if (a[0].equalsIgnoreCase("목록")){
            Text.msg(p,"&6--- 파칭코 기계 목록 ("+mm.getAll().size()+") ---");
            for (Machine m : mm.getAll().values()){
                Text.msg(p, "&e#"+m.getId()+" &7베이스="+fmt(m.getBase()));
            }
            return true;
        }

        if (a[0].equalsIgnoreCase("리로드")){
            Main.get().reloadConfig();
            Text.msg(p,"&aconfig.yml 리로드 및 구슬 동기화 완료.");
            return true;
        }

        if (a[0].equalsIgnoreCase("설정")){
            if (a.length<2){ Text.msg(p,"&c사용법: /파칭코 설정 <하위명령> ..."); return true; }
            String sub = a[1].toLowerCase();
            if (sub.equals("구슬저장")){
                ItemStack in = p.getInventory().getItemInMainHand();
                if (in==null || in.getType()== Material.AIR){ Text.msg(p,"&c손에 아이템을 들어주세요."); return true; }
                ConfigurationSection sec = Main.get().getConfig().getConfigurationSection("defaultBall");
                if (sec==null) sec = Main.get().getConfig().createSection("defaultBall");
                sec.set("type", in.getType().name());
                if (in.hasItemMeta()){
                    if (in.getItemMeta().hasDisplayName()) sec.set("name", in.getItemMeta().getDisplayName());
                    if (in.getItemMeta().hasLore()) sec.set("lore", in.getItemMeta().getLore());
                }
                Main.get().saveConfig();
                Text.msg(p,"&a전역 구슬 저장 완료. 모든 기계에 즉시 반영.");
                return true;
            }
            if (sub.equals("기계구슬")){
                if (a.length<3){ Text.msg(p,"&c/파칭코 설정 기계구슬 <번호> (저장|clear)"); return true; }
                int id = toInt(a[2], -1);
                Machine m = mm.getAll().get(id);
                if (m==null){ Text.msg(p,"&c기계가 없습니다."); return true; }
                if (a.length>=4 && a[3].equalsIgnoreCase("clear")){
                    m.setMachineBallItem(null); mm.save(); Text.msg(p,"&7#"+id+" 개별 구슬 해제.");
                }else{
                    ItemStack in = p.getInventory().getItemInMainHand();
                    if (in==null || in.getType()== Material.AIR){ Text.msg(p,"&c손에 아이템을 들어주세요."); return true; }
                    m.setMachineBallItem(in.clone()); mm.save();
                    Text.msg(p,"&a#"+id+" 전용 구슬 저장 완료.");
                }
                return true;
            }
            if (sub.equals("전역가중치")){
                if (a.length<9){ Text.msg(p,"&c/파칭코 설정 전역가중치 <w1..w7>"); return true; }
                List<Integer> w = new ArrayList<Integer>();
                for (int i=2;i<9;i++) w.add(toInt(a[i],14));
                Main.get().getConfig().set("globalWeights", w);
                Main.get().saveConfig();
                Text.msg(p,"&a전역 가중치 저장: "+w);
                return true;
            }
            if (sub.equals("가중치")){
                if (a.length<10){ Text.msg(p,"&c/파칭코 설정 가중치 <번호> <w1..w7>"); return true; }
                int id = toInt(a[2],-1);
                Machine m = mm.getAll().get(id);
                if (m==null){ Text.msg(p,"&c기계 없음"); return true; }
                int[] w = new int[7]; for (int i=0;i<7;i++) w[i]=toInt(a[3+i],14);
                m.setWeights(w); mm.save();
                Text.msg(p,"&a#"+id+" 가중치 저장");
                return true;
            }
            if (sub.equals("삼중확률")){
                if (a.length<3){ Text.msg(p,"&c/파칭코 설정 삼중확률 <0~1>"); return true; }
                Main.get().getConfig().set("centerSlot.tripleMatchChance", toD(a[2],0.09));
                Main.get().saveConfig();
                Text.msg(p,"&a전역 tripleMatchChance 저장");
                return true;
            }
            if (sub.equals("보급수")){
                if (a.length<3){ Text.msg(p,"&c/파칭코 설정 보급수 <개수>"); return true; }
                Main.get().getConfig().set("goldEject.supplyCount", toInt(a[2],5));
                Main.get().saveConfig();
                Text.msg(p,"&a금블럭 보급수 저장");
                return true;
            }
            if (sub.equals("중앙모드")){
                if (a.length<3){ Text.msg(p,"&c/파칭코 설정 중앙모드 <accumulate|directDraw>"); return true; }
                Main.get().getConfig().set("stageMode.type", a[2]);
                Main.get().saveConfig();
                Text.msg(p,"&a중앙모드 저장");
                return true;
            }
            if (sub.equals("스테이지테이블")){
                if (a.length<3){ Text.msg(p,"&c/파칭코 설정 스테이지테이블 (추가|삭제|확률|캡) ..."); return true; }
                List<?> list = Main.get().getConfig().getList("stages");
                List<Object> stages = new ArrayList<Object>();
                if (list!=null) stages.addAll(list);
                String act = a[2].toLowerCase();
                if (act.equals("추가")){
                    String name = a.length>=4 ? a[3] : "&a새 스테이지";
                    java.util.Map<String,Object> m = new java.util.LinkedHashMap<String,Object>();
                    m.put("name", name); m.put("advanceChance", 0.15); m.put("tripleMatchChance", 0.1);
                    m.put("payoutPerHit", 2); m.put("payoutCap", 96); m.put("bgm","MUSIC_DISC_CHIRP");
                    stages.add(m);
                    Main.get().getConfig().set("stages", stages); Main.get().saveConfig();
                    Text.msg(p,"&a스테이지 추가됨. 인덱스="+(stages.size()-1));
                }else if (act.equals("삭제")){
                    if (a.length<4){ Text.msg(p,"&c/삭제 <index>"); return true; }
                    int idx = toInt(a[3], -1);
                    if (idx<0 || idx>=stages.size()){ Text.msg(p,"&c인덱스 범위"); return true; }
                    stages.remove(idx);
                    Main.get().getConfig().set("stages", stages); Main.get().saveConfig();
                    Text.msg(p,"&c스테이지 삭제됨.");
                }else if (act.equals("확률")){
                    if (a.length<6){ Text.msg(p,"&c/확률 <index> <advanceChance> <tripleMatchChance>"); return true; }
                    int idx = toInt(a[3],-1);
                    double adv = toD(a[4],0.15);
                    double tri = toD(a[5],0.1);
                    java.util.Map m = (java.util.Map) stages.get(idx);
                    m.put("advanceChance", adv); m.put("tripleMatchChance", tri);
                    stages.set(idx, m); Main.get().getConfig().set("stages", stages); Main.get().saveConfig();
                    Text.msg(p,"&a설정 완료.");
                }else if (act.equals("캡")){
                    if (a.length<5){ Text.msg(p,"&c/캡 <index> <payoutCap>"); return true; }
                    int idx = toInt(a[3],-1);
                    int cap = toInt(a[4],64);
                    java.util.Map m = (java.util.Map) stages.get(idx);
                    m.put("payoutCap", cap); stages.set(idx, m);
                    Main.get().getConfig().set("stages", stages); Main.get().saveConfig();
                    Text.msg(p,"&a캡 저장.");
                }else{
                    Text.msg(p,"&c(추가|삭제|확률|캡) 지원");
                }
                return true;
            }
            if (sub.equals("저장")){
                Main.get().saveConfig();
                mm.save();
                Text.msg(p,"&aconfig.yml + machines.yml 저장됨.");
                return true;
            }
            Text.msg(p,"&c알 수 없는 설정 하위명령.");
            return true;
        }

        Text.msg(p,"&c알 수 없는 하위명령. /파칭코 도움말");
        return true;
    }

    private String fmt(Location l){
        if (l==null) return "null";
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }
}
