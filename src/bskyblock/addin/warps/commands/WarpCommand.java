package bskyblock.addin.warps.commands;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import bskyblock.addin.warps.Warp;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class WarpCommand extends CompositeCommand {

    private Warp plugin;

    public WarpCommand(Warp plugin, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, "warp");
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "island.warp");
        this.setOnlyPlayer(true);
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
