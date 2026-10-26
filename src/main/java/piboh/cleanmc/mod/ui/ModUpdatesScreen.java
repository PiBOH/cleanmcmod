package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import piboh.cleanmc.mod.update.ModUpdaterService;

/** Mod Updates screen for the 26.x input API, where clicks arrive as a {@link MouseButtonEvent}. */
public final class ModUpdatesScreen extends AbstractUpdatesScreen {

    public ModUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(parent, service);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (clickRow(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }
}
