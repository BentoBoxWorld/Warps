package world.bentobox.warps.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.warps.Warp;

/**
 * The /is warp <name> command
 *
 * @author tastybento
 *
 */
public class WarpCommand extends CompositeCommand {

    private Warp addon;
    private String perm = "island";

    public WarpCommand(Warp addon, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, addon.getSettings().getWarpCommand());
        this.addon = addon;
    }

    public WarpCommand(Warp addon) {
        super(addon.getSettings().getWarpCommand());
        this.addon = addon;
        perm = Warp.WELCOME_WARP_SIGNS;
    }

    @Override
    public void setup() {
        this.setPermission(perm + ".warp");
        this.setOnlyPlayer(true);
        this.setParametersHelp("warp.help.parameters");
        this.setDescription("warp.help.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        World world = getWorld() == null ? user.getWorld() : getWorld();
        if (args.size() == 1) {
            // Warp somewhere command
            Set<UUID> warpList = addon.getWarpSignsManager().listWarps(world);
            if (warpList.isEmpty()) {
                user.sendMessage("warps.error.no-warps-yet");
                user.sendMessage("warps.warpTip", "[text]", addon.getSettings().getWelcomeLine());
                return true;
            } else {
                // Check if this is part of a name
                UUID foundWarp = warpList.stream().filter(u -> getPlayers().getName(u).equalsIgnoreCase(args.get(0))
                        || getPlayers().getName(u).toLowerCase().startsWith(args.get(0).toLowerCase())).findFirst().orElse(null);
                if (foundWarp == null) {
                    user.sendMessage("warps.error.does-not-exist");
                    return false;
                } else {
                    // Warp exists!
                    addon.getWarpSignsManager().warpPlayer(world, user, foundWarp);
                    return true;
                }
            }
        }
        showHelp(this, user);
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        World world = getWorld() == null ? user.getWorld() : getWorld();
        List<String> options = new ArrayList<>();
        final Set<UUID> warpList = addon.getWarpSignsManager().listWarps(world);

        for (UUID warp : warpList) {
            options.add(addon.getPlugin().getPlayers().getName(warp));
        }

        return Optional.of(options);
    }


}
