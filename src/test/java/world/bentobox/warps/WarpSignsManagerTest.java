package world.bentobox.warps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.event.WarpInitiateEvent;
import world.bentobox.warps.managers.SignCacheItem;
import world.bentobox.warps.managers.SignCacheManager;
import world.bentobox.warps.managers.WarpSignsManager;
import world.bentobox.warps.objects.WarpsData;


/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, Util.class, DatabaseSetup.class, ChatColor.class})
public class WarpSignsManagerTest {

    @Mock
    private Warp addon;
    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private static AbstractDatabaseHandler<Object> handler;


    private WarpSignsManager wsm;
    @Mock
    private Logger logger;
    @Mock
    private WarpsData load;
    private final UUID uuid = UUID.randomUUID();
    @Mock
    private Location location;
    @Mock
    private Block block;
    @Mock
    private PluginManager pim;
    @Mock
    private Server server;
    @Mock
    private Player player;
    @Mock
    private SignCacheManager wpm;
    @Mock
    private PlayersManager pm;
    @Mock
    private OfflinePlayer offlinePlayer;
    @Mock
    private Settings settings;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;


    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
    }

    /**
     * @throws java.lang.Exception exception
     */
    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getLogger()).thenReturn(logger);

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        User.setPlugin(plugin);
        User.getInstance(player);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn(null);
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the same string
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getPlaceholdersManager()).thenReturn(phm);


        // Server
        when(addon.getServer()).thenReturn(server);
        when(server.getPlayer(any(UUID.class))).thenReturn(player);

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenAnswer((Answer<World>) invocation -> invocation.getArgument(0, World.class));
        when(Util.sameWorld(any(), any())).thenReturn(true);
        when(Util.translateColorCodes(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlock()).thenReturn(block);
        when(location.getBlockX()).thenReturn(23);
        when(location.getBlockY()).thenReturn(24);
        when(location.getBlockZ()).thenReturn(25);
        when(player.getLocation()).thenReturn(location);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);

        // Block
        when(block.getType()).thenReturn(Material.ACACIA_SIGN);
        when(block.getLocation()).thenReturn(location);
        Sign sign = mock(Sign.class);
        String[] lines = {"[Welcome]", "line2", "line3", "line4"};
        when(sign.getLines()).thenReturn(lines);
        when(sign.getLine(anyInt())).thenReturn("[Welcome]");
        when(sign.getType()).thenReturn(Material.ACACIA_SIGN);
        when(block.getState()).thenReturn(sign);
        org.bukkit.block.data.type.Sign signBd = mock(org.bukkit.block.data.type.Sign.class);
        when(signBd.getRotation()).thenReturn(BlockFace.EAST);
        when(block.getBlockData()).thenReturn(signBd);
        when(block.getRelative(any())).thenReturn(block);

        // Handler
        when(handler.objectExists("warps")).thenReturn(true);
        Map<Location, UUID> warpMap = Collections.singletonMap(location, uuid);
        when(load.getWarpSigns()).thenReturn(warpMap);
        when(handler.loadObject(anyString())).thenReturn(load);

        // Settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getWelcomeLine()).thenReturn("[Welcome]");
        when(settings.getLoreFormat()).thenReturn("&f");

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(uuid)).thenReturn("tastybento");

        // Offline player
        when(server.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getLastPlayed()).thenReturn(System.currentTimeMillis());

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        // Island Manager
        when(addon.getIslands()).thenReturn(im);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.isSafeLocation(any())).thenReturn(true);

        // WarpPanelManager
        when(addon.getSignCacheManager()).thenReturn(wpm);

        wsm = new WarpSignsManager(addon, plugin);
    }

    /**
     */
    @After
    public void tearDown() {
        User.clearUsers();
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMap() {
        assertFalse("Map is empty", wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapNullWorld() {
        when(location.getWorld()).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue("Map is not empty", wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapWrongBlockType() {
        when(block.getType()).thenReturn(Material.COAL_ORE);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue("Map is not empty", wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapNullLocation() {
        Map<Location, UUID> warpMap = Collections.singletonMap(null, uuid);
        when(load.getWarpSigns()).thenReturn(warpMap);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue("Map is not empty", wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     * @throws Exception exception
     */
    @Test
    public void testGetWarpMapNullDatabaseObject() throws Exception {
        when(handler.loadObject(anyString())).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue("Map is not empty", wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapNothingInDatabase() {
        when(handler.objectExists("warps")).thenReturn(false);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue("Map is not empty", wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#WarpSignsManager(world.bentobox.warps.Warp, world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testWarpSignsManager() {
        verify(addon).log("Loading warps...");
        verify(load).getWarpSigns();
        verify(block).getType();
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarpNullPlayer() {
        assertFalse(wsm.addWarp(null, null));
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarpNullLocation() {
        assertFalse(wsm.addWarp(uuid, null));
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarpReplaceOldSign() {
        assertTrue(wsm.addWarp(uuid, location));
        verify(player).sendMessage("warps.sign-removed");
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarpReplaceOldSignDifferentPlayer() {
        assertTrue(wsm.addWarp(UUID.randomUUID(), location));
        verify(player).sendMessage("warps.sign-removed");
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarp() {
        Location loc = mock(Location.class);
        assertTrue(wsm.addWarp(uuid, loc));
        verify(pim).callEvent(any(WarpInitiateEvent.class));
    }

    /**
     * Test method for {@link WarpSignsManager#getWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetWarpWorldWorld() {
        assertNull(wsm.getWarp(mock(World.class), uuid));
    }

    /**
     * Test method for {@link WarpSignsManager#getWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetWarp() {
        assertEquals(location, wsm.getWarp(world, uuid));
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpOwner(org.bukkit.Location)}.
     */
    @Test
    public void testGetWarpOwner() {
        assertEquals("tastybento", wsm.getWarpOwner(location));
    }

    /**
     * Test method for {@link WarpSignsManager#getSortedWarps(org.bukkit.World)}.
     */
    @Test
    public void testGetSortedWarps() {
        CompletableFuture<List<UUID>> r = new CompletableFuture<>();
        assertEquals(1, wsm.processWarpMap(r, world).size());
    }

    /**
     * Test method for {@link WarpSignsManager#listWarps(org.bukkit.World)}.
     */
    @Test
    public void testListWarps() {
        assertEquals(1, wsm.listWarps(world).size());
        assertEquals(uuid, wsm.listWarps(world).toArray()[0]);
    }

    /**
     * Test method for {@link WarpSignsManager#removeWarp(org.bukkit.Location)}.
     */
    @Test
    public void testRemoveWarpLocation() {
        wsm.removeWarp(location);
        assertTrue(wsm.listWarps(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#removeWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemoveWarpWorldUUID() {
        wsm.removeWarp(world, uuid);
        assertTrue(wsm.listWarps(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#saveWarpList()}.
     * @throws Exception general exception
     */
    @Test
    public void testSaveWarpList() throws Exception {
        wsm.saveWarpList();
        verify(handler, Mockito.atLeastOnce()).saveObject(any());
    }

    /**
     * Test method for {@link WarpSignsManager#warpPlayer(org.bukkit.World, world.bentobox.bentobox.api.user.User, java.util.UUID)}.
     */
    @Test
    public void testWarpPlayer() {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getWorld()).thenReturn(world);
        when(p.getName()).thenReturn("tastybento");
        when(p.getLocation()).thenReturn(location);
        @Nullable
        User u = User.getInstance(p);
        wsm.warpPlayer(world, u, uuid);
        PowerMockito.verifyStatic(Util.class);
        Util.teleportAsync(eq(p), any(), eq(TeleportCause.COMMAND));
        verify(player).sendMessage("warps.player-warped");
    }

    /**
     * Test method for {@link WarpSignsManager#hasWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testHasWarp() {
        assertTrue(wsm.hasWarp(world, uuid));
        assertFalse(wsm.hasWarp(mock(World.class), uuid));
        assertFalse(wsm.hasWarp(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link WarpSignsManager#loadWarpList()}.
     */
    @Test
    public void testLoadWarpListNoWarpTable() {
        // Run again but with no database table
        when(handler.objectExists(anyString())).thenReturn(false);
        wsm = new WarpSignsManager(addon, plugin);
        // Save
        wsm.saveWarpList();
        // Default load in constructor check
        verify(addon, times(2)).log("Loading warps...");
        assertTrue(wsm.getWarpMap(world).isEmpty());
    }

    /**
     * Test method for {@link WarpSignsManager#loadWarpList()}.
     * @throws Exception exception
     */
    @Test
    public void testLoadWarpListEmptyWarpTable() throws Exception {
        // Run again but with no data in table
        when(handler.loadObject(anyString())).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        // Save
        wsm.saveWarpList();
        // Default load in constructor check
        verify(addon, times(2)).log("Loading warps...");
        assertTrue(wsm.getWarpMap(world).isEmpty());
    }
}
