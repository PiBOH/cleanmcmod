package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import piboh.cleanmc.mod.update.ModUpdaterService;

/** Mod Updates screen for the 1.21.9+ input API; rows are widgets, so clicks are theirs. */
public final class ModUpdatesScreen extends AbstractUpdatesScreen {

    public ModUpdatesScreen(Screen parent, ModUpdaterService service) {
        super(parent, service);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }
}
