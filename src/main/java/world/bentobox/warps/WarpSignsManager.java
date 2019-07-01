package world.bentobox.warps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

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

    /**
     * Get the warp map for this world
     * @param world - world
     * @return map of warps
     */
    public Map<UUID, Location> getWarpMap(World world) {
        return worldsWarpList.computeIfAbsent(Util.getWorld(world), k -> new HashMap<>());
    }

    /**
     * @param addon - addon
     * @param plugin - plugin
     */
    public WarpSignsManager(Warp addon, BentoBox plugin) {
        this.addon = addon;
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
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
        // Do not allow null players to set warps or warps to be in the same location
        if (playerUUID == null || getWarpMap(loc.getWorld()).containsValue(loc)) {
            return false;
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
    public Location getWarp(World world, UUID playerUUID) {
        return getWarpMap(world).get(playerUUID);
    }

    /**
     * Get the name of the warp owner by location
     * @param location to search
     * @return Name of warp owner or empty string if there is none
     */
    public String getWarpOwner(Location location) {
        return getWarpMap(location.getWorld()).entrySet().stream().filter(en -> en.getValue().equals(location))
                .findFirst().map(en -> plugin.getPlayers().getName(en.getKey())).orElse("");
    }

    /**
     * Get sorted list of warps with most recent players listed first
     * @return UUID list
     */
    public List<UUID> getSortedWarps(World world) {
        // Remove any null locations - this can happen if an admin changes the name of the world and signs point to old locations
        getWarpMap(world).values().removeIf(Objects::isNull);
        // Bigger value of time means a more recent login
        TreeMap<Long, UUID> map = new TreeMap<Long, UUID>();
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
        addon.getServer().getPluginManager().callEvent(event);
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
    public Set<UUID> listWarps(World world) {
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
        WarpsData warps = new WarpsData();
        if (handler.objectExists("warps")) {
            warps = handler.loadObject("warps");
            // Load into map
            if (warps != null) {
                warps.getWarpSigns().forEach((k,v) -> {
                    if (k != null && k.getWorld() != null && k.getBlock().getType().name().contains("SIGN")) {
                        // Add to map
                        getWarpMap(k.getWorld()).put(v, k);
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
            if (s != null) {
                if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getSettings().getWelcomeLine())) {
                    s.setLine(0, ChatColor.RED + addon.getSettings().getWelcomeLine());
                    s.update(true, false);
                }
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
        handler.saveObject(new WarpsData().save(worldsWarpList));
    }

    /**
     * Gets the warp sign text and material type for player's UUID in world
     *
     * @param world - world to look in
     * @param uuid - player's uuid
     * @return Sign's content and type
     */
    public SignCache getSignInfo(World world, UUID uuid) {
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
            // Get the sign type
            Material type = Material.valueOf(sign.getType().name().replace("WALL_", ""));
            return new SignCache(result, type);
        } else {
            addon.getWarpSignsManager().removeWarp(world, uuid);
        }
        return new SignCache(Collections.emptyList(), Material.AIR);
    }

    /**
     * Warps a player to a spot in front of a sign.
     * @param user - user who is warping
     * @param inFront - location in front of sign - previously checked for safety
     * @param signOwner - warp sign owner
     * @param directionFacing - direction that sign is facing
     * @param pvp - true if this location allowed PVP
     */
    private void warpPlayer(User user, Location inFront, UUID signOwner, BlockFace directionFacing, boolean pvp) {
        // convert blockface to angle
        float yaw = blockFaceToFloat(directionFacing);
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
     * Converts block face direction to radial degrees. Returns 0 if block face
     * is not radial.
     *
     * @param face
     * @return degrees
     */
    private float blockFaceToFloat(BlockFace face) {
        switch (face) {
        case EAST:
            return 90F;
        case EAST_NORTH_EAST:
            return 67.5F;
        case EAST_SOUTH_EAST:
            return 0F;
        case NORTH:
            return 0F;
        case NORTH_EAST:
            return 45F;
        case NORTH_NORTH_EAST:
            return 22.5F;
        case NORTH_NORTH_WEST:
            return 337.5F;
        case NORTH_WEST:
            return 315F;
        case SOUTH:
            return 180F;
        case SOUTH_EAST:
            return 135F;
        case SOUTH_SOUTH_EAST:
            return 157.5F;
        case SOUTH_SOUTH_WEST:
            return 202.5F;
        case SOUTH_WEST:
            return 225F;
        case WEST:
            return 270F;
        case WEST_NORTH_WEST:
            return 292.5F;
        case WEST_SOUTH_WEST:
            return 247.5F;
        default:
            return 0F;
        }
    }

    /**
     * Warps a user to the warp owner by owner
     *
     * @param world - world to check
     * @param user - user who is warping
     * @param owner - owner of the warp
     */
    public void warpPlayer(World world, User user, UUID owner) {
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

        Island island = addon.getPlugin().getIslands().getIsland(world, owner);
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
                addon.getWarpSignsManager().warpPlayer(user, inFront, owner, directionFacing, pvp);
                return;
            } else if (plugin.getIslands().isSafeLocation(oneDown)) {
                // Try one block down if this is a wall sign
                addon.getWarpSignsManager().warpPlayer(user, oneDown, owner, directionFacing, pvp);
                return;
            }
        } else if (b.getType().name().contains("SIGN")) {
            org.bukkit.block.data.type.Sign s = (org.bukkit.block.data.type.Sign) b.getBlockData();
            BlockFace directionFacing = s.getRotation();
            Location inFront = b.getRelative(directionFacing).getLocation();
            if ((plugin.getIslands().isSafeLocation(inFront))) {
                addon.getWarpSignsManager().warpPlayer(user, inFront, owner, directionFacing, pvp);
                return;
            }
        } else {
            // Warp has been removed
            user.sendMessage("warps.error.does-not-exist");
            addon.getWarpSignsManager().removeWarp(warpSpot);
            return;
        }
        if (!(plugin.getIslands().isSafeLocation(warpSpot))) {
            user.sendMessage("warps.error.not-safe");
            return;
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
            return;
        }
    }

    /**
     * Check if a player has a warp
     * @param playerUUID - player's UUID
     * @return true if they have warp
     */
    public boolean hasWarp(World world, UUID playerUUID) {
        return getWarpMap(world).containsKey(playerUUID);
    }

}
