package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.screens.Screen;
import piboh.cleanmc.mod.update.ModUpdaterService;

/** Mod Updates screen for the 26.x input API; rows are widgets, so clicks are theirs. */
public final class ModUpdatesScreen extends AbstractUpdatesScreen {

    public ModUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(parent, service);
    }
}
