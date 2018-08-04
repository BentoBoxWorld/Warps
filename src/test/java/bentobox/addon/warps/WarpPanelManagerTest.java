/**
 * 
 */
package bentobox.addon.warps;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import bentobox.addon.warps.Warp;
import bentobox.addon.warps.WarpPanelManager;
import bentobox.addon.warps.WarpSignsManager;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author ben
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
public class WarpPanelManagerTest {

    private WarpSignsManager wsm;
    private Warp addon;
    private Player player;
    private User user;
    private World world;
    private Inventory top;
    private UUID uuid;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        addon = mock(Warp.class);
        wsm = mock(WarpSignsManager.class);
        when(addon.getWarpSignsManager()).thenReturn(wsm);
        // Fill with 200 fake warps (I'm banking on them all being different, but there could be a clash)
        List<UUID> list = new ArrayList<>();
        for (int i = 0; i< 200; i++) {
            list.add(UUID.randomUUID());
        }
        // One final one
        uuid = UUID.randomUUID();
        list.add(uuid);
        
        when(wsm.getSortedWarps(Mockito.any())).thenReturn(list);

        user = mock(User.class);
        player = mock(Player.class);
        when(user.getPlayer()).thenReturn(player);
        when(user.getTranslation(Mockito.anyVararg())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgumentAt(0, String.class);
            }});
        
        // World
        world = mock(World.class);
        
        // BentoBox
        BentoBox plugin = mock(BentoBox.class);
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(Mockito.any())).thenReturn("name");
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getPlugin()).thenReturn(plugin);
        
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta imeta = mock(ItemMeta.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(imeta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

        top = mock(Inventory.class);
        when(top.getSize()).thenReturn(9);

        when(Bukkit.createInventory(Mockito.any(), Mockito.anyInt(), Mockito.any())).thenReturn(top);    }

    /**
     * Test method for {@link bentobox.addon.warps.WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelFirst() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 0);
        Mockito.verify(player).openInventory(Mockito.eq(top));
        // Just next sign
        Mockito.verify(top, Mockito.times(53)).setItem(Mockito.anyInt(), Mockito.any(ItemStack.class));        
    }
    
    /**
     * Test method for {@link bentobox.addon.warps.WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelMiddle() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 1);
        Mockito.verify(player).openInventory(Mockito.eq(top));
        // includes previous and next signs
        Mockito.verify(top, Mockito.times(54)).setItem(Mockito.anyInt(), Mockito.any(ItemStack.class));        
    }

    /**
     * Test method for {@link bentobox.addon.warps.WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelLast() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 3);
        Mockito.verify(player).openInventory(Mockito.eq(top));
        // Final amount, just previous sign
        Mockito.verify(top, Mockito.times(46)).setItem(Mockito.anyInt(), Mockito.any(ItemStack.class));        
    }

    /**
     * Test method for {@link bentobox.addon.warps.WarpPanelManager#showWarpPanel(org.bukkit.World, world.bentobox.bbox.api.user.User, int)}.
     */
    @Test
    public void testShowWarpPanelTestCache() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        // Do 45 initial lookups of sign text
        wpm.showWarpPanel(world, user, 3);
        // Get the panel again
        wpm.showWarpPanel(world, user, 3);
        // Should only check this 45 times because the sign text  is cached
        Mockito.verify(wsm, Mockito.times(45)).getSignText(Mockito.any(), Mockito.any());        
    }
    
    
    /**
     * Test method for {@link bentobox.addon.warps.WarpPanelManager#removeWarp(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemoveWarp() {
        WarpPanelManager wpm = new WarpPanelManager(addon);
        wpm.showWarpPanel(world, user, 3);
        wpm.showWarpPanel(world, user, 3);
        wpm.removeWarp(world, uuid);
        wpm.showWarpPanel(world, user, 3);
        // Removing the UUID should force a refresh and therefore 46 lookups
        Mockito.verify(wsm, Mockito.times(46)).getSignText(Mockito.any(), Mockito.any());
        
    }

}
