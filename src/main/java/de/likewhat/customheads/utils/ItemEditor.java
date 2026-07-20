package de.likewhat.customheads.utils;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import de.likewhat.customheads.CustomHeads;

import de.likewhat.customheads.utils.reflection.helpers.Version;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/*
 *  Project: CustomHeads in ItemEditor
 *     by LikeWhat
 *
 *  Just like every ItemBuilder out there
 */

public class ItemEditor {

    private ItemStack itemStack;

    private final ItemMeta meta;

    public ItemEditor(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        meta = itemStack.getItemMeta();
    }

    public ItemEditor(Material material) {
        this(material, 0);
    }

    public ItemEditor(Material material, int damage) {
        this(new ItemStack(material, 1,  (short) damage));
    }

    public short getDamage() {
        return itemStack.getDurability();
    }

    public ItemEditor setDamage(short damage) {
        itemStack.setDurability(damage);
        return this;
    }

    public int getAmount() {
        return itemStack.getAmount();
    }

    public ItemEditor setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public boolean hasDisplayName() {
        return meta.hasDisplayName();
    }

    public String getDisplayName() {
        return meta.getDisplayName();
    }

    public ItemEditor setDisplayName(String displayName) {
        meta.setDisplayName(Utils.format(displayName));
        return this;
    }

    public ItemEditor setLore(String[] lore) {
        return setLore(lore != null && lore.length > 0 ? Arrays.asList(lore) : null);
    }

    public ItemEditor setLore(String lore) {
        return setLore(lore.isEmpty() ? null : Arrays.asList(lore.split("\n")));
    }

    public ItemEditor addLoreLine(String lore) {
        if (lore == null)
            return this;
        List<String> itemLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        itemLore.addAll(Arrays.asList(lore.split("\n")));
        meta.setLore(itemLore);
        return this;
    }

    public ItemEditor addLoreLines(List<String> lore) {
        if (lore == null)
            return this;
        List<String> itemLore = hasLore() ? getLore() : new ArrayList<>();
        itemLore.addAll(lore);
        setLore(itemLore);
        return this;
    }

    public ItemEditor removeLoreLine(int line) {
        if (!hasLore()) return this;
        List<String> itemLore = meta.getLore();
        itemLore.remove(line);
        setLore(itemLore);
        return this;
    }

    public boolean hasLore() {
        return meta.hasLore();
    }

    public ItemEditor insertLoreLine(String lore, int line) {
        List<String> itemLore = hasLore() ? getLore() : new ArrayList<>();
        itemLore.add(line, lore);
        setLore(itemLore);
        return this;
    }

    public List<String> getLore() {
        return meta.getLore();
    }

    public ItemEditor setLore(List<String> itemLore) {
        meta.setLore(itemLore);
        return this;
    }

    public String getLoreAsString() {
        if (getLore().isEmpty()) return "";
        StringBuilder b = new StringBuilder();
        for (String l : getLore()) {
            b.append(l).append("\n");
        }
        b.setLength(b.length() - 1);
        return b.toString();
    }

    public String getTexture() {
        return CustomHeads.getApi().getSkullTexture(itemStack);
    }

    public ItemEditor setTexture(String texture) {
        if (itemStack.getType() != Utils.getPlayerHeadMaterial())
            throw new IllegalArgumentException("ItemStack is not an Player Head");

        try {
            // Try Paper's PlayerProfile API first (supports direct Base64 properties)
            Class<?> paperProfileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            java.lang.reflect.Method createProfileMethod = org.bukkit.Bukkit.class.getMethod("createProfile", java.util.UUID.class, String.class);
            Object profile = createProfileMethod.invoke(null, java.util.UUID.randomUUID(), null);
            
            Class<?> profilePropertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            Object property = profilePropertyClass.getConstructor(String.class, String.class).newInstance("textures", texture);
            
            paperProfileClass.getMethod("setProperty", profilePropertyClass).invoke(profile, property);
            org.bukkit.inventory.meta.SkullMeta.class.getMethod("setPlayerProfile", paperProfileClass).invoke(meta, profile);
            return this;
        } catch (Exception e) {
            CustomHeads.getPluginLogger().warning("Paper API fallback failed: " + e.toString());
        }
        
        try {
            // Try Spigot's Bukkit PlayerProfile API (1.18+)
            java.lang.reflect.Method createProfileMethod = null;
            for (java.lang.reflect.Method m : org.bukkit.Bukkit.class.getMethods()) {
                if (m.getName().equals("createPlayerProfile") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == java.util.UUID.class && m.getReturnType().getName().equals("org.bukkit.profile.PlayerProfile")) {
                    createProfileMethod = m;
                    break;
                }
            }
            if (createProfileMethod != null) {
                Object profile = createProfileMethod.invoke(null, java.util.UUID.randomUUID(), null);
                String url = null;
                try {
                    String decoded = new String(java.util.Base64.getDecoder().decode(texture), java.nio.charset.StandardCharsets.UTF_8);
                    com.google.gson.JsonObject jsonObject = new com.google.gson.JsonParser().parse(decoded).getAsJsonObject();
                    url = jsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                } catch (Exception ex) {}
                
                if (url != null) {
                    Class<?> spigotProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
                    Object playerTextures = spigotProfileClass.getMethod("getTextures").invoke(profile);
                    Class<?> playerTexturesClass = Class.forName("org.bukkit.profile.PlayerTextures");
                    playerTexturesClass.getMethod("setSkin", java.net.URL.class).invoke(playerTextures, new java.net.URL(url));
                    spigotProfileClass.getMethod("setTextures", playerTexturesClass).invoke(profile, playerTextures);
                    org.bukkit.inventory.meta.SkullMeta.class.getMethod("setOwnerProfile", spigotProfileClass).invoke(meta, profile);
                    return this;
                }
            }
        } catch (Exception e) {
            CustomHeads.getPluginLogger().warning("Spigot API fallback failed: " + e.toString());
        }
        
        // Fallback for 1.14 - 1.17 (using GameProfile reflection)
        try {
            GameProfile profile = new GameProfile(java.util.UUID.randomUUID(), "0");
            Object propertyMap = profile.getClass().getMethod("getProperties").invoke(profile);
            Object property = Class.forName("com.mojang.authlib.properties.Property").getConstructor(String.class, String.class).newInstance("textures", texture);
            propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", property);

            java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            CustomHeads.getPluginLogger().warning("Failed to set Skull Texture: " + e.getMessage());
        }
        return this;
    }

    public String getOwner() {
        return ((SkullMeta) meta).getOwner();
    }

    public ItemEditor setOwner(String owner) {
        if (itemStack.getType() != Utils.getPlayerHeadMaterial())
            throw new IllegalArgumentException("ItemStack is not an Player Head");
        SkullMeta skullMeta = ((SkullMeta) meta);
        skullMeta.setOwner(owner);
        itemStack.setItemMeta(skullMeta);
        return this;
    }

    public ItemEditor addEnchantment(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemEditor removeEnchantment(Enchantment enchantment) {
        meta.removeEnchant(enchantment);
        return this;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return meta.getEnchants();
    }

    public ItemEditor hideAllFlags() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }



    public ItemStack getItem() {
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
