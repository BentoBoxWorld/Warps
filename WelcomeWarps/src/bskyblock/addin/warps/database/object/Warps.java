package bskyblock.addin.warps.database.object;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.database.objects.DataObject;

public class Warps extends DataObject {
    
    private String uniqueId = "warps";
    private Map<UUID, Location> warpSigns; 

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Map<UUID, Location> getWarpSigns() {
        return warpSigns;
    }

    public void setWarpSigns(Map<UUID, Location> warpSigns) {
        this.warpSigns = warpSigns;
    }

    public Warps save(Map<UUID, Location> warpList) {
        this.warpSigns = warpList;
        return this;
    }

}
