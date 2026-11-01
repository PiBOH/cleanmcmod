package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * A screen that hit tests part of itself and still compiles for every 1.21.x target.
 *
 * <p>The pointer signature changed in 1.21.9, so the override lives in one half per API under
 * {@code src/compat} and the screen implements only {@link #navClick}, which takes plain numbers.</p>
 */
public abstract class NavScreen extends Screen {

    protected NavScreen(Text title) {
        super(title);
    }

    /** Handles a left click before the widgets see it; true when the click was ours. */
    protected abstract boolean navClick(double mouseX, double mouseY, int button);

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return navClick(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }
}
