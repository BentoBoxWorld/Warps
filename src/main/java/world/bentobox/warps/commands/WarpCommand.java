package world.bentobox.warps.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import world.bentobox.warps.Warp;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * The /is warp <name> command
 *
 * @author tastybento
 *
 */
public class WarpCommand extends CompositeCommand {

    private Warp addon;

    public WarpCommand(Warp plugin, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, "warp");
        this.addon = plugin;
    }

    @Override
    public void setup() {
        this.setPermission("island.warp");
        this.setOnlyPlayer(true);
        this.setParametersHelp("warp.help.parameters");
        this.setDescription("warp.help.description");
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        final Set<UUID> warpList = addon.getWarpSignsManager().listWarps(getWorld());

        for (UUID warp : warpList) {
            options.add(addon.getPlugin().getPlayers().getName(warp));
        }

        return Optional.of(options);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            // Warp somewhere command
            Set<UUID> warpList = addon.getWarpSignsManager().listWarps(getWorld());
            if (warpList.isEmpty()) {
                user.sendMessage("warps.error.no-warps-yet");
                user.sendMessage("warps.warpTip", "[text]", getAddon().getConfig().getString("welcomeLine", "[WELCOME]"));
                return true;
            } else {
                // Check if this is part of a name
                UUID foundWarp = warpList.stream().filter(u -> getPlayers().getName(u).toLowerCase().equals(args.get(0).toLowerCase())
                        || getPlayers().getName(u).toLowerCase().startsWith(args.get(0).toLowerCase())).findFirst().orElse(null);
                if (foundWarp == null) {
                    user.sendMessage("warps.error.does-not-exist");
                    return false;
                } else {
                    // Warp exists!
                    addon.getWarpSignsManager().warpPlayer(getWorld(), user, foundWarp);
                    return true;
                }
            }
        }
        return false;
    }


}
