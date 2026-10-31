package piboh.cleanmc.mod.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import piboh.cleanmc.mod.CleanMcModClient;
import piboh.cleanmc.mod.ui.CleanMenuScreen;

/**
 * Optional Mod Menu integration.
 *
 * <p>The button Mod Menu shows for this mod opens the main CleanMCMod screen, not the settings page:
 * that is where "Update mods..." lives, so checking for updates from Mod Menu is one click, which is
 * the shortcut Mod Menu Updater offers.</p>
 *
 * <p>This class is named only by the {@code modmenu} entrypoint, which Mod Menu resolves itself. A
 * game without Mod Menu therefore never loads this class and never needs
 * {@code com.terraformersmc.modmenu.api} on the classpath.</p>
 */
public final class CleanMcModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CleanMenuScreen(CleanMcModClient.service(), CleanMcModClient.updater());
    }
}
