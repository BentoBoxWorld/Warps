package bskyblock.addin.warps;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import bskyblock.addin.warps.database.object.WarpsDO;
import bskyblock.addin.warps.event.WarpInitiateEvent;
import bskyblock.addin.warps.event.WarpListEvent;
import bskyblock.addin.warps.event.WarpRemoveEvent;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.Util;

/**
 * Handles warping. Players can add one sign
 * 
 * @author tastybento
 * 
 */
public class WarpSignsManager implements Listener {
    private static final boolean DEBUG = true;
    private BSkyBlock bSkyBlock;
    // Map of all warps stored as player, warp sign Location
    private Map<UUID, Location> warpList;
    // Database handler for level data
    private AbstractDatabaseHandler<WarpsDO> handler;

    // The BSkyBlock database object
    private BSBDatabase database;

    private Warp plugin;


    /**
     * @param plugin
     */
    @SuppressWarnings("unchecked")
    public WarpSignsManager(Warp plugin, BSkyBlock bSkyBlock) {
        this.plugin = plugin;
        this.bSkyBlock = bSkyBlock;
        // Get the BSkyBlock database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = (AbstractDatabaseHandler<WarpsDO>) database.getHandler(bSkyBlock, WarpsDO.class);
        // Load the warps
        loadWarpList();
    }

    /**
     * Stores warps in the warp array
     * 
     * @param playerUUID
     * @param loc
     */
    public boolean addWarp(final UUID playerUUID, final Location loc) {
        if (playerUUID == null) {
            return false;
        }
        // Do not allow warps to be in the same location
        if (warpList.containsValue(loc)) {
            return false;
        }
        // Remove the old warp if it existed
        if (warpList.containsKey(playerUUID)) {
            warpList.remove(playerUUID);
        }
        warpList.put(playerUUID, loc);
        saveWarpList();
        Bukkit.getPluginManager().callEvent(new WarpInitiateEvent(plugin, loc, playerUUID));
        return true;
    }

    /**
     * Provides the location of the warp for player or null if one is not found
     * 
     * @param playerUUID
     *            - the warp requested
     * @return Location of warp
     */
    public Location getWarp(UUID playerUUID) {
        if (playerUUID != null && warpList.containsKey(playerUUID)) {
            if (warpList.get(playerUUID) == null) {
                warpList.remove(playerUUID);
                return null;
            }
            return warpList.get(playerUUID);
        } else {
            return null;
        }
    }

    /**
     * @param location
     * @return Name of warp owner
     */
    public String getWarpOwner(Location location) {
        for (UUID playerUUID : warpList.keySet()) {
            if (location.equals(warpList.get(playerUUID))) {
                return bSkyBlock.getPlayers().getName(playerUUID);
            }
        }
        return "";
    }

    /**
     * Get sorted list of warps with most recent players listed first
     * @return UUID collenction
     */
    public Collection<UUID> getSortedWarps() {
        // Bigger value of time means a more recent login
        TreeMap<Long, UUID> map = new TreeMap<Long, UUID>();
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            // Check if the location of the warp still exists, if not, delete it
            if (en.getValue() == null) {
                it.remove();
            } else {
                UUID uuid = en.getKey();
                // If never played, will be zero
                long lastPlayed = plugin.getServer().getOfflinePlayer(uuid).getLastPlayed();
                // This aims to avoid the chance that players logged off at exactly the same time
                if (!map.isEmpty() && map.containsKey(lastPlayed)) {
                    lastPlayed = map.firstKey() - 1;
                }
                map.put(lastPlayed, uuid);
            }
        }

