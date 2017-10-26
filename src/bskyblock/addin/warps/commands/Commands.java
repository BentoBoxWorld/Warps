package bskyblock.addin.warps.commands;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bskyblock.addin.warps.AddonHelper;
import bskyblock.addin.warps.Warp;
import us.tastybento.bskyblock.api.commands.ArgumentHandler;
import us.tastybento.bskyblock.api.commands.CanUseResp;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class Commands extends AddonHelper {

    public Commands(Warp plugin) {
        super(plugin);
        setupCommands();
    }

    private void setupCommands() {
        // island warp command
        bSkyBlock.addSubCommand(new ArgumentHandler("island") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (!(sender instanceof Player)) {
                    return new CanUseResp(false);
                }
                if (VaultHelper.hasPerm((Player)sender, Settings.PERMPREFIX + "island.warp")) {
                    return new CanUseResp(true);
                }
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    // Warp somewhere command
                    Player player = (Player)sender;
                    final Set<UUID> warpList = plugin.getWarpSigns().listWarps();
                    if (warpList.isEmpty()) {
                        Util.sendMessage(player, ChatColor.YELLOW + plugin.getLocale(player.getUniqueId()).get("warps.errorNoWarpsYet"));
                        if (VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.addwarp")) {
                            Util.sendMessage(player, ChatColor.YELLOW + plugin.getLocale().get("warps.warpTip"));
                        } else {
                            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("error.NoPermission"));
                        }
                        return;
                    } else {
                        // Check if this is part of a name
                        UUID foundWarp = null;
                        for (UUID warp : warpList) {
                            if (warp == null)
                                continue;
                            if (bSkyBlock.getPlayers().getName(warp).toLowerCase().equals(args[0].toLowerCase())) {
                                foundWarp = warp;
                                break;
                            } else if (bSkyBlock.getPlayers().getName(warp).toLowerCase().startsWith(args[0].toLowerCase())) {
                                foundWarp = warp;
                            }
                        }
                        if (foundWarp == null) {
                            Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.DoesNotExist"));
                            return;
                        } else {
                            // Warp exists!
                            final Location warpSpot = plugin.getWarpSigns().getWarp(foundWarp);
                            // Check if the warp spot is safe
                            if (warpSpot == null) {
                                Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.NotReadyYet"));
                                plugin.getLogger().warning("Null warp found, owned by " + bSkyBlock.getPlayers().getName(foundWarp));
                                return;
                            }
                            // Find out if island is locked
                            // TODO: Fire event

                            Island island = bSkyBlock.getIslands().getIsland(foundWarp);
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
                                    warpPlayer(player, inFront, foundWarp, directionFacing, pvp);
                                    return;
                                } else if (b.getType().equals(Material.WALL_SIGN) && Util.isSafeLocation(oneDown)) {
                                    // Try one block down if this is a wall sign
                                    warpPlayer(player, oneDown, foundWarp, directionFacing, pvp);
                                    return;
                                }
                            } else {
                                // Warp has been removed
                                Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.DoesNotExist"));
                                plugin.getWarpSigns().removeWarp(warpSpot);
                                return;
                            }
                            if (!(Util.isSafeLocation(warpSpot))) {
                                Util.sendMessage(player, ChatColor.RED + plugin.getLocale(player.getUniqueId()).get("warps.error.NotSafe"));
                                // WALL_SIGN's will always be unsafe if the place in front is obscured.
                                if (b.getType().equals(Material.SIGN_POST)) {
                                    plugin.getLogger().warning(
                                            "Unsafe warp found at " + warpSpot.toString() + " owned by " + bSkyBlock.getPlayers().getName(foundWarp));

                                }
                                return;
                            } else {
                                final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
                                        warpSpot.getBlockZ() + 0.5D);
                                player.teleport(actualWarp);
                                if (pvp) {
                                    Util.sendMessage(player, ChatColor.BOLD + "" + ChatColor.RED + bSkyBlock.getLocale(player.getUniqueId()).get("igs." + SettingsFlag.PVP_OVERWORLD + " " + bSkyBlock.getLocale(player.getUniqueId()).get("igs.Allowed")));
                                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);
                                } else {
                                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);
                                }
                                return;
                            }
                        }
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"[player]", "Warp to player's warp sign"};
            }
        }.alias("warp"));

        // island warps command
        bSkyBlock.addSubCommand(new ArgumentHandler("island") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                if (sender instanceof Player) {
                    VaultHelper.hasPerm((Player)sender, Settings.PERMPREFIX + "island.warp");
                    return new CanUseResp(true);
                }
                return new CanUseResp(false);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (plugin.getWarpSigns().listWarps().isEmpty()) {
                        Util.sendMessage(player, ChatColor.YELLOW + plugin.getLocale(player.getUniqueId()).get("warps.error.no-warps-yet"));
                        if (VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.addwarp") && bSkyBlock.getIslands().playerIsOnIsland(player)) {
                            Util.sendMessage(player, ChatColor.YELLOW + plugin.getLocale().get("warps.warpTip"));
                        }
                    } else {
                        player.openInventory(plugin.getWarpPanel().getWarpPanel(0));
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{"", "View warps"};
            }
        }.alias("warps"));


    }

    /**
     * Warps a player to a spot in front of a sign
     * @param player
     * @param inFront
     * @param foundWarp
     * @param directionFacing
     */
    private void warpPlayer(Player player, Location inFront, UUID foundWarp, BlockFace directionFacing, boolean pvp) {
        // convert blockface to angle
        float yaw = blockFaceToFloat(directionFacing);
        final Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5D, inFront.getBlockY(),
                inFront.getBlockZ() + 0.5D, yaw, 30F);
        player.teleport(actualWarp);
        if (pvp) {
            Util.sendMessage(player, ChatColor.BOLD + "" + ChatColor.RED + bSkyBlock.getLocale(player.getUniqueId()).get("igs." + SettingsFlag.PVP_OVERWORLD + " " + bSkyBlock.getLocale(player.getUniqueId()).get("igs.allowed")));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT, 1F, 1F);

        } else {

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 1F);

        }
        Player warpOwner = plugin.getServer().getPlayer(foundWarp);
        if (warpOwner != null && !warpOwner.equals(player)) {
            Util.sendMessage(warpOwner, plugin.getLocale(foundWarp).get("warps.PlayerWarped").replace("[name]", player.getName()));
        }
    }

    /**
     * Converts block face direction to radial degrees. Returns 0 if block face
     * is not radial.
     * 
     * @param face
     * @return degrees
     */
    public float blockFaceToFloat(BlockFace face) {
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
}
