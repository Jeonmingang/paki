// SPDX-License-Identifier: MIT
package com.minkang.pachinko.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.function.Consumer;

public class SpinRenderer {
    private static final Random RNG = new Random();

    /**
     * 3자리 숫자 롤연출 (순차 정지)
     * @param useTitle true면 Title, false면 ActionBar 출력
     * @param onFinish onFinish.accept(new boolean[]{match})
     */
    public static void rollThreeDigits(JavaPlugin plugin, Player p,
                                       boolean useTitle,
                                       int totalTicks, int firstStop, int secondStop,
                                       Consumer<boolean[]> onFinish) {
        final int[] d = {RNG.nextInt(10), RNG.nextInt(10), RNG.nextInt(10)};
        final boolean[] stopped = {false, false, false};

        for (int t = 0; t <= totalTicks; t++) {
            final int tick = t;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (tick == firstStop) stopped[0] = true;
                if (tick == secondStop) stopped[1] = true;
                if (tick == totalTicks) stopped[2] = true;

                for (int i = 0; i < 3; i++) {
                    if (!stopped[i]) d[i] = (d[i] + 1) % 10;
                }

                String panel = String.format("§7[ §e%d §7| §e%d §7| §e%d §7]", d[0], d[1], d[2]);
                if (useTitle) {
                    p.sendTitle(panel, "", 0, 10, 0);
                } else {
                    ActionBarUtil.send(p, panel);
                }

                if (tick == totalTicks) {
                    boolean match = (d[0] == d[1] && d[1] == d[2]);
                    onFinish.accept(new boolean[]{match});
                }
            }, tick);
        }
    }
}
