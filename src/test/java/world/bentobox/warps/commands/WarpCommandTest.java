package world.bentobox.warps.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.warps.Warp;
import world.bentobox.warps.managers.WarpSignsManager;
import world.bentobox.warps.config.Settings;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class WarpCommandTest {

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
    private WarpCommand wc;
    @Mock
    private Settings settings;
    @Mock
    private WarpSignsManager wsm;
    @Mock
    private PlayersManager pm;
    @Mock
    private PluginManager pim;
    @Mock
    private world.bentobox.bentobox.Settings s;
    @Mock
    private BukkitScheduler sch;

    /**
     */
    @Before
    public void setUp() {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Addon
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getWorld()).thenReturn(world);

        // settings
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getWarpCommand()).thenReturn("warp");
        when(settings.getWelcomeLine()).thenReturn(WELCOME_LINE);

        // Warp Signs Manager
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        @NonNull
        Set<UUID> set = new HashSet<>();
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        when(wsm.listWarps(world)).thenReturn(set);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getPlayers()).thenReturn(pm);
        // Repeat twice because it is asked twice
        when(pm.getName(any())).thenReturn("tastybento", "tastybento", "poslovich", "poslovich", "BONNe", "BONNe", "Joe");

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // BentoBox settings
        when(plugin.getSettings()).thenReturn(s);
        when(s.getDelayTime()).thenReturn(0);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    public void warpCommandWarpCompositeCommand() {
        // Command under test
        wc = new WarpCommand(addon, ic);
    }

    public void warpCommandWarp() {
        // Command under test
        wc = new WarpCommand(addon);
    }


    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#setup()}.
     */
    @Test
    public void testSetupWarpCompositeCommand() {
        warpCommandWarpCompositeCommand();
        assertEquals("bskyblock.island.warp", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warp.help.parameters", wc.getParameters());
        assertEquals("warp.help.description", wc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#setup()}.
     */
    @Test
    public void testSetupWarp() {
        warpCommandWarp();
        assertEquals(Warp.WELCOME_WARP_SIGNS + ".warp", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warp.help.parameters", wc.getParameters());
        assertEquals("warp.help.description", wc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgs() {
        warpCommandWarpCompositeCommand();
        wc.execute(user, "warp", Collections.emptyList());
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "BSkyBlock");
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayer() {
        warpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tastybento")));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayerWarp() {
        warpCommandWarp();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tastybento")));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayerMixedCase() {
        warpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tAsTyBEnTo")));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayerStartOnly() {
        when(pm.getName(any())).thenReturn("tastybento");
        warpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tAsTy")));
    }


    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownPlayer() {
        warpCommandWarpCompositeCommand();
        assertFalse(wc.execute(user, "warp", Collections.singletonList("LSPVicky")));
        verify(user).sendMessage("warps.error.does-not-exist");
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoWarpsYet() {
        when(wsm.listWarps(world)).thenReturn(Collections.emptySet());
        warpCommandWarpCompositeCommand();
        assertFalse(wc.execute(user, "warp", Collections.singletonList("LSPVicky")));
        verify(user).sendMessage("warps.error.no-warps-yet");
        verify(user).sendMessage("warps.warpTip", "[text]", WELCOME_LINE);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        warpCommandWarpCompositeCommand();
        List<String> op = wc.tabComplete(user, "warp", Collections.singletonList("tas")).get();
        assertEquals("tastybento", op.get(0));
        assertEquals("tastybento", op.get(1));
        assertEquals("poslovich", op.get(2));
    }

}
