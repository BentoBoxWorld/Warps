package world.bentobox.warps.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.warps.objects.PlayerWarp;
import world.bentobox.warps.Warp;
import world.bentobox.warps.event.WarpCreateEvent;
import world.bentobox.warps.event.WarpInitiateEvent;
import world.bentobox.warps.objects.WarpsData;
import world.bentobox.warps.panels.Utils;


/**
 * Handles warping. Players can add one sign
 *
 * @author tastybento
 *
 */
public class WarpSignsManager {
    private static final int MAX_WARPS = 600;
    private static final String WARPS = "warps";
    private final BentoBox plugin;
    // Map of all warps stored as player, warp sign Location
    private Map<World, Map<UUID, PlayerWarp>> worldsWarpList;
    // Database handler for level data
    private final Database<WarpsData> handler;

    private final Warp addon;
    private WarpsData warpsData = new WarpsData();

    /**
     * Get the warp map for this world
     * @param world - world
     * @return map of warps
     */
    @NonNull
    public Map<UUID, PlayerWarp> getWarpMap(@Nullable World world) {
        return worldsWarpList.computeIfAbsent(Util.getWorld(world), k -> new HashMap<>());
    }

    /**
     * @param addon - addon
     * @param plugin - plugin
     */
    public WarpSignsManager(Warp addon, BentoBox plugin) {
        this.addon = addon;
        this.plugin = plugin;
        this.worldsWarpList = new HashMap<>();
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
        for (PlayerWarp playerWarp : getWarpMap(loc.getWorld()).values()) {
            if (playerWarp.getLocation().equals(loc)) {
                this.removeWarp(loc);
                break;
            }
        }
        getWarpMap(loc.getWorld()).put(playerUUID, new PlayerWarp(loc, true));
        saveWarpList();
        Bukkit.getPluginManager().callEvent(new WarpCreateEvent(addon, loc, playerUUID));
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
        PlayerWarp playerWarp = getWarpMap(world).get(playerUUID);
        return playerWarp != null ? playerWarp.getLocation() : null;
    }

    /**
     * Get the name of the warp owner by location
     * @param location to search
     * @return Name of warp owner or empty string if there is none
     */
    @NonNull
    public String getWarpOwner(Location location) {
        return getWarpMap(location.getWorld()).entrySet().stream().filter(en -> en.getValue().getLocation().equals(location))
                .findFirst().map(en -> plugin.getPlayers().getName(en.getKey())).orElse("");
    }

    /**
     * Get the optional UUID of the warp owner by location
     * @param location to search
     * @return Optional UUID of warp owner or empty if there is none
     */
    public Optional<UUID> getWarpOwnerUUID(Location location) {
        return getWarpMap(location.getWorld()).entrySet().stream().filter(en -> en.getValue().getLocation().equals(location))
                .findFirst().map(Map.Entry::getKey);
    }

