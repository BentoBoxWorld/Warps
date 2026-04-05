package world.bentobox.warps.objects;

import com.google.gson.annotations.Expose;
import org.bukkit.Location;

import java.io.Serializable;

public class PlayerWarp implements Serializable {

    @Expose
    private final Location location;

    @Expose
    private boolean isEnabled;

    public PlayerWarp(Location location, boolean isEnabled) {
        this.location = location;
        this.isEnabled = isEnabled;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void toggle() {
        isEnabled = !isEnabled;
    }
}
