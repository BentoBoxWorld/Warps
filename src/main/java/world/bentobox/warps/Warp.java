package world.bentobox.warps;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.warps.commands.WarpCommand;
import world.bentobox.warps.commands.WarpsCommand;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.listeners.WarpSignsListener;
import world.bentobox.warps.managers.SignCacheManager;
import world.bentobox.warps.managers.WarpSignsManager;


/**
 * Addin to BentoBox that enables welcome warp signs
 * @author tastybento
 *
 */
public class Warp extends Addon {
    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------

    /**
     * This variable stores string for Level addon.
     */
    private static final String LEVEL_ADDON_NAME = "Level";

    /**
     * Permission prefix for non-game world operation
     */
    public static final String WELCOME_WARP_SIGNS = "welcomewarpsigns";

    /**
     * Worlds Sign manager.
     */
    private WarpSignsManager warpSignsManager;

    /**
     * Sign Cache Manager
     */
    private SignCacheManager signCacheManager;

    /**
     * This variable stores in which worlds this addon is working.
     */
    private Set<World> registeredWorlds;

    /**
     * This variable stores if addon settings.
     */
    private Settings settings;

    /**
     * This variable stores if addon is hooked or not.
     */
    private boolean hooked;

    /**
     * Settings config object
     */
    private Config<Settings> settingsConfig;

    /**
     * Create Warp Flag
     */
    private Flag createWarpFlag;

    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Executes code when loading the addon. This is called before {@link #onEnable()}. This should preferably
     * be used to setup configuration and worlds.
     */
    @Override
    public void onLoad()
    {
        super.onLoad();
        // Save default config.yml
        this.saveDefaultConfig();
        // Load the plugin's config
        if (this.loadSettings() && getSettings().isAllowInOtherWorlds()) {
            // Load the master warp and warps command
            new WarpCommand(this);
            new WarpsCommand(this);
        }
    }


    /**
     * Executes code when reloading the addon.
     */
    @Override
    public void onReload()
    {
        super.onReload();

        if (this.hooked || getSettings().isAllowInOtherWorlds()) {
            this.warpSignsManager.saveWarpList();

            this.loadSettings();
            this.getLogger().info("Warps addon reloaded.");
        }
    }


    @Override
    public void onEnable() {
        // Check if it is enabled - it might be loaded, but not enabled.
        if (!this.getPlugin().isEnabled()) {
            this.setState(State.DISABLED);
            return;
        }

        registeredWorlds = new HashSet<>();

        // Register commands
        this.getPlugin().getAddonsManager().getGameModeAddons().forEach(gameModeAddon -> {
            if (!this.settings.getDisabledGameModes().contains(gameModeAddon.getDescription().getName())
                    && gameModeAddon.getPlayerCommand().isPresent())
            {
                this.registeredWorlds.add(gameModeAddon.getOverWorld());

                new WarpCommand(this, gameModeAddon.getPlayerCommand().get());
                new WarpsCommand(this, gameModeAddon.getPlayerCommand().get());
                this.hooked = true;
            }
        });

        if (hooked || getSettings().isAllowInOtherWorlds())
        {
            // Start warp signs
            warpSignsManager = new WarpSignsManager(this, this.getPlugin());
            signCacheManager = new SignCacheManager(this);
            // Load the listener
            this.registerListener(new WarpSignsListener(this));
        } else {
            logWarning("Addon did not hook into anything and is not running stand-alone");
            this.setState(State.DISABLED);
        }

        this.createWarpFlag = new Flag.Builder("PLACE_WARP", Material.OAK_SIGN)
                .addon(this)
                .defaultRank(RanksManager.MEMBER_RANK)
                .clickHandler(new CycleClick("PLACE_WARP",
                    RanksManager.MEMBER_RANK,
                    RanksManager.OWNER_RANK)).
                .defaultSetting(false).
                .mode(Flag.Mode.EXPERT)
                .build();

        getPlugin().getFlagsManager().registerFlag(this, this.createWarpFlag);
    }


    @Override
    public void onDisable(){
        // Save the warps
        if (warpSignsManager != null) {
            warpSignsManager.saveWarpList();
        }
    }


    /**
     * This method loads addon configuration settings in memory.
     */
    private boolean loadSettings() {
        if (settingsConfig == null) {
            settingsConfig = new Config<>(this, Settings.class);
        }
        this.settings = settingsConfig.loadConfigObject();
        if (this.settings == null) {
            // Disable
            this.logError("WelcomeWarp settings could not load! Addon disabled.");
            this.setState(State.DISABLED);
            return false;
        }

        // Save existing panels.
        this.saveResource("panels/warps_panel.yml", false);

        settingsConfig.saveConfigObject(settings);
        return true;
    }


    /**
     * Get sign cache manager
     * @return Sign Cache Manager
     */
    public SignCacheManager getSignCacheManager() {
        return signCacheManager;
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
    public Settings getSettings() {
        return settings;
    }

    /**
     * @return the createWarpFlag
     */
    public Flag getCreateWarpFlag() {
        return createWarpFlag;
    }

    /**
     * Get the island level
     * @param world - world
     * @param uniqueId - player's UUID
     * @return island level or null if there is no level plugin or Level is not operating in this world
     */
    public Long getLevel(World world, UUID uniqueId) {
        // Get name of the game mode
        String name = this.getPlugin().getIWM().getAddon(world).map(g -> g.getDescription().getName()).orElse("");
        return this.getPlugin().getAddonsManager().getAddonByName(LEVEL_ADDON_NAME)
                .map(l -> {
                    if (!name.isEmpty() && ((Level) l).getSettings().getGameModes().contains(name)) {
                        return ((Level) l).getIslandLevel(world, uniqueId);
                    }
                    return null;
                }).orElse(null);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.addons.Addon#request(java.lang.String, java.util.Map)
     *
     * This API enables plugins to request data from the WarpSignsManager
     *
     */
    @Override
    public Object request(String requestLabel, Map<String, Object> metaData) {
        if (metaData.isEmpty()) return null;
        World world = null;
        UUID uuid = null;
        // Parse keys
        if (metaData.containsKey("world")) {
            world = Bukkit.getWorld((String)metaData.get("world"));
        }
        if (world == null) return null;
        if (metaData.containsKey("uuid")) {
            try {
                uuid = UUID.fromString((String)metaData.get("uuid"));
            } catch (Exception e) {
                logError("Requested UUID is invalid");
                return null;
            }
        }
        return switch (requestLabel) {
            case "getSortedWarps" -> getWarpSignsManager().getSortedWarps(world);
            case "getWarp" -> uuid == null ? null : getWarpSignsManager().getWarp(world, uuid);
            case "getWarpMap" -> getWarpSignsManager().getWarpMap(world);
            case "hasWarp" -> uuid == null ? null : getWarpSignsManager().hasWarp(world, uuid);
            case "listWarps" -> getWarpSignsManager().listWarps(world);
            default -> null;
        };

    }

}
