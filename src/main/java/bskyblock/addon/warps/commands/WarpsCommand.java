/**
 *
 */
package bskyblock.addon.warps.commands;

import java.util.List;

import bskyblock.addon.warps.Warp;
import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;

/**
 * @author ben
 *
 */
public class WarpsCommand extends CompositeCommand {

    private Warp plugin;

    public WarpsCommand(Warp plugin, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, "warps");
        this.plugin = plugin;
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
        if (plugin.getWarpSignsManager().listWarps(getWorld()).isEmpty()) {
            user.sendMessage("warps.error.no-warps-yet");
            user.sendMessage("warps.warpTip");
        } else {
            plugin.getWarpPanelManager().showWarpPanel(getWorld(), user,0);
        }
        return true;
    }

}
