package com.minkang.ultimate.pachinko.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Text {
    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    public static void msg(CommandSender p, String s){
        p.sendMessage(color(s));
    }
    public static void action(Player p, String s) {
        try{ p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(color(s))); }catch(Throwable ignored){
            msg(p, s);
        }
    }
}
