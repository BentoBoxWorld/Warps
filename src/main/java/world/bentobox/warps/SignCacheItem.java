package world.bentobox.warps;

import java.util.List;

import org.bukkit.Material;

import com.google.gson.annotations.Expose;

/**
 * Stores info on a warp sign
 * @author tastybento
 *
 */
public class SignCacheItem {
    @Expose
    private final List<String> signText;
    @Expose
    private final Material type;
    /**
     * @param signText
     * @param type
     */
    public SignCacheItem(List<String> signText, Material type) {
        this.signText = signText;
        this.type = type;
    }
    /**
     * @return the signText
     */
    public List<String> getSignText() {
        return signText;
    }
    /**
     * @return the type
     */
    public Material getType() {
        return type;
    }

}