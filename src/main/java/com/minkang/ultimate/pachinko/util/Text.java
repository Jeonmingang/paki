
package com.minkang.ultimate.pachinko.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Text {
    private Text(){}
    public static String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
    public static void msg(CommandSender to, String s){ if (to!=null) to.sendMessage(color(s)); }
}
