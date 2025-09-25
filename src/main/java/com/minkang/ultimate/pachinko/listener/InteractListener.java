
package com.minkang.ultimate.pachinko.listener;

import com.minkang.ultimate.pachinko.Main;
import com.minkang.ultimate.pachinko.model.Machine;
import com.minkang.ultimate.pachinko.model.MachineManager;
import com.minkang.ultimate.pachinko.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InteractListener implements Listener {

    private final Main plugin;
    private final MachineManager manager;

    public InteractListener(Main plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMachineManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // 메인핸드만 처리하여 2중 클릭 방지
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;

        Machine m = manager.getMachineBySpecialBlock(b.getLocation());
        if (m == null) return; // 이 플러그인의 구조가 아니라면 무시

        e.setCancelled(true); // 기본 작동 취소(중복 동작 방지)

        Material type = b.getType();
        Player p = e.getPlayer();

        if (type == Material.GOLD_BLOCK) {
            // 금블럭: 추첨권 +1 (구슬 1개 소모 필요)
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!ItemUtil.isBall(hand)) {
                p.sendMessage("§7구슬을 손에 들어주세요.");
                return;
            }
            m.onGoldClicked(p);
            return;
        }

        if (type == Material.COAL_BLOCK) {
            // 석탄블럭: 쌓인 추첨권만큼 추첨 실행
            m.onCoalClicked(p);
            return;
        }

        // 다이아몬드 블럭은 클릭해도 동작하지 않음(무한 배출 방지)
    }
}
