package world.bentobox.warps.event;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This event is fired when a Warp is removed (when a warp sign is broken)
 * A Listener to this event can use it only to get informations. e.g: broadcast something
 * 
 * @author Poslovitch
 *
 */
public class WarpRemoveEvent extends Event{
	private static final HandlerList handlers = new HandlerList();
	
	private final Location warpLoc;
	private final UUID remover;
	private final UUID owner;
	
	/**
	 * @param warpLoc - Warp location
	 * @param remover - UUID of remover
	 * @param owner - UUID of warp owner - rarely, may be null
	 */
	public WarpRemoveEvent(@NonNull Location warpLoc, UUID remover, @Nullable UUID owner){
		this.warpLoc = warpLoc;
		this.remover = remover;
		this.owner = owner;
	}
	
	/**
	 * Get the location of the removed Warp
	 * @return removed warp's location
	 */
	@NonNull
	public Location getWarpLocation(){
	    return this.warpLoc;
	    }
	
	/**
	 * Get who has removed the warp
	 * @return the warp's remover
	 */
	@NonNull
	public UUID getRemover(){
	    return this.remover;
	    }
	
    /**
     * @return the owner
     */
	@Nullable
    protected UUID getOwner() {
        return owner;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}