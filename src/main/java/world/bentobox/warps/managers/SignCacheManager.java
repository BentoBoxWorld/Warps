package world.bentobox.warps.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.database.Database;
import world.bentobox.warps.Warp;
import world.bentobox.warps.objects.SignCache;

public class SignCacheManager {
    private final Map<World, Map<UUID, SignCacheItem>> cachedSigns = new HashMap<>();
    private final Warp addon;
    // Database handler for level data
    private final Database<SignCache> handler;

    public SignCacheManager(Warp addon) {
        this.addon = addon;
        handler = new Database<>(addon, SignCache.class);
        // Load the sign caches
        loadCache();
    }

    private void loadCache() {
        cachedSigns.clear();
        handler.loadObjects().forEach(w -> {
            World world = Bukkit.getWorld(w.getUniqueId());
            if (world != null) {
                w.getSigns().values().removeIf(sci -> sci.getType().equals(Material.AIR));
                cachedSigns.put(world, w.getSigns());
            }
        });
    }

    public void saveCache() {
        cachedSigns.forEach((w, m) -> handler.saveObjectAsync(new SignCache(w, m)));
    }

    /**
     * Get the sign item from cache or get it from the world if it is not in the cache
     * @param world - world
     * @param warpOwner - warp owner
     * @return SignCacheItem
     */
    @NonNull
    public SignCacheItem getSignItem(World world, UUID warpOwner) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        // Get from cache if available
        if (cachedSigns.get(world).containsKey(warpOwner)) {
            return cachedSigns.get(world).get(warpOwner);
        }
        // Generate and add to cache
        SignCacheItem result = addon.getWarpSignsManager().getSignInfo(world, warpOwner);
        if (result.isReal()) {
            cachedSigns.get(world).put(warpOwner, result);
        } else {
            cachedSigns.get(world).remove(warpOwner);
        }
        return result;
    }

    /**
     * Removes sign text from the cache
     * @param world - world
     * @param key - uuid of owner
     * @return true if item is removed from cache
     */
    public boolean removeWarp(World world, UUID key) {
        if (cachedSigns.containsKey(world)) {
            return cachedSigns.get(world).remove(key) != null;
        }
        return false;
    }

}
