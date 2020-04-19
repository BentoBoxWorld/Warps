package world.bentobox.warps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.warps.config.Settings;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, DatabaseSetup.class})
public class WarpPanelManagerTest {

    @Mock
    private WarpSignsManager wsm;
    @Mock
    private Warp addon;
    @Mock
    private Player player;
    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private Inventory top;
    private UUID uuid;
    @Mock
    private Settings settings;
    @Mock
    private static AbstractDatabaseHandler<Object> handler;
    
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        // Fill with 200 fake warps (I'm banking on them all being different, but there could be a clash)
        List<UUID> list = new ArrayList<>();
        for (int i = 0; i< 200; i++) {
            list.add(UUID.randomUUID());
        }
        // One final one
        uuid = UUID.randomUUID();
        list.add(uuid);

        when(wsm.getSortedWarps(any())).thenReturn(list);

        // User and player
        when(user.getPlayer()).thenReturn(player);
        when(user.getTranslation(Mockito.any())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0, String.class);
            }});


        // BentoBox
        BentoBox plugin = mock(BentoBox.class);
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(any())).thenReturn("name");
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getPlugin()).thenReturn(plugin);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta imeta = mock(ItemMeta.class);
        when(itemF.getItemMeta(any())).thenReturn(imeta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

        // Inventory
        when(top.getSize()).thenReturn(9);

        when(Bukkit.createInventory(any(), Mockito.anyInt(), any())).thenReturn(top);

        when(settings.getIcon()).thenReturn("SIGN");
        when(addon.getSettings()).thenReturn(settings);

        Location location = mock(Location.class);
        Block block = mock(Block.class);
        Material sign_type;
        try {
            sign_type = Material.valueOf("SIGN");
        } catch (Exception e) {
            sign_type = Material.valueOf("OAK_SIGN");
        }
        when(block.getType()).thenReturn(sign_type);
        when(location.getBlock()).thenReturn(block);
        // Sign block
        when(wsm.getWarp(any(), any())).thenReturn(location);

        // Sign cache
        SignCacheItem sc = mock(SignCacheItem.class);
        when(sc.getSignText()).thenReturn(Collections.singletonList("[welcome]"));
        when(sc.getType()).thenReturn(sign_type);
        when(wsm.getSignInfo(any(), any())).thenReturn(sc);
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelFirst() {
        ArgumentCaptor<ItemStack> argument = ArgumentCaptor.forClass(ItemStack.class);
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 0);
        verify(player).openInventory(Mockito.eq(top));
        // Just next sign
        verify(top, Mockito.times(53)).setItem(Mockito.anyInt(),argument.capture());
        assertEquals(Material.STONE, argument.getAllValues().get(52).getType());
    }
    
    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelFirstRandom() {
        when(settings.isRandomAllowed()).thenReturn(true);
        ArgumentCaptor<ItemStack> argument = ArgumentCaptor.forClass(ItemStack.class);
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 0);
        verify(player).openInventory(Mockito.eq(top));
        // Check crystal
        verify(top, Mockito.atLeastOnce()).setItem(anyInt(), argument.capture());
        assertEquals(Material.END_CRYSTAL, argument.getAllValues().get(0).getType());
    }
    
    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelNoRandom() {
        when(settings.isRandomAllowed()).thenReturn(false);
        ArgumentCaptor<ItemStack> argument = ArgumentCaptor.forClass(ItemStack.class);
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 0);
        verify(player).openInventory(Mockito.eq(top));
        // Check crystal
        verify(top, Mockito.atLeastOnce()).setItem(anyInt(), argument.capture());
        assertFalse(argument.getAllValues().get(0).getType().equals(Material.END_CRYSTAL));
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelMiddle() {
        ArgumentCaptor<ItemStack> argument = ArgumentCaptor.forClass(ItemStack.class);
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 1);
        verify(player).openInventory(Mockito.eq(top));
        // includes previous and next signs
        verify(top, Mockito.times(54)).setItem(Mockito.anyInt(), argument.capture());
        assertEquals(Material.STONE, argument.getAllValues().get(52).getType());
        assertEquals(Material.COBBLESTONE, argument.getAllValues().get(53).getType());
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelLast() {
        ArgumentCaptor<ItemStack> argument = ArgumentCaptor.forClass(ItemStack.class);
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 3);
        verify(player).openInventory(Mockito.eq(top));
        // Final amount, just previous sign
        verify(top, Mockito.times(46)).setItem(Mockito.anyInt(), argument.capture());
        assertEquals(Material.COBBLESTONE, argument.getAllValues().get(45).getType());
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelTestCache() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        // Do 45 initial lookups of sign text
        wpm.showWarpPanel(world, user, 3);
        // Get the panel again
        wpm.showWarpPanel(world, user, 3);
        // Should only check this 45 times because the sign text  is cached
        verify(wsm, Mockito.times(45)).getSignInfo(any(), any());
    }


    /**
     * Test method for {@link WarpPanelManager#removeWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemoveWarp() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 3);
        wpm.showWarpPanel(world, user, 3);
        wpm.removeWarp(world, uuid);
        wpm.showWarpPanel(world, user, 3);
        // Removing the UUID should force a refresh and therefore 46 lookups
        verify(wsm, Mockito.times(46)).getSignInfo(any(), any());

    }

}
