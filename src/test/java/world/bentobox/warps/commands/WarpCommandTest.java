package world.bentobox.warps.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.warps.Warp;
import world.bentobox.warps.WarpSignsManager;
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
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private GameModeAddon gameModeAddon;
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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Addon
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getLabel()).thenReturn("island");
        when(ic.getTopLabel()).thenReturn("island");
        when(ic.getWorld()).thenReturn(world);
        when(ic.getTopLabel()).thenReturn("bsb");

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        Optional<GameModeAddon> optionalAddon = Optional.of(gameModeAddon);
        when(iwm.getAddon(any())).thenReturn(optionalAddon);
        when(plugin.getIWM()).thenReturn(iwm);

        // Game Mode Addon
        @NonNull
        Optional<CompositeCommand> optionalAdmin = Optional.of(ic);
        when(gameModeAddon.getAdminCommand()).thenReturn(optionalAdmin);

        // World
        when(world.toString()).thenReturn("world");

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(-1);
        when(user.isPlayer()).thenReturn(true);
        when(user.getWorld()).thenReturn(world);

        // Mock item factory (for itemstacks)
        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);

        // Island
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);

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
        when(wsm.listWarps(eq(world))).thenReturn(set);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getPlayers()).thenReturn(pm);
        // Repeat twice because it is asked twice
        when(pm.getName(any())).thenReturn("tastybento", "tastybento", "poslovich", "poslovich", "BONNe", "BONNe", "Joe");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#WarpCommand(world.bentobox.warps.Warp, world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testWarpCommandWarpCompositeCommand() {
        // Command under test
        wc = new WarpCommand(addon, ic);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#WarpCommand(world.bentobox.warps.Warp)}.
     */
    @Test
    public void testWarpCommandWarp() {
        // Command under test
        wc = new WarpCommand(addon);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#setup()}.
     */
    @Test
    public void testSetupWarpCompositeCommand() {
        testWarpCommandWarpCompositeCommand();
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
        testWarpCommandWarp();
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
        testWarpCommandWarpCompositeCommand();
        wc.execute(user, "warp", Collections.emptyList());
        verify(user).sendMessage(eq("commands.help.header"), eq(TextVariables.LABEL), eq("BSkyBlock"));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayer() {
        testWarpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tastybento")));
        verify(wsm).warpPlayer(eq(world), eq(user), any());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayerWarp() {
        testWarpCommandWarp();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tastybento")));
        verify(wsm).warpPlayer(eq(world), eq(user), any());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayerMixedCase() {
        testWarpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tAsTyBEnTo")));
        verify(wsm).warpPlayer(eq(world), eq(user), any());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringKnownPlayerStartOnly() {
        testWarpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tAsTy")));
        verify(wsm).warpPlayer(eq(world), eq(user), any());
    }


    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownPlayer() {
        testWarpCommandWarpCompositeCommand();
        assertFalse(wc.execute(user, "warp", Collections.singletonList("LSPVicky")));
        verify(user).sendMessage(eq("warps.error.does-not-exist"));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoWarpsYet() {
        when(wsm.listWarps(eq(world))).thenReturn(Collections.emptySet());
        testWarpCommandWarpCompositeCommand();
        assertFalse(wc.execute(user, "warp", Collections.singletonList("LSPVicky")));
        verify(user).sendMessage(eq("warps.error.no-warps-yet"));
        verify(user).sendMessage(eq("warps.warpTip"), eq("[text]"), eq(WELCOME_LINE));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        testWarpCommandWarpCompositeCommand();
        List<String> op = wc.tabComplete(user, "warp", Collections.singletonList("tas")).get();
        assertEquals("tastybento", op.get(0));
        assertEquals("tastybento", op.get(1));
        assertEquals("poslovich", op.get(2));
    }

}
