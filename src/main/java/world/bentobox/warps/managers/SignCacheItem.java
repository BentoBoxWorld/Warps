package world.bentobox.warps.managers;

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
     * @param signText sign text
     * @param type material of sign
     */
    public SignCacheItem(List<String> signText, Material type) {
        this.signText = signText;
        this.type = type;
    }

    /**
     * This sign is not real
     */
    public SignCacheItem() {
        this.signText = null;
        this.type = null;
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
    /**
     * @return the isReal
     */
    public boolean isReal() {
        return getType() != null;
    }


}