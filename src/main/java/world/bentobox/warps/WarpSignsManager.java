package world.bentobox.warps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.warps.event.WarpInitiateEvent;
import world.bentobox.warps.event.WarpListEvent;
import world.bentobox.warps.objects.WarpsData;

/**
 * Handles warping. Players can add one sign
 *
 * @author tastybento
 *
 */
public class WarpSignsManager {
    private static final int MAX_WARPS = 600;
    private BentoBox plugin;
    // Map of all warps stored as player, warp sign Location
    private Map<World, Map<UUID, Location>> worldsWarpList;
    // Database handler for level data
    private Database<WarpsData> handler;

    private Warp addon;
    private WarpsData warpsData = new WarpsData();

    /**
     * Get the warp map for this world
     * @param world - world
     * @return map of warps
     */
    @NonNull
    public Map<UUID, Location> getWarpMap(@Nullable World world) {
        return worldsWarpList.computeIfAbsent(Util.getWorld(world), k -> new HashMap<>());
    }

    /**
     * @param addon - addon
     * @param plugin - plugin
     */
    public WarpSignsManager(Warp addon, BentoBox plugin) {
        this.addon = addon;
        this.plugin = plugin;
        // Set up the database handler
        // Note that these are saved by the BentoBox database
        handler = new Database<>(addon, WarpsData.class);
        // Load the warps
        loadWarpList();
    }

    /**
     * Stores warps in the warp array. If successful, fires an event
     *
     * @param playerUUID - the player's UUID
     * @param loc - location of warp sign
     * @return true if successful, false if not
     */
    public boolean addWarp(final UUID playerUUID, final Location loc) {
        // Do not allow null players to set warps
        if (playerUUID == null || loc == null) {
            return false;
        }
        // Check for warps placed in a location where there was a warp before
        if (getWarpMap(loc.getWorld()).containsValue(loc)) {
            // remove the warp at this location, then place it
            this.removeWarp(loc);
        }
        getWarpMap(loc.getWorld()).put(playerUUID, loc);
        saveWarpList();
        Bukkit.getPluginManager().callEvent(new WarpInitiateEvent(addon, loc, playerUUID));
        return true;
    }

    /**
     * Provides the location of the warp for player or null if one is not found
     *
     * @param world - world to search in
     * @param playerUUID - the player's UUID
     *            - the warp requested
     * @return Location of warp or null
     */
    @Nullable
    public Location getWarp(World world, UUID playerUUID) {
        return getWarpMap(world).get(playerUUID);
    }

    /**
     * Get the name of the warp owner by location
     * @param location to search
     * @return Name of warp owner or empty string if there is none
     */
    @NonNull
    public String getWarpOwner(Location location) {
        return getWarpMap(location.getWorld()).entrySet().stream().filter(en -> en.getValue().equals(location))
                .findFirst().map(en -> plugin.getPlayers().getName(en.getKey())).orElse("");
    }

    /**
     * Get sorted list of warps with most recent players listed first
     * @return UUID list
     */
    @NonNull
    public List<UUID> getSortedWarps(@NonNull World world) {
        // Remove any null locations - this can happen if an admin changes the name of the world and signs point to old locations
        getWarpMap(world).values().removeIf(Objects::isNull);
        // Bigger value of time means a more recent login
        TreeMap<Long, UUID> map = new TreeMap<>();
        getWarpMap(world).entrySet().forEach(en -> {
            UUID uuid = en.getKey();
            // If never played, will be zero
            long lastPlayed = addon.getServer().getOfflinePlayer(uuid).getLastPlayed();
            // This aims to avoid the chance that players logged off at exactly the same time
            if (!map.isEmpty() && map.containsKey(lastPlayed)) {
                lastPlayed = map.firstKey() - 1;
            }
            map.put(lastPlayed, uuid);
        });
        Collection<UUID> result = map.descendingMap().values();
        List<UUID> list = new ArrayList<>(result);
        if (list.size() > MAX_WARPS) {
            list.subList(0, MAX_WARPS).clear();
        }
        // Fire event
        WarpListEvent event = new WarpListEvent(addon, list);
        Bukkit.getPluginManager().callEvent(event);
        // Get the result of any changes by listeners
        list = event.getWarps();
        return list;
    }

