
package com.minkang.ultimate.pachinko.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Text {
    public static String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }
    public static void msg(CommandSender to, String s){ to.sendMessage(color(s)); }
}
