
package com.minkang.ultimate.pachinko;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RunBall {

    private static final Random rnd = new Random();

    /**
     * 3-슬롯 연출을 실행한다.
     * - config slot.spin-effect / slot.suspense.* 키를 따름 (3.4.0 스키마 유지 + 선택 확장)
     * - 결과(잭팟/센터)는 선결정하고, 연출은 시각/청각 효과만 담당 (확률 공정성 유지)
     */
    public static void runSlots(final Main plugin, final Player p,
                                final Runnable onJackpotCenter, final Runnable onJackpotOther){
        final boolean spin = plugin.getConfig().getBoolean("slot.spin-effect", true);

        // 결과를 먼저 결정 (공정성)
        int a = 1 + rnd.nextInt(9);
        int b = 1 + rnd.nextInt(9);
        int c = 1 + rnd.nextInt(9);
        boolean jackpot = (a==b && b==c);

        // 잭팟이면 '중앙 취급' 확률 적용 (3.4.0 스키마: center-next-stage-chance 0.0~1.0)
        double centerProb = plugin.getConfig().getDouble("center-next-stage-chance", 0.5);
        // 새 옵션(백분율)이 있으면 그 값을 우선
        int centerPercent = plugin.getConfig().getInt("slot.suspense.center-roll-chance", -1);
        if (centerPercent >= 0) centerProb = Math.max(0.0, Math.min(1.0, centerPercent / 100.0));
        final boolean centerFlag = jackpot && (rnd.nextDouble() < centerProb);

        if (!spin){
            // 즉시 결과 표시
            showLine(plugin, p, a, b, c);
            if (jackpot){
                if (centerFlag) onJackpotCenter.run(); else onJackpotOther.run();
            }
            return;
        }

        // ── 연출 파라미터 로드 ──
        final int tick      = Math.max(1, plugin.getConfig().getInt("slot.suspense.tick", 2));
        final int minSpins  = Math.max(1, plugin.getConfig().getInt("slot.suspense.min-spins", 10));
        final int maxSpins  = Math.max(minSpins, plugin.getConfig().getInt("slot.suspense.max-spins", 20));
        final int lastMin   = Math.max(0, plugin.getConfig().getInt("slot.suspense.last-extra-min", 20));
        final int lastMax   = Math.max(lastMin, plugin.getConfig().getInt("slot.suspense.last-extra-max", 40));
        final String reveal = plugin.getConfig().getString("slot.suspense.reveal", "horizontal").toLowerCase(Locale.ROOT);
        final String order  = plugin.getConfig().getString("slot.suspense.stop-order", "LMR").toUpperCase(Locale.ROOT);
        final String loopS  = plugin.getConfig().getString("slot.suspense.loop-sound", "BLOCK_NOTE_BLOCK_HAT");
        final String stopS  = plugin.getConfig().getString("slot.suspense.stop-sound", "UI_BUTTON_CLICK");
        final float vol     = (float) plugin.getConfig().getDouble("slot.suspense.volume", 1.0);
        final float pit     = (float) plugin.getConfig().getDouble("slot.suspense.pitch", 1.0);

        // 회전 횟수 결정
        final int spins = minSpins + rnd.nextInt(maxSpins - minSpins + 1);

        // 정지 순서 계산
        final int[] stopIdx; // 0=L,1=M,2=R
        if ("random".equals(order)){
            java.util.List<Integer> tmp = new ArrayList<>(Arrays.asList(0,1,2));
            java.util.Collections.shuffle(tmp, rnd);
            stopIdx = new int[]{tmp.get(0), tmp.get(1), tmp.get(2)};
        } else if ("MLR".equals(order)){
            stopIdx = new int[]{1,0,2};
        } else if ("RLM".equals(order)){
            stopIdx = new int[]{2,0,1};
        } else { // LMR (default)
            stopIdx = new int[]{0,1,2};
        }

        // 현재 보이는 숫자
        final int[] cur = new int[]{1,1,1};
        final boolean[] locked = new boolean[]{false,false,false};
        final int[] result = new int[]{a,b,c};

        new BukkitRunnable(){
            int t = 0;
            boolean finished = false;
            int phase = 0; // 0=스핀, 1=정지 진행, 2=마지막 지연

            @Override public void run(){
                if (finished){ cancel(); return; }

                // 스핀 단계: 모든 자리 증가
                if (phase == 0){
                    for (int i=0;i<3;i++){
                        if (!locked[i]){
                            cur[i] = 1 + rnd.nextInt(9);
                        }
                    }
                    playLoopSound(p, loopS, vol, pit);
                    showLine(plugin, p, cur[0], cur[1], cur[2]);

                    t++;
                    if (t >= spins){
                        phase = 1; t = 0;
                    }
                    return;
                }

                // 정지 진행: stopIdx 순서로 하나씩 결과 고정
                if (phase == 1){
                    int idx = -1;
                    for (int i=0;i<3;i++){
                        if (!locked[stopIdx[i]]){ idx = stopIdx[i]; break; }
                    }
                    if (idx >= 0){
                        locked[idx] = true;
                        cur[idx] = result[idx];
                        showLine(plugin, p, cur[0], cur[1], cur[2]);
                        playStopSound(p, stopS, vol, pit);
                        return;
                    }else{
                        // 모두 잠겼으면 마지막 추가 지연
                        phase = 2; t = 0;
                        return;
                    }
                }

                // 마지막 지연
                if (phase == 2){
                    int extra = lastMin + rnd.nextInt(lastMax - lastMin + 1);
                    t++;
                    if (t >= extra){
                        finished = true;
                        // 최종 결과 처리
                        boolean jackpotFinal = (result[0]==result[1] && result[1]==result[2]);
                        if (jackpotFinal){
                            if (centerFlag) onJackpotCenter.run(); else onJackpotOther.run();
                        }
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, tick);
    }

    private static void playLoopSound(Player p, String key, float vol, float pit){
        try{
            p.playSound(p.getLocation(), Sound.valueOf(key), vol, pit);
        }catch(Exception ignored){}
    }
    private static void playStopSound(Player p, String key, float vol, float pit){
        try{
            p.playSound(p.getLocation(), Sound.valueOf(key), vol, pit);
        }catch(Exception ignored){}
    }

    private static void showLine(Main plugin, Player p, int a, int b, int c){
        String fmt = plugin.getConfig().getString("messages.slot-line", "&7슬롯: &f[{a}] &7- &f[{b}] &7- &f[{c}]");
        String msg = fmt.replace("{a}", String.valueOf(a)).replace("{b}", String.valueOf(b)).replace("{c}", String.valueOf(c)).replace("&","§");
        p.sendMessage(msg);
        try{ p.sendTitle("", msg, 0, 10, 0); }catch(Exception ignored){}
    }
}
