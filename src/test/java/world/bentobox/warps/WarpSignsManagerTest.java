package world.bentobox.warps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
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
public class WarpSignsManagerTest extends CommonTestSetup {

    @Mock
    private Warp addon;
    @Mock
    private WarpsData load;
    @Mock
    private Block block;
    @Mock
    private SignCacheManager wpm;
    @Mock
    private OfflinePlayer offlinePlayer;
    @Mock
    private Settings settings;

    private WarpSignsManager wsm;

    @SuppressWarnings("unchecked")
    private static AbstractDatabaseHandler<Object> handler;
    private MockedStatic<DatabaseSetup> mockedDbSetup;

    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

        // Location
        when(location.getBlock()).thenReturn(block);
        when(location.getBlockX()).thenReturn(23);
        when(location.getBlockY()).thenReturn(24);
        when(location.getBlockZ()).thenReturn(25);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getName()).thenReturn("world");

        // Block - use wall sign so Tag checks work with MockBukkit
        when(block.getType()).thenReturn(Material.OAK_WALL_SIGN);
        when(block.getLocation()).thenReturn(location);
        Sign sign = mock(Sign.class);
        String[] lines = {"[Welcome]", "line2", "line3", "line4"};
        when(sign.getLines()).thenReturn(lines);
        when(sign.getLine(anyInt())).thenReturn("[Welcome]");
        when(sign.getType()).thenReturn(Material.OAK_WALL_SIGN);
        when(block.getState()).thenReturn(sign);
        org.bukkit.block.data.type.WallSign wallSignBd = mock(org.bukkit.block.data.type.WallSign.class);
        when(wallSignBd.getFacing()).thenReturn(BlockFace.EAST);
        when(block.getBlockData()).thenReturn(wallSignBd);
        when(block.getRelative(any())).thenReturn(block);

        // Database
        handler = mock(AbstractDatabaseHandler.class);
        mockedDbSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDbSetup.when(DatabaseSetup::getDatabase).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);

        when(handler.objectExists("warps")).thenReturn(true);
        Map<PlayerWarp, UUID> warpMap = Collections.singletonMap(new PlayerWarp(location, true), uuid);
        when(load.getWarpSigns()).thenReturn(warpMap);
        when(handler.loadObject(anyString())).thenReturn(load);

        // Settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getWelcomeLine()).thenReturn("[Welcome]");
        when(settings.getLoreFormat()).thenReturn("&f");
        when(settings.isShowWarpsOnMap()).thenReturn(true);

        // Server - use a mock server since ServerMock doesn't support stubbing
        org.bukkit.Server mockServer = mock(org.bukkit.Server.class);
        when(addon.getServer()).thenReturn(mockServer);
        when(mockServer.getPlayer(any(UUID.class))).thenReturn(mockPlayer);

        // Offline player
        when(mockServer.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getLastPlayed()).thenReturn(System.currentTimeMillis());

        // Island Manager
        when(addon.getIslands()).thenReturn(im);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // World Settings (needed for Flag.isSetForWorld)
        world.bentobox.bentobox.api.configuration.WorldSettings worldSettings = mock(world.bentobox.bentobox.api.configuration.WorldSettings.class);
        when(worldSettings.getWorldFlags()).thenReturn(new HashMap<>());
        when(iwm.getWorldSettings(any())).thenReturn(worldSettings);

        // WarpPanelManager
        when(addon.getSignCacheManager()).thenReturn(wpm);

        wsm = new WarpSignsManager(addon, plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        mockedDbSetup.closeOnDemand();
        super.tearDown();
    }

    @Test
    public void testGetWarpMap() {
        assertFalse(wsm.getWarpMap(world).isEmpty(), "Map is empty");
    }

    @Test
    public void testGetWarpMapNullWorld() {
        when(location.getWorld()).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    @Test
    public void testGetWarpMapWrongBlockType() {
        when(block.getType()).thenReturn(Material.COAL_ORE);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    @Test
    public void testGetWarpMapNullLocation() {
        PlayerWarp playerWarp = new PlayerWarp(null, true);
        Map<PlayerWarp, UUID> warpMap = Collections.singletonMap(playerWarp, uuid);
        when(load.getWarpSigns()).thenReturn(warpMap);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    @Test
    public void testGetWarpMapNullDatabaseObject() throws Exception {
        when(handler.loadObject(anyString())).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    @Test
    public void testGetWarpMapNothingInDatabase() {
        when(handler.objectExists("warps")).thenReturn(false);
        wsm = new WarpSignsManager(addon, plugin);
        assertTrue(wsm.getWarpMap(world).isEmpty(), "Map is not empty");
    }

    @Test
    public void testWarpSignsManager() {
        verify(addon).log("Loading warps...");
        verify(load).getWarpSigns();
        verify(block, times(2)).getType();
    }

    @Test
    public void testAddWarpNullPlayer() {
        assertFalse(wsm.addWarp(null, null));
    }

    @Test
    public void testAddWarpNullLocation() {
        assertFalse(wsm.addWarp(uuid, null));
    }

    @Test
    public void testAddWarpReplaceOldSign() {
        assertTrue(wsm.addWarp(uuid, location));
        this.checkSpigotMessage("warps.sign-removed");
    }

    @Test
    public void testAddWarpReplaceOldSignDifferentPlayer() {
        assertTrue(wsm.addWarp(UUID.randomUUID(), location));
        this.checkSpigotMessage("warps.sign-removed");
    }

    @Test
    public void testAddWarp() {
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        Block locBlock = mock(Block.class);
        when(loc.getBlock()).thenReturn(locBlock);
        when(locBlock.getType()).thenReturn(Material.OAK_WALL_SIGN);
        Sign locSign = mock(Sign.class);
        when(locSign.getLines()).thenReturn(new String[]{"[Welcome]", "My Warp", "", ""});
        when(locSign.getType()).thenReturn(Material.OAK_WALL_SIGN);
        when(locBlock.getState()).thenReturn(locSign);
        assertTrue(wsm.addWarp(uuid, loc));
        verify(pim).callEvent(any(WarpCreateEvent.class));
    }

    @Test
    public void testGetWarpWorldWorld() {
        assertNull(wsm.getWarpLocation(mock(World.class), uuid));
    }

    @Test
    public void testGetWarp() {
        assertEquals(location, wsm.getWarpLocation(world, uuid));
    }

    @Test
    public void testGetWarpOwner() {
        assertEquals("tastybento", wsm.getWarpOwner(location));
    }

    @Test
    public void testGetSortedWarps() {
        CompletableFuture<List<UUID>> r = new CompletableFuture<>();
        assertEquals(1, wsm.processWarpMap(r, world).size());
    }

    @Test
    public void testListWarps() {
        assertEquals(1, wsm.listWarps(world).size());
        assertEquals(uuid, wsm.listWarps(world).toArray()[0]);
    }

    @Test
    public void testRemoveWarpLocation() {
        wsm.removeWarp(location);
        assertTrue(wsm.listWarps(world).isEmpty());
    }

    @Test
    public void testRemoveWarpWorldUUID() {
        wsm.removeWarp(world, uuid);
        assertTrue(wsm.listWarps(world).isEmpty());
    }

    @Test
    public void testSaveWarpList() throws Exception {
        wsm.saveWarpList();
        verify(handler, Mockito.atLeastOnce()).saveObject(any());
    }

    @Test
    public void testWarpPlayer() {
        Player p = mock(Player.class);
        Player.Spigot pSpigot = mock(Player.Spigot.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getWorld()).thenReturn(world);
        when(p.getName()).thenReturn("tastybento");
        when(p.getLocation()).thenReturn(location);
        when(p.isOnline()).thenReturn(true);
        when(p.canSee(any(Player.class))).thenReturn(true);
        when(p.spigot()).thenReturn(pSpigot);
        @Nullable
        User u = User.getInstance(p);
        mockedUtil.when(() -> Util.teleportAsync(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
        // Use STONE block type to test the "warp removed" path since MockBukkit
        // doesn't populate sign Tag constants. This verifies the warp removal branch.
        when(block.getType()).thenReturn(Material.STONE);
        wsm.warpPlayer(world, u, uuid);
        // The warp should be detected as removed since block is no longer a sign
        verify(pim, never()).callEvent(any(WarpInitiateEvent.class));
    }

    @Test
    public void testHasWarp() {
        assertTrue(wsm.hasWarp(world, uuid));
        assertFalse(wsm.hasWarp(mock(World.class), uuid));
        assertFalse(wsm.hasWarp(world, UUID.randomUUID()));
    }

    @Test
    public void testLoadWarpListNoWarpTable() {
        when(handler.objectExists(anyString())).thenReturn(false);
        wsm = new WarpSignsManager(addon, plugin);
        wsm.saveWarpList();
        verify(addon, times(2)).log("Loading warps...");
        assertTrue(wsm.getWarpMap(world).isEmpty());
    }

    @Test
    public void testLoadWarpListEmptyWarpTable() throws Exception {
        when(handler.loadObject(anyString())).thenReturn(null);
        wsm = new WarpSignsManager(addon, plugin);
        wsm.saveWarpList();
        verify(addon, times(2)).log("Loading warps...");
        assertTrue(wsm.getWarpMap(world).isEmpty());
    }

    @Test
    public void testMapMarkersCreatedOnStartup() {
        verify(mapManager).createMarkerSet("warps", "Warp Signs");
        // Label should be the sign text (lines 2-4), not the player name
        verify(mapManager).addPointMarker(eq("warps"), eq("world:" + uuid), eq("line2 line3 line4"), eq(location), eq("sign"));
    }

    @Test
    public void testAddWarpCreatesMapMarker() {
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        Block newBlock = mock(Block.class);
        when(loc.getBlock()).thenReturn(newBlock);
        when(newBlock.getType()).thenReturn(Material.OAK_WALL_SIGN);
        Sign newSign = mock(Sign.class);
        when(newSign.getLines()).thenReturn(new String[]{"[Welcome]", "Shop", "Free stuff", ""});
        when(newSign.getType()).thenReturn(Material.OAK_WALL_SIGN);
        when(newBlock.getState()).thenReturn(newSign);
        UUID newUuid = UUID.randomUUID();
        wsm.addWarp(newUuid, loc);
        // Label should be the sign text, not the player name
        verify(mapManager).addPointMarker(eq("warps"), eq("world:" + newUuid), eq("Shop Free stuff"), eq(loc), eq("sign"));
    }

    @Test
    public void testRemoveWarpByLocationRemovesMapMarker() {
        wsm.removeWarp(location);
        verify(mapManager).removePointMarker("warps", "world:" + uuid);
    }

    @Test
    public void testRemoveWarpByUuidRemovesMapMarker() {
        wsm.removeWarp(world, uuid);
        verify(mapManager).removePointMarker("warps", "world:" + uuid);
    }

    @Test
    public void testMapMarkersDisabled() {
        when(settings.isShowWarpsOnMap()).thenReturn(false);
        Mockito.clearInvocations(mapManager);
        wsm = new WarpSignsManager(addon, plugin);
        verify(mapManager, never()).createMarkerSet(anyString(), anyString());
    }
}
