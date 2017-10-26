package bskyblock.addin.warps;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import bskyblock.addin.warps.commands.Commands;
import bskyblock.addin.warps.config.LocaleManager;
import bskyblock.addin.warps.config.PluginConfig;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.BSBLocale;

/**
 * Addin to BSkyBlock that enables welcome warp signs
 * @author tastybento
 *
 */
public class Warp extends JavaPlugin {

    // The BSkyBlock plugin instance.
    private BSkyBlock bSkyBlock;

    // Locale manager for this plugin
    private LocaleManager localeManager;

    // Warp panel object
    private WarpPanel warpPanel;

    // Warps signs object
    private WarpSigns warpSigns;

    @Override
    public void onEnable() {
        // Load the plugin's config
        new PluginConfig(this);
        // Get the BSkyBlock plugin. This will be available because this plugin depends on it in plugin.yml.
        bSkyBlock = BSkyBlock.getPlugin();
        // Check if it is enabled - it might be loaded, but not enabled.
        if (!bSkyBlock.isEnabled()) {
            this.setEnabled(false);
            return;
        }
        // Local locales
        localeManager = new LocaleManager(this);
        // Start warp signs
        warpSigns = new WarpSigns(this, bSkyBlock);
        getServer().getPluginManager().registerEvents(warpSigns, this);
        // Start the warp panel and register it for clicks
        warpPanel = new WarpPanel(this);
        getServer().getPluginManager().registerEvents(warpPanel, this);
        // Register commands
        new Commands(this);
        // Done
    }

    @Override
    public void onDisable(){
        // Save the warps
        warpSigns.saveWarpList();
    }
    
    /**
     * Get the locale for this player
     * @param sender
     * @return Locale object for sender
     */
    public BSBLocale getLocale(CommandSender sender) {
        return localeManager.getLocale(sender);
    }

    /**
     * Get the locale for this UUID
     * @param uuid
     * @return Locale object for UUID
     */
    public BSBLocale getLocale(UUID uuid) {
        return localeManager.getLocale(uuid);
    }
    
    /**
     * @return default locale object
     */
    public BSBLocale getLocale() {
        return localeManager.getLocale();
    }

    public WarpPanel getWarpPanel() {
        return warpPanel;
    }

    public WarpSigns getWarpSigns() {
        return warpSigns;
    }

}
