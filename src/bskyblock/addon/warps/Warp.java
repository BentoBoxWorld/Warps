package bskyblock.addon.warps;

import java.util.Optional;

import bskyblock.addon.warps.commands.WarpCommand;
import bskyblock.addon.warps.commands.WarpsCommand;
import bskyblock.addon.warps.config.PluginConfig;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.commands.CompositeCommand;

/**
 * Addin to BSkyBlock that enables welcome warp signs
 * @author tastybento
 *
 */
public class Warp extends Addon {

    private static final String BSKYBLOCK_LEVEL = "BSkyBlock-Level";

    // The BSkyBlock plugin instance.
    private BSkyBlock bSkyBlock;

    // Warp panel object
    private WarpPanelManager warpPanelManager;

    // Warps signs object
    private WarpSignsManager warpSignsManager;

    // Level addon
    private Optional<Addon> levelAddon;

    @Override
    public void onEnable() {
        // Load the plugin's config
        new PluginConfig(this);
        // Get the BSkyBlock plugin. This will be available because this plugin depends on it in plugin.yml.
        bSkyBlock = BSkyBlock.getInstance();
        // Check if it is enabled - it might be loaded, but not enabled.
        if (!bSkyBlock.isEnabled()) {
            this.setEnabled(false);
            return;
        }
        // We have to wait for the worlds to load, so we do the rest 1 tick later
        getServer().getScheduler().runTask(this.getBSkyBlock(), () -> {
            // Start warp signs
            warpSignsManager = new WarpSignsManager(this, bSkyBlock);
            warpPanelManager = new WarpPanelManager(this);
            // Load the listener
            getServer().getPluginManager().registerEvents(warpSignsManager, bSkyBlock);
            // Register commands
            CompositeCommand bsbIslandCmd = (CompositeCommand) BSkyBlock.getInstance().getCommandsManager().getCommand(Constants.ISLANDCOMMAND);
            new WarpCommand(this, bsbIslandCmd);
            new WarpsCommand(this, bsbIslandCmd);
            
            // Get the level addon if it exists
            setLevelAddon(getBSkyBlock().getAddonsManager().getAddonByName(BSKYBLOCK_LEVEL));
        });
        // Done
    }

    @Override
    public void onDisable(){
        // Save the warps
        if (warpSignsManager != null)
            warpSignsManager.saveWarpList();
    }

    /**
     * Get warp panel manager
     * @return
     */
    public WarpPanelManager getWarpPanelManager() {
        return warpPanelManager;
    }

    public WarpSignsManager getWarpSignsManager() {
        return warpSignsManager;
    }

    public Optional<Addon> getLevelAddon() {
        return levelAddon;
    }

    public void setLevelAddon(Optional<Addon> levelAddon) {
        this.levelAddon = levelAddon;
    }

}