    /**
     * Get sorted list of warps with most recent players listed first
     * @return UUID list
     */
    public CompletableFuture<List<UUID>> getSortedWarps(@NonNull World world) {
        CompletableFuture<List<UUID>> r = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> processWarpMap(r, world));
        return r;
    }

    public List<UUID> processWarpMap(CompletableFuture<List<UUID>> r, @NonNull World world) {
        // Remove any null locations - this can happen if an admin changes the name of the world and signs point to old locations
        getWarpMap(world).values().removeIf(Objects::isNull);
        // Bigger value of time means a more recent login
        TreeMap<Long, UUID> map = new TreeMap<>();
        getWarpMap(world).forEach((uuid, value) -> {
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
        // Return to main thread
        Bukkit.getScheduler().runTask(plugin, () -> r.complete(list));
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
        return getWarpMap(world).entrySet().stream().filter(e -> Util.sameWorld(world, Objects.requireNonNull(e.getValue().getLocation().getWorld()))).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * Load the warps and check if they still exist
     */
    public void loadWarpList() {
        addon.log("Loading warps...");
        worldsWarpList = new HashMap<>();
        if (handler.objectExists(WARPS)) {
            warpsData = handler.loadObject(WARPS);
            // Load into map
            if (warpsData != null) {
                warpsData.getWarpSigns().forEach((pw, uuid) -> {
                    Location location = pw.getLocation();
                    if (location != null && location.getWorld() != null) {
                        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                                && !location.getBlock().getType().name().contains("SIGN")) {
                            return;
                        }

                        // Add to map
                        getWarpMap(location.getWorld()).put(uuid, new PlayerWarp(location, true));
                    }
                });
            } else {
                warpsData = new WarpsData();
            }
        }
    }

    /**
     * Changes the sign to red if it exists
     * @param loc location to pop
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
     * @param loc location to remove
     */
    public void removeWarp(Location loc) {
        popSign(loc);
        Iterator<Entry<UUID, PlayerWarp>> it = getWarpMap(loc.getWorld()).entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, PlayerWarp> en = it.next();
            if (en.getValue().getLocation().equals(loc)) {
                // Inform player
                Optional.ofNullable(addon.getServer().getPlayer(en.getKey()))
                .map(User::getInstance)
                .ifPresent(user -> user.sendMessage("warps.sign-removed"));
                // Remove sign from warp panel cache
                addon.getSignCacheManager().removeWarp(loc.getWorld(), en.getKey());
                it.remove();
            }
        }
        saveWarpList();
    }

    /**
     * Remove warp sign owned by UUID
     *
     * @param uuid UUID of owner to remove
     */
    public void removeWarp(World world, UUID uuid) {
        if (getWarpMap(world).containsKey(uuid)) {
            popSign(getWarpMap(world).get(uuid).getLocation());
            getWarpMap(world).remove(uuid);

        }
        // Remove sign from warp panel cache
        addon.getSignCacheManager().removeWarp(world, uuid);
        saveWarpList();
    }

    /**
     * Remove the warp from the warp map
     * @param world - world
     * @param uuid - uuid of owner
     */
    public void removeWarpFromMap(World world, UUID uuid) {
        getWarpMap(world).remove(uuid);
    }

    /**
     * Saves the warp lists to the database
     */
    public void saveWarpList() {
        handler.saveObjectAsync(warpsData.save(worldsWarpList));
        addon.getSignCacheManager().saveCache();
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
        //get the sign info
        Location signLocation = getWarp(world, uuid);
        if (signLocation == null || !signLocation.getBlock().getType().name().contains("SIGN")) {
            return new SignCacheItem();
        }
        Sign sign = (Sign)signLocation.getBlock().getState();
        List<String> result = new ArrayList<>(Arrays.asList(sign.getLines()));
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
                    Utils.getPermissionValue(User.getInstance(uuid), prefix + "island.warp",
                            Material.OAK_SIGN.name()));
        }
        else
        {
            icon = null;
        }

        if (icon != null && icon.name().contains("SIGN")) {
            return new SignCacheItem(result, Material.valueOf(sign.getType().name().replace("WALL_", "")));
        }
        else
        {
            return new SignCacheItem(result, icon);
        }
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
        WarpInitiateEvent e = new WarpInitiateEvent(addon, actualWarp, user.getUniqueId());
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return;
        }
        //BentoBox prevents people from teleporting to an island when
        //the user is banned from the island for example.
        //By checking if the teleport succeeded before sending the messages,
        //we prevent issues where no one teleported, but people still
        //get messages about it.
        Util.teleportAsync(user.getPlayer(), actualWarp, TeleportCause.COMMAND).thenAccept(tpResult -> {
            if(Boolean.FALSE.equals(tpResult)) return;

            User warpOwner = Objects.requireNonNull(User.getInstance(signOwner));
            // Hide invisible players
            if (warpOwner.isOnline() && !warpOwner.getPlayer().canSee(user.getPlayer())) {
                return;
            }
            if (pvp) {
                user.sendMessage("protection.flags.PVP_OVERWORLD.enabled");
                user.getWorld().playSound(Objects.requireNonNull(user.getLocation()), Sound.ENTITY_ARROW_HIT, 1F, 1F);
            } else {
                user.getWorld().playSound(Objects.requireNonNull(user.getLocation()), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
            }
            if (!warpOwner.equals(user)) {
                final String gameMode = BentoBox
                        .getInstance()
                        .getIWM()
                        .getFriendlyName(actualWarp.getWorld());
                warpOwner.sendMessage("warps.player-warped", "[name]", user.getName(), "[gamemode]", gameMode);
            }
        });
    }

    /**
     * Warps a user to the warp owned by owner
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
            switch (Objects.requireNonNull(warpSpot.getWorld()).getEnvironment()) {
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
        if (Tag.WALL_SIGNS.isTagged(b.getType())) {
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
        } else if (Tag.ALL_HANGING_SIGNS.isTagged(b.getType())) {
            Location below = b.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getLocation();
            if ((addon.getIslands().isSafeLocation(below))) {
                warpPlayer(user, below, owner, BlockFace.DOWN, pvp);
                return;
            }
        } else if (Tag.STANDING_SIGNS.isTagged(b.getType())) {
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
                user.sendMessage("protection.flags.PVP_OVERWORLD.enabled");
                user.getWorld().playSound(Objects.requireNonNull(user.getLocation()), Sound.ENTITY_ARROW_HIT, 1F, 1F);
            } else {
                user.getWorld().playSound(Objects.requireNonNull(user.getLocation()), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
            }
            Util.teleportAsync(user.getPlayer(), actualWarp, TeleportCause.COMMAND);
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
}
