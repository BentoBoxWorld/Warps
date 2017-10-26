package bskyblock.addin.warps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.Util;

public class WarpPanel extends AddonHelper implements Listener {
    private static final int PANELSIZE = 45; // Must be a multiple of 9
    private final static boolean DEBUG = false;
    private List<Inventory> warpPanel;
    private HashMap<UUID, ItemStack> cachedWarps;

    /**
     * @param plugin
     */
    public WarpPanel(Warp plugin) {
        super(plugin);
        warpPanel = new ArrayList<Inventory>();
        cachedWarps = new HashMap<UUID,ItemStack>();
        //plugin.getLogger().info("DEBUG: loading the warp panel of size " + plugin.getWarpSigns().listSortedWarps().size());
        // Load the cache
        for (UUID playerUUID : plugin.getWarpSigns().listSortedWarps()) {
            addWarp(playerUUID);
        }
        // Make the panels
        updatePanel();
    }

    /**
     * Adds a new warp to the cache. Does NOT update the panels
     * @param playerUUID
     */
    public void addWarp(UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Adding warp");
        // Check cached warps
        if (cachedWarps.containsKey(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Found in cache");
            // Get the item
            ItemStack playerSkull = cachedWarps.get(playerUUID);
            playerSkull = updateText(playerSkull, playerUUID);
            return;
        }
        //plugin.getLogger().info("DEBUG: New skull");
        // Get the item
        ItemStack playerSkull = getSkull(playerUUID);
        if (playerSkull == null) {
            // Nothing found and not available on the server
            return;
        }
        // Update the sign text
        playerSkull = updateText(playerSkull, playerUUID);
        cachedWarps.put(playerUUID, playerSkull);
    }

    /**
     * Gets the skull for this player UUID
     * @param playerUUID
     * @return Player skull item
     */
    @SuppressWarnings("deprecation")
    private ItemStack getSkull(UUID playerUUID) {
        String playerName = bSkyBlock.getPlayers().getName(playerUUID);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: name of warp = " + playerName);
        ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (playerName == null) {
            if (DEBUG)
                plugin.getLogger().warning("Warp for Player: UUID " + playerUUID.toString() + " is unknown on this server, skipping...");
            return null;
            //playerName = playerUUID.toString().substring(0, 10);
        }
        SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(ChatColor.WHITE + playerName);
        playerSkull.setItemMeta(meta);
        return playerSkull;
    }
    public Inventory getWarpPanel(int panelNumber) {
        //makePanel();
        if (panelNumber < 0) {
            panelNumber = 0;
        } else if (panelNumber > warpPanel.size()-1) {
            panelNumber = warpPanel.size()-1;
        }
        return warpPanel.get(panelNumber);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        if (inventory.getName() == null) {
            return;
        }
        // The player that clicked the item
        final Player player = (Player) event.getWhoClicked();
        String title = inventory.getTitle();
        if (!inventory.getTitle().startsWith(plugin.getLocale(player.getUniqueId()).get("warps.title") + " #")) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlotType().equals(SlotType.OUTSIDE)) {
            player.closeInventory();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            player.updateInventory();
            return;
        }
        ItemStack clicked = event.getCurrentItem(); // The item that was clicked
        if (DEBUG)
            plugin.getLogger().info("DEBUG: inventory size = " + inventory.getSize());
        if (DEBUG)
            plugin.getLogger().info("DEBUG: clicked = " + clicked);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: rawslot = " + event.getRawSlot());
        if (event.getRawSlot() >= event.getInventory().getSize() || clicked.getType() == Material.AIR) {
            return;
        }
        int panelNumber = 0;
        try {
            panelNumber = Integer.valueOf(title.substring(title.indexOf('#')+ 1));
        } catch (Exception e) {
            panelNumber = 0;
        }
        if (clicked.getItemMeta().hasDisplayName()) {
            String command = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (DEBUG)
                plugin.getLogger().info("DEBUG: command = " + command);
            if (command != null) {
                if (command.equalsIgnoreCase(ChatColor.stripColor(plugin.getLocale().get("warps.next")))) {
                    player.closeInventory();
                    Util.runCommand(player, Settings.ISLANDCOMMAND + " warps " + (panelNumber+1));
                } else if (command.equalsIgnoreCase(ChatColor.stripColor(plugin.getLocale().get("warps.previous")))) {
                    player.closeInventory();
                    Util.runCommand(player, Settings.ISLANDCOMMAND + " warps " + (panelNumber-1));
                } else {
                    player.closeInventory();
                    Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player.getUniqueId()).get("warps.warpToPlayersSign").replace("[player]", command));
                    Util.runCommand(player, Settings.ISLANDCOMMAND + " warp " + command);
                }
            }
        }
    }

    /**
     * Update the text on all the warp icons.
     */
    public void updateAllWarpText() {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: update all Warps");
        for (UUID playerUUID : cachedWarps.keySet()) {
            // Get the item
            ItemStack playerSkull = cachedWarps.get(playerUUID);
            playerSkull = updateText(playerSkull, playerUUID);
        }
        updatePanel();
    }

    /**
     * Creates the inventory panels from the warp list and adds nav buttons
     */
    public void updatePanel() {
        // Clear the inventory panels
        warpPanel.clear();
        Collection<UUID> activeWarps = plugin.getWarpSigns().listSortedWarps();
        // Create the warp panels
        if (DEBUG)
            plugin.getLogger().info("DEBUG: warps size = " + activeWarps.size());
        int size = activeWarps.size();
        int panelNumber = size / (PANELSIZE-2);
        int remainder = (size % (PANELSIZE-2)) + 8 + 2;
        remainder -= (remainder % 9);
        if (DEBUG)
            plugin.getLogger().info("DEBUG: panel number = " + panelNumber + " remainder = " + remainder);
        int i = 0;
        // TODO: Make panel title a string
        for (i = 0; i < panelNumber; i++) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: created panel " + (i+1));
            warpPanel.add(Bukkit.createInventory(null, PANELSIZE, plugin.getLocale().get("warps.title") + " #" + (i+1)));
        }
        // Make the last panel
        if (DEBUG)
            plugin.getLogger().info("DEBUG: created panel " + (i+1));
        warpPanel.add(Bukkit.createInventory(null, remainder, plugin.getLocale().get("warps.title") + " #" + (i+1)));
        panelNumber = 0;
        int slot = 0;
        // Run through all the warps and add them to the inventories with nav buttons
        for (UUID playerUUID: activeWarps) {
            ItemStack icon = cachedWarps.get(playerUUID);
            if (icon != null) {
                warpPanel.get(panelNumber).setItem(slot++, icon);

                // Check if the panel is full
                if (slot == PANELSIZE-2) {
                    // Add navigation buttons
                    if (panelNumber > 0) {
                        warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.SIGN,plugin.getLocale().get("warps.previous"),"warps " + (panelNumber-1),"").getItem());
                    }
                    warpPanel.get(panelNumber).setItem(slot, new CPItem(Material.SIGN,plugin.getLocale().get("warps.next"),"warps " + (panelNumber+1),"").getItem());
                    // Move onto the next panel
                    panelNumber++;
                    slot = 0;
                }
            }
        }
        if (remainder != 0 && panelNumber > 0) {
            warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.SIGN,plugin.getLocale().get("warps.previous"),"warps " + (panelNumber-1),"").getItem());
        }
    }

    /**
     * Updates the meta text on the skull by looking at the warp sign
     * This MUST be run 1 TICK AFTER the sign has been created otherwise the sign is blank
     * @param playerSkull
     * @param playerUUID
     * @return updated skull item stack
     */
    private ItemStack updateText(ItemStack playerSkull, final UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Updating text on item");
        ItemMeta meta = playerSkull.getItemMeta();
        //get the sign info
        Location signLocation = plugin.getWarpSigns().getWarp(playerUUID);
        if (signLocation == null) {
            plugin.getWarpSigns().removeWarp(playerUUID);
            return playerSkull;
        }            
        //plugin.getLogger().info("DEBUG: block type = " + signLocation.getBlock().getType());
        // Get the sign info if it exists
        if (signLocation.getBlock().getType().equals(Material.SIGN_POST) || signLocation.getBlock().getType().equals(Material.WALL_SIGN)) {
            Sign sign = (Sign)signLocation.getBlock().getState();
            List<String> lines = new ArrayList<String>(Arrays.asList(sign.getLines()));
            // Check for PVP and add warning
            Island island = bSkyBlock.getIslands().getIsland(playerUUID);
            if (island != null) {
                if ((signLocation.getWorld().equals(IslandWorld.getIslandWorld()) && island.getFlag(SettingsFlag.PVP_OVERWORLD))
                        || (signLocation.getWorld().equals(IslandWorld.getNetherWorld()) && island.getFlag(SettingsFlag.PVP_NETHER))) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: pvp warning added");
                    lines.add(ChatColor.RED + bSkyBlock.getLocale(playerUUID).get("igs." + SettingsFlag.PVP_OVERWORLD));
                }
            }
            meta.setLore(lines);
            if (DEBUG)
                plugin.getLogger().info("DEBUG: lines = " + lines);
        }
        playerSkull.setItemMeta(meta);
        return playerSkull;
    }

    /**
     * Only change the text of the warp
     * @param playerUUID
     */
    public void updateWarp(UUID playerUUID) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: update Warp");

        if (cachedWarps.containsKey(playerUUID)) {
            // Get the item
            ItemStack playerSkull = cachedWarps.get(playerUUID);
            playerSkull = updateText(playerSkull, playerUUID);
            updatePanel();
        } else {
            plugin.getLogger().warning("Warps: update requested, but player unknown " + playerUUID.toString()); 
        }
    }
}
