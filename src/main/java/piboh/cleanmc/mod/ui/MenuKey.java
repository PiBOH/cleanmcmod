package piboh.cleanmc.mod.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** Builds the F7 key binding for the 26.x key-mapping API. */
public final class MenuKey {

    private MenuKey() {
    }

    public static KeyMapping create() {
        // The keyboard type enum was renamed between 26.2 and 26.3 (KEYSYM ->
        // KEYBOARD), so this takes the shortcut constructor that implies it.
        // The category is a KeyMapping.Category record; MISC reuses a vanilla label
        // instead of inventing a translation key this build has no entry for.
        return new KeyMapping("key.cleanmcmod.open", InputConstants.KEY_F7, KeyMapping.Category.MISC);
    }
}
