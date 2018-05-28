package bskyblock.addon.warps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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

import bskyblock.addon.warps.database.object.WarpsData;
import bskyblock.addon.warps.event.WarpInitiateEvent;
import bskyblock.addon.warps.event.WarpListEvent;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

/**
 * Handles warping. Players can add one sign
 * 
 * @author tastybento
 * 
 */
public class WarpSignsManager {
    private static final boolean DEBUG2 = false;
    private static final int MAX_WARPS = 600;
    private BSkyBlock plugin;
    // Map of all warps stored as player, warp sign Location
    private Map<World, Map<UUID, Location>> worldsWarpList;
    // Database handler for level data
    private BSBDatabase<WarpsData> handler;

    private Warp addon;

    public Map<UUID, Location> getWarpList(World world) {
        worldsWarpList.putIfAbsent(world, new HashMap<>());
        return worldsWarpList.get(world);
    }

    /**
     * @param addon - addon
     * @param plugin - BSB plugin
     */
    public WarpSignsManager(Warp addon, BSkyBlock plugin) {
        this.addon = addon;
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = new BSBDatabase<>(addon, WarpsData.class);
        // Load the warps
        loadWarpList();
    }

    /**
     * Stores warps in the warp array
     * 
     * @param playerUUID - the player's UUID
     * @param loc
     */
    public boolean addWarp(final UUID playerUUID, final Location loc) {
        if (playerUUID == null) {
            return false;
        }
        // Do not allow warps to be in the same location
        if (getWarpList(loc.getWorld()).containsValue(loc)) {
            return false;
        }
        // Remove the old warp if it existed
        if (getWarpList(loc.getWorld()).containsKey(playerUUID)) {
            getWarpList(loc.getWorld()).remove(playerUUID);
        }
        getWarpList(loc.getWorld()).put(playerUUID, loc);
        saveWarpList();
        Bukkit.getPluginManager().callEvent(new WarpInitiateEvent(addon, loc, playerUUID));
        return true;
    }

    /**
     * Provides the location of the warp for player or null if one is not found
     * 
     * @param playerUUID - the player's UUID
     *            - the warp requested
     * @return Location of warp
     */
    public Location getWarp(World world, UUID playerUUID) {
        if (playerUUID != null && getWarpList(world).containsKey(playerUUID)) {
            if (getWarpList(world).get(playerUUID) == null) {
                getWarpList(world).remove(playerUUID);
                return null;
            }
            return getWarpList(world).get(playerUUID);
        } else {
            return null;
        }
    }

    /**
     * @param location
     * @return Name of warp owner
     */
    public String getWarpOwner(Location location) {
        for (UUID playerUUID : getWarpList(location.getWorld()).keySet()) {
            if (location.equals(getWarpList(location.getWorld()).get(playerUUID))) {
                return plugin.getPlayers().getName(playerUUID);
            }
        }
        return "";
    }

