package piboh.cleanmc.mod.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * The darkening behind our screens.
 *
 * <p>Minecraft's own menu background blurs the whole frame with a multi-pass shader on every frame.
 * These screens only need the world dimmed to read text on it, so they draw the same gradient the
 * game draws after the blur and skip the blur pass entirely: that pass is what made the settings
 * drop frames while the world kept rendering behind them.</p>
 */
final class ScreenBackdrop {

    private ScreenBackdrop() {
    }

    /** A translucent dim: enough to read text, light enough to keep seeing the world. */
    static void dim(DrawContext context, int width, int height) {
        context.fillGradient(0, 0, width, height, 0x30101010, 0x50101010);
    }
}
