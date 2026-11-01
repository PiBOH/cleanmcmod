package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The dimming behind our screens.
 *
 * <p>Minecraft's menu background blurs the whole frame with a multi-pass shader on every frame.
 * These screens only need the world dimmed to read text on it, so they draw a translucent gradient
 * and skip the blur and the black-out entirely.</p>
 */
final class ScreenBackdrop {

    private ScreenBackdrop() {
    }

    /** Light enough that the world still shows through. */
    static void dim(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, 0x30101010, 0x50101010);
    }
}
