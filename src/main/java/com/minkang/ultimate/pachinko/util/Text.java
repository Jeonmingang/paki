
package com.minkang.ultimate.pachinko.util;

import com.minkang.ultimate.pachinko.Main;

public class Text {
    public static String prefix(Main plugin) {
        return plugin.getConfig().getString("messages.prefix", "§6[파칭코] §f");
    }
}
