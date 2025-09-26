
package com.example.pachinko.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Text {
    public static String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }
    public static String prefix(){ return color("&b[파칭코]&r "); }
    public static void actionbar(Player p, String msg){
        if (p==null) return;
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(msg)));
    }
}
