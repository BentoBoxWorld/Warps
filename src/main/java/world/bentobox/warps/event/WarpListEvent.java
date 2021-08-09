package world.bentobox.warps.event;

import java.util.List;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import world.bentobox.warps.Warp;

/**
 * This event is fired when request is made for a sorted list of warps or when
 * the API updateWarpPanel method is called.
 * A listener to this event can reorder or rewrite the warp list by using setWarps.
 * This new order will then be used in the warp panel.
 *
 * @author tastybento
 * @deprecated this event is not fired any more because the task is async
 */
@Deprecated
public class WarpListEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private List<UUID> warps;

    /**
     * @param plugin - BSkyBlock plugin objects
     * @param warps list of warp UUIDs
     */
    public WarpListEvent(Warp plugin, List<UUID> warps) {
        this.warps = warps;
    }


    /**
     *The warp list is a collection of player UUID's and the default order is
     * that players with the most recent login will be first.
     * @return the warps
     */
    public List<UUID> getWarps() {
        return warps;
    }

    /**
     * @param warps the warps to set
     */
    public void setWarps(List<UUID> warps) {
        this.warps = warps;
    }


    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
