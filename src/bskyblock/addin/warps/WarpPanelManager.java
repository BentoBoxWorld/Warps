package bskyblock.addin.warps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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
    private Map<UUID, ItemStack> cachedHeads = new HashMap<>();


    public WarpPanelManager(Warp addon) {
        this.addon = addon;
        addon.getWarpSignsManager().getSortedWarps().forEach(this :: getSkull);
    }

    private PanelItem getPanelItem(UUID warpOwner) {
        return new PanelItemBuilder()
                .icon(cachedHeads.getOrDefault(warpOwner, getSkull(warpOwner)))
                .name(addon.getBSkyBlock().getPlayers().getName(warpOwner))
                .description(addon.getWarpSignsManager().getSignText(warpOwner))
                .clickHandler(new ClickHandler() {

                    @Override
                    public boolean onClick(User user, ClickType click) {
                        addon.getWarpSignsManager().warpPlayer(user, warpOwner);
                        return true;
                    }
                }).build();
    }

    /**
     * Gets the skull for this player UUID
     * @param playerUUID - the player's UUID
     * @return Player skull item
     */
    @SuppressWarnings("deprecation")
    private ItemStack getSkull(UUID playerUUID) {
        String playerName = addon.getBSkyBlock().getPlayers().getName(playerUUID);
        if (DEBUG)
            addon.getLogger().info("DEBUG: name of warp = " + playerName);
        if (playerName == null) {
            if (DEBUG)
                addon.getLogger().warning("Warp for Player: UUID " + playerUUID.toString() + " is unknown on this server, skipping...");
            return null;
        }
        ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + playerName);
        playerSkull.setItemMeta(meta);
        cachedHeads.put(playerUUID, playerSkull);
        Bukkit.getScheduler().runTaskAsynchronously(addon.getBSkyBlock(), () -> {
            meta.setOwner(playerName);
            playerSkull.setItemMeta(meta);
            cachedHeads.put(playerUUID, playerSkull);
        });
        return playerSkull;
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
                .name(user.getTranslation("panel.title") + " " + String.valueOf(index + 1));

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

}
