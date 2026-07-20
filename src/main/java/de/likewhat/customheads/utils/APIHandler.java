package de.likewhat.customheads.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.likewhat.customheads.CustomHeads;
import de.likewhat.customheads.api.CustomHeadsAPI;
import de.likewhat.customheads.api.CustomHeadsPlayer;
import de.likewhat.customheads.category.Category;
import de.likewhat.customheads.category.CustomHead;
import de.likewhat.customheads.headwriter.HeadFontType;
import de.likewhat.customheads.headwriter.HeadWriter;
import de.likewhat.customheads.utils.reflection.helpers.Version;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Level;

public class APIHandler implements CustomHeadsAPI {

    private static final FireworkEffect.Type[] FX_TYPES = {FireworkEffect.Type.BALL, FireworkEffect.Type.BALL_LARGE, FireworkEffect.Type.BURST, FireworkEffect.Type.STAR};

    // Head Util Impl
    public String getSkullTexture(ItemStack itemStack) {
        if(itemStack == null) {
            throw new NullPointerException("Item cannot be null");
        }
        if (!itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof SkullMeta)) {
            return null;
        }
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile profile = (GameProfile) profileField.get(meta);
            if (profile != null) {
                Collection<Property> properties = profile.getProperties().get("textures");
                if (properties != null && !properties.isEmpty()) {
                    return properties.iterator().next().getValue();
                }
            }
        } catch (Exception e) {
            CustomHeads.getPluginLogger().log(Level.WARNING, "Something went wrong while getting the Texture of a Skull item", e);
        }
        return null;
    }

    public String getSkullTexture(Block block) {
        if (!(block.getState() instanceof Skull)) {
            throw new IllegalArgumentException("Block must be a Skull");
        }
        Skull skull = (Skull) block.getState();
        try {
            // In 1.14+, we can just reflect the GameProfile from the Skull block state
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile profile = (GameProfile) profileField.get(skull);
            if (profile != null) {
                Collection<Property> properties = profile.getProperties().get("textures");
                if (properties != null && !properties.isEmpty()) {
                    return properties.iterator().next().getValue();
                }
            }
        } catch (Exception e) {
            CustomHeads.getPluginLogger().log(Level.WARNING, "Error getting Texture from Skull Block", e);
        }
        return null;
    }

    public ItemStack getAlphabetHead(String character, HeadFontType font) {
        if (!font.getCharacterItems().containsKey(character.charAt(0)))
            throw new UnsupportedOperationException("Unsupported Character: '" + character.charAt(0) + "'");
        return font.getCharacter(character.charAt(0));
    }

    public void writeText(String text, Location location, HeadFontType fontType) {
        new HeadWriter(fontType, text).writeAt(location);
    }

    public void setSkull(Block block, String texture, BlockFace blockFace) {
        try {
            block.setType(Utils.getPlayerHeadMaterial());
            Skull skull = (Skull) block.getState();
            
            // Try setting profile directly using reflection
            GameProfile profile = new GameProfile(java.util.UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skull, profile);
            
            // Set rotation (if supported by the API)
            try {
                org.bukkit.block.data.BlockData blockData = block.getBlockData();
                if (blockData instanceof org.bukkit.block.data.Rotatable) {
                    ((org.bukkit.block.data.Rotatable) blockData).setRotation(blockFace);
                    block.setBlockData(blockData);
                }
            } catch (Exception ignored) {}
            
            skull.update(true, false);
        } catch (Exception e) {
            CustomHeads.getPluginLogger().log(Level.WARNING, "Error placing Skull", e);
        }
    }

    public CustomHead getHead(String categoryId, int headId) {
        Category category = CustomHeads.getCategoryManager().getCategory(categoryId);
        if(category == null) {
            throw new NullPointerException("Unknown Category ID: " + categoryId);
        }
        return category.getHeads().stream().filter(customHead -> customHead.getId() == headId).findFirst().orElse(null);
    }

    public CustomHead getHead(Category category, int headId) {
        return category.getHeads().stream().filter(customHead -> customHead.getId() == headId).findFirst().orElse(null);
    }

    // API Impl
    public CustomHeadsPlayer wrapPlayer(Player player) {
        return PlayerWrapper.wrapPlayer(player);
    }

    public void createFireworkBattery(Location location, int shots, int delay) {
        createDefaultFireworkBattery(location, shots, delay);
    }

    public void createDefaultFireworkBattery(Location location, int shots, int delay) {
        Random random = new Random();
        createFireworkBattery(location, shots, delay, new FireworksBatteryHandler() {
            public void onStart() {}
            public void onNext() {
                Firework f = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
                FireworkMeta fm = f.getFireworkMeta();
                FireworkEffect.Builder fx = FireworkEffect.builder();
                fx.flicker(random.nextBoolean()).trail(random.nextBoolean()).with(FX_TYPES[random.nextInt(FX_TYPES.length)]);
                int c = random.nextInt(2) + 2;
                for (int i = 0; i < c; i++) {
                    fx.withColor(Color.fromRGB(random.nextInt(200) + 50, random.nextInt(200) + 50, random.nextInt(200) + 50));
                    if (random.nextBoolean()) {
                        fx.withFade(Color.fromRGB(random.nextInt(200) + 50, random.nextInt(200) + 50, random.nextInt(200) + 50));
                    }
                }
                fm.addEffect(fx.build());
                fm.setPower(random.nextInt(2) + 1);
                f.setFireworkMeta(fm);
                f.setVelocity(new Vector(random.nextDouble() * (random.nextBoolean() ? .01 : -.01), .2, random.nextDouble() * (random.nextBoolean() ? .01 : -.01)));
            }
            public void onEnd() {}
        });
    }

    public void createFireworkBattery(Location location, int shots, int delay, FireworksBatteryHandler handler) {
        handler.onStart();
        new BukkitRunnable() {
            int counter = shots;
            public void run() {
                if (counter == 0) {
                    handler.onEnd();
                    cancel();
                    return;
                }
                handler.onNext();
                counter--;
            }
        }.runTaskTimer(CustomHeads.getInstance(), 10, delay);
    }
}
