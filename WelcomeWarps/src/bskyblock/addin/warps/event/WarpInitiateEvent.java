package bskyblock.addin.warps.event;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import bskyblock.addin.warps.Warp;

/**
 * This event is fired when a player tries to do a warp 
 * A Listener to this event can use it to get informations. e.g: broadcast something
 * 
 * @author tastybento
 *
 */
public class WarpInitiateEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	private Location warpLoc;
	private final UUID player;
	
	/**
	 * @param plugin
	 * @param warpLoc - where the player is warping to
	 * @param player - the UUID of the player
	 */
	public WarpInitiateEvent(Warp plugin, Location warpLoc, UUID player){
		this.warpLoc = warpLoc;
		this.player = player;
	}
	
	/**
	 * Get the location of the Warp
	 * @return created warp's location
	 */
	public Location getWarpLoc(){return this.warpLoc;}
	
    /**
     * Set a different location to where the player will go
     * @param warpLoc
     */
    public void setWarpLoc(Location warpLoc) {
        this.warpLoc = warpLoc;
    }

    /**
	 * Get who is warping
	 * @return the warping player's uuid
	 */
	public UUID getPlayer(){return this.player;}
	
	@Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        
    }
}
