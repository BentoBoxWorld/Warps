package world.bentobox.warps.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.md_5.bungee.api.chat.TextComponent;
import world.bentobox.bentobox.api.events.flags.FlagProtectionChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.warps.CommonTestSetup;
import world.bentobox.warps.Warp;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.managers.WarpSignsManager;
import world.bentobox.warps.objects.PlayerWarp;

/**
 * @author tastybento
 *
 */
public class WarpSignsListenerTest extends CommonTestSetup {

    @Mock
    private Warp addon;
    @Mock
    private Block block;
    @Mock
    private Player player;
    @Mock
    private Sign s;
    @Mock
    private SignSide signSide;
    @Mock
    private WarpSignsManager wsm;
    @Mock
    private FileConfiguration config;
    @Mock
    private Settings settings;

    private String[] lines;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        when(addon.inRegisteredWorld(any())).thenReturn(true);
        when(config.getString(anyString())).thenReturn("[WELCOME]");
        when(addon.getConfig()).thenReturn(config);

        // Block
        Material sign;
        try {
            sign = Material.valueOf("OAK_WALL_SIGN");
        } catch (Exception e) {
            sign = Material.valueOf("WALL_SIGN");
        }
        when(block.getType()).thenReturn(sign);
        when(block.getWorld()).thenReturn(world);

