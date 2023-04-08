package world.bentobox.warps;


import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


public class WarpsPladdon extends Pladdon {

    @Override
    public Addon getAddon() {
        return new Warp();
    }
}
