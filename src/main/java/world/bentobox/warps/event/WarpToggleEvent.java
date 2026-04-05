package world.bentobox.warps.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import world.bentobox.warps.objects.PlayerWarp;

import java.util.UUID;

/**
 * This event is fired when a warp is toggled
 * A Listener to this event can use it only to get information. e.g: broadcast something
 *
 * @since 1.16.0
 * @author TreemanKing
 */
public class WarpToggleEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID user;
    private final PlayerWarp playerWarp;

    public WarpToggleEvent(UUID user, PlayerWarp playerWarp) {
        this.playerWarp = playerWarp;
        this.user = user;
    }

    /**
     * Gets the user who has toggled the warp
     *
     * @return the UUID of the player who toggled the warp
     */
    public UUID getUser() {
        return user;
    }

    /**
     * Gets the state of the warp
     *
     * @return true if the warp is enabled, false otherwise
     */
    public boolean isEnabled() {
        return playerWarp.isEnabled();
    }

    /**
     * Gets the PlayerWarp object
     *
     * @return the PlayerWarp object
     */
    public PlayerWarp getPlayerWarp() {
        return playerWarp;
    }

    /**
     * Gets the location of the toggled warp
     *
     * @return the location of the warp
     */
    public Location getLocation() {
        return playerWarp.getLocation();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
