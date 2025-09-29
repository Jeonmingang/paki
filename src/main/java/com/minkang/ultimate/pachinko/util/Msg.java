package com.minkang.ultimate.pachinko.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Msg {
    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    public static void action(Player p, String s) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(s));
    }
}
