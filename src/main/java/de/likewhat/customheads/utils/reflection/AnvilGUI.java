package de.likewhat.customheads.utils.reflection;

import de.likewhat.customheads.CustomHeads;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;

public class AnvilGUI {
    
    private final Player player;
    private final String title;
    private final AnvilClickEventHandler handler;
    private ItemStack inputLeft;
    
    public AnvilGUI(Player player, String title, AnvilClickEventHandler handler) {
        this.player = player;
        this.title = title;
        this.handler = handler;
    }
    
    public void setSlot(AnvilSlot slot, ItemStack item) {
        if (slot == AnvilSlot.INPUT_LEFT) {
            this.inputLeft = item;
        }
    }
    
    public void open() {
        new net.wesjd.anvilgui.AnvilGUI.Builder()
            .plugin(CustomHeads.getInstance())
            .title(title)
            .itemLeft(inputLeft)
            .onClick((slot, stateSnapshot) -> {
                AnvilClickEvent event = new AnvilClickEvent(slot, stateSnapshot.getText(), player);
                handler.onAnvilClick(event);
                if (event.isCancelled()) {
                    return Collections.emptyList();
                } else {
                    return Arrays.asList(net.wesjd.anvilgui.AnvilGUI.ResponseAction.close());
                }
            })
            .onClose(stateSnapshot -> {
                handler.onAnvilClose();
            })
            .open(player);
    }
    
    public interface AnvilClickEventHandler {
        void onAnvilClick(AnvilClickEvent event);
        default void onAnvilClose() {}
    }
    
    public static class AnvilClickEvent {
        private final int slot;
        private final String name;
        private final Player player;
        private boolean cancelled = false;
        
        public AnvilClickEvent(int slot, String name, Player player) {
            this.slot = slot;
            this.name = name;
            this.player = player;
        }
        
        public AnvilSlot getSlot() {
            return slot == 2 ? AnvilSlot.OUTPUT : AnvilSlot.INPUT_LEFT;
        }
        
        public String getName() { return name; }
        public Player getPlayer() { return player; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
    
    public enum AnvilSlot {
        INPUT_LEFT, INPUT_RIGHT, OUTPUT
    }
}
