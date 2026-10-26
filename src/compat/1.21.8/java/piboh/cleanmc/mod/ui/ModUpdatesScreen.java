package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.screen.Screen;
import piboh.cleanmc.mod.update.ModUpdaterService;

/** Mod Updates screen for the 1.21.x input API. */
public final class ModUpdatesScreen extends AbstractUpdatesScreen {

    public ModUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(parent, service);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickRow(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
