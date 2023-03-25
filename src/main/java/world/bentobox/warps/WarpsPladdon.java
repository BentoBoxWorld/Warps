package world.bentobox.warps;

import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Plugin;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;

@Plugin(name="Warps", version="1.0")
@ApiVersion(ApiVersion.Target.v1_16)
@Dependency(value = "BentoBox")
public class WarpsPladdon extends Pladdon {

    @Override
    public Addon getAddon() {
        return new Warp();
    }
}
