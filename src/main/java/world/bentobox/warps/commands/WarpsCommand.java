/**
 *
 */
package world.bentobox.warps.commands;

import java.util.List;

import world.bentobox.warps.Warp;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * @author ben
 *
 */
public class WarpsCommand extends CompositeCommand {

    private Warp addon;

    public WarpsCommand(Warp addon, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, "warps");
        this.addon = addon;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("island.warp");
        this.setOnlyPlayer(true);
        this.setDescription("warps.help.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.commands.User, java.util.List)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (addon.getWarpSignsManager().listWarps(getWorld()).isEmpty()) {
            user.sendMessage("warps.error.no-warps-yet");
            user.sendMessage("warps.warpTip", "[text]", addon.getSettings().getWelcomeLine());
        } else {
            addon.getWarpPanelManager().showWarpPanel(getWorld(), user,0);
        }
        return true;
    }

}
