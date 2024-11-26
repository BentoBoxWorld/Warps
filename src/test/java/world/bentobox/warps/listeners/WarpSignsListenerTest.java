package world.bentobox.warps.listeners;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player.Spigot;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import net.md_5.bungee.api.chat.TextComponent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.flags.FlagProtectionChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.warps.Warp;
import world.bentobox.warps.config.Settings;
import world.bentobox.warps.managers.WarpSignsManager;
import world.bentobox.warps.objects.PlayerWarp;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, Util.class, NamespacedKey.class, Tag.class})
public class WarpSignsListenerTest {

    @Mock
    private Warp addon;
    @Mock
    private Block block;
    @Mock
    private Player player;
    @Mock
    private World world;
    private Sign s;
    @Mock
    private WarpSignsManager wsm;
    private PluginManager pm;
    private String[] lines;
    @Mock
    private FileConfiguration config;
    @Mock
    private Settings settings;
    @Mock
    private IslandsManager im;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Island island;
    @Mock
    private Spigot spigot;

    @Before
    public void setUp() {
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        pm = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pm);

        Server server = mock(Server.class);
        when(server.getVersion()).thenReturn("1.14");
        when(Bukkit.getServer()).thenReturn(server);
        Bukkit.setServer(server);

        PowerMockito.mockStatic(NamespacedKey.class);
        NamespacedKey keyValue = mock(NamespacedKey.class);
        when(NamespacedKey.minecraft(anyString())).thenReturn(keyValue);

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
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.spigot()).thenReturn(spigot);
        s = mock(Sign.class);
        when(s.getLine(anyInt())).thenReturn(ChatColor.GREEN + "[WELCOME]");
        when(block.getState()).thenReturn(s);
        // warp signs manager
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        Map<UUID, PlayerWarp> list = new HashMap<>();
        Location location = mock(Location.class);
        when(location.getBlock()).thenReturn(block);
        when(s.getLocation()).thenReturn(location);
        when(block.getLocation()).thenReturn(location);
        list.put(uuid, new PlayerWarp(location, true));
        // Player is in world
        when(wsm.getWarpMap(world)).thenReturn(list);
        //Player has a warp sign already here
        when(wsm.getWarpLocation(any(), any())).thenReturn(location);
        // Unique spot
        when(wsm.addWarp(any(), any())).thenReturn(true);
        // Bentobox
        BentoBox plugin = mock(BentoBox.class);
        when(addon.getPlugin()).thenReturn(plugin);
        User.setPlugin(plugin);
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn(null);
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Lines
        lines = new String[] {"[WELCOME]", "line2", "line3", "line4"};

        when(settings.getWarpLevelRestriction()).thenReturn(10);
        when(settings.getWelcomeLine()).thenReturn("[WELCOME]");
        when(addon.getSettings()).thenReturn(settings);

        island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // On island
        when(plugin.getIslands()).thenReturn(im);
        when(im.userIsOnIsland(any(World.class), any(User.class))).thenReturn(true);

        // Sufficient level
        when(addon.getLevel(any(), any())).thenReturn(100L);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        Answer<String> answer = invocation -> invocation.getArgument(1, String.class);

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);
        when(Util.stripSpaceAfterColorCodes(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        when(Util.translateColorCodes(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Locales
        when(lm.get(any(User.class), anyString())).thenAnswer(answer);
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

    }

    /**
     * Check that spigot sent the message
     * @param message - message to check
     */
    public void checkSpigotMessage(String expectedMessage) {
        checkSpigotMessage(expectedMessage, 1);
    }

    public void checkSpigotMessage(String expectedMessage, int expectedOccurrences) {
        // Capture the argument passed to spigot().sendMessage(...) if messages are sent
        ArgumentCaptor<TextComponent> captor = ArgumentCaptor.forClass(TextComponent.class);

        // Verify that sendMessage() was called at least 0 times (capture any sent messages)
        verify(spigot, atLeast(0)).sendMessage(captor.capture());

        // Get all captured TextComponents
        List<TextComponent> capturedMessages = captor.getAllValues();

        // Count the number of occurrences of the expectedMessage in the captured messages
        long actualOccurrences = capturedMessages.stream().map(component -> component.toLegacyText()) // Convert each TextComponent to plain text
                .filter(messageText -> messageText.contains(expectedMessage)) // Check if the message contains the expected text
                .count(); // Count how many times the expected message appears

        // Assert that the number of occurrences matches the expectedOccurrences
        assertEquals("Expected message occurrence mismatch: " + expectedMessage, expectedOccurrences,
                actualOccurrences);
    }

    public void checkNoSpigotMessages() {
        try {
            // Verify that sendMessage was never called
            verify(spigot, never()).sendMessage(any(TextComponent.class));
        } catch (AssertionError e) {
            fail("Expected no messages to be sent, but some messages were sent.");
        }
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
        when(s.getLine(Mockito.anyInt())).thenReturn(ChatColor.RED + "[WELCOME]");
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        verify(s).getLine(0);
        verify(settings).getWelcomeLine();

    }

    @Test
    public void testOnSignNotRealWelcomeSign() {
        // Right text, but not in the right position
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
        // Success!
        assertFalse(e.isCancelled());
        verify(pm).callEvent(any());
    }

    @Test
    public void testOnSignRemovePlayerSignPlayerHasPerm() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(player.hasPermission(anyString())).thenReturn(true);
        wsl.onSignBreak(e);
        // Success!
        assertFalse(e.isCancelled());
        verify(pm).callEvent(any());
    }

    @Test
    public void testOnSignRemoveCorrectPlayer() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        wsl.onSignBreak(e);
        // Success!
        assertFalse(e.isCancelled());
        verify(pm).callEvent(any());
    }



    /**
     * Sign create
     */
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
        verify(settings).getWelcomeLine();
        checkNoSpigotMessages();
    }

    @Test
    public void testOnCreateNoPerm() {
        when(player.hasPermission(anyString())).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        this.checkSpigotMessage("warps.error.no-permission");
    }

    @Test
    public void testOnLevelPresentNotHighEnough() {
        when(player.hasPermission(anyString())).thenReturn(true);
        when(addon.getLevel(any(), any())).thenReturn(1L);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        this.checkSpigotMessage("warps.error.not-enough-level");
    }

    @Test
    public void testOnNoIsland() {
        when(im.userIsOnIsland(any(World.class), any(User.class))).thenReturn(false);
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        this.checkSpigotMessage("warps.error.not-on-island");
        assertEquals(ChatColor.RED + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignAlreadyUniqueSpot() {
        when(wsm.getWarpLocation(any(), any())).thenReturn(null);
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        this.checkSpigotMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignDeactivateOldSign() {
        when(player.hasPermission(anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        this.checkSpigotMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
        verify(s).setLine(0, ChatColor.RED + "[WELCOME]");
    }


}
