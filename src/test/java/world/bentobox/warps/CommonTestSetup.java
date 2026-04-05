package world.bentobox.warps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player.Spigot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import net.md_5.bungee.api.chat.TextComponent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.MapManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Common items for testing. Don't forget to use super.setUp()!
 */
public abstract class CommonTestSetup {

    protected UUID uuid = UUID.randomUUID();

    @Mock
    protected Player mockPlayer;
    @Mock
    protected PluginManager pim;
    @Mock
    protected ItemFactory itemFactory;
    @Mock
    protected Location location;
    @Mock
    protected World world;
    @Mock
    protected IslandWorldManager iwm;
    @Mock
    protected IslandsManager im;
    @Mock
    protected Island island;
    @Mock
    protected BentoBox plugin;
    @Mock
    protected Spigot spigot;
    @Mock
    protected HooksManager hooksManager;
    @Mock
    protected PlayersManager pm;
    @Mock
    protected MapManager mapManager;

    protected ServerMock server;

    protected MockedStatic<Bukkit> mockedBukkit;
    protected MockedStatic<Util> mockedUtil;

    protected AutoCloseable closeable;

    @Mock
    protected BukkitScheduler sch;
    @Mock
    protected LocalesManager lm;
    @Mock
    protected PlaceholdersManager phm;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        server = MockBukkit.mock();

        // Set up plugin
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        // Bukkit static mock
        mockedBukkit = Mockito.mockStatic(Bukkit.class, Mockito.RETURNS_DEEP_STUBS);
        mockedBukkit.when(Bukkit::getMinecraftVersion).thenReturn("1.21.10");
        mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn("");
        mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pim);
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);
        mockedBukkit.when(Bukkit::getServer).thenReturn(server);
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(sch);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(location.clone()).thenReturn(location);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("tastybento");

        // Player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getLocation()).thenReturn(location);
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getName()).thenReturn("tastybento");
        when(mockPlayer.spigot()).thenReturn(spigot);
        when(mockPlayer.isOnline()).thenReturn(true);
        when(mockPlayer.canSee(any(Player.class))).thenReturn(true);

        User.setPlugin(plugin);
        User.clearUsers();
        User.getInstance(mockPlayer);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));
        when(im.isSafeLocation(any())).thenReturn(true);
        when(island.isAllowed(any())).thenReturn(false);
        when(island.getOwner()).thenReturn(uuid);

        // Locales & Placeholders
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(lm.getAvailablePrefixes(any())).thenReturn(Collections.emptySet());
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Util
        mockedUtil = Mockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        mockedUtil.when(() -> Util.getWorld(any())).thenAnswer((Answer<World>) invocation -> invocation.getArgument(0, World.class));
        mockedUtil.when(() -> Util.sameWorld(any(), any())).thenReturn(true);
        mockedUtil.when(() -> Util.translateColorCodes(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        mockedUtil.when(() -> Util.stripSpaceAfterColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        Util.setPlugin(plugin);

        // Hooks
        when(hooksManager.getHook(anyString())).thenReturn(Optional.empty());
        when(plugin.getHooks()).thenReturn(hooksManager);

        // Map Manager
        when(plugin.getMapManager()).thenReturn(mapManager);

        // BentoBox settings
        world.bentobox.bentobox.Settings bentoSettings = new world.bentobox.bentobox.Settings();
        when(plugin.getSettings()).thenReturn(bentoSettings);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockedBukkit.closeOnDemand();
        mockedUtil.closeOnDemand();
        closeable.close();
        MockBukkit.unmock();
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }

    protected static void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    /**
     * Check that spigot sent the message
     * @param expectedMessage - message to check
     */
    public void checkSpigotMessage(String expectedMessage) {
        checkSpigotMessage(expectedMessage, 1);
    }

    @SuppressWarnings("deprecation")
    public void checkSpigotMessage(String expectedMessage, int expectedOccurrences) {
        ArgumentCaptor<TextComponent> captor = ArgumentCaptor.forClass(TextComponent.class);
        verify(spigot, atLeast(0)).sendMessage(captor.capture());
        List<TextComponent> capturedMessages = captor.getAllValues();
        long actualOccurrences = capturedMessages.stream()
                .map(component -> component.toLegacyText())
                .filter(messageText -> messageText.contains(expectedMessage))
                .count();
        assertEquals(expectedOccurrences, actualOccurrences,
                "Expected message occurrence mismatch: " + expectedMessage);
    }
}
