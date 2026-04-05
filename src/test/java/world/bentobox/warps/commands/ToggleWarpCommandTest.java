package world.bentobox.warps.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.warps.CommonTestSetup;
import world.bentobox.warps.Warp;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.managers.WarpSignsManager;
import world.bentobox.warps.objects.PlayerWarp;

/**
 * Tests for {@link ToggleWarpCommand}.
 */
public class ToggleWarpCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Warp addon;
    @Mock
    private Settings settings;
    @Mock
    private WarpSignsManager wsm;

    private ToggleWarpCommand cmd;
    private UUID userUUID;
    private Location warpLocation;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Composite command parent
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);

        // User
        userUUID = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(userUUID);
        when(user.getWorld()).thenReturn(world);

        // Settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getToggleWarpCommand()).thenReturn("togglewarp");
        when(settings.isShowWarpsOnMap()).thenReturn(true);

        // WarpSignsManager
        when(addon.getWarpSignsManager()).thenReturn(wsm);

        // Warp location
        warpLocation = mock(Location.class);
        when(warpLocation.getWorld()).thenReturn(world);

        cmd = new ToggleWarpCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // --- setup ---

    @Test
    public void testSetup() {
        assertTrue(cmd.isOnlyPlayer());
        assertTrue(cmd.getPermission().contains("island.warp.toggle"));
    }

    @Test
    public void testSetupStandalone() {
        cmd = new ToggleWarpCommand(addon);
        assertTrue(cmd.isOnlyPlayer());
        assertTrue(cmd.getPermission().contains("togglewarp"));
    }

    // --- execute: user has no warp ---

    @Test
    public void testExecuteNoWarp() {
        when(wsm.hasWarp(any(World.class), any(UUID.class))).thenReturn(false);
        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));
        verify(user).sendMessage("togglewarp.error.no-warp");
        verify(wsm, never()).addMapMarker(any(), any(), any());
        verify(wsm, never()).removeMapMarker(any(), any());
    }

    // --- execute: PlayerWarp is null (edge case) ---

    @Test
    public void testExecuteNullPlayerWarp() {
        when(wsm.hasWarp(any(World.class), any(UUID.class))).thenReturn(true);
        when(wsm.getPlayerWarp(any(World.class), any(UUID.class))).thenReturn(null);
        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));
        verify(user).sendMessage("togglewarp.error.generic");
        verify(wsm, never()).addMapMarker(any(), any(), any());
        verify(wsm, never()).removeMapMarker(any(), any());
    }

    // --- execute: warp currently enabled → toggle off → remove marker ---

    @Test
    public void testExecuteToggleOffRemovesMarker() {
        PlayerWarp warp = new PlayerWarp(warpLocation, true); // enabled
        when(wsm.hasWarp(world, userUUID)).thenReturn(true);
        when(wsm.getPlayerWarp(world, userUUID)).thenReturn(warp);

        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));

        // After toggle, warp is disabled → marker should be removed
        assertFalse(warp.isEnabled());
        verify(wsm).removeMapMarker(world, userUUID);
        verify(wsm, never()).addMapMarker(any(), any(), any());
        verify(user).sendMessage("togglewarp.disabled");
    }

    // --- execute: warp currently disabled → toggle on → add marker ---

    @Test
    public void testExecuteToggleOnAddsMarker() {
        PlayerWarp warp = new PlayerWarp(warpLocation, false); // disabled
        when(wsm.hasWarp(world, userUUID)).thenReturn(true);
        when(wsm.getPlayerWarp(world, userUUID)).thenReturn(warp);

        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));

        // After toggle, warp is enabled → marker should be added
        assertTrue(warp.isEnabled());
        verify(wsm).addMapMarker(world, userUUID, warpLocation);
        verify(wsm, never()).removeMapMarker(any(), any());
        verify(user).sendMessage("togglewarp.enabled");
    }

    // --- execute: show-warps-on-map is false → addMapMarker is called but WarpSignsManager
    //     itself will suppress the underlying API call ---

    @Test
    public void testExecuteToggleOnMapMarkersDisabled() {
        when(settings.isShowWarpsOnMap()).thenReturn(false);
        PlayerWarp warp = new PlayerWarp(warpLocation, false); // disabled
        when(wsm.hasWarp(world, userUUID)).thenReturn(true);
        when(wsm.getPlayerWarp(world, userUUID)).thenReturn(warp);

        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));

        // Command still delegates to wsm.addMapMarker; the manager guards internally
        verify(wsm).addMapMarker(world, userUUID, warpLocation);
        verify(user).sendMessage("togglewarp.enabled");
    }

    @Test
    public void testExecuteToggleOffMapMarkersDisabled() {
        when(settings.isShowWarpsOnMap()).thenReturn(false);
        PlayerWarp warp = new PlayerWarp(warpLocation, true); // enabled
        when(wsm.hasWarp(world, userUUID)).thenReturn(true);
        when(wsm.getPlayerWarp(world, userUUID)).thenReturn(warp);

        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));

        // Command still delegates to wsm.removeMapMarker; the manager handles it
        verify(wsm).removeMapMarker(world, userUUID);
        verify(user).sendMessage("togglewarp.disabled");
    }

    // --- execute: MapManager absent → no NPE ---

    @Test
    public void testExecuteToggleOnNoMapManager() {
        when(plugin.getMapManager()).thenReturn(null);
        PlayerWarp warp = new PlayerWarp(warpLocation, false);
        when(wsm.hasWarp(world, userUUID)).thenReturn(true);
        when(wsm.getPlayerWarp(world, userUUID)).thenReturn(warp);

        // Should not throw
        assertFalse(cmd.execute(user, "togglewarp", Collections.emptyList()));
        verify(wsm).addMapMarker(world, userUUID, warpLocation);
        verify(user).sendMessage("togglewarp.enabled");
    }
}
