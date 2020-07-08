package world.bentobox.warps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

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
        cachedSigns.forEach((w, m) -> handler.saveObjectAsync(new SignCache(w, m)));
    }

    Material getSignIcon(World world, UUID warpOwner) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        if (cachedSigns.get(world).containsKey(warpOwner)) {
            return cachedSigns.get(world).get(warpOwner).getType();
        }
        // Not in cache
        SignCacheItem sc = addon.getWarpSignsManager().getSignInfo(world, warpOwner);
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
        SignCacheItem result = addon.getWarpSignsManager().getSignInfo(world, playerUUID);
        cachedSigns.get(world).put(playerUUID, result);
        return result.getSignText();
    }

    /**
     * Removes sign text from the cache
     * @param world - world
     * @param key - uuid of owner
     */
    void removeWarp(World world, UUID key) {
        if (cachedSigns.containsKey(world)) {
            cachedSigns.get(world).remove(key);
        }
    }

}
