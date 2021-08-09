package world.bentobox.warps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
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
    @Mock
    private Settings settings;
    @Mock
    private static AbstractDatabaseHandler<Object> handler;
    @Mock
    private BukkitScheduler scheduler;
    private List<UUID> list;

    // Class under test
    private WarpPanelManager wpm;

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
     */
    @Before
    public void setUp() {
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        // Fill with 200 fake warps (I'm banking on them all being different, but there could be a clash)
        list = new ArrayList<>();
        for (int i = 0; i< 200; i++) {
            list.add(UUID.randomUUID());
        }
        // One final one
        UUID uuid = UUID.randomUUID();
        list.add(uuid);

        when(wsm.getSortedWarps(any())).thenReturn(CompletableFuture.completedFuture(list));

        // User and player
        when(user.getPlayer()).thenReturn(player);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));


        // BentoBox
        BentoBox plugin = mock(BentoBox.class);
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(any())).thenReturn("name");
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getPlugin()).thenReturn(plugin);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta imeta = mock(ItemMeta.class);
        when(itemF.getItemMeta(any())).thenReturn(imeta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        // Inventory
        when(top.getSize()).thenReturn(9);

        when(Bukkit.createInventory(any(), anyInt(), any())).thenReturn(top);

        when(settings.getIcon()).thenReturn("SIGN");
        when(addon.getSettings()).thenReturn(settings);

        Location location = mock(Location.class);
        Block block = mock(Block.class);
        Material signType;
        try {
            signType = Material.valueOf("SIGN");
        } catch (Exception e) {
            signType = Material.valueOf("OAK_SIGN");
        }
        when(block.getType()).thenReturn(signType);
        when(location.getBlock()).thenReturn(block);
        // Sign block
        when(wsm.getWarp(any(), any())).thenReturn(location);

        // Sign cache

        SignCacheItem sc = new SignCacheItem(Collections.singletonList("[welcome]"), signType);
        when(wsm.getSignInfo(any(), any())).thenReturn(sc);

        // Class under test
        wpm = new WarpPanelManager(addon);
    }

    /**
     * Test method for {@link WarpPanelManager#processSigns(CompletableFuture, PanelBuilder, User, int, World)}.
     */
    @Test
    public void testShowWarpPanelTestCache() {
        PanelBuilder pb = mock(PanelBuilder.class);
        // Do initial lookups of sign text
        wpm.processSigns(new CompletableFuture<>(), pb, user, 3, world);
        // Get the panel again
        wpm.processSigns(new CompletableFuture<>(), pb, user, 3, world);
        // Should only check this 201 times in total because the sign text  is cached
        verify(wsm, times(201)).getSignInfo(any(), any());
    }


    /**
     * Test method for {@link WarpPanelManager#removeWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemoveWarp() {
        assertFalse(wpm.removeWarp(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link WarpPanelManager#buildPanel(PanelBuilder, User, int, World)}
     */
    @Test
    public void testBuildPanel() {
        PanelBuilder pb = mock(PanelBuilder.class);
        wpm.buildPanel(pb, user, 3, world);
        // Removing the UUID should force a refresh and therefore 201 lookups
        verify(wsm, times(201)).getSignInfo(any(), any());

    }

    /**
     * Test method for {@link WarpPanelManager#addNavigation(PanelBuilder, User, World, int, int, int)}
     */
    @Test
    public void testAddNavigationNoNav() {
        PanelBuilder pb = mock(PanelBuilder.class);
        wpm.addNavigation(pb, user, world, 0, 0, 0);
        verify(pb, never()).item(any());
    }

    /**
     * Test method for {@link WarpPanelManager#addNavigation(PanelBuilder, User, World, int, int, int)}
     */
    @Test
    public void testAddNavigationNoNavNext() {
        PanelBuilder pb = mock(PanelBuilder.class);
        wpm.addNavigation(pb, user, world, 0, 0, 100);
        verify(pb).item(any());
        verify(user).getTranslation("warps.next");
    }

    /**
     * Test method for {@link WarpPanelManager#addNavigation(PanelBuilder, User, World, int, int, int)}
     */
    @Test
    public void testAddNavigationNoNavPrev() {
        PanelBuilder pb = mock(PanelBuilder.class);
        wpm.addNavigation(pb, user, world, 60, 2, 20);
        verify(pb).item(any());
        verify(user).getTranslation("warps.previous");
    }

    /**
     * Test method for {@link WarpPanelManager#addNavigation(PanelBuilder, User, World, int, int, int)}
     */
    @Test
    public void testAddNavigationNoNavNextAndPrev() {
        PanelBuilder pb = mock(PanelBuilder.class);
        wpm.addNavigation(pb, user, world, 60, 2, 100);
        verify(pb, times(2)).item(any());
        verify(user).getTranslation("warps.previous");
        verify(user).getTranslation("warps.next");
    }


    private int mainBod(int page, int j, boolean random) {
        when(settings.isRandomAllowed()).thenReturn(random);
        PanelBuilder pb = mock(PanelBuilder.class);
        int r = wpm.buildMainBody(pb, user, page, world, list);
        verify(pb, times(j)).item(any());
        if (random && page <= 0) {
            verify(user).getTranslation("warps.random");
        } else {
            verify(user, never()).getTranslation("warps.random");
        }
        return r;
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyNoRandomPage0() {
        assertEquals(201, mainBod(0, 201, false));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyNoRandomPage1() {
        assertEquals(201, mainBod(1, 149, false));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyNoRandomPage2() {
        assertEquals(201, mainBod(2, 97, false));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyNoRandomPage3() {
        assertEquals(201, mainBod(3, 45, false));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyNoRandomPageMinus1() {
        assertEquals(201, mainBod(-1, 201, false));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyRandomPage0() {
        assertEquals(201, mainBod(0, 201, true));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyRandomPage1() {
        assertEquals(201, mainBod(1, 149, true));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyRandomPage2() {
        assertEquals(201, mainBod(2, 97, true));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyRandomPage3() {
        assertEquals(201, mainBod(3, 45, true));
    }

    /**
     * Test method for {@link WarpPanelManager#buildMainBody(PanelBuilder, User, int, World, List, boolean)}
     */
    @Test
    public void testBuildMainBodyRandomPageMinus1() {
        assertEquals(201, mainBod(-1, 201, true));
    }
}
