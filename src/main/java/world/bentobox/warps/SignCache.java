package world.bentobox.warps;

import java.util.List;

import org.bukkit.Material;

/**
 * Stores info on a warp sign
 * @author tastybento
 *
 */
public class SignCache {
    private final List<String> signText;
    private final Material type;
    /**
     * @param signText
     * @param type
     */
    public SignCache(List<String> signText, Material type) {
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