package bentobox.addon.warps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import bentobox.addon.warps.config.PluginConfig;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
public class WarpSignsListenerTest {

    private Warp addon;
    private Block block;
    private Player player;
    private World world;
    private Sign s;
    private WarpSignsManager wsm;
    private PluginManager pm;
    private UUID uuid;
    private String[] lines;
    private FileConfiguration config;
    private PluginConfig settings;
    private IslandsManager im;

    @Before
    public void setUp() throws Exception {
        addon = mock(Warp.class);
        when(addon.inRegisteredWorld(Mockito.any())).thenReturn(true);
        config = mock(FileConfiguration.class);
        when(config.getString(Mockito.anyString())).thenReturn("[WELCOME]");
        when(addon.getConfig()).thenReturn(config);
        block = mock(Block.class);
        when(block.getType()).thenReturn(Material.WALL_SIGN);
        world = mock(World.class);
        when(block.getWorld()).thenReturn(world);
        player = mock(Player.class);
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        s = mock(Sign.class);
        when(s.getLine(Mockito.anyInt())).thenReturn(ChatColor.GREEN + "[WELCOME]");
        when(block.getState()).thenReturn(s);
        wsm = mock(WarpSignsManager.class);
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        Map<UUID, Location> list = new HashMap<>();
        Location location = mock(Location.class);
        when(location.getBlock()).thenReturn(block);
        when(s.getLocation()).thenReturn(location);
        list.put(uuid, location);
        // Player is in world
        when(wsm.getWarpMap(Mockito.eq(world))).thenReturn(list);
        //Player has a warp sign already here
        when(wsm.getWarp(Mockito.any(), Mockito.any())).thenReturn(location);
        // Unique spot
        when(wsm.addWarp(Mockito.any(), Mockito.any())).thenReturn(true);
        // Bentobox
        BentoBox plugin = mock(BentoBox.class);
        when(addon.getPlugin()).thenReturn(plugin);
        User.setPlugin(plugin);
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer(new Answer<String>(){

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgumentAt(1, String.class);
            }});
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        pm = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pm);

        // Lines
        lines = new String[] {"[WELCOME]", "line2", "line3", "line4"};

        settings = mock(PluginConfig.class);
        when(settings.getWarpLevelRestriction()).thenReturn(10);
        when(addon.getSettings()).thenReturn(settings);

        im = mock(IslandsManager.class);
        // On island
        when(plugin.getIslands()).thenReturn(im);
        when(im.userIsOnIsland(Mockito.any(World.class), Mockito.any(User.class))).thenReturn(true);

        // Sufficient level
        when(addon.getLevel(Mockito.any(), Mockito.any())).thenReturn(100L);
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
        Mockito.verify(block, Mockito.times(2)).getType();
    }

    @Test
    public void testOnSignBreakWrongWorld() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(addon.inRegisteredWorld(Mockito.any())).thenReturn(false);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        Mockito.verify(addon).inRegisteredWorld(Mockito.eq(world));
    }

    @Test
    public void testOnSignBreakNullState() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(block.getState()).thenReturn(null);
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        Mockito.verify(block).getState();
    }

    @Test
    public void testOnSignNotWelcomeSign() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(s.getLine(Mockito.anyInt())).thenReturn(ChatColor.RED + "[WELCOME]");
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        Mockito.verify(s).getLine(Mockito.eq(0));
        Mockito.verify(addon).getConfig();
    }

    @Test
    public void testOnSignNotRealWelcomeSign() {
        // Right text, but not in the right position
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(s.getLocation()).thenReturn(mock(Location.class));
        wsl.onSignBreak(e);
        assertFalse(e.isCancelled());
        Mockito.verify(wsm).getWarpMap(Mockito.eq(world));
        Mockito.verify(s).getLocation();
    }

    @Test
    public void testOnSignRemovePlayerSignWrongPlayer() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        wsl.onSignBreak(e);
        assertTrue(e.isCancelled());
        Mockito.verify(player).sendMessage("warps.error.no-remove");
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
        Mockito.verify(pm).callEvent(Mockito.any());
    }

    @Test
    public void testOnSignRemovePlayerSignPlayerHasPerm() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        wsl.onSignBreak(e);
        // Success!
        assertFalse(e.isCancelled());
        Mockito.verify(pm).callEvent(Mockito.any());
    }

    @Test
    public void testOnSignRemoveCorrectPlayer() {
        WarpSignsListener wsl = new WarpSignsListener(addon);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        wsl.onSignBreak(e);
        // Success!
        assertFalse(e.isCancelled());
        Mockito.verify(pm).callEvent(Mockito.any());
    }



    /**
     * Sign create
     */
    @Test
    public void testOnCreateWrongWorld() {
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        when(addon.inRegisteredWorld(Mockito.any())).thenReturn(false);
        wsl.onSignWarpCreate(e);
        Mockito.verify(addon).inRegisteredWorld(Mockito.eq(world));
    }

    @Test
    public void testOnCreateWrongText() {
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        lines = new String[] {"line1", "line2", "line3", "line4"};
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(config).getString(Mockito.anyString());
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.anyString());
    }

    @Test
    public void testOnCreateNoPerm() {
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(player).sendMessage("warps.error.no-permission");
    }

    @Test
    public void testOnLevelPresentNotHighEnough() {
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        when(addon.getLevel(Mockito.any(), Mockito.any())).thenReturn(1L);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(player).sendMessage("warps.error.not-enough-level");
    }

    @Test
    public void testOnNoIsland() {
        when(im.userIsOnIsland(Mockito.any(World.class), Mockito.any(User.class))).thenReturn(false);
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(player).sendMessage("warps.error.not-on-island");
        assertEquals(ChatColor.RED + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignAlreadyUniqueSpot() {
        when(wsm.getWarp(Mockito.any(), Mockito.any())).thenReturn(null);
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(player).sendMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignAlreadyDuplicateSpot() {
        when(wsm.addWarp(Mockito.any(), Mockito.any())).thenReturn(false);
        when(wsm.getWarp(Mockito.any(), Mockito.any())).thenReturn(null);
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(player).sendMessage("warps.error.duplicate");
        assertEquals(ChatColor.RED + "[WELCOME]", e.getLine(0));
    }

    @Test
    public void testCreateNoSignDeactivateOldSign() {
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        WarpSignsListener wsl = new WarpSignsListener(addon);
        SignChangeEvent e = new SignChangeEvent(block, player, lines);
        wsl.onSignWarpCreate(e);
        Mockito.verify(player).sendMessage("warps.success");
        assertEquals(ChatColor.GREEN + "[WELCOME]", e.getLine(0));
        Mockito.verify(s).setLine(0, ChatColor.RED + "[WELCOME]");
    }


}