        // Player
        when(player.hasPermission(anyString())).thenReturn(false);
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getWorld()).thenReturn(world);
        when(player.spigot()).thenReturn(spigot);
        when(s.getLine(anyInt())).thenReturn(ChatColor.GREEN + "[WELCOME]");
        when(block.getState()).thenReturn(s);
        when(s.getSide(any())).thenReturn(signSide);
        when(signSide.getLine(anyInt())).thenReturn(ChatColor.GREEN + "[WELCOME]");

        // Warp signs manager
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        Map<UUID, PlayerWarp> list = new HashMap<>();
        Location loc = mock(Location.class);
        when(loc.getBlock()).thenReturn(block);
        when(s.getLocation()).thenReturn(loc);
        when(block.getLocation()).thenReturn(loc);
        list.put(playerUuid, new PlayerWarp(loc, true));
        when(wsm.getWarpMap(world)).thenReturn(list);
        when(wsm.getWarpLocation(any(), any())).thenReturn(loc);
        when(wsm.addWarp(any(), any())).thenReturn(true);

        // BentoBox
        when(addon.getPlugin()).thenReturn(plugin);
        User.setPlugin(plugin);

        // Lines
        lines = new String[] {"[WELCOME]", "line2", "line3", "line4"};

        when(settings.getWarpLevelRestriction()).thenReturn(10);
        when(settings.getWelcomeLine()).thenReturn("[WELCOME]");
        when(addon.getSettings()).thenReturn(settings);

        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // On island
        when(im.userIsOnIsland(any(World.class), any(User.class))).thenReturn(true);

        // Sufficient level
        when(addon.getLevel(any(), any())).thenReturn(100L);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    public void checkSpigotMessage(String expectedMessage, int expectedOccurrences) {
        checkSpigotMessage(player, expectedMessage, expectedOccurrences);
    }

    @Test
    public void testWarpSignsListener() {
        assertNotNull(new WarpSignsListener(addon));
    }

    @Test
    public void testOnSignBreakNotSign() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(block.getType()).thenReturn(Material.STONE);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnSignNotGameWorld() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(addon.inRegisteredWorld(any())).thenReturn(false);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(addon).inRegisteredWorld(world);
    }

    @Test
    public void testOnSignNotWelcomeSign() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(signSide.getLine(anyInt())).thenReturn(ChatColor.RED + "[WELCOME]");
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(signSide).getLine(0);
        verify(settings).getWelcomeLine();
    }

    @Test
    public void testOnSignNotRealWelcomeSign() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(s.getLocation()).thenReturn(mock(Location.class));
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(wsm).getWarpMap(world);
        verify(s).getLocation();
    }

    @Test
    public void testOnSignRemovePlayerSignWrongPlayer() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        wsl.onSignBreak(e);
        assertTrue(e.isCancelled());
        checkSpigotMessage("warps.error.no-remove");
    }

    @Test
    public void testOnSignRemovePlayerSignPlayerIsOp() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(player.isOp()).thenReturn(true);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(pim).callEvent(any());
    }

    @Test
    public void testOnSignRemovePlayerSignPlayerHasPerm() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(player.hasPermission(anyString())).thenReturn(true);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(pim).callEvent(any());
    }

    @Test
    public void testOnSignRemoveCorrectPlayer() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(pim).callEvent(any());
    }

    @Test
    public void testOnCreateWrongWorldGameWorld() {
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        when(addon.inRegisteredWorld(any())).thenReturn(false);
        wsl.onSignWarpCreate(e);
        verify(addon).inRegisteredWorld(world);
    }

    @Test
    public void testOnCreateNotGameWorldAllowed() {
        when(settings.isAllowInOtherWorlds()).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        when(addon.inRegisteredWorld(any())).thenReturn(false);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testOnCreateWithoutCorrectRankNotAllowed() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(addon.inRegisteredWorld(any())).thenReturn(true);
        when(island.getRank(player.getUniqueId())).thenReturn(0);
        when(island.getFlag(any())).thenReturn(1000);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.error.not-correct-rank");
    }

    @Test
    public void testOnFlagChangeWhenSettingIsOffNothingHappens() {
        Flag flag = mock(Flag.class);
        when(addon.getCreateWarpFlag()).thenReturn(flag);
        when(settings.getRemoveExistingWarpsWhenFlagChanges()).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        FlagProtectionChangeEvent e = new FlagProtectionChangeEvent(island, player.getUniqueId(), flag, 1000);
        wsl.onFlagChange(e);
        verifyNoInteractions(island);
    }

    @Test
    public void testOnFlagChangeWhenSettingIsOnWarpGetsRemoved() {
        Flag flag = mock(Flag.class);
        when(addon.getCreateWarpFlag()).thenReturn(flag);
        when(settings.getRemoveExistingWarpsWhenFlagChanges()).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        Map<UUID, PlayerWarp> warps = Map.of(
                player.getUniqueId(), new PlayerWarp(block.getLocation(), true)
        );
        when(wsm.getWarpMap(any())).thenReturn(warps);
        when(island.inIslandSpace(any(Location.class))).thenReturn(true);
        FlagProtectionChangeEvent e = new FlagProtectionChangeEvent(island, player.getUniqueId(), flag, 1000);
        wsl.onFlagChange(e);
        verify(addon.getWarpSignsManager()).removeWarp(any(), any());
    }

    @Test
    public void testOnCreateNotGameWorldNotAllowed() {
        when(settings.isAllowInOtherWorlds()).thenReturn(false);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        when(addon.inRegisteredWorld(any())).thenReturn(false);
        wsl.onSignWarpCreate(e);
        verify(player, never()).sendMessage("warps.success");
    }

    @Test
    public void testOnCreateNotGameWorldNoPerm() {
        when(settings.isAllowInOtherWorlds()).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        when(addon.inRegisteredWorld(any())).thenReturn(false);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.error.no-permission");
    }

    @Test
    public void testOnCreateWrongText() {
        when(player.hasPermission(anyString())).thenReturn(true);
        lines = new String[] {"line1", "line2", "line3", "line4"};
        when(player.hasPermission(anyString())).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        verify(settings, times(2)).getWelcomeLine();
        // Verify no messages sent
        try {
            verify(spigot, never()).sendMessage(any(TextComponent.class));
        } catch (AssertionError ex) {
            fail("Expected no messages to be sent, but some messages were sent.");
        }
    }

    @Test
    public void testOnCreateNoPerm() {
        when(player.hasPermission(anyString())).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.error.no-permission");
    }

    @Test
    public void testOnLevelPresentNotHighEnough() {
        when(player.hasPermission(anyString())).thenReturn(true);
        when(addon.getLevel(any(), any())).thenReturn(1L);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.error.not-enough-level");
    }

    @Test
    public void testOnNoIsland() {
        when(im.userIsOnIsland(any(World.class), any(User.class))).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.error.not-on-island");
        assertEquals(ChatColor.RED + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignAlreadyUniqueSpot() {
        when(wsm.getWarpLocation(any(), any())).thenReturn(null);
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignDeactivateOldSign() {
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        checkSpigotMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
        verify(signSide).setLine(0, ChatColor.RED + "[WELCOME]");
    }
}
