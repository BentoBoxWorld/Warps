package world.bentobox.warps.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.warps.Warp;
import world.bentobox.warps.WhiteBox;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.managers.SignCacheManager;
import world.bentobox.warps.managers.WarpSignsManager;

/**
 * @author tastybento
 *
 */
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
    private SignCacheManager wpm;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

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
        when(wsm.listWarps(world)).thenReturn(set);

        CompletableFuture<List<UUID>> warps = new CompletableFuture<>();
        warps.complete(set.stream().toList());

        when(wsm.getSortedWarps(world)).thenReturn(warps);

        // Warp Panel Manager
        when(addon.getSignCacheManager()).thenReturn(wpm);

    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
        Mockito.framework().clearInlineMocks();
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
        assertEquals("bskyblock.island.warps", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warps.help.description", wc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#setup()}.
     */
    @Test
    public void testSetupWarp() {
        warpCommandWarps();
        assertEquals(Warp.WELCOME_WARP_SIGNS + ".warps", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warps.help.description", wc.getDescription());
    }


    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoWarpsYet() {
        when(wsm.listWarps(world)).thenReturn(Collections.emptySet());
        warpCommandWarpsCompositeCommand();
        assertFalse(wc.execute(user, "warps", Collections.emptyList()));
        verify(user).sendMessage("warps.error.no-warps-yet");
        verify(user).sendMessage("warps.warpTip", "[text]", WELCOME_LINE);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoWarpsYetNoAddon() {
        when(wsm.listWarps(world)).thenReturn(Collections.emptySet());
        warpCommandWarps();
        assertFalse(wc.execute(user, "warps", Collections.emptyList()));
        verify(user).sendMessage("warps.error.no-warps-yet");
        verify(user).sendMessage("warps.warpTip", "[text]", WELCOME_LINE);
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        warpCommandWarpsCompositeCommand();
        assertTrue(wc.execute(user, "warps", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.warps.commands.WarpsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoAddon() {
        warpCommandWarps();
        assertTrue(wc.execute(user, "warps", Collections.emptyList()));
    }

}
