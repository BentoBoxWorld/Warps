package bentobox.addon.warps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

public class WarpPanelManager {

    private static final boolean DEBUG = false;
    private static final int PANEL_MAX_SIZE = 52;
    private Warp addon;
    // This is a cache of signs
    private Map<World, Map<UUID, List<String>>> cachedSigns = new HashMap<>();


    public WarpPanelManager(Warp addon) {
        this.addon = addon;
        //addon.getWarpSignsManager().getSortedWarps().forEach(this :: getSign);
    }

    private PanelItem getPanelItem(World world, UUID warpOwner) {
        // Return a sign panel item
        return new PanelItemBuilder()
                .icon(Material.SIGN)
                .name(addon.getPlugin().getPlayers().getName(warpOwner))
                .description(getSign(world, warpOwner))
                .clickHandler((panel, clicker, click, slot) -> { {
                    clicker.closeInventory();
                    addon.getWarpSignsManager().warpPlayer(world, clicker, warpOwner);
                    return true;
                }
                }).build();
    }

    /**
     * Gets sign text and cache it
     * @param playerUUID
     * @return sign text in a list
     */
    private List<String> getSign(World world, UUID playerUUID) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        if (cachedSigns.get(world).containsKey(playerUUID)) {
            return cachedSigns.get(world).get(playerUUID);
        }
        List<String> result = addon.getWarpSignsManager().getSignText(world, playerUUID);
        cachedSigns.get(world).put(playerUUID, result);
        return result;
    }

    /**
     * Show the warp panel for the user
     * @param world - world
     * @param user - user
     * @param index - page to show - 0 is first
     */
    public void showWarpPanel(World world, User user, int index) {
        List<UUID> warps = new ArrayList<>(addon.getWarpSignsManager().getSortedWarps(world));
        if (DEBUG) {
            Bukkit.getLogger().info("DEBUG: showing warps. warps list is " + warps.size());
        }
        if (index < 0) {
            index = 0;
        } else if (index > (warps.size() / PANEL_MAX_SIZE)) {
            index = warps.size() / PANEL_MAX_SIZE;
        }
        PanelBuilder panelBuilder = new PanelBuilder()
                .user(user)
                .name(user.getTranslation("warps.title") + " " + String.valueOf(index + 1));

        int i = index * PANEL_MAX_SIZE;
        for (; i < (index * PANEL_MAX_SIZE + PANEL_MAX_SIZE) && i < warps.size(); i++) {
            panelBuilder.item(getPanelItem(world, warps.get(i)));
        }
        final int panelNum = index;
        // Add signs
        if (i < warps.size()) {
            // Next
            panelBuilder.item(new PanelItemBuilder()
                    .name("Next")
                    .icon(new ItemStack(Material.SIGN))
                    .clickHandler((panel, clicker, click, slot) -> {
                        user.closeInventory();
                        showWarpPanel(world, user, panelNum+1);
                        return true;
                    }).build());
        }
        if (i > PANEL_MAX_SIZE) {
            // Previous
            panelBuilder.item(new PanelItemBuilder()
                    .name("Previous")
                    .icon(new ItemStack(Material.SIGN))
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
     * @param key
     */
    public void removeWarp(World world, UUID key) {
        cachedSigns.putIfAbsent(world, new HashMap<>());
        cachedSigns.get(world).remove(key);
    }

}
