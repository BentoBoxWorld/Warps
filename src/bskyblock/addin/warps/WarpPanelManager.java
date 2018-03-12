package bskyblock.addin.warps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private Warp plugin;
    // This is a cache of heads, so they don't need to be created everytime
    private HashMap<UUID, PanelItem> cachedWarps;


    public WarpPanelManager(Warp plugin) {
        this.plugin = plugin;
        cachedWarps = new HashMap<>();
        createWarpCache();
    }


    /**
     * This method makes the cache of heads based on the warps available
     */
    private void createWarpCache() {
        if (DEBUG)
            Bukkit.getLogger().info("DEBUG: creating warp cache");
        cachedWarps.clear();
        for (UUID warpOwner : plugin.getWarpSignsManager().getSortedWarps()) {
            if (DEBUG)
                Bukkit.getLogger().info("DEBUG: adding warp");
            cachedWarps.put(warpOwner, getPanelItem(warpOwner));          
        }
    }

    private PanelItem getPanelItem(UUID warpOwner) {
        return new PanelItemBuilder()
                .icon(getSkull(warpOwner))
                .name(plugin.getBSkyBlock().getPlayers().getName(warpOwner))
                .description(plugin.getWarpSignsManager().getSignText(warpOwner))
                .clickHandler(new ClickHandler() {

                    @Override
                    public boolean onClick(User user, ClickType click) {
                        plugin.getWarpSignsManager().warpPlayer(user, warpOwner);
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
        String playerName = plugin.getBSkyBlock().getPlayers().getName(playerUUID);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: name of warp = " + playerName);
        ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (playerName == null) {
            if (DEBUG)
                plugin.getLogger().warning("Warp for Player: UUID " + playerUUID.toString() + " is unknown on this server, skipping...");
            return null;
        }
        SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(ChatColor.WHITE + playerName);
        playerSkull.setItemMeta(meta);
        return playerSkull;
    }

    /**
     * Show the warp panel for the user
     * @param user
     * @param index
     */
    public void showWarpPanel(User user, int index) { 
        List<UUID> warps = new ArrayList<>(plugin.getWarpSignsManager().getSortedWarps());
        if (DEBUG) {
            Bukkit.getLogger().info("DEBUG: showing warps. warps list is " + warps.size());
        }
        if (index < 0) {
            index = 0;
        } else if (index > (warps.size() / PANEL_MAX_SIZE)) {
            index = warps.size() / PANEL_MAX_SIZE;
        }
        // TODO use when locales are done.
        //PanelBuilder panelBuilder = new PanelBuilder().setUser(user).setName(user.getTranslation("panel.title", "[number]", String.valueOf(index + 1)));
        PanelBuilder panelBuilder = new PanelBuilder()
                .user(user)
                .name(user.getTranslation("panel.title") + " " + String.valueOf(index + 1));
        
        int i = index * PANEL_MAX_SIZE;
        for (; i < (index * PANEL_MAX_SIZE + PANEL_MAX_SIZE) && i < warps.size(); i++) {
            UUID owner = warps.get(i);
            if (!cachedWarps.containsKey(owner)) {
                cachedWarps.put(owner, getPanelItem(owner));
            }
            panelBuilder.item(cachedWarps.get(owner)); 
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


    public void remove(UUID uuid) {
        cachedWarps.remove(uuid);    
    }
}
