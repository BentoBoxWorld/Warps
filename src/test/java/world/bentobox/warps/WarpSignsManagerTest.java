package world.bentobox.warps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player.Spigot;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import net.md_5.bungee.api.chat.TextComponent;
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
import world.bentobox.warps.event.WarpCreateEvent;
import world.bentobox.warps.event.WarpInitiateEvent;
import world.bentobox.warps.managers.SignCacheManager;
import world.bentobox.warps.managers.WarpSignsManager;
import world.bentobox.warps.objects.PlayerWarp;
import world.bentobox.warps.objects.WarpsData;


/**
 * @author tastybento
 *
 */
public class WarpSignsManagerTest {

    @Mock
    private Warp addon;
    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @SuppressWarnings("rawtypes")
    private AbstractDatabaseHandler handler;


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
    @Mock
    private Spigot spigot;

    private AutoCloseable closeable;
    private ServerMock mockServer;
    private MockedStatic<Bukkit> mockedBukkit;
    private MockedStatic<Util> mockedUtil;
    private MockedStatic<DatabaseSetup> mockedDb;

    /**
     * Check that spigot sent the message
     * @param message - message to check
     */
    public void checkSpigotMessage(String expectedMessage) {
        checkSpigotMessage(expectedMessage, 1);
    }

    public void checkSpigotMessage(String expectedMessage, int expectedOccurrences) {
        ArgumentCaptor<net.kyori.adventure.text.Component> captor = ArgumentCaptor
                .forClass(net.kyori.adventure.text.Component.class);
        verify(player, atLeast(0)).sendMessage(captor.capture());
        List<net.kyori.adventure.text.Component> capturedMessages = captor.getAllValues();
        long actualOccurrences = capturedMessages.stream()
                .map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c))
                .filter(messageText -> messageText.contains(expectedMessage))
                .count();
        assertEquals(expectedOccurrences, actualOccurrences,
                "Expected message occurrence mismatch: " + expectedMessage);
    }

    public void checkNoSpigotMessages() {
        try {
            verify(player, never()).sendMessage(any(net.kyori.adventure.text.Component.class));
        } catch (AssertionError e) {
            fail("Expected no messages to be sent, but some messages were sent.");
        }
    }

    /**
     * @throws java.lang.Exception exception
     */
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        mockServer = MockBukkit.mock();

        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        // Eagerly init Tag constants under real MockBukkit before we stub Bukkit statically
        @SuppressWarnings("unused")
        var unusedTagRef = Tag.STANDING_SIGNS;

        // Database setup static mock
        handler = mock(AbstractDatabaseHandler.class);
        mockedDb = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDb.when(DatabaseSetup::getDatabase).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);

        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getLogger()).thenReturn(logger);

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getWorld()).thenReturn(world);
        when(player.isOnline()).thenReturn(true);
        when(player.canSee(any(Player.class))).thenReturn(true);
        when(player.spigot()).thenReturn(spigot);
        User.setPlugin(plugin);
        User.getInstance(player);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.getAvailablePrefixes(any())).thenReturn(Collections.emptySet());
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the same string
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getPlaceholdersManager()).thenReturn(phm);


        // Server
        when(addon.getServer()).thenReturn(server);
        when(server.getPlayer(any(UUID.class))).thenReturn(player);

        // Util
        mockedUtil = Mockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        mockedUtil.when(() -> Util.getWorld(any())).thenAnswer((Answer<World>) invocation -> invocation.getArgument(0, World.class));
        mockedUtil.when(() -> Util.sameWorld(any(), any())).thenReturn(true);
        mockedUtil.when(() -> Util.translateColorCodes(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

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
        Map<PlayerWarp, UUID> warpMap = Collections.singletonMap(new PlayerWarp(location, true), uuid);
        when(load.getWarpSigns()).thenReturn(warpMap);
        when(handler.loadObject(anyString())).thenReturn(load);

        // Settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getWelcomeLine()).thenReturn("[Welcome]");
        when(settings.getLoreFormat()).thenReturn("&f");

        // Bukkit
        mockedBukkit = Mockito.mockStatic(Bukkit.class, Mockito.RETURNS_DEEP_STUBS);
        mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pim);
        mockedBukkit.when(Bukkit::getServer).thenReturn(mockServer);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(uuid)).thenReturn("tastybento");

        // Offline player
        when(server.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getLastPlayed()).thenReturn(System.currentTimeMillis());

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

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
    @AfterEach
    public void tearDown() throws Exception {
        mockedBukkit.closeOnDemand();
        mockedUtil.closeOnDemand();
        mockedDb.closeOnDemand();
        closeable.close();
        MockBukkit.unmock();
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMap() {
        assertFalse(wsm.getWarpMap(world).isEmpty(), "Map is empty");
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapNullWorld() {
        when(location.getWorld()).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapWrongBlockType() {
        when(block.getType()).thenReturn(Material.COAL_ORE);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapNullLocation() {
        PlayerWarp playerWarp = new PlayerWarp(null, true);
        Map<PlayerWarp, UUID> warpMap = Collections.singletonMap(playerWarp, uuid);
        when(load.getWarpSigns()).thenReturn(warpMap);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     * @throws Exception exception
     */
    @Test
    public void testGetWarpMapNullDatabaseObject() throws Exception {
        when(handler.loadObject(anyString())).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpMap(org.bukkit.World)}.
     */
    @Test
    public void testGetWarpMapNothingInDatabase() {
        when(handler.objectExists("warps")).thenReturn(false);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
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
        this.checkSpigotMessage("warps.sign-removed");
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarpReplaceOldSignDifferentPlayer() {
        assertTrue(wsm.addWarp(UUID.randomUUID(), location));
        this.checkSpigotMessage("warps.sign-removed");
    }

    /**
     * Test method for {@link WarpSignsManager#addWarp(java.util.UUID, org.bukkit.Location)}.
     */
    @Test
    public void testAddWarp() {
        Location loc = mock(Location.class);
        assertTrue(wsm.addWarp(uuid, loc));
        verify(pim).callEvent(any(WarpCreateEvent.class));
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpLocation(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetWarpWorldWorld() {
        assertNull(wsm.getWarpLocation(mock(World.class), uuid));
    }

    /**
     * Test method for {@link WarpSignsManager#getWarpLocation(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetWarp() {
        assertEquals(location, wsm.getWarpLocation(world, uuid));
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
    @SuppressWarnings("unchecked")
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
        when(p.isOnline()).thenReturn(true);
        when(p.canSee(any(Player.class))).thenReturn(true);
        @Nullable
        User u = User.getInstance(p);
        mockedUtil.when(() -> Util.teleportAsync(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
        wsm.warpPlayer(world, u, uuid);
        mockedUtil.verify(() -> Util.teleportAsync(eq(p), any(), eq(TeleportCause.COMMAND)));
        verify(pim).callEvent(any(WarpInitiateEvent.class));
    }

    /**
     * Test method for {@link WarpSignsManager#warpPlayer(org.bukkit.World, world.bentobox.bentobox.api.user.User, java.util.UUID)}.
     */
    @Test
    public void testWarpPlayerEventCancelled() {
        // Capture the event passed to callEvent
        ArgumentCaptor<WarpInitiateEvent> eventCaptor = ArgumentCaptor.forClass(WarpInitiateEvent.class);

        // Simulate the event being called and cancelled
        doAnswer(invocation -> {
            WarpInitiateEvent event = (WarpInitiateEvent) invocation.getArgument(0);
            event.setCancelled(true);
            return null;
        }).when(pim).callEvent(eventCaptor.capture());

        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getWorld()).thenReturn(world);
        when(p.getName()).thenReturn("tastybento");
        when(p.getLocation()).thenReturn(location);
        when(p.isOnline()).thenReturn(true);
        when(p.canSee(any(Player.class))).thenReturn(true);
        @Nullable
        User u = User.getInstance(p);
        mockedUtil.when(() -> Util.teleportAsync(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
        wsm.warpPlayer(world, u, uuid);
        mockedUtil.verify(() -> Util.teleportAsync(eq(p), any(), eq(TeleportCause.COMMAND)), never());
        verify(player, never()).sendMessage(anyString());
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
