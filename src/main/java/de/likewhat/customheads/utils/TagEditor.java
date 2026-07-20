package de.likewhat.customheads.utils;

import de.likewhat.customheads.CustomHeads;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagEditor {

    private final String rootNode;
    private final NamespacedKey key;

    public TagEditor(String rootNode) {
        this.rootNode = rootNode;
        this.key = new NamespacedKey(CustomHeads.getInstance(), rootNode);
    }

    public ItemStack addTags(ItemStack itemStack, List<String> tags) {
        return addTags(itemStack, tags.toArray(new String[0]));
    }

    public ItemStack addTags(ItemStack itemStack, String... tags) {
        if (itemStack == null || itemStack.getItemMeta() == null) return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentTags = pdc.get(key, PersistentDataType.STRING);
        List<String> tagList = new ArrayList<>();
        if (currentTags != null && !currentTags.isEmpty()) {
            tagList.addAll(Arrays.asList(currentTags.split("\0")));
        }
        tagList.addAll(Arrays.asList(tags));
        pdc.set(key, PersistentDataType.STRING, String.join("\0", tagList));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public ItemStack removeTags(ItemStack itemStack, String... tags) {
        if (itemStack == null || itemStack.getItemMeta() == null) return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentTags = pdc.get(key, PersistentDataType.STRING);
        if (currentTags != null && !currentTags.isEmpty()) {
            List<String> tagList = new ArrayList<>(Arrays.asList(currentTags.split("\0")));
            tagList.removeAll(Arrays.asList(tags));
            if (tagList.isEmpty()) {
                pdc.remove(key);
            } else {
                pdc.set(key, PersistentDataType.STRING, String.join("\0", tagList));
            }
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public ItemStack copyTags(ItemStack source, ItemStack target) {
        if (source == null || target == null || source.getItemMeta() == null || target.getItemMeta() == null) return target;
        ItemMeta sourceMeta = source.getItemMeta();
        ItemMeta targetMeta = target.getItemMeta();
        PersistentDataContainer sourcePdc = sourceMeta.getPersistentDataContainer();
        PersistentDataContainer targetPdc = targetMeta.getPersistentDataContainer();
        if (sourcePdc.has(key, PersistentDataType.STRING)) {
            targetPdc.set(key, PersistentDataType.STRING, sourcePdc.get(key, PersistentDataType.STRING));
        }
        target.setItemMeta(targetMeta);
        return target;
    }

    public ItemStack setTags(ItemStack itemStack, String... tags) {
        if (itemStack == null || itemStack.getItemMeta() == null) return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key, PersistentDataType.STRING, String.join("\0", Arrays.asList(tags)));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public List<String> getTags(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return new ArrayList<>();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentTags = pdc.get(key, PersistentDataType.STRING);
        if (currentTags != null && !currentTags.isEmpty()) {
            return new ArrayList<>(Arrays.asList(currentTags.split("\0")));
        }
        return new ArrayList<>();
    }

    public boolean hasMyTags(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public int indexOf(ItemStack itemStack, String tag) {
        return getTags(itemStack).indexOf(tag);
    }

    public static ItemStack clearTags(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return itemStack;
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(new NamespacedKey(CustomHeads.getInstance(), "tags"));
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
