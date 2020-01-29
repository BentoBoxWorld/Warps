package world.bentobox.warps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.warps.objects.SignCache;

public class SignCacheManager {
    private Map<World, Map<UUID, SignCacheItem>> cachedSigns = new HashMap<>();
    private Warp addon;
    // Database handler for level data
    private Database<SignCache> handler;
    
    public SignCacheManager(Warp addon) {
        this.addon = addon;
        handler = new Database<>(addon, SignCache.class);
        // Load the sign caches
        loadCache();
    }

    private void loadCache() {
        handler.loadObjects().forEach(w -> {
            World world = Bukkit.getWorld(w.getUniqueId());
            if (world != null) {
                cachedSigns.put(world, w.getSigns());
            }
        });
    }
    
    void saveCache() {
        cachedSigns.forEach((w, m) -> handler.saveObject(new SignCache(w, m)));
    }

    Material getSignIcon(World world, UUID warpOwner) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        if (cachedSigns.get(world).containsKey(warpOwner)) {
            return cachedSigns.get(world).get(warpOwner).getType();
        }
        // Not in cache        
        return cacheSign(world, warpOwner);
    }
    
    /**
     * Cache the sign text
     * @param world - world
     * @param warpOwner - warp owner
     * @return Material of sign
     */
    public Material cacheSign(World world, UUID warpOwner) {
        SignCacheItem sc = getSignInfo(world, warpOwner);
        cachedSigns.get(world).put(warpOwner, sc);
        return sc.getType();
    }

    /**
     * Gets sign text and cache it
     * @param playerUUID
     * @return sign text in a list
     */
    List<String> getSign(World world, UUID playerUUID) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        if (cachedSigns.get(world).containsKey(playerUUID)) {
            return cachedSigns.get(world).get(playerUUID).getSignText();
        }
        SignCacheItem result = getSignInfo(world, playerUUID);
        cachedSigns.get(world).put(playerUUID, result);
        return result.getSignText();
    }
    
    /**
     * Removes sign text from the cache
     * @param world - world
     * @param key - uuid of owner
     */
    void removeWarp(World world, UUID key) {
        cachedSigns.putIfAbsent(world, new HashMap<>());
        cachedSigns.get(world).remove(key);
    }

    /**
     * Gets the warp sign text and material type for player's UUID in world
     *
     * @param world - world to look in
     * @param uuid - player's uuid
     * @return Sign's content and type
     */
    @NonNull
    public SignCacheItem getSignInfo(@NonNull World world, @NonNull UUID uuid) {
        List<String> result = new ArrayList<>();
        //get the sign info
        Location signLocation = getWarp(world, uuid);
        if (signLocation != null && signLocation.getBlock().getType().name().contains("SIGN")) {
            Sign sign = (Sign)signLocation.getBlock().getState();
            result.addAll(Arrays.asList(sign.getLines()));
            // Clean up - remove the [WELCOME] line
            result.remove(0);
            // Remove any trailing blank lines
            result.removeIf(String::isEmpty);
            // Set the initial color per lore setting
            for (int i = 0; i< result.size(); i++) {
                result.set(i, ChatColor.translateAlternateColorCodes('&', addon.getSettings().getLoreFormat()) + result.get(i));
            }
            // Get the sign type

            String prefix = plugin.getIWM().getAddon(world).map(Addon::getPermissionPrefix).orElse("");

            Material icon;

            if (!prefix.isEmpty())
            {
                icon = Material.matchMaterial(
                        this.getPermissionValue(User.getInstance(uuid),
                                prefix + "island.warp",
                                this.addon.getSettings().getIcon()));
            }
            else
            {
                icon = Material.matchMaterial(this.addon.getSettings().getIcon());
            }

            if (icon == null || icon.name().contains("SIGN")) {
                return new SignCacheItem(result, Material.valueOf(sign.getType().name().replace("WALL_", "")));
            } else {
                return new SignCacheItem(result, icon);
            }
        } else {
            addon.getWarpSignsManager().removeWarp(world, uuid);
        }
        return new SignCacheItem(Collections.emptyList(), Material.AIR);
    }

}
