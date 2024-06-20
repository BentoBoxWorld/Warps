package world.bentobox.warps.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

@Table(name = "WarpsData")
public class WarpsData implements DataObject {

    @Expose
    private String uniqueId = "warps";
    @Expose
    private Map<PlayerWarp, UUID> warpSigns = new HashMap<>();

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
        if (warpSigns == null)
            return new HashMap<>();
        return warpSigns;
    }

    public void setWarpSigns(Map<PlayerWarp, UUID> warpSigns) {
        this.warpSigns = warpSigns;
    }

    /**
     * Puts all the data from the map into these objects ready for saving
     * @param worldsWarpList 2D map of warp locations by world vs UUID
     * @return this class filled with data
     */
    public WarpsData save(Map<World, Map<UUID, PlayerWarp>> worldsWarpList) {
        getWarpSigns().clear();
        worldsWarpList.values().forEach(world -> world.forEach((uuid,playerWarp) -> warpSigns.put(playerWarp, uuid)));
        return this;
    }

}
