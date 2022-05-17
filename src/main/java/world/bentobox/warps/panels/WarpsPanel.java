//
// Created by BONNe
// Copyright - 2021
//

package world.bentobox.warps.panels;


import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.warps.managers.SignCacheItem;
import world.bentobox.warps.managers.SignCacheManager;
import world.bentobox.warps.Warp;


/**
 * This class shows how to set up easy panel by using BentoBox PanelBuilder API
 */
public class WarpsPanel
{
// ---------------------------------------------------------------------
// Section: Constructor
// ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param addon VisitAddon object
     * @param world World where user will be teleported
     * @param user User who opens panel
     */
    private WarpsPanel(Warp addon,
        World world,
        User user)
    {
        this.addon = addon;
        this.manager = this.addon.getSignCacheManager();
        this.user = user;
        this.world = world;
    }


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


    /**
     * This method collects and validates sign warps that could be displayed in GUI.
     * @param completed CompletableFeature that triggers panel opening.
     */
    private void collectValidWarps(CompletableFuture<Boolean> completed)
    {
        this.addon.getWarpSignsManager().getSortedWarps(this.world).
            thenAccept(warps ->
            {
                // Cache and clean the signs
                Iterator<UUID> iterator = warps.iterator();

                while (iterator.hasNext())
                {
                    UUID warpOwner = iterator.next();
                    @NonNull
                    SignCacheItem sign = this.manager.getSignItem(this.world, warpOwner);

                    if (!sign.isReal())
                    {
                        iterator.remove();
                        this.addon.getWarpSignsManager().removeWarpFromMap(this.world, warpOwner);
                    }
                }

                // Assign warps to element list.
                this.elementList = warps;

                // Build the main body
                completed.complete(true);
            });
    }


    /**
     * This is wrapper around builder to trigger main GUI building.
     */
    private void initBuild()
    {
        CompletableFuture<Boolean> collectWarps = new CompletableFuture<>();
        this.collectValidWarps(collectWarps);
        collectWarps.thenAccept(done -> {
            if (done)
            {
                this.build();
            }
        });
    }


    /**
     * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy to use and users can get nice
     * panels.
     */
    private void build()
    {
        // Do not open gui if there is no magic sticks.
        if (this.elementList.isEmpty())
        {
            this.addon.logError("There are no available islands for visiting!");
            Utils.sendMessage(this.user, "warps.error.no-warps-yet",
                "[gamemode]", this.addon.getPlugin().getIWM().getAddon(this.world).
                    map(gamemode -> gamemode.getDescription().getName()).
                    orElse(""));
            return;
        }

        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        // Set main template.
        panelBuilder.template("warps_panel", new File(this.addon.getDataFolder(), "panels"));
        panelBuilder.user(this.user);
        panelBuilder.world(this.user.getWorld());

        // Register button builders
        panelBuilder.registerTypeBuilder("WARP", this::createWarpButton);
        panelBuilder.registerTypeBuilder("RANDOM", this::createRandomButton);

        // Register next and previous builders
        panelBuilder.registerTypeBuilder("NEXT", this::createNextButton);
        panelBuilder.registerTypeBuilder("PREVIOUS", this::createPreviousButton);

        // Register unknown type builder.
        panelBuilder.build();
    }


// ---------------------------------------------------------------------
// Section: Buttons
// ---------------------------------------------------------------------


    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    @Nullable
    private PanelItem createNextButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        int size = this.elementList.size();

        if (size <= slot.amountMap().getOrDefault("WARP", 1) ||
            1.0 * size / slot.amountMap().getOrDefault("WARP", 1) <= this.pageIndex + 1)
        {
            // There are no next elements
            return null;
        }