    /**
     * Lists all the known warps for this world
     * @param world - world
     *
     * @return UUID set of warps
     */
    @NonNull
    public Set<UUID> listWarps(@NonNull World world) {
        // Remove any null locations
        getWarpMap(world).values().removeIf(Objects::isNull);
        return getWarpMap(world).entrySet().stream().filter(e -> Util.sameWorld(world, e.getValue().getWorld())).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * Load the warps and check if they still exist
     */
    private void loadWarpList() {
        addon.getLogger().info("Loading warps...");
        worldsWarpList = new HashMap<>();
        if (handler.objectExists("warps")) {
            warpsData = handler.loadObject("warps");
            // Load into map
            if (warpsData != null) {
                warpsData.getWarpSigns().forEach((k,v) -> {
                    if (k != null && k.getWorld() != null && k.getBlock().getType().name().contains("SIGN")) {
                        // Add to map
                        getWarpMap(k.getWorld()).put(v, k);
                        // Add to cache
                        addon.getWarpPanelManager().getSignCacheManager().cacheSign(k.getWorld(), v);
                    }
                });
            }
        }
    }

    /**
     * Changes the sign to red if it exists
     * @param loc
     */
    private void popSign(Location loc) {
        Block b = loc.getBlock();
        if (b.getType().name().contains("SIGN")) {
            Sign s = (Sign) b.getState();
            if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getSettings().getWelcomeLine())) {
                s.setLine(0, ChatColor.RED + addon.getSettings().getWelcomeLine());
                s.update(true, false);
            }
        }
    }

    /**
     * Removes a warp at a location.
     *
     * @param loc
     */
    public void removeWarp(Location loc) {
        popSign(loc);
        Iterator<Entry<UUID, Location>> it = getWarpMap(loc.getWorld()).entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            if (en.getValue().equals(loc)) {
                // Inform player
                User user = User.getInstance(addon.getServer().getPlayer(en.getKey()));
                if (user != null) {
                    // Inform the player
                    user.sendMessage("warps.sign-removed");
                }
                // Remove sign from warp panel cache
                addon.getWarpPanelManager().removeWarp(loc.getWorld(), en.getKey());
                it.remove();
            }
        }
        saveWarpList();
    }

    /**
     * Remove warp sign owned by UUID
     *
     * @param uuid
     */
    public void removeWarp(World world, UUID uuid) {
        if (getWarpMap(world).containsKey(uuid)) {
            popSign(getWarpMap(world).get(uuid));
            getWarpMap(world).remove(uuid);
            // Remove sign from warp panel cache
            addon.getWarpPanelManager().removeWarp(world, uuid);
        }
        saveWarpList();
    }

    /**
     * Saves the warp lists to the database
     */
    public void saveWarpList() {
        handler.saveObject(warpsData .save(worldsWarpList));
    }

    /**
     * Warps a player to a spot in front of a sign.
     * @param user - user who is warping
     * @param inFront - location in front of sign - previously checked for safety
     * @param signOwner - warp sign owner
     * @param directionFacing - direction that sign is facing
     * @param pvp - true if this location allowed PVP
     */
    private void warpPlayer(@NonNull User user, @NonNull Location inFront, @NonNull UUID signOwner, @NonNull BlockFace directionFacing, boolean pvp) {
        // convert blockface to angle
        float yaw = Util.blockFaceToFloat(directionFacing);
        final Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5D, inFront.getBlockY(),
                inFront.getBlockZ() + 0.5D, yaw, 30F);
        user.teleport(actualWarp);
        if (pvp) {
            user.sendMessage("protection.flags.PVP_OVERWORLD.active");
            user.getWorld().playSound(user.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
        } else {
            user.getWorld().playSound(user.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
        }
        User warpOwner = User.getInstance(signOwner);
        if (!warpOwner.equals(user)) {
            warpOwner.sendMessage("warps.player-warped", "[name]", user.getName());
        }
    }

    /**
     * Warps a user to the warp owner by owner
     *
     * @param world - world to check
     * @param user - user who is warping
     * @param owner - owner of the warp
     */
    public void warpPlayer(@NonNull World world, @NonNull User user, @NonNull UUID owner) {
        final Location warpSpot = getWarp(world, owner);
        // Check if the warp spot is safe
        if (warpSpot == null) {
            user.sendMessage("warps.error.does-not-exist");
            addon.getWarpSignsManager().removeWarp(world, owner);
            return;
        }

        if (this.plugin.getIWM().inWorld(user.getWorld()) &&
                Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()) &&
                user.getPlayer().getFallDistance() > 0) {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return;
        }

        Island island = addon.getIslands().getIsland(world, owner);
        boolean pvp = false;
        if (island != null) {
            // Check for PVP
            switch (warpSpot.getWorld().getEnvironment()) {
            case NETHER:
                pvp = island.isAllowed(Flags.PVP_NETHER);
                break;
            case NORMAL:
                pvp = island.isAllowed(Flags.PVP_OVERWORLD);
                break;
            case THE_END:
                pvp = island.isAllowed(Flags.PVP_END);
                break;
            default:
                break;

            }
        }
        // Find out which direction the warp is facing
        Block b = warpSpot.getBlock();
        if (b.getType().name().contains("WALL_SIGN")) {
            org.bukkit.block.data.type.WallSign s = (org.bukkit.block.data.type.WallSign) b.getBlockData();
            BlockFace directionFacing = s.getFacing();
            Location inFront = b.getRelative(directionFacing).getLocation();
            Location oneDown = b.getRelative(directionFacing).getRelative(BlockFace.DOWN).getLocation();
            if ((plugin.getIslands().isSafeLocation(inFront))) {
                warpPlayer(user, inFront, owner, directionFacing, pvp);
                return;
            } else if (plugin.getIslands().isSafeLocation(oneDown)) {
                // Try one block down if this is a wall sign
                warpPlayer(user, oneDown, owner, directionFacing, pvp);
                return;
            }
        } else if (b.getType().name().contains("SIGN")) {
            org.bukkit.block.data.type.Sign s = (org.bukkit.block.data.type.Sign) b.getBlockData();
            BlockFace directionFacing = s.getRotation();
            Location inFront = b.getRelative(directionFacing).getLocation();
            if ((addon.getIslands().isSafeLocation(inFront))) {
                warpPlayer(user, inFront, owner, directionFacing, pvp);
                return;
            }
        } else {
            // Warp has been removed
            user.sendMessage("warps.error.does-not-exist");
            removeWarp(warpSpot);
            return;
        }
        if (!(plugin.getIslands().isSafeLocation(warpSpot))) {
            user.sendMessage("warps.error.not-safe");
        } else {
            final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
                    warpSpot.getBlockZ() + 0.5D);
            if (pvp) {
                user.sendMessage("protection.flags.PVP_OVERWORLD.active");
                user.getWorld().playSound(user.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
            } else {
                user.getWorld().playSound(user.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
            }
            user.teleport(actualWarp);
        }
    }

    /**
     * Check if a player has a warp
     * @param playerUUID - player's UUID
     * @return true if they have warp
     */
    public boolean hasWarp(@NonNull World world, @NonNull UUID playerUUID) {
        return getWarpMap(world).containsKey(playerUUID);
    }


    // ---------------------------------------------------------------------
    // Section: Other methods
    // ---------------------------------------------------------------------


    /**
     * This method gets string value of given permission prefix. If user does not have
     * given permission or it have all (*), then return default value.
     * @param user User who's permission should be checked.
     * @param permissionPrefix Prefix that need to be found.
     * @param defaultValue Default value that will be returned if permission not found.
     * @return String value that follows permissionPrefix.
     */
    private String getPermissionValue(@NonNull User user, @NonNull String permissionPrefix, @NonNull String defaultValue)
    {
        if (user.isPlayer())
        {
            if (permissionPrefix.endsWith("."))
            {
                permissionPrefix = permissionPrefix.substring(0, permissionPrefix.length() - 1);
            }

            String permPrefix = permissionPrefix + ".";

            List<String> permissions = user.getEffectivePermissions().stream().
                    map(PermissionAttachmentInfo::getPermission).
                    filter(permission -> permission.startsWith(permPrefix)).
                    collect(Collectors.toList());

            for (String permission : permissions)
            {
                if (permission.contains(permPrefix + "*"))
                {
                    // * means all. So continue to search more specific.
                    continue;
                }

                String[] parts = permission.split(permPrefix);

                if (parts.length > 1)
                {
                    return parts[1];
                }
            }
        }

        return defaultValue;
    }
}
