package world.bentobox.warps;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
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
                w.getSigns().values().removeIf(sci -> sci.getType().equals(Material.AIR));
                cachedSigns.put(world, w.getSigns());
            }
        });
    }

    void saveCache() {
        cachedSigns.forEach((w, m) -> handler.saveObjectAsync(new SignCache(w, m)));
    }

    /**
     * Get the sign item from cache or get it from the world if it is not in the cache
     * @param world - world
     * @param warpOwner - warp owner
     * @return SignCacheItem
     */
    @NonNull
    SignCacheItem getSignItem(World world, UUID warpOwner) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        // Get from cache if available
        if (cachedSigns.get(world).containsKey(warpOwner)) {
            return cachedSigns.get(world).get(warpOwner);
        }
        // Generate and add to cache
        SignCacheItem result = addon.getWarpSignsManager().getSignInfo(world, warpOwner);
        if (result.isReal()) {
            BentoBox.getInstance().logDebug("Warp is real - caching");
            cachedSigns.get(world).put(warpOwner, result);
        } else {
            BentoBox.getInstance().logDebug("Warp is not real - removing");
            addon.getWarpSignsManager().removeWarp(world, warpOwner);
        }
        return result;
    }

    /**
     * Removes sign text from the cache
     * @param world - world
     * @param key - uuid of owner
     * @return true if item is removed from cache
     */
    boolean removeWarp(World world, UUID key) {
        if (cachedSigns.containsKey(world)) {
            return cachedSigns.get(world).remove(key) != null;
        }
        return false;
    }

}
