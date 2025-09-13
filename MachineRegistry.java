
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MachineRegistry {
    private final Main plugin;
    private final Map<Integer, Machine> map = new LinkedHashMap<>();

    public MachineRegistry(Main plugin){
        this.plugin = plugin;
    }

    public Main plugin(){ return plugin; }

    public Collection<Machine> all(){ return map.values(); }

    public Machine get(int id){ return map.get(id); }

    private int nextId(){
        int max = 0;
        for (Integer k : map.keySet()) if (k != null && k > max) max = k;
        return max + 1;
    }

    /** 설치 커맨드에서 호출: 기준 위치와 열 수만 받아 기계 추가 */
    public int addWithCols(Location base, int cols){
        int rows = plugin.getConfig().getInt("machine-default.rows", 8);
        int id = nextId();
        map.put(id, new Machine(id, base, rows, cols));
        return id;
    }

    public boolean remove(int id){
        return map.remove(id) != null;
    }

    /** 서버 시작/리로드 시 config에서 기계 목록 읽기 */
    public void loadFromConfig(){
        map.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("machines");
        if (sec == null) return;
        for (String key : sec.getKeys(false)){
            try{
                int id = Integer.parseInt(key);
                Object val = sec.get(key);

                if (val instanceof String){
                    // world,x,y,z,rows,cols
                    String[] sp = ((String) val).split(",");
                    if (sp.length < 6) continue;
                    World w = Bukkit.getWorld(sp[0]);
                    if (w == null) continue;
                    double x = Double.parseDouble(sp[1]);
                    double y = Double.parseDouble(sp[2]);
                    double z = Double.parseDouble(sp[3]);
                    int rows = Integer.parseInt(sp[4]);
                    int cols = Integer.parseInt(sp[5]);
                    map.put(id, new Machine(id, new Location(w,x,y,z), rows, cols));
                } else if (val instanceof ConfigurationSection){
                    ConfigurationSection ms = (ConfigurationSection) val;
                    String locStr = ms.getString("location", "");
                    String[] loc = locStr.split(",");
                    if (loc.length < 6) continue;
                    World w = Bukkit.getWorld(loc[0]);
                    if (w == null) continue;
                    double x = Double.parseDouble(loc[1]);
                    double y = Double.parseDouble(loc[2]);
                    double z = Double.parseDouble(loc[3]);
                    int rows = Integer.parseInt(loc[4]);
                    int cols = Integer.parseInt(loc[5]);

                    // 선택: 기계별 전용 구슬 정보가 있을 수 있음
                    String ballItem = ms.getString("ball-item", null);
                    String ballName = ms.getString("ball-name", null);
                    java.util.List<String> ballLore = ms.getStringList("ball-lore");
                    if (ballLore != null && ballLore.isEmpty()) ballLore = null;

                    try{
                        // Machine에 해당 생성자가 있으면 사용, 없으면 기본 생성자 사용
                        map.put(id, new Machine(id, new Location(w,x,y,z), rows, cols, ballItem, ballName, ballLore));
                    }catch (NoSuchMethodError | NoClassDefFoundError e){
                        map.put(id, new Machine(id, new Location(w,x,y,z), rows, cols));
                    }
                }
            }catch (Throwable ignored){}
        }
    }

    /** 현재 메모리의 기계 목록을 config에 저장 */
    public void saveToConfig(){
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("machines");
        if (sec == null) sec = plugin.getConfig().createSection("machines");
        for (String k : new java.util.ArrayList<>(sec.getKeys(false))) sec.set(k, null);

        for (Map.Entry<Integer, Machine> e : map.entrySet()){
            Integer id = e.getKey();
            Machine m = e.getValue();
            if (id == null || m == null) continue;
            ConfigurationSection ms = sec.createSection(String.valueOf(id));
            Location b = m.base;
            String loc = b.getWorld().getName()+","+b.getBlockX()+","+b.getBlockY()+","+b.getBlockZ()+","+m.rows+","+m.cols;
            ms.set("location", loc);
            // 기계별 전용 구슬 세팅이 있을 수 있으므로 키만 남겨둡니다(값은 외부에서 저장하도록).
            // ms.set("ball-item", ...); ms.set("ball-name", ...); ms.set("ball-lore", ...);
        }
    }

    public String cfgStr(String path){ return plugin.getConfig().getString(path); }

    /** config 리스트를 int[]로 변환 (예: structure.sign.offset) */
    public int[] cfgIntArray(String path){
        java.util.List<Integer> l = plugin.getConfig().getIntegerList(path);
        if (l == null || l.isEmpty()) return new int[]{0,0,0};
        int[] a = new int[l.size()];
        for (int i = 0; i < l.size(); i++) a[i] = l.get(i);
        return a;
    }

    /** 리로드/설치 직후, 모든 표지판을 최신 확률로 갱신 */
    public void refreshSigns(){
        for (Machine m : all()){
            try { m.updateSignLines(this); } catch (Throwable ignored) {}
        }
    }
}
