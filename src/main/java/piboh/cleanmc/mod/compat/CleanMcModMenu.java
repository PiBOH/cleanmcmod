package piboh.cleanmc.mod.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import piboh.cleanmc.mod.CleanMcModClient;
import piboh.cleanmc.mod.ui.CacheSettingsScreen;

/**
 * Optional Mod Menu integration: the mod's settings screen is registered as its
 * config screen.
 *
 * <p>This class is named only by the {@code modmenu} entrypoint, which Mod Menu
 * resolves itself. A game without Mod Menu therefore never loads this class and
 * never needs {@code com.terraformersmc.modmenu.api} on the classpath.</p>
 */
public final class CleanMcModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CacheSettingsScreen(parent, CleanMcModClient.service());
    }
}
