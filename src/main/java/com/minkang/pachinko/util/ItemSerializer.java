
package com.minkang.pachinko.util;

import org.bukkit.inventory.ItemStack;
import java.io.*;
import java.util.Base64;

public class ItemSerializer {
    public static String toBase64(ItemStack item){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(item);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }catch(Exception e){ return null; }
    }
    public static ItemStack fromBase64(String b64){
        try{
            byte[] data = Base64.getDecoder().decode(b64);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object obj = ois.readObject();
            ois.close();
            return (ItemStack) obj;
        }catch(Exception e){ return null; }
    }
    // 호환용 구 메서드명
    public static String itemToBase64(ItemStack item){ return toBase64(item); }
    public static ItemStack itemFromBase64(String b64){ return fromBase64(b64); }
}
