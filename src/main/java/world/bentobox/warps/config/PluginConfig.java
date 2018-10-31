package world.bentobox.warps.config;

import world.bentobox.warps.Warp;

public class PluginConfig {

    private int warpLevelRestriction;

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public PluginConfig(Warp plugin) {
        plugin.saveDefaultConfig();
        warpLevelRestriction  = plugin.getConfig().getInt("warplevelrestriction",10);
        // All done
    }

    /**
     * @return the warpLevelRestriction
     */
    public int getWarpLevelRestriction() {
        return warpLevelRestriction;
    }
}
