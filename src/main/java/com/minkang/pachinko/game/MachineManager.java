package com.minkang.pachinko.game;

import com.minkang.pachinko.PachinkoPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MachineManager {
        private final com.minkang.pachinko.PachinkoPlugin plugin;


    private final Map<Integer, Machine> machines = new HashMap<>();
    private final File file;
    private YamlConfiguration y;

    public MachineManager(PachinkoPlugin plugin) {
        
        
        this.plugin = plugin;
new org.bukkit.scheduler.BukkitRunnable(){ public void run(){
            int t = com.minkang.pachinko.PachinkoPlugin.get().getSettings().getIdleTimeoutSeconds();
            for (Machine m : machines.values()) m.checkIdleTimeout(t);
        }}.runTaskTimer(plugin, 20L, 20L);
        this.file = new File(plugin.getDataFolder(), "machines.yml");
        reload();
    }

    public void reload() {
        machines.clear();
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        y = YamlConfiguration.loadConfiguration(file);
        if (y.isConfigurationSection("machines")) {
            for (String k : y.getConfigurationSection("machines").getKeys(false)) {
                try {
                    int id = Integer.parseInt(k);
                    Machine m = Machine.from(y, id);
                    if (m != null) machines.put(id, m);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveAll() {
        for (Integer id : machines.keySet()) machines.get(id).saveTo(y);
        try { y.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Machine getMachine(int id) { return machines.get(id); }

    public Machine getByOccupant(java.util.UUID u) {
        for (Machine m : machines.values()) if (u.equals(m.getOccupant())) return m;
        return null;
    }

    public Machine getByBlock(org.bukkit.Location l) {
        for (Machine m : machines.values()) if (m.isInteractionBlock(l)) return m;
        return null;
    }

    
    // Helper: yaw -> cardinal BlockFace
    private org.bukkit.block.BlockFace faceFromYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135) return org.bukkit.block.BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return org.bukkit.block.BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return org.bukkit.block.BlockFace.EAST;
        return org.bukkit.block.BlockFace.SOUTH;
    }

    public Machine createTemplate(org.bukkit.entity.Player p, int id, Settings s) {
        org.bukkit.World w = p.getWorld();
        org.bukkit.block.BlockFace f = faceFromYaw(p.getLocation().getYaw());

        int fx=0,fz=0, rx=0,rz=0;
        switch (f) {
            case NORTH: fx=0; fz=-1; rx=1;  rz=0;  break;
            case SOUTH: fx=0; fz=1;  rx=-1; rz=0;  break;
            case EAST:  fx=1; fz=0;  rx=0;  rz=1;  break;
            case WEST:  fx=-1;fz=0;  rx=0;  rz=-1; break;
            default: fx=0; fz=1; rx=-1; rz=0; break;
        }

        org.bukkit.Location pl = p.getLocation();
        int baseX = pl.getBlockX() + fx*3;
        int baseY = pl.getBlockY();
        int baseZ = pl.getBlockZ() + fz*3;

        // floor blocks & bases
        org.bukkit.Location coal = new org.bukkit.Location(w, baseX + rx*-1, baseY, baseZ + rz*-1);
        org.bukkit.Location gold = new org.bukkit.Location(w, baseX,            baseY, baseZ);
        org.bukkit.Location diamond = new org.bukkit.Location(w, baseX + rx*1,  baseY, baseZ + rz*1);
        w.getBlockAt(coal).setType(org.bukkit.Material.COAL_BLOCK);
        w.getBlockAt(gold).setType(org.bukkit.Material.GOLD_BLOCK);
        w.getBlockAt(diamond).setType(org.bukkit.Material.DIAMOND_BLOCK);

        // top 7 hoppers in a straight line + locations
        int hopperY = baseY + s.getHeight() + 1;
        org.bukkit.Location[] hs = new org.bukkit.Location[7];
        for (int i=-3; i<=3; i++) {
            org.bukkit.Location hp = new org.bukkit.Location(w, baseX + rx*i, hopperY, baseZ + rz*i);
            w.getBlockAt(hp).setType(org.bukkit.Material.HOPPER);
            hs[i+3] = hp;
        }

        // columns
        for (int i=-3; i<=3; i++) {
            for (int y=1; y<=s.getHeight(); y++) {
                org.bukkit.Material mat;
                if (s.hasMatrix()) {
                    org.bukkit.Material mm = s.materialForRowLane(y, i+3);
                    mat = (mm != null) ? mm : ((i==-3 || i==3) ? s.edgeForY(y) : s.innerForY(y));
                } else if (s.hasRowUniform()) {
                    mat = s.materialForY(y);
                } else {
                    mat = (i==-3 || i==3) ? s.edgeForY(y) : s.innerForY(y);
                }
                w.getBlockAt(baseX + rx*i, baseY + y, baseZ + rz*i).setType(mat);
            }
        }

        // indicator: 중심 위쪽 한 칸
        org.bukkit.Location indicator = new org.bukkit.Location(w, gold.getBlockX(), baseY + s.getHeight() + 2, gold.getBlockZ());

        Machine m = new Machine(id, coal, gold, diamond, hs, indicator);
        machines.put(id, m);
        return m;
    }
}