        int nextPageIndex = this.pageIndex + 2;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((Boolean) template.dataMap().getOrDefault("indexing", false))
            {
                clone.setAmount(nextPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                "[number]", String.valueOf(nextPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (clickType == action.clickType()  || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("NEXT".equalsIgnoreCase(action.actionType()))
                    {
                        // Next button ignores click type currently.
                        this.pageIndex++;
                        this.build();
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * Create previous button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    @Nullable
    private PanelItem createPreviousButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.pageIndex == 0)
        {
            // There are no next elements
            return null;
        }

        int previousPageIndex = this.pageIndex;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((Boolean) template.dataMap().getOrDefault("indexing", false))
            {
                clone.setAmount(previousPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                "[number]", String.valueOf(previousPageIndex)));
        }

        // Add ClickHandler
        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (clickType == action.clickType()  || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("PREVIOUS".equalsIgnoreCase(action.actionType()))
                    {
                        // Next button ignores click type currently.
                        this.pageIndex--;
                        this.build();
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method creates and returns warp button.
     *
     * @return PanelItem that represents warp button.
     */
    @Nullable
    private PanelItem createWarpButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.elementList.isEmpty())
        {
            // Does not contain any sticks.
            return null;
        }

        int index = this.pageIndex * slot.amountMap().getOrDefault("WARP", 1) + slot.slot();

        if (index >= this.elementList.size())
        {
            // Out of index.
            return null;
        }

        return this.createWarpButton(template, this.elementList.get(index), "warp");
    }


    /**
     * This method creates and returns random warp button.
     *
     * @return PanelItem that represents random warp button.
     */
    @Nullable
    private PanelItem createRandomButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.elementList.size() < 2)
        {
            // Does not contain any sticks.
            return null;
        }

        int index = random.nextInt(this.elementList.size());
        return this.createWarpButton(template, this.elementList.get(index), "random");
    }


// ---------------------------------------------------------------------
// Section: Other methods
// ---------------------------------------------------------------------


    /**
     * This method creates and returns button that switch to next page in view mode.
     *
     * @return PanelItem that allows to select next owner view page.
     */
    private PanelItem createWarpButton(ItemTemplateRecord template, UUID ownerUUID, String type)
    {
        if (ownerUUID == null)
        {
            // return as owner has no owner. Empty button will be created.
            return null;
        }

        SignCacheItem signCache = this.manager.getSignItem(this.world, ownerUUID);

        if (!signCache.isReal())
        {
            this.addon.getWarpSignsManager().removeWarpFromMap(this.world, ownerUUID);
            // return as signCache is not real anymore.
            return null;
        }

        final String reference = "warps.gui.buttons." + type + ".";

        String ownerName = addon.getPlugin().getPlayers().getName(ownerUUID);

        // Get settings for owner.
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            if (template.icon().getType().equals(Material.PLAYER_HEAD))
            {
                builder.icon(ownerName);
            }
            else
            {
                builder.icon(template.icon().clone());
            }
        }
        else
        {
            // Check owner for a specific icon
            Material material = signCache.getType();

            if (material == null)
            {
                // Set oak sign as icon was not specified.
                material = Material.OAK_SIGN;
            }

            if (material == Material.PLAYER_HEAD)
            {
                builder.icon(ownerName);
            }
            else
            {
                builder.icon(material);
            }
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title(),
                "[name]", ownerName));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name",
                "[name]", ownerName));
        }

        // Process Description of the button.
        // Generate [sign_text] text

        String descriptionText;

        if (template.description() != null)
        {
            descriptionText = this.user.getTranslationOrNothing(template.description(),
                    "[name]", ownerName,
                    "[sign_text]", String.join("\n", signCache.getSignText())).
                replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|");
        }
        else
        {
            descriptionText = this.user.getTranslationOrNothing(reference + "description",
                "[name]", ownerName,
                "[sign_text]", String.join("|", signCache.getSignText()));

            // Clean up description text and split it into parts.
            descriptionText = descriptionText.replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|");
        }

        builder.description(descriptionText);

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            template.actions().forEach(action -> {
                if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                {
                    if ("WARP".equalsIgnoreCase(action.actionType()))
                    {
                        this.runCommandCall(ownerName);
                    }
                }
            });

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }


    /**
     * This method runs command call that allows player to visit clicked island.
     */
    private void runCommandCall(String ownerName)
    {
        // Get first player command label.
        String command = this.addon.getSettings().getWarpCommand().split(" ")[0];

        String gamemodeCommand = this.addon.getPlugin().getIWM().getAddon(this.world).
            map(gamemode -> gamemode.getPlayerCommand().map(Command::getLabel).orElse("")).
            orElse("");

        if (gamemodeCommand.isEmpty())
        {
            this.addon.log(this.user.getName() + " called: `" + command + " " + ownerName + "`");
            this.user.performCommand(command + " " + ownerName);
        }
        else
        {
            this.addon.log(this.user.getName() + " called: `" + gamemodeCommand + " " + command + " " + ownerName + "`");
            this.user.performCommand(gamemodeCommand + " " + command + " " + ownerName);
        }

        // Close inventory
        this.user.closeInventory();
    }


// ---------------------------------------------------------------------
// Section: Static methods
// ---------------------------------------------------------------------


    /**
     * This method is used to open UserPanel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param addon Warps object
     * @param world World where user will be teleported
     * @param user User who opens panel
     */
    public static void openPanel(Warp addon,
        World world,
        User user)
    {
        new WarpsPanel(addon, world, user).initBuild();
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * This variable allows to access addon object.
     */
    private final Warp addon;

    /**
     * This variable allows to access addon manager object.
     */
    private final SignCacheManager manager;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    private final User user;

    /**
     * This variable holds world where panel is opened. Without it panel cannot be opened.
     */
    private final World world;

    /**
     * This variable stores filtered elements.
     */
    private List<UUID> elementList;

    /**
     * This variable holds current pageIndex for multi-page island choosing.
     */
    private int pageIndex;

    /**
     * Random for finding random warp.
     */
    private static final Random random = new Random();
}
