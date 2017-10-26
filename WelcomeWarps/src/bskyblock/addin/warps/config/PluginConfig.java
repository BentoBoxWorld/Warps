package bskyblock.addin.warps.config;

import bskyblock.addin.warps.Warp;

public class PluginConfig {

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public PluginConfig(Warp plugin) {
        plugin.saveDefaultConfig();
        Settings.warpLevelRestriction = plugin.getConfig().getInt("warplevelrestriction",10);
        // All done
    }
}
