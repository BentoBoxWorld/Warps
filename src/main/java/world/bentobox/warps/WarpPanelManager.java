package world.bentobox.warps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

public class WarpPanelManager {

    private static final int PANEL_MAX_SIZE = 52;
    private Warp addon;
    // This is a cache of signs
    private SignCacheManager signCacheManager;

    public WarpPanelManager(Warp addon) {
        this.addon = addon;
        signCacheManager = new SignCacheManager(addon);
    }

    private PanelItem getPanelItem(World world, UUID warpOwner) {
        PanelItemBuilder pib = new PanelItemBuilder()
                .name(addon.getSettings().getNameFormat() + addon.getPlugin().getPlayers().getName(warpOwner))
                .description(signCacheManager.getSign(world, warpOwner))
                .clickHandler((panel, clicker, click, slot) -> hander(world, clicker, warpOwner));
        Material icon = signCacheManager.getSignIcon(world, warpOwner);
        if (icon.equals(Material.PLAYER_HEAD)) {
            return pib.icon(addon.getPlayers().getName(warpOwner)).build();
        } else {
            return pib.icon(icon).build();
        }
    }

    private boolean hander(World world, User clicker, UUID warpOwner) {
        clicker.closeInventory();
        String playerCommand = addon.getPlugin().getIWM().getAddon(world).map(gm -> gm.getPlayerCommand().map(c -> c.getLabel()).orElse("")).orElse("");
        String command = addon.getSettings().getWarpCommand() + " " + addon.getPlayers().getName(warpOwner);
        clicker.getPlayer().performCommand((playerCommand.isEmpty() ? "" : playerCommand + " ") + command);
        //addon.getWarpSignsManager().warpPlayer(world, clicker, warpOwner);
        return true;
    }

    private PanelItem getRandomButton(World world, User user, UUID warpOwner) {
        ///give @p minecraft:player_head{display:{Name:"{\"text\":\"Question Mark\"}"},SkullOwner:"MHF_Question"} 1
        return new PanelItemBuilder()
                .name(addon.getSettings().getNameFormat() + user.getTranslation("warps.random"))
                .clickHandler((panel, clicker, click, slot) -> hander(world, clicker, warpOwner))
                .icon(Material.END_CRYSTAL).build();
    }

    /**
     * Show the warp panel for the user
     * @param world - world
     * @param user - user
     * @param index - page to show - 0 is first
     */
    public void showWarpPanel(World world, User user, int index) {
        List<UUID> warps = new ArrayList<>(addon.getWarpSignsManager().getSortedWarps(world));
        UUID randomWarp = null;
        // Add random UUID
        if (!warps.isEmpty() && addon.getSettings().isRandomAllowed()) {
            randomWarp = warps.get(new Random().nextInt(warps.size()));
            warps.add(0, randomWarp);
        }
        if (index < 0) {
            index = 0;
        } else if (index > (warps.size() / PANEL_MAX_SIZE)) {
            index = warps.size() / PANEL_MAX_SIZE;
        }
        PanelBuilder panelBuilder = new PanelBuilder()
                .user(user)
                .name(user.getTranslation("warps.title") + " " + (index + 1));

        int i = index * PANEL_MAX_SIZE;
        for (; i < (index * PANEL_MAX_SIZE + PANEL_MAX_SIZE) && i < warps.size(); i++) {
            if (i == 0 && randomWarp != null) {
                panelBuilder.item(getRandomButton(world, user, randomWarp));
            } else {
                panelBuilder.item(getPanelItem(world, warps.get(i)));
            }
        }
        final int panelNum = index;
        // Add signs
        if (i < warps.size()) {
            // Next
            panelBuilder.item(new PanelItemBuilder()
                    .name(user.getTranslation("warps.next"))
                    .icon(new ItemStack(Material.STONE))
                    .clickHandler((panel, clicker, click, slot) -> {
                        user.closeInventory();
                        showWarpPanel(world, user, panelNum+1);
                        return true;
                    }).build());
        }
        if (i > PANEL_MAX_SIZE) {
            // Previous
            panelBuilder.item(new PanelItemBuilder()
                    .name(user.getTranslation("warps.previous"))
                    .icon(new ItemStack(Material.COBBLESTONE))
                    .clickHandler((panel, clicker, click, slot) -> {
                        user.closeInventory();
                        showWarpPanel(world, user, panelNum-1);
                        return true;
                    }).build());
        }
        panelBuilder.build();
    }

    /**
     * Removes sign text from the cache
     * @param world - world
     * @param key - uuid of owner
     */
    public void removeWarp(World world, UUID key) {
        signCacheManager.removeWarp(world, key);        
    }

    public void saveCache() {
        signCacheManager.saveCache();        
    }

}
