package world.bentobox.warps.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

@Table(name = "WarpsData")
public class WarpsData implements DataObject {

    @Expose
    private String uniqueId = "warps";

    @Deprecated @Expose
    private Map<Location, UUID> warpSigns = new HashMap<>();

    @Expose
    private Map<PlayerWarp, UUID> newWarpSigns = new HashMap<>();

    public WarpsData() {
        // Required by YAML database
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Map<PlayerWarp,  UUID> getWarpSigns() {
        convertOldWarpSigns();
        if (newWarpSigns == null)
            return new HashMap<>();
        return newWarpSigns;
    }

    /**
     * Method for converting old warp signs to new warp signs
     */
    public void convertOldWarpSigns() {
        if (warpSigns == null) {
            return;
        }

        for (Map.Entry<Location, UUID> entry : warpSigns.entrySet()) {
            PlayerWarp playerWarp = new PlayerWarp(entry.getKey(), true);
            newWarpSigns.put(playerWarp, entry.getValue());
        }
    }

    public void setWarpSigns(Map<PlayerWarp, UUID> warpSigns) {
        this.newWarpSigns = warpSigns;
    }

    /**
     * Puts all the data from the map into these objects ready for saving
     * @param worldsWarpList 2D map of warp locations by world vs UUID
     * @return this class filled with data
     */
    public WarpsData save(Map<World, Map<UUID, PlayerWarp>> worldsWarpList) {
        getWarpSigns().clear();
        worldsWarpList.values().forEach(world -> world.forEach((uuid,playerWarp) -> newWarpSigns.put(playerWarp, uuid)));
        return this;
    }

}
