package world.bentobox.warps.commands;

import java.util.List;

import org.bukkit.World;

import world.bentobox.warps.Warp;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class WarpsCommand extends CompositeCommand {

    private Warp addon;
    private String perm = "island";

    public WarpsCommand(Warp addon, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, addon.getSettings().getWarpsCommand());
        this.addon = addon;
    }

    public WarpsCommand(Warp addon) {
        super(addon.getSettings().getWarpsCommand());
        this.addon = addon;
        perm = "welcomewarpsigns";
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission(perm + ".warp");
        this.setOnlyPlayer(true);
        this.setDescription("warps.help.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.commands.User, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        World world = getWorld() == null ? user.getWorld() : getWorld();
        if (addon.getWarpSignsManager().listWarps(world).isEmpty()) {
            user.sendMessage("warps.error.no-warps-yet");
            user.sendMessage("warps.warpTip", "[text]", addon.getSettings().getWelcomeLine());
        } else {
            addon.getWarpPanelManager().showWarpPanel(world, user,0);
        }
        return true;
    }

}
