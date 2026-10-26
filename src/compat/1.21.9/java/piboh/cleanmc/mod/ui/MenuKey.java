package piboh.cleanmc.mod.ui;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Builds the F7 key binding for the 1.21.9+ key-binding API. */
public final class MenuKey {

    private MenuKey() {
    }

    public static KeyBinding create() {
        // 1.21.9 replaced the category string with KeyBinding.Category. MISC reuses a
        // vanilla label instead of inventing a translation key this build has no entry for.
        return new KeyBinding("key.cleanmcmod.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7,
                KeyBinding.Category.MISC);
    }
}
