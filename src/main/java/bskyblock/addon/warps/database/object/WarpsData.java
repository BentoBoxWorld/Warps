package bskyblock.addon.warps.database.object;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;

public class WarpsData implements DataObject {
    
    @Expose
    private String uniqueId = "warps";
    @Expose
    private Map<Location, UUID> warpSigns = new HashMap<>();
    
    public WarpsData() {}

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Map<Location,  UUID> getWarpSigns() {
    		if (warpSigns == null)
    			return new HashMap<>();
        return warpSigns;
    }

    public void setWarpSigns(Map<Location, UUID> warpSigns) {
        this.warpSigns = warpSigns;
    }

    /**
     * Puts all the data from the map into this object ready for saving
     * @param worldsWarpList
     * @return this class filled with data
     */
    public WarpsData save(Map<World, Map<UUID, Location>> worldsWarpList) {
        worldsWarpList.values().forEach(world -> world.forEach((uuid,location) -> warpSigns.put(location, uuid)));
        return this;
    }

}
