package bskyblock.addin.warps;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import bskyblock.addin.warps.database.object.Warps;
import bskyblock.addin.warps.event.WarpInitiateEvent;
import bskyblock.addin.warps.event.WarpListEvent;
import bskyblock.addin.warps.event.WarpRemoveEvent;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * Handles warping. Players can add one sign
 * 
 * @author tastybento
 * 
 */
public class WarpSigns extends AddonHelper implements Listener {
    //private final static boolean DEBUG = false;
    private BSkyBlock bSkyBlock;
    // Map of all warps stored as player, warp sign Location
    private Map<UUID, Location> warpList;
    // Database handler for level data
    private AbstractDatabaseHandler<Warps> handler;

    // The BSkyBlock database object
    private BSBDatabase database;


    /**
     * @param plugin
     */
    @SuppressWarnings("unchecked")
    public WarpSigns(Warp plugin, BSkyBlock bSkyBlock) {
        super(plugin);
        this.bSkyBlock = bSkyBlock;
        // Get the BSkyBlock database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = (AbstractDatabaseHandler<Warps>) database.getHandler(bSkyBlock, Warps.class);
        // Load the warps
        loadWarpList();
    }

    /**
     * Checks to see if a sign has been broken
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player player = e.getPlayer();
        if (b.getWorld().equals(IslandWorld.getIslandWorld()) || b.getWorld().equals(IslandWorld.getNetherWorld())) {
            if (b.getType().equals(Material.SIGN_POST) || b.getType().equals(Material.WALL_SIGN)) {
                Sign s = (Sign) b.getState();
                if (s != null) {
                    //plugin.getLogger().info("DEBUG: sign found at location " + s.toString());
                    if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.getLocale().get("warps.welcomeLine"))) {
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
                                Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player.getUniqueId()).get("warps.removed"));
                                removeWarp(s.getLocation());
                                Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, s.getLocation(), player.getUniqueId()));
                            } else {
                                // Someone else's sign - not allowed
                                Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.no-remove"));
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
        //plugin.getLogger().info("DEBUG: SignChangeEvent called");
        String title = e.getLine(0);
        Player player = e.getPlayer();
        if (player.getWorld().equals(IslandWorld.getIslandWorld()) || player.getWorld().equals(IslandWorld.getNetherWorld())) {
            //plugin.getLogger().info("DEBUG: Correct world");
            if (e.getBlock().getType().equals(Material.SIGN_POST) || e.getBlock().getType().equals(Material.WALL_SIGN)) {

                //plugin.getLogger().info("DEBUG: The first line of the sign says " + title);
                // Check if someone is changing their own sign
                // This should never happen !!
                if (title.equalsIgnoreCase(plugin.getLocale().get("warps.welcomeLine"))) {
                    //plugin.getLogger().info("DEBUG: Welcome sign detected");
                    // Welcome sign detected - check permissions
                    if (!(VaultHelper.hasPerm(player, us.tastybento.bskyblock.config.Settings.PERMPREFIX + "island.addwarp"))) {
                        Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.no-permission"));
                        return;
                    }
                    /*
                    if(!(ASkyBlockAPI.getInstance().getLongIslandLevel(player.getUniqueId()) > Settings.warpLevelsRestriction)){
                        Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.NotEnoughLevel"));
                        return;
                    }
                    */
                    // Check that the player is on their island
                    if (!(bSkyBlock.getIslands().playerIsOnIsland(player))) {
                        Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.not-on-island"));
                        e.setLine(0, ChatColor.RED + plugin.getLocale().get("warps.welcomeLine"));
                        return;
                    }
                    // Check if the player already has a sign
                    final Location oldSignLoc = getWarp(player.getUniqueId());
                    if (oldSignLoc == null) {
                        //plugin.getLogger().info("DEBUG: Player does not have a sign already");
                        // First time the sign has been placed or this is a new
                        // sign
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player.getUniqueId()).get("warps.success"));
                            e.setLine(0, ChatColor.GREEN + plugin.getLocale().get("warps.welcomeLine"));
                            for (int i = 1; i<4; i++) {
                                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
                            }
                        } else {
                            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.duplicate"));
                            e.setLine(0, ChatColor.RED + plugin.getLocale().get("warps.welcomeLine"));
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
                                if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.getLocale().get("warps.welcomeLine"))) {
                                    //plugin.getLogger().info("DEBUG: Old sign had a green welcome");
                                    oldSign.setLine(0, ChatColor.RED + plugin.getLocale().get("warps.welcomeLine"));
                                    oldSign.update(true, false);
                                    Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.deactivate"));
                                    removeWarp(player.getUniqueId());
                                    Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(plugin, oldSign.getLocation(), player.getUniqueId()));
                                }
                            }
                        }
                        // Set up the warp
                        if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
                            Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player.getUniqueId()).get("warps.error.success"));
                            e.setLine(0, ChatColor.GREEN + plugin.getLocale().get("warps.welcomeLine"));
                        } else {
                            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.duplicate"));
                            e.setLine(0, ChatColor.RED + plugin.getLocale().get("warps.welcomeLine"));
                        }
                    }
                }
            }
        }
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
            handler.saveObject(new Warps().save(warpList));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
                | InstantiationException | NoSuchMethodException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Loads the warps and checks if they still exist
     */
    public void loadWarpList() {
        plugin.getLogger().info("Loading warps...");
        warpList = new HashMap<>();
        try {
            Warps warps = handler.loadObject("warps");
            // If there's nothing there, start fresh
            if (warps == null) {
                warpList = new HashMap<>();
            } else {
                warpList = warps.getWarpSigns();
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (Entry<UUID, Location> en : warpList.entrySet()) {
            plugin.getLogger().info("DEBUG: " + en.getKey() + " " + en.getValue());
        }
        Iterator<Entry<UUID, Location>> it = warpList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Location> en = it.next();
            // Chck the warp sign
            Block b = en.getValue().getBlock();
            // Check that a warp sign is still there
            if (!b.getType().equals(Material.SIGN_POST) && !b.getType().equals(Material.WALL_SIGN)) {
                plugin.getLogger().warning("Warp at location " + en.getValue() + " has no sign - removing.");
                it.remove();
            }
        }
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
        // Update warp signs
        // Run one tick later because text gets updated at the end of tick
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

            @Override
            public void run() {
                plugin.getWarpPanel().addWarp(playerUUID);
                plugin.getWarpPanel().updatePanel();
                Bukkit.getPluginManager().callEvent(new WarpInitiateEvent(plugin, loc, playerUUID));
            }});
        return true;
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
        // Update warp signs
        // Run one tick later because text gets updated at the end of tick
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

            @Override
            public void run() {
                plugin.getWarpPanel().updatePanel();

            }});
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
                if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + plugin.getLocale().get("warps.welcomeLine"))) {
                    s.setLine(0, ChatColor.RED + plugin.getLocale().get("warps.welcomeLine"));
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
                Player p = plugin.getServer().getPlayer(en.getKey());
                if (p != null) {
                    // Inform the player
                    Util.sendMessage(p, ChatColor.RED + plugin.getLocale(p.getUniqueId()).get("warps.sign-removed"));
                } 
                /*
                else {
                    plugin.getMessages().setMessage(en.getKey(), ChatColor.RED + plugin.myLocale(en.getKey()).warpssignRemoved);
                }
                */
                it.remove();
            }
        }
        saveWarpList();
        plugin.getWarpPanel().updatePanel();
    }

    /**
     * Lists all the known warps
     * 
     * @return String set of warps
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
     * @return Sorted list of warps with most recent players listed first
     */
    public Collection<UUID> listSortedWarps() {
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

    
}