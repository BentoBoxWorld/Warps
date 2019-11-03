package world.bentobox.warps.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.warps.Warp;
import world.bentobox.warps.WarpPanelManager;
import world.bentobox.warps.WarpSignsManager;
import world.bentobox.warps.config.Settings;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class WarpsCommandTest {

    private static final String WELCOME_LINE = "[Welcome]";
    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Warp addon;
    // Command under test
    private WarpsCommand wc;
    @Mock
    private Settings settings;
    @Mock
    private WarpSignsManager wsm;
    @Mock
    private PlayersManager pm;
    @Mock
    private WarpPanelManager wpm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Addon
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");

        // World
        when(world.toString()).thenReturn("world");

        // Player
        when(user.getWorld()).thenReturn(world);

        // settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getWarpsCommand()).thenReturn("warps");
        when(settings.getWelcomeLine()).thenReturn(WELCOME_LINE);

        // Warp Signs Manager
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        @NonNull
        Set<UUID> set = new HashSet<>();
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        when(wsm.listWarps(eq(world))).thenReturn(set);

        // Warp Panel Manager
        when(addon.getWarpPanelManager()).thenReturn(wpm);

    }

    public void warpCommandWarpsCompositeCommand() {
        // Command under test
        wc = new WarpsCommand(addon, ic);
    }

    public void warpCommandWarps() {
        // Command under test
        wc = new WarpsCommand(addon);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#setup()}.
     */
    @Test
    public void testSetupWarpCompositeCommand() {
        warpCommandWarpsCompositeCommand();
        assertEquals("bskyblock.island.warp", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warps.help.description", wc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#setup()}.
     */
    @Test
    public void testSetupWarp() {
        warpCommandWarps();
        assertEquals(Warp.WELCOME_WARP_SIGNS + ".warp", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warps.help.description", wc.getDescription());
    }


    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoWarpsYet() {
        when(wsm.listWarps(eq(world))).thenReturn(Collections.emptySet());
        warpCommandWarpsCompositeCommand();
        assertFalse(wc.execute(user, "warps", Collections.emptyList()));
        verify(user).sendMessage(eq("warps.error.no-warps-yet"));
        verify(user).sendMessage(eq("warps.warpTip"), eq("[text]"), eq(WELCOME_LINE));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoWarpsYetNoAddon() {
        when(wsm.listWarps(eq(world))).thenReturn(Collections.emptySet());
        warpCommandWarps();
        assertFalse(wc.execute(user, "warps", Collections.emptyList()));
        verify(user).sendMessage(eq("warps.error.no-warps-yet"));
        verify(user).sendMessage(eq("warps.warpTip"), eq("[text]"), eq(WELCOME_LINE));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        warpCommandWarpsCompositeCommand();
        assertTrue(wc.execute(user, "warps", Collections.emptyList()));
        verify(wpm).showWarpPanel(world, user, 0);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoAddon() {
        warpCommandWarps();
        assertTrue(wc.execute(user, "warps", Collections.emptyList()));
        verify(wpm).showWarpPanel(world, user, 0);
    }

}
