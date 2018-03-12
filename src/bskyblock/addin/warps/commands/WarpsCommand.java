/**
 * 
 */
package bskyblock.addin.warps.commands;

import java.util.List;

import bskyblock.addin.warps.Warp;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

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
        this.setPermission(Constants.PERMPREFIX + "island.warp");
        this.setOnlyPlayer(true);
        this.setDescription("warps.help.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.commands.User, java.util.List)
     */
    @Override
    public boolean execute(User user, List<String> args) {
        if (plugin.getWarpSignsManager().listWarps().isEmpty()) {
            user.sendMessage("warps.error.no-warps-yet");
            user.sendMessage("warps.warpTip");                
        } else {
            plugin.getWarpPanelManager().showWarpPanel(user,0);
        }
        return true;
    }

}
