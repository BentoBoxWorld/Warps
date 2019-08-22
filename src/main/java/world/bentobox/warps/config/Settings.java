package world.bentobox.warps.config;

import java.util.HashSet;
import java.util.Set;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;


@StoreAt(filename="config.yml", path="addons/WelcomeWarps")
@ConfigComment("WelcomeWarps Configuration [version]")
@ConfigComment("This config file is dynamic and saved when the server is shutdown.")
@ConfigComment("You cannot edit it while the server is running because changes will")
@ConfigComment("be lost! Use in-game settings GUI or edit when server is offline.")
@ConfigComment("")
public class Settings implements ConfigObject
{
    @ConfigComment("")
    @ConfigComment("Warp Restriction - needed levels to be able to create a warp")
    @ConfigComment("0 or negative values will disable this restriction 10 is default")
    @ConfigEntry(path = "warplevelrestriction")
    private int warpLevelRestriction;

    @ConfigComment("")
    @ConfigComment("Text that player must put on sign to make it a warp sign")
    @ConfigComment("Not case sensitive!")
    @ConfigEntry(path = "welcomeLine")
    private String welcomeLine;

    @ConfigComment("")
    @ConfigComment("Icon that will be displayed in Warps list. SIGN counts for any kind of sign and the type of")
    @ConfigComment("wood used will be reflected in the panel if the server supports it.")
    @ConfigComment("It uses native Minecraft material strings, but using string 'PLAYER_HEAD', it is possible to")
    @ConfigComment("use player heads instead. Beware that Mojang API rate limiting may prevent heads from loading.")
    @ConfigEntry(path = "icon")
    private String icon = "SIGN";

    @ConfigComment("")
    @ConfigComment("This list stores GameModes in which Level addon should not work.")
    @ConfigComment("To disable addon it is necessary to write its name in new line that starts with -. Example:")
    @ConfigComment("disabled-gamemodes:")
    @ConfigComment(" - BSkyBlock")
    @ConfigEntry(path = "disabled-gamemodes")
    private Set<String> disabledGameModes = new HashSet<>();

    // ---------------------------------------------------------------------
    // Section: Constructor
    // ---------------------------------------------------------------------


    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public Settings()
    {
        // empty constructor
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * @return the warpLevelRestriction
     */
    public int getWarpLevelRestriction()
    {
        return warpLevelRestriction;
    }

    /**
     * This method sets the warpLevelRestriction object value.
     * @param warpLevelRestriction the warpLevelRestriction object new value.
     *
     */
    public void setWarpLevelRestriction(int warpLevelRestriction)
    {
        this.warpLevelRestriction = warpLevelRestriction;
    }


    /**
     * This method returns the welcomeLine object.
     * @return the welcomeLine object.
     */
    public String getWelcomeLine()
    {
        return welcomeLine;
    }


    /**
     * This method sets the welcomeLine object value.
     * @param welcomeLine the welcomeLine object new value.
     *
     */
    public void setWelcomeLine(String welcomeLine)
    {
        this.welcomeLine = welcomeLine;
    }


    /**
     * This method returns the disabledGameModes object.
     * @return the disabledGameModes object.
     */
    public Set<String> getDisabledGameModes()
    {
        return disabledGameModes;
    }


    /**
     * This method sets the disabledGameModes object value.
     * @param disabledGameModes the disabledGameModes object new value.
     *
     */
    public void setDisabledGameModes(Set<String> disabledGameModes)
    {
        this.disabledGameModes = disabledGameModes;
    }


    /**
     * This method returns the icon object.
     * @return the icon object.
     */
    public String getIcon()
    {
        return icon;
    }


    /**
     * This method sets the icon object value.
     * @param icon the icon object new value.
     */
    public void setIcon(String icon)
    {
        this.icon = icon;
    }
}
