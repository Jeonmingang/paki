package com.minkang.ultimate.pachinko;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PCommand implements CommandExecutor {
    private final Main plugin;
    public PCommand(Main p){ this.plugin = p; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) return true;
        Player p = (Player)s;

        if (a.length==0 || a[0].equals("시작")) return start(p);
        if (a[0].equals("설치")) return place(p);
        if (a[0].equals("환전")) return buyBalls(p);
        if (a[0].equals("교환")) return cashOut(p);

        p.sendMessage("§7/파칭코 시작 | 환전 | 교환 | 설치");
        return true;
    }

    private boolean place(Player p){
        Location base = p.getLocation().getBlock().getLocation();
        World w = base.getWorld();
        for (int y=0;y<14;y++){
            for (int x=0;x<9;x++){
                w.getBlockAt(base.clone().add(x, y, 0)).setType(Material.WHITE_STAINED_GLASS);
            }
        }
        for (int y=1;y<12;y++){
            for (int x=(y%2); x<9; x+=2){
                w.getBlockAt(base.clone().add(x, y, 0)).setType(Material.IRON_BARS);
            }
        }
        for (int x=0;x<9;x++){
            w.getBlockAt(base.clone().add(x, 12, 0)).setType(Material.HOPPER);
        }
        plugin.getRegistry().add(base.clone().add(0,0,1));
        p.sendMessage("§a파칭코 기계 설치 완료!");
        return true;
    }

    private boolean start(Player p){
        boolean ok = plugin.getStore().takeBall(p.getUniqueId());
        if (!ok){
            p.sendMessage(plugin.getConfig().getString("messages.no-ball").replace("&","§"));
            return true;
        }
        Location base = plugin.getRegistry().nearest(p.getWorld(), p.getLocation());
        boolean exists = base != null;
        if (!exists){
            p.sendMessage("§c근처에 기계가 없습니다. /파칭코 설치 로 배치하세요.");
            plugin.getStore().addBalls(p.getUniqueId(),1);
            return true;
        }
        p.sendMessage(plugin.getConfig().getString("messages.start").replace("&","§"));
        new RunBall(plugin, p, base).begin();
        return true;
    }

    private boolean buyBalls(Player p){
        int unit = plugin.getConfig().getInt("economy.balls-per-exchange",10);
        if (plugin.hasVault()){
            int price = plugin.getConfig().getInt("economy.yen-per-medal",100) * unit / 2;
            EconomyResponse r = plugin.getEcon().withdrawPlayer(p, price);
            boolean ok = r.transactionSuccess();
            if (!ok){
                p.sendMessage("§c잔액이 부족합니다.");
                return true;
            }
        } else {
            int need = plugin.getConfig().getInt("economy.emerald-per-ball",1)*unit;
            boolean ok = p.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(Material.EMERALD), need);
            if (!ok){ p.sendMessage("§c에메랄드가 부족합니다."); return true; }
            p.getInventory().removeItem(new org.bukkit.inventory.ItemStack(Material.EMERALD, need));
        }
        plugin.getStore().addBalls(p.getUniqueId(), unit);
        p.sendMessage("§a구슬 "+unit+"개를 받았습니다.");
        return true;
    }

    private boolean cashOut(Player p){
        int have = plugin.getStore().getMedals(p.getUniqueId());
        boolean ok = have > 0;
        if (!ok){ p.sendMessage("§7교환할 메달이 없습니다."); return true; }

        int per = plugin.getConfig().getInt("economy.yen-per-medal",100);
        if (plugin.hasVault()){
            plugin.getEcon().depositPlayer(p, have*per);
            plugin.getStore().takeMedals(p.getUniqueId(), have);
            p.sendMessage("§b메달 "+have+"개 → "+(have*per)+" 지급");
        } else {
            int give = Math.max(1, (have*per) / 100);
            p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EMERALD, give));
            plugin.getStore().takeMedals(p.getUniqueId(), have);
            p.sendMessage("§b메달 "+have+"개 → 에메랄드 "+give+"개 지급");
        }
        return true;
    }
}