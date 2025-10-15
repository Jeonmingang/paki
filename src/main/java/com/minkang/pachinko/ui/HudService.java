// SPDX-License-Identifier: MIT
package com.minkang.pachinko.ui;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class HudService {
    private final BossBar bar;

    public HudService(String title, BarColor color, BarStyle style) {
        this.bar = Bukkit.createBossBar(title, color, style);
        this.bar.setVisible(false);
    }

    public void show(Player p) {
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        bar.setVisible(true);
    }

    public void hide(Player p) {
        bar.removePlayer(p);
    }

    public void updateTitle(String title) {
        bar.setTitle(title);
    }

    public void updateProgress(double progress01) {
        if (progress01 < 0) progress01 = 0;
        if (progress01 > 1) progress01 = 1;
        bar.setProgress(progress01);
    }
}
