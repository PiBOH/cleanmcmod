package piboh.cleanmc.mod.ui;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Builds the F7 menu key and the F8 update key for the 1.21.x key-binding API. */
public final class MenuKey {

    private MenuKey() {
    }

    public static KeyBinding create() {
        return new KeyBinding("key.cleanmcmod.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7,
                "key.categories.cleanmcmod");
    }

    public static KeyBinding createUpdates() {
        return new KeyBinding("key.cleanmcmod.updates", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8,
                "key.categories.cleanmcmod");
    }
}
