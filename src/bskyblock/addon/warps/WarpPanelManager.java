package bskyblock.addon.warps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.panels.ClickType;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;

public class WarpPanelManager {

    private static final boolean DEBUG = false;
    private static final int PANEL_MAX_SIZE = 52;
    private Warp addon;
    // This is a cache of heads, so they don't need to be created everytime
    private Map<UUID, List<String>> cachedHeads = new HashMap<>();


    public WarpPanelManager(Warp addon) {
        this.addon = addon;
        addon.getWarpSignsManager().getSortedWarps().forEach(this :: getSign);
    }

    private PanelItem getPanelItem(UUID warpOwner) {
        return new PanelItemBuilder()
                .icon(Material.SIGN)
                .name(addon.getBSkyBlock().getPlayers().getName(warpOwner))
                .description(cachedHeads.getOrDefault(warpOwner, getSign(warpOwner)))
                .clickHandler(new ClickHandler() {

                    @Override
                    public boolean onClick(User user, ClickType click) {
                        addon.getWarpSignsManager().warpPlayer(user, warpOwner);
                        return true;
                    }
                }).build();
    }

    /**
     * Gets sign text and caches it
     * @param playerUUID
     * @return
     */
    private List<String> getSign(UUID playerUUID) {
        List<String> result = addon.getWarpSignsManager().getSignText(playerUUID);
        cachedHeads.put(playerUUID, result);
        return result;
    }

    /**
     * Show the warp panel for the user
     * @param user
     * @param index
     */
    public void showWarpPanel(User user, int index) { 
        List<UUID> warps = new ArrayList<>(addon.getWarpSignsManager().getSortedWarps());
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
            panelBuilder.item(getPanelItem(warps.get(i))); 
        }
        final int panelNum = index;
        // Add signs
        if (i < warps.size()) {
            // Next
            panelBuilder.item(new PanelItemBuilder()
                    .name("Next")
                    .icon(new ItemStack(Material.SIGN))
                    .clickHandler(new ClickHandler() {

                        @Override
                        public boolean onClick(User user, ClickType click) {
                            user.closeInventory();
                            showWarpPanel(user, panelNum+1);
                            return true;
                        }

                    }).build());
        }
        if (i > PANEL_MAX_SIZE) {
            // Previous
            panelBuilder.item(new PanelItemBuilder()
                    .name("Previous")
                    .icon(new ItemStack(Material.SIGN))
                    .clickHandler(new ClickHandler() {

                        @Override
                        public boolean onClick(User user, ClickType click) {
                            user.closeInventory();
                            showWarpPanel(user, panelNum-1);
                            return true;
                        }

                    }).build());
        }
        panelBuilder.build();
    }

    /**
     * Removes sign text from the cache
     * @param key
     */
    public void removeWarp(UUID key) {
        cachedHeads.remove(key);
    }

}
