package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The darkening behind our screens.
 *
 * <p>Minecraft's menu background blurs the whole frame with a multi-pass shader on every frame.
 * These screens only need the world dimmed to read text on it, so they draw the same gradient the
 * game draws after the blur and skip the blur pass entirely.</p>
 */
final class ScreenBackdrop {

    private ScreenBackdrop() {
    }

    /** The two ends of the gradient the game itself uses. */
    static void dim(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, 0xC0101010, 0xD0101010);
    }
}
