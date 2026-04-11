package world.bentobox.warps.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.warps.CommonTestSetup;
import world.bentobox.warps.Warp;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.managers.WarpSignsManager;

/**
 * @author tastybento
 *
 */
public class WarpCommandTest extends CommonTestSetup {

    private static final String WELCOME_LINE = "[Welcome]";
    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Warp addon;
    private WarpCommand wc;
    @Mock
    private Settings settings;
    @Mock
    private WarpSignsManager wsm;
    @Mock
    private world.bentobox.bentobox.Settings s;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Addon
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);

        // Player
        UUID userUuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(userUuid);
        when(user.getWorld()).thenReturn(world);

        // Settings
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
        when(addon.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("tastybento", "tastybento", "poslovich", "poslovich", "BONNe", "BONNe", "Joe");

        // BentoBox settings
        when(plugin.getSettings()).thenReturn(s);
        when(s.getDelayTime()).thenReturn(0);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void warpCommandWarpCompositeCommand() {
        wc = new WarpCommand(addon, ic);
    }

    public void warpCommandWarp() {
        wc = new WarpCommand(addon);
    }

    @Test
    public void testSetupWarpCompositeCommand() {
        warpCommandWarpCompositeCommand();
        assertEquals("bskyblock.island.warp", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warp.help.parameters", wc.getParameters());
        assertEquals("warp.help.description", wc.getDescription());
    }

    @Test
    public void testSetupWarp() {
        warpCommandWarp();
        assertEquals(Warp.WELCOME_WARP_SIGNS + ".warp", wc.getPermission());
        assertTrue(wc.isOnlyPlayer());
        assertEquals("warp.help.parameters", wc.getParameters());
        assertEquals("warp.help.description", wc.getDescription());
    }

    @Test
    public void testExecuteUserStringListOfStringNoArgs() {
        warpCommandWarpCompositeCommand();
        wc.execute(user, "warp", Collections.emptyList());
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "BSkyBlock");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownPlayer() {
        warpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tastybento")));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownPlayerWarp() {
        warpCommandWarp();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tastybento")));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownPlayerMixedCase() {
        warpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tAsTyBEnTo")));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownPlayerStartOnly() {
        when(pm.getName(any())).thenReturn("tastybento");
        warpCommandWarpCompositeCommand();
        assertTrue(wc.execute(user, "warp", Collections.singletonList("tAsTy")));
    }

    @Test
    public void testExecuteUserStringListOfStringUnknownPlayer() {
        warpCommandWarpCompositeCommand();
        assertFalse(wc.execute(user, "warp", Collections.singletonList("LSPVicky")));
        verify(user).sendMessage("warps.error.does-not-exist");
    }

    @Test
    public void testExecuteUserStringListOfStringNoWarpsYet() {
        when(wsm.listWarps(world)).thenReturn(Collections.emptySet());
        warpCommandWarpCompositeCommand();
        assertFalse(wc.execute(user, "warp", Collections.singletonList("LSPVicky")));
        verify(user).sendMessage("warps.error.no-warps-yet");
        verify(user).sendMessage("warps.warpTip", "[text]", WELCOME_LINE);
    }

    @Test
    public void testTabCompleteUserStringListOfString() {
        warpCommandWarpCompositeCommand();
        List<String> op = wc.tabComplete(user, "warp", Collections.singletonList("tas")).get();
        assertEquals("tastybento", op.get(0));
        assertEquals("tastybento", op.get(1));
        assertEquals("poslovich", op.get(2));
    }
}
