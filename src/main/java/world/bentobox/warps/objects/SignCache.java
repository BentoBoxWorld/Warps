package world.bentobox.warps.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.warps.SignCacheItem;

public class SignCache implements DataObject {

    @Expose
    private String uniqueId = "";
    @Expose
    private Map<UUID, SignCacheItem> signs = new HashMap<>();

    public SignCache() {
        // Required by YAML database
    }

    public SignCache(World w, Map<UUID, SignCacheItem> m) {
        this.uniqueId = w.getName();
        this.signs = m;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the signs
     */
    public Map<UUID, SignCacheItem> getSigns() {
        return signs;
    }

    /**
     * @param signs the signs to set
     */
    public void setSigns(Map<UUID, SignCacheItem> signs) {
        this.signs = signs;
    }

}
