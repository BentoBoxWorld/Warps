package world.bentobox.warps;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.warps.config.Settings;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
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

        Settings settings = mock(Settings.class);
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
        SignCache sc = mock(SignCache.class);
        when(sc.getSignText()).thenReturn(Collections.singletonList("[welcome]"));
        when(sc.getType()).thenReturn(sign_type);
        when(wsm.getSignInfo(any(), any())).thenReturn(sc);
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelFirst() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 0);
        verify(player).openInventory(Mockito.eq(top));
        // Just next sign
        verify(top, Mockito.times(53)).setItem(Mockito.anyInt(), Mockito.any(ItemStack.class));
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelMiddle() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 1);
        verify(player).openInventory(Mockito.eq(top));
        // includes previous and next signs
        verify(top, Mockito.times(54)).setItem(Mockito.anyInt(), Mockito.any(ItemStack.class));
    }

    /**
     * Test method for {@link WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelLast() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 3);
        verify(player).openInventory(Mockito.eq(top));
        // Final amount, just previous sign
        verify(top, Mockito.times(46)).setItem(Mockito.anyInt(), Mockito.any(ItemStack.class));
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
