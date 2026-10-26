package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import piboh.cleanmc.mod.update.ModUpdaterService;

/** Mod Updates screen for the 1.21.9+ input API, where clicks arrive as a {@link Click}. */
public final class ModUpdatesScreen extends AbstractUpdatesScreen {

    public ModUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(parent, service);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (clickRow(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }
}
