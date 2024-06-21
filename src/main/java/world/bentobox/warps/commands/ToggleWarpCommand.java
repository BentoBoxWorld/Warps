package world.bentobox.warps.commands;

import org.bukkit.World;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.warps.Warp;
import world.bentobox.warps.objects.PlayerWarp;

import java.util.List;
import java.util.UUID;

public class ToggleWarpCommand extends CompositeCommand {

    private final Warp addon;

    public ToggleWarpCommand(Warp addon, CompositeCommand bsbIslandCmd) {
        super(bsbIslandCmd, addon.getSettings().getToggleWarpCommand());
        this.addon = addon;
    }

    public ToggleWarpCommand(Warp addon) {
        super(addon.getSettings().getToggleWarpCommand());
        this.addon = addon;
    }


    @Override
    public void setup() {
        this.setPermission(this.getParent() == null ? Warp.WELCOME_WARP_SIGNS + ".togglewarp" : "island.warp.toggle");
        this.setOnlyPlayer(true);
        this.setDescription("togglewarp.help.description");
    }

    @Override
    public boolean execute(User user, String s, List<String> list) {
        UUID userUUID = user.getUniqueId();
        World userWorld = user.getWorld();

        // Check if the user has a warp
        boolean hasWarp = addon.getWarpSignsManager().hasWarp(userWorld, userUUID);

        if (hasWarp) {
            // If the user has a warp, toggle its visibility
            PlayerWarp warp = addon.getWarpSignsManager().getPlayerWarp(userWorld, userUUID);
            // Check extreme case if PlayerWarp is null
            if (warp == null) {
                user.sendMessage("togglewarp.error.generic");
                return false;
            }
            warp.toggle();
            String message = warp.isEnabled() ? "togglewarp.enabled" : "togglewarp.disabled";
            user.sendMessage(message);
        } else {
            user.sendMessage("togglewarp.error.no-warp");
        }
        return false;
    }
}
