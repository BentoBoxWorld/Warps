package bskyblock.addin.warps.commands;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import bskyblock.addin.warps.Warp;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

/**
 * The /is warp <name> command
 * 
 * @author ben
 *
 */
public class WarpCommand extends CompositeCommand {

    private Warp plugin;

    public WarpCommand(Warp plugin, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, "warp");
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.warp");
        this.setOnlyPlayer(true);
        this.setParameters("warp.help.parameters");
        this.setDescription("warp.help.description");
    }
    
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, LinkedList<String> args) {
        List<String> options = new ArrayList<>();
        final Set<UUID> warpList = plugin.getWarpSignsManager().listWarps();

        for (UUID warp : warpList) {
            options.add(plugin.getBSkyBlock().getPlayers().getName(warp));
        }
        
        return Optional.of(options);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (args.size() == 1) {
            // Warp somewhere command
            final Set<UUID> warpList = plugin.getWarpSignsManager().listWarps();
            if (warpList.isEmpty()) {
                user.sendMessage("warps.errorNoWarpsYet");
                user.sendMessage("warps.warpTip");
                return true;
            } else {
                // Check if this is part of a name
                UUID foundWarp = null;
                for (UUID warp : warpList) {
                    if (warp == null)
                        continue;
                    if (getPlayers().getName(warp).toLowerCase().equals(args.get(0).toLowerCase())) {
                        foundWarp = warp;
                        break;
                    } else if (getPlayers().getName(warp).toLowerCase().startsWith(args.get(0).toLowerCase())) {
                        foundWarp = warp;
                    }
                }
                if (foundWarp == null) {
                    user.sendMessage("warps.error.DoesNotExist");
                    return true;
                } else {
                    // Warp exists!
                    plugin.getWarpSignsManager().warpPlayer(user, foundWarp);
                }
            }
        }
        return false;
    }


}
