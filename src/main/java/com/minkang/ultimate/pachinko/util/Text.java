
package com.minkang.ultimate.pachinko.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text(){}
    public static String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
    public static void msg(CommandSender s, String msg){ s.sendMessage(color(msg)); }
}
