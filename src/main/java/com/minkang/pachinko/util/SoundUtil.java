// SPDX-License-Identifier: MIT
package com.minkang.pachinko.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class SoundUtil {
    public static void playBallInsert(Player p) {
        Location loc = p.getLocation();
        // 1.16.5에서 안전하게 존재하는 사운드
        p.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
    }
}
