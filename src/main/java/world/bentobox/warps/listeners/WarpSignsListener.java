package world.bentobox.warps.listeners;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamKickEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamLeaveEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.warps.Warp;
import world.bentobox.warps.event.WarpRemoveEvent;

/**
 * Handles warping. Players can add one sign
 *
 * @author tastybento
 *
 */
public class WarpSignsListener implements Listener {

    private BentoBox plugin;

    private Warp addon;

    /**
     * @param addon - addon
     */
    public WarpSignsListener(Warp addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerLeave(TeamLeaveEvent e) {
        // Remove any warp signs from this game mode
        addon.getWarpSignsManager().removeWarp(e.getIsland().getWorld(), e.getPlayerUUID());
        User.getInstance(e.getPlayerUUID()).sendMessage("warps.deactivate");
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerLeave(TeamKickEvent e) {
        // Remove any warp signs from this game mode
        addon.getWarpSignsManager().removeWarp(e.getIsland().getWorld(), e.getPlayerUUID());
        User.getInstance(e.getPlayerUUID()).sendMessage("warps.deactivate");
    }
    
    /**
     * Checks to see if a sign has been broken
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        boolean inWorld = addon.getPlugin().getIWM().inWorld(b.getWorld());
        // Signs only
        // FIXME: When we drop support for 1.13, switch to Tag.SIGNS
        if (!e.getBlock().getType().name().contains("SIGN")
                || (inWorld && !addon.inRegisteredWorld(b.getWorld()))
                || (!inWorld && !addon.getSettings().isAllowInOtherWorlds()) ) {
            return;
        }
        User user = User.getInstance(e.getPlayer());
        if (isWarpSign(b)) {
            if (isPlayersSign(e.getPlayer(), b, inWorld)) {
                addon.getWarpSignsManager().removeWarp(b.getLocation());
                Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(addon, b.getLocation(), user.getUniqueId()));
            } else {
                // Someone else's sign - not allowed
                user.sendMessage("warps.error.no-remove");
                e.setCancelled(true);
            }
        }
    }

    private boolean isPlayersSign(Player player, Block b, boolean inWorld) {
        // Welcome sign detected - check to see if it is this player's sign
        Map<UUID, Location> list = addon.getWarpSignsManager().getWarpMap(b.getWorld());
        String reqPerm = inWorld ? addon.getPermPrefix(b.getWorld()) + "mod.removesign" : Warp.WELCOME_WARP_SIGNS + ".mod.removesign";
        return ((list.containsKey(player.getUniqueId()) && list.get(player.getUniqueId()).equals(b.getLocation()))
                || player.isOp()  || player.hasPermission(reqPerm));
    }

    private boolean isWarpSign(Block b) {
        Sign s = (Sign) b.getState();
        return s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getSettings().getWelcomeLine())
                && addon.getWarpSignsManager().getWarpMap(b.getWorld()).containsValue(s.getLocation());
    }

    /**
     * Event handler for Sign Changes
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignWarpCreate(SignChangeEvent e) {
        Block b = e.getBlock();
        boolean inWorld = addon.getPlugin().getIWM().inWorld(b.getWorld());
        if ((inWorld && !addon.inRegisteredWorld(b.getWorld())) || (!inWorld && !addon.getSettings().isAllowInOtherWorlds()) ) {
            return;
        }
        String title = e.getLine(0);
        User user = User.getInstance(e.getPlayer());
        // Check if someone is changing their own sign
        if (title.equalsIgnoreCase(addon.getSettings().getWelcomeLine())) {
            // Welcome sign detected - check permissions
            if (noPerms(user, b.getWorld(), inWorld)) {
                return;
            }
            if (inWorld && noLevelOrIsland(user, b.getWorld())) {
                e.setLine(0, ChatColor.RED + addon.getSettings().getWelcomeLine());
                return;
            }
            // Check if the player already has a sign
            final Location oldSignLoc = addon.getWarpSignsManager().getWarp(b.getWorld(), user.getUniqueId());
            if (oldSignLoc == null) {
                // First time the sign has been placed or this is a new
                // sign
                addSign(e, user, b);
            } else {
                // A sign already exists. Check if it still there and if
                // so,
                // deactivate it
                Block oldSignBlock = oldSignLoc.getBlock();
                // FIXME: When we drop support for 1.13, switch to Tag.SIGNS
                if (oldSignBlock.getType().name().contains("SIGN")) {
                    // The block is still a sign
                    Sign oldSign = (Sign) oldSignBlock.getState();
                    if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getSettings().getWelcomeLine())) {
                        oldSign.setLine(0, ChatColor.RED + addon.getSettings().getWelcomeLine());
                        oldSign.update(true, false);
                        user.sendMessage("warps.deactivate");
                        addon.getWarpSignsManager().removeWarp(oldSignBlock.getWorld(), user.getUniqueId());
                        Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(addon, oldSign.getLocation(), user.getUniqueId()));
                    }
                }
                // Set up the new warp sign
                addSign(e, user, b);
            }
        }

    }

    private boolean noLevelOrIsland(User user, World world) {
        // Get level if level addon is available
        Long level = addon.getLevel(Util.getWorld(world), user.getUniqueId());
        if (level != null && level < addon.getSettings().getWarpLevelRestriction()) {
            user.sendMessage("warps.error.not-enough-level");
            user.sendMessage("warps.error.your-level-is",
                    "[level]", String.valueOf(level),
                    "[required]", String.valueOf(addon.getSettings().getWarpLevelRestriction()));
            return true;
        }

        // Check that the player is on their island
        if (!(plugin.getIslands().userIsOnIsland(world, user))) {
            user.sendMessage("warps.error.not-on-island");
            return true;
        }
        return false;
    }

    /**
     * Check if player has permission to execute command
     * @param user - user
     * @param world - world that the warp is in
     * @param inWorld  - true if warp is in a game world
     * @return true if player does not have the required perms, false otherwise
     */
    private boolean noPerms(User user, World world, boolean inWorld) {
        String permReq = inWorld ? addon.getPermPrefix(world) + "island.addwarp" : Warp.WELCOME_WARP_SIGNS + ".addwarp";
        if (!(user.hasPermission(permReq))) {
            user.sendMessage("warps.error.no-permission");
            user.sendMessage("general.errors.no-permission", "[permission]", permReq);
            return true;
        }
        return false;
    }

    private void addSign(SignChangeEvent e, User user, Block b) {
        if (addon.getWarpSignsManager().addWarp(user.getUniqueId(), b.getLocation())) {
            user.sendMessage("warps.success");
            e.setLine(0, ChatColor.GREEN + addon.getSettings().getWelcomeLine());
            for (int i = 1; i<4; i++) {
                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
            }
        }
        // Else null player
    }

}
