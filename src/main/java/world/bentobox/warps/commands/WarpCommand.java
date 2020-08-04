package world.bentobox.warps.commands;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.warps.Warp;

/**
 * The /is warp <name> command
 *
 * @author tastybento
 */
public class WarpCommand extends DelayedTeleportCommand {

    private Warp addon;

    public WarpCommand(Warp addon, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, addon.getSettings().getWarpCommand());
        this.addon = addon;
    }

    public WarpCommand(Warp addon) {
        super(addon, addon.getSettings().getWarpCommand());
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission(this.getParent() == null ? Warp.WELCOME_WARP_SIGNS + ".warp" : "island.warp");
        this.setOnlyPlayer(true);
        this.setParametersHelp("warp.help.parameters");
        this.setDescription("warp.help.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            World world = getWorld() == null ? user.getWorld() : getWorld();
            // Warp somewhere command
            Set<UUID> warpList = addon.getWarpSignsManager().listWarps(world);
            if (warpList.isEmpty()) {
                user.sendMessage("warps.error.no-warps-yet");
                user.sendMessage("warps.warpTip", "[text]", addon.getSettings().getWelcomeLine());
                return false;
            } else {
                // Attemp to find warp with exact player's name
                UUID foundWarp = warpList.stream().filter(u -> getPlayers().getName(u).equalsIgnoreCase(args.get(0))).findFirst().orElse(null);

                if (foundWarp == null) {

                    // Atempt to find warp which starts with the given name
                    UUID foundAlernativeWarp = warpList.stream().filter(u -> getPlayers().getName(u).toLowerCase().startsWith(args.get(0).toLowerCase())).findFirst().orElse(null);

                    if (foundAlernativeWarp == null) {
                        user.sendMessage("warps.error.does-not-exist");
                        return false;
                    } else {
                        // Alternative warp found!
                        this.delayCommand(user, () -> addon.getWarpSignsManager().warpPlayer(world, user, foundAlernativeWarp));
                        return true;
                    }
                } else {
                    // Warp exists!
                    this.delayCommand(user, () -> addon.getWarpSignsManager().warpPlayer(world, user, foundWarp));
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
        return Optional.of(addon.getWarpSignsManager().listWarps(world).stream().map(getPlayers()::getName).collect(Collectors.toList()));
    }


}
