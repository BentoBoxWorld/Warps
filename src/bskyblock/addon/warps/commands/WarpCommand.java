package bskyblock.addon.warps.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import bskyblock.addon.warps.Warp;
import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;

/**
 * The /is warp <name> command
 *
 * @author tastybento
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
        this.setPermission("island.warp");
        this.setOnlyPlayer(true);
        this.setParameters("warp.help.parameters");
        this.setDescription("warp.help.description");
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        final Set<UUID> warpList = plugin.getWarpSignsManager().listWarps(getWorld());

        for (UUID warp : warpList) {
            options.add(plugin.getPlugin().getPlayers().getName(warp));
        }

        return Optional.of(options);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            // Warp somewhere command
            final Set<UUID> warpList = plugin.getWarpSignsManager().listWarps(getWorld());
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
                    plugin.getWarpSignsManager().warpPlayer(getWorld(), user, foundWarp);
                }
            }
        }
        return false;
    }


}
