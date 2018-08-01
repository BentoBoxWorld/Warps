package bskyblock.addon.warps;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.bukkit.World;

import bskyblock.addon.warps.commands.WarpCommand;
import bskyblock.addon.warps.commands.WarpsCommand;
import bskyblock.addon.warps.config.PluginConfig;
import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.api.addons.Addon;
import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.util.Util;

/**
 * Addin to BSkyBlock that enables welcome warp signs
 * @author tastybento
 *
 */
public class Warp extends Addon {

    private static final String LEVEL_PLUGIN_NAME = "BSkyBlock-Level";

    // The plugin instance.
    private BentoBox plugin;

    // Warp panel object
    private WarpPanelManager warpPanelManager;

    // Warps signs object
    private WarpSignsManager warpSignsManager;

    // Level addon
    private Optional<Addon> levelAddon;

    private Set<World> registeredWorlds;

    private PluginConfig settings;

    @Override
    public void onEnable() {
        // Load the plugin's config
        settings = new PluginConfig(this);
        // Get the BSkyBlock plugin. This will be available because this plugin depends on it in plugin.yml.
        plugin = this.getPlugin();
        // Check if it is enabled - it might be loaded, but not enabled.
        if (!plugin.isEnabled()) {
            this.setEnabled(false);
            return;
        }
        // We have to wait for the worlds to load, so we do the rest 1 tick later
        getServer().getScheduler().runTask(this.getPlugin(), () -> {
            registeredWorlds = new HashSet<>();
            // Start warp signs
            warpSignsManager = new WarpSignsManager(this, plugin);
            warpPanelManager = new WarpPanelManager(this);
            // Load the listener
            getServer().getPluginManager().registerEvents(new WarpSignsListener(this), plugin);
            // Register commands
            getServer().getScheduler().runTask(getPlugin(), () -> {
                // BSkyBlock hook in
                this.getPlugin().getAddonsManager().getAddonByName("BSkyBlock").ifPresent(acidIsland -> {
                    CompositeCommand bsbIslandCmd = BentoBox.getInstance().getCommandsManager().getCommand("island");
                    if (bsbIslandCmd != null) {
                        new WarpCommand(this, bsbIslandCmd);
                        new WarpsCommand(this, bsbIslandCmd);
                        registeredWorlds.add(plugin.getIWM().getWorld("BSkyBlock"));
                    }
                });
                // AcidIsland hook in
                this.getPlugin().getAddonsManager().getAddonByName("AcidIsland").ifPresent(acidIsland -> {
                    CompositeCommand acidIslandCmd = getPlugin().getCommandsManager().getCommand("ai");
                    if (acidIslandCmd != null) {
                        new WarpCommand(this, acidIslandCmd);
                        new WarpsCommand(this, acidIslandCmd);
                        registeredWorlds.add(plugin.getIWM().getWorld("AcidIsland"));
                    }
                });
            });
            // Get the level addon if it exists
            setLevelAddon(getPlugin().getAddonsManager().getAddonByName(LEVEL_PLUGIN_NAME));
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

    public String getPermPrefix(World world) {
        this.getPlugin().getIWM().getPermissionPrefix(world);
        return null;
    }

    /**
     * Check if an event is in a registered world
     * @param world - world to check
     * @return true if it is
     */
    public boolean inRegisteredWorld(World world) {
        return registeredWorlds.contains(Util.getWorld(world));
    }

    /**
     * @return the settings
     */
    public PluginConfig getSettings() {
        return settings;
    }

}