        Collection<UUID> result = map.descendingMap().values();
        // Fire event
        WarpListEvent event = new WarpListEvent(plugin, result);
        plugin.getServer().getPluginManager().callEvent(event);
        // Get the result of any changes by listeners
        result = event.getWarps();
        return result;
    }

    /**
     * Lists all the known warps
     * 
     * @return UUID set of warps
     */
    public Set<UUID> listWarps() {
        // Check if any of the warp locations are null
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            // Check if the location of the warp still exists, if not, delete it
            if (en.getValue() == null) {
                it.remove();
            }
        }
        return warpList.keySet();
    }

    /**
     * Load the warps and checks if they still exist
     */
    public void loadWarpList() {
        plugin.getLogger().info("Loading warps...");
        warpList = new HashMap<>();
        try {
            WarpsDO warps = handler.loadObject("warps");
            // If there's nothing there, start fresh
            if (warps == null) {
                if (DEBUG) 
                    Bukkit.getLogger().info("DEBUG: nothing in the database");              
                warpList = new HashMap<>();
            } else {
                if (DEBUG) 
                    Bukkit.getLogger().info("DEBUG: something in the database");
                warpList = warps.getWarpSigns();
                if (DEBUG) 
                    Bukkit.getLogger().info("DEBUG: warpList size = " + warpList.size());
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            // Check the warp sign
            Block b = en.getValue().getBlock();
            // Check that a warp sign is still there
            if (!b.getType().equals(Material.SIGN_POST) && !b.getType().equals(Material.WALL_SIGN)) {
                plugin.getLogger().warning("Warp at location " + en.getValue() + " has no sign - removing.");
                it.remove();
            }
        }
    }

    /**
     * Checks to see if a sign has been broken
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        User player = User.getInstance(e.getPlayer());
        if (b.getWorld().equals(IslandWorld.getIslandWorld()) || b.getWorld().equals(IslandWorld.getNetherWorld())) {
            if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
                Sign s = (Sign) b.getState();
                if (s != null) {
                    //plugin.getLogger().info("DEBUG: sign found at location " + s.toString());
                    if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.getConfig().getString("welcomeLine"))) {
                        // Do a quick check to see if this sign location is in
                        //plugin.getLogger().info("DEBUG: welcome sign");
                        // the list of warp signs
                        if (warpList.containsValue(s.getLocation())) {
                            //plugin.getLogger().info("DEBUG: warp sign is in list");
                            // Welcome sign detected - check to see if it is
                            // this player's sign
                            if ((warpList.containsKey(player.getUniqueId()) && warpList.get(player.getUniqueId()).equals(s.getLocation()))) {
                                // Player removed sign
                                removeWarp(s.getLocation());
                                Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, s.getLocation(), player.getUniqueId()));
                            } else if (player.isOp()  || player.hasPermission(us.tastybento.bskyblock.config.Settings.PERMPREFIX + "mod.removesign")) {
                                // Op or mod removed sign
                                player.sendMessage("warps.removed");
                                removeWarp(s.getLocation());
                                Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, s.getLocation(), player.getUniqueId()));
                            } else {
                                // Someone else's sign - not allowed
                                player.sendMessage("warps.error.no-remove");
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Event handler for Sign Changes
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignWarpCreate(SignChangeEvent e) {
        plugin.getLogger().info("DEBUG: SignChangeEvent called");
        String title = e.getLine(0);
        User player = User.getInstance(e.getPlayer());
        if (player.getWorld().equals(IslandWorld.getIslandWorld()) || player.getWorld().equals(IslandWorld.getNetherWorld())) {
            plugin.getLogger().info("DEBUG: Correct world");
            if (e.getBlock().getType().equals(Material.SIGN_POST) || e.getBlock().getType().equals(Material.WALL_SIGN)) {

                plugin.getLogger().info("DEBUG: The first line of the sign says " + title);
                // Check if someone is changing their own sign
                // This should never happen !!
                if (title.equalsIgnoreCase(plugin.getConfig().getString("welcomeLine"))) {
                    plugin.getLogger().info("DEBUG: Welcome sign detected");
                    // Welcome sign detected - check permissions
                    if (!(player.hasPermission(us.tastybento.bskyblock.config.Settings.PERMPREFIX + "island.addwarp"))) {
                        player.sendMessage("warps.error.no-permission");
                        return;
                    }
                    /*
                    if(!(ASkyBlockAPI.getInstance().getLongIslandLevel(player.getUniqueId()) > Settings.warpLevelsRestriction)){
                        player.sendMessage(ChatColor.RED + "warps.error.NotEnoughLevel"));
                        return;
                    }
                     */
                    // Check that the player is on their island
                    if (!(bSkyBlock.getIslands().playerIsOnIsland(player))) {
                        player.sendMessage("warps.error.not-on-island");
                        e.setLine(0, ChatColor.RED + plugin.getConfig().getString("welcomeLine"));
                        return;
                    }
                    // Check if the player already has a sign
                    final Location oldSignLoc = getWarp(player.getUniqueId());
                    if (oldSignLoc == null) {
                        //plugin.getLogger().info("DEBUG: Player does not have a sign already");
                        // First time the sign has been placed or this is a new
                        // sign
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            player.sendMessage("warps.success");
                            e.setLine(0, ChatColor.GREEN + plugin.getConfig().getString("welcomeLine"));
                            for (int i = 1; i<4; i++) {
                                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
                            }
                        } else {
                            player.sendMessage("warps.error.duplicate");
                            e.setLine(0, ChatColor.RED + plugin.getConfig().getString("welcomeLine"));
                            for (int i = 1; i<4; i++) {
                                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
                            }
                        }
                    } else {
                        //plugin.getLogger().info("DEBUG: Player already has a Sign");
                        // A sign already exists. Check if it still there and if
                        // so,
                        // deactivate it
                        Block oldSignBlock = oldSignLoc.getBlock();
                        if (oldSignBlock.getType().equals(Material.SIGN_POST) || oldSignBlock.getType().equals(Material.WALL_SIGN)) {
                            // The block is still a sign
                            //plugin.getLogger().info("DEBUG: The block is still a sign");
                            Sign oldSign = (Sign) oldSignBlock.getState();
                            if (oldSign != null) {
                                //plugin.getLogger().info("DEBUG: Sign block is a sign");
                                if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.getConfig().getString("welcomeLine"))) {
                                    //plugin.getLogger().info("DEBUG: Old sign had a green welcome");
                                    oldSign.setLine(0, ChatColor.RED + plugin.getConfig().getString("welcomeLine"));
                                    oldSign.update(true, false);
                                    player.sendMessage("warps.deactivate");
                                    removeWarp(player.getUniqueId());
                                    Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, oldSign.getLocation(), player.getUniqueId()));
                                }
                            }
                        }
                        // Set up the warp
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            player.sendMessage("warps.error.success");
                            e.setLine(0, ChatColor.GREEN + plugin.getConfig().getString("welcomeLine"));
                        } else {
                            player.sendMessage("warps.error.duplicate");
                            e.setLine(0, ChatColor.RED + plugin.getConfig().getString("welcomeLine"));
                        }
                    }
                }
            }
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
                if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.getConfig().getString("welcomeLine"))) {
                    s.setLine(0, ChatColor.RED + plugin.getConfig().getString("welcomeLine"));
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
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            if (en.getValue().equals(loc)) {
                // Inform player
                User p = User.getInstance(plugin.getServer().getPlayer(en.getKey()));
                if (p != null) {
                    // Inform the player
                    p.sendMessage("warps.sign-removed");
                } 
                it.remove();
            }
        }
        saveWarpList();
    }
    
    /**
     * Removes a warp when the welcome sign is destroyed. Called by
     * WarpSigns.java.
     * 
     * @param uuid
     */
    public void removeWarp(UUID uuid) {
        if (warpList.containsKey(uuid)) {
            popSign(warpList.get(uuid));
            warpList.remove(uuid);
        }
        saveWarpList();
    }

    /**
     * Saves the warp lists to the database
     */
    public void saveWarpList() {
        if (warpList == null) {
            return;
        }
        //plugin.getLogger().info("Saving warps...");
        try {
            handler.saveObject(new WarpsDO().save(warpList));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
                | InstantiationException | NoSuchMethodException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Gets the warp sign text
     * @param uuid
     * @return List of lines
     */
    public List<String> getSignText(UUID uuid) {
        List<String> result = new ArrayList<>();
        //get the sign info
        Location signLocation = getWarp(uuid);
        if (signLocation == null) {
            plugin.getWarpSignsManager().removeWarp(uuid);
        } else          
            // Get the sign info if it exists
            if (signLocation.getBlock().getType().equals(Material.SIGN_POST) || signLocation.getBlock().getType().equals(Material.WALL_SIGN)) {
                Sign sign = (Sign)signLocation.getBlock().getState();
                result.addAll(Arrays.asList(sign.getLines()));
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
            user.sendLegacyMessage(user.getTranslation("igs." + SettingsFlag.PVP_OVERWORLD) + " " + user.getTranslation("igs.allowed"));
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
    public void warpPlayer(User user, UUID owner) {
        final Location warpSpot = plugin.getWarpSignsManager().getWarp(owner);
        // Check if the warp spot is safe
        if (warpSpot == null) {
            user.sendMessage("warps.error.NotReadyYet");
            plugin.getLogger().warning("Null warp found, owned by " + plugin.getBSkyBlock().getPlayers().getName(owner));
            return;
        }
        // Find out if island is locked
        // TODO: Fire event

        Island island = plugin.getBSkyBlock().getIslands().getIsland(owner);
        boolean pvp = false;
        if ((warpSpot.getWorld().equals(IslandWorld.getIslandWorld()) && island.getFlag(SettingsFlag.PVP_OVERWORLD)) 
                || (warpSpot.getWorld().equals(IslandWorld.getNetherWorld()) && island.getFlag(SettingsFlag.PVP_NETHER))) {
            pvp = true;
        }
        // Find out which direction the warp is facing
        Block b = warpSpot.getBlock();
        if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
            Sign sign = (Sign) b.getState();
            org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
            BlockFace directionFacing = s.getFacing();
            Location inFront = b.getRelative(directionFacing).getLocation();
            Location oneDown = b.getRelative(directionFacing).getRelative(BlockFace.DOWN).getLocation();
            if ((Util.isSafeLocation(inFront))) {
                plugin.getWarpSignsManager().warpPlayer(user, inFront, owner, directionFacing, pvp);
                return;
            } else if (b.getType().equals(Material.WALL_SIGN) && Util.isSafeLocation(oneDown)) {
                // Try one block down if this is a wall sign
                plugin.getWarpSignsManager().warpPlayer(user, oneDown, owner, directionFacing, pvp);
                return;
            }
        } else {
            // Warp has been removed
            user.sendMessage("warps.error.DoesNotExist");
            plugin.getWarpSignsManager().removeWarp(warpSpot);
            return;
        }
        if (!(Util.isSafeLocation(warpSpot))) {
            user.sendMessage("warps.error.NotSafe");
            // WALL_SIGN's will always be unsafe if the place in front is obscured.
            if (b.getType().equals(Material.SIGN_POST)) {
                plugin.getLogger().warning(
                        "Unsafe warp found at " + warpSpot.toString() + " owned by " + plugin.getBSkyBlock().getPlayers().getName(owner));

            }
            return;
        } else {
            final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
                    warpSpot.getBlockZ() + 0.5D);
            user.teleport(actualWarp);
            if (pvp) {
                user.sendLegacyMessage(user.getTranslation("igs." + SettingsFlag.PVP_OVERWORLD) + " " + user.getTranslation("igs.Allowed"));
                user.getWorld().playSound(user.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
            } else {
                user.getWorld().playSound(user.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
            }
            return;
        }
    }

}
