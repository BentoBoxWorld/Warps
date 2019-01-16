package world.bentobox.warps;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.warps.commands.WarpCommand;
import world.bentobox.warps.commands.WarpsCommand;
import world.bentobox.warps.config.PluginConfig;

/**
 * Addin to BSkyBlock that enables welcome warp signs
 * @author tastybento
 *
 */
public class Warp extends Addon {

    private static final String BSKYBLOCK = "BSkyBlock";
    private static final String ACIDISLAND = "AcidIsland";
    private static final String LEVEL_ADDON_NAME = "Level";

    // The plugin instance.
    private BentoBox plugin;

    // Warp panel objects
    private WarpPanelManager warpPanelManager;

    // Warps signs objects
    private WarpSignsManager warpSignsManager;

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
            this.setState(State.DISABLED);
            return;
        }
        registeredWorlds = new HashSet<>();
        // Start warp signs
        warpSignsManager = new WarpSignsManager(this, plugin);
        warpPanelManager = new WarpPanelManager(this);
        // Load the listener
        getServer().getPluginManager().registerEvents(new WarpSignsListener(this), plugin);
        // Register commands
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(a -> a.getDescription().getName().equals(BSKYBLOCK) || a.getDescription().getName().equals(ACIDISLAND))
        .forEach(a -> {
            a.getPlayerCommand().ifPresent(c ->  {
                new WarpCommand(this, c);
                new WarpsCommand(this, c);
                registeredWorlds.add(c.getWorld());
            });

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

    public String getPermPrefix(World world) {
        return this.getPlugin().getIWM().getPermissionPrefix(world);
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

    /**
     * Get the island level
     * @param world - world
     * @param uniqueId - player's UUID
     * @return island level or null if there is no level plugin
     */
    public Long getLevel(World world, UUID uniqueId) {
        return plugin.getAddonsManager().getAddonByName(LEVEL_ADDON_NAME).map(l -> ((Level) l).getIslandLevel(world, uniqueId)).orElse(null);
    }

}