    /**
     * Get sorted list of warps with most recent players listed first
     * @return UUID collection
     */
    public List<UUID> getSortedWarps(World world) {
        // Bigger value of time means a more recent login
        TreeMap<Long, UUID> map = new TreeMap<Long, UUID>();
        Iterator<Entry<UUID, Location>> it = getWarpList(world).entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            // Check if the location of the warp still exists, if not, delete it
            if (en.getValue() == null) {
                it.remove();
            } else {
                UUID uuid = en.getKey();
                // If never played, will be zero
                long lastPlayed = addon.getServer().getOfflinePlayer(uuid).getLastPlayed();
                // This aims to avoid the chance that players logged off at exactly the same time
                if (!map.isEmpty() && map.containsKey(lastPlayed)) {
                    lastPlayed = map.firstKey() - 1;
                }
                map.put(lastPlayed, uuid);
            }
        }
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
     * Lists all the known warps
     * @param world 
     * 
     * @return UUID set of warps
     */
    public Set<UUID> listWarps(World world) {
        // Remove any null locations
        getWarpList(world).values().removeIf(Objects::isNull);
        return getWarpList(world).entrySet().stream().filter(e -> Util.sameWorld(world, e.getValue().getWorld())).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * Load the warps and checks if they still exist
     */
    public void loadWarpList() {
        addon.getLogger().info("Loading warps...");
        worldsWarpList = new HashMap<>();
        WarpsData warps = handler.loadObject("warps");
        // Load into map
        if (warps != null) {
            warps.getWarpSigns().forEach((k,v) -> {
                if (k != null && (k.getBlock().getType().equals(Material.SIGN_POST) || k.getBlock().getType().equals(Material.WALL_SIGN))) {
                    // Add to map
                    getWarpList(k.getWorld()).put(v, k);
                }
            });
        }
    }

    /**
     * Changes the sign to red if it exists
     * @param loc
     */
    private void popSign(Location loc) {
        Block b = loc.getBlock();
        if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
            Sign s = (Sign) b.getState();
            if (s != null) {
                if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getConfig().getString("welcomeLine"))) {
                    s.setLine(0, ChatColor.RED + addon.getConfig().getString("welcomeLine"));
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
        //plugin.getLogger().info("Asked to remove warp at " + loc);
        popSign(loc);
        Iterator<Entry<UUID, Location>> it = getWarpList(loc.getWorld()).entrySet().iterator();
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
        if (getWarpList(world).containsKey(uuid)) {
            popSign(getWarpList(world).get(uuid));
            getWarpList(world).remove(uuid);
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
     * Gets the warp sign text
     * @param uuid
     * @return List of lines
     */
    public List<String> getSignText(World world, UUID uuid) {
        List<String> result = new ArrayList<>();
        //get the sign info
        Location signLocation = getWarp(world, uuid);
        if (signLocation == null) {
            addon.getWarpSignsManager().removeWarp(world, uuid);
        } else { 
            if (DEBUG2)
                Bukkit.getLogger().info("DEBUG: getting sign text");
            // Get the sign info if it exists
            if (signLocation.getBlock().getType().equals(Material.SIGN_POST) || signLocation.getBlock().getType().equals(Material.WALL_SIGN)) {
                if (DEBUG2)
                    Bukkit.getLogger().info("DEBUG: sign is a sign");
                Sign sign = (Sign)signLocation.getBlock().getState();
                result.addAll(Arrays.asList(sign.getLines()));
                if (DEBUG2)
                    Bukkit.getLogger().info("DEBUG: " + result.toString());
            }
            // Clean up - remove the [WELCOME] line
            result.remove(0);
            // Remove any trailing blank lines
            ListIterator<String> it = result.listIterator(result.size());
            while (it.hasPrevious()) {
                String line = it.previous();
                if (line.isEmpty())
                    it.remove();
                else
                    break;
            }
        }

        return result;
    }

    /**
     * Warps a player to a spot in front of a sign
     * @param user
     * @param inFront
     * @param foundWarp
     * @param directionFacing
     */
    private void warpPlayer(User user, Location inFront, UUID foundWarp, BlockFace directionFacing, boolean pvp) {
        // convert blockface to angle
        float yaw = blockFaceToFloat(directionFacing);
        final Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5D, inFront.getBlockY(),
                inFront.getBlockZ() + 0.5D, yaw, 30F);
        user.teleport(actualWarp);
        if (pvp) {
            //user.sendLegacyMessage(user.getTranslation("igs." + SettingsFlag.PVP_OVERWORLD) + " " + user.getTranslation("igs.allowed"));
            user.getWorld().playSound(user.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
        } else {
            user.getWorld().playSound(user.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
        }
        User warpOwner = User.getInstance(foundWarp);
        if (!warpOwner.equals(user)) {
            warpOwner.sendMessage("warps.PlayerWarped", "[name]", user.getName());
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
     * @param user
     * @param owner
     */
    public void warpPlayer(World world, User user, UUID owner) {
        final Location warpSpot = addon.getWarpSignsManager().getWarp(world, owner);
        // Check if the warp spot is safe
        if (warpSpot == null) {
            user.sendMessage("warps.error.NotReadyYet");
            addon.getLogger().warning("Null warp found, owned by " + addon.getBSkyBlock().getPlayers().getName(owner));
            return;
        }
        // Find out if island is locked
        // TODO: Fire event

        Island island = addon.getBSkyBlock().getIslands().getIsland(world, owner);
        boolean pvp = false;
        if (island != null) {
            //if ((warpSpot.getWorld().equals(IslandWorld.getIslandWorld()) && island.getFlag(SettingsFlag.PVP_OVERWORLD)) 
            //        || (warpSpot.getWorld().equals(IslandWorld.getNetherWorld()) && island.getFlag(SettingsFlag.PVP_NETHER))) {
            //    pvp = true;
            //}
        }
        // Find out which direction the warp is facing
        Block b = warpSpot.getBlock();
        if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
            Sign sign = (Sign) b.getState();
            org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
            BlockFace directionFacing = s.getFacing();
            Location inFront = b.getRelative(directionFacing).getLocation();
            Location oneDown = b.getRelative(directionFacing).getRelative(BlockFace.DOWN).getLocation();
            if ((plugin.getIslands().isSafeLocation(inFront))) {
                addon.getWarpSignsManager().warpPlayer(user, inFront, owner, directionFacing, pvp);
                return;
            } else if (b.getType().equals(Material.WALL_SIGN) && plugin.getIslands().isSafeLocation(oneDown)) {
                // Try one block down if this is a wall sign
                addon.getWarpSignsManager().warpPlayer(user, oneDown, owner, directionFacing, pvp);
                return;
            }
        } else {
            // Warp has been removed
            user.sendMessage("warps.error.DoesNotExist");
            addon.getWarpSignsManager().removeWarp(warpSpot);
            return;
        }
        if (!(plugin.getIslands().isSafeLocation(warpSpot))) {
            user.sendMessage("warps.error.NotSafe");
            // WALL_SIGN's will always be unsafe if the place in front is obscured.
            if (b.getType().equals(Material.SIGN_POST)) {
                addon.getLogger().warning(
                        "Unsafe warp found at " + warpSpot.toString() + " owned by " + addon.getBSkyBlock().getPlayers().getName(owner));

            }
            return;
        } else {
            final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
                    warpSpot.getBlockZ() + 0.5D);
            user.teleport(actualWarp);
            if (pvp) {
                //user.sendLegacyMessage(user.getTranslation("igs." + SettingsFlag.PVP_OVERWORLD) + " " + user.getTranslation("igs.Allowed"));
                user.getWorld().playSound(user.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
            } else {
                user.getWorld().playSound(user.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
            }
            return;
        }
    }

    /**
     * Check if a player has a warp
     * @param playerUUID - player's UUID
     * @return true if they have warp
     */
    public boolean hasWarp(World world, UUID playerUUID) {
        return getWarpList(world).containsKey(playerUUID);
    }

}
