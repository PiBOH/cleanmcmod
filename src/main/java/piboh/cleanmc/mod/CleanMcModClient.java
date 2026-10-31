package piboh.cleanmc.mod;

import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.command.CleanMcModCommand;
import piboh.cleanmc.mod.ui.CleanMenuScreen;
import piboh.cleanmc.mod.ui.MenuKey;
import piboh.cleanmc.mod.ui.ModUpdatesScreen;
import piboh.cleanmc.mod.update.ModrinthClient;
import piboh.cleanmc.mod.update.ModUpdaterService;
import piboh.cleanmc.mod.update.UpdateSources;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Client-only entrypoint. CleanMCMod never runs on a dedicated server. */
public final class CleanMcModClient implements ClientModInitializer {

    /** Mod id of Mod Menu Updater with everything but letters and digits removed. */
    private static final String MOD_MENU_UPDATER = "modmenuupdater";

    private static CleanMcModService service;
    private static ModUpdaterService updater;
    private static KeyMapping openMenuKey;
    private static KeyMapping openUpdatesKey;

    public static CleanMcModService service() {
        return service;
    }

    public static ModUpdaterService updater() {
        return updater;
    }

    public static KeyMapping openMenuKey() {
        return openMenuKey;
    }

    public static KeyMapping openUpdatesKey() {
        return openUpdatesKey;
    }

    /**
     * True when Mod Menu Updater is installed. It adds in-game updating to Mod Menu itself, so two
     * updaters writing {@code mods/} would fight over the same jars: this mod then lists the updates
     * and leaves installing to it.
     */
    public static boolean modMenuUpdaterInstalled() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(container -> container.getMetadata().getId().toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", ""))
                .anyMatch(MOD_MENU_UPDATER::equals);
    }

    @Override
    public void onInitializeClient() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        service = new CleanMcModService(gameDir, FabricLoader.getInstance().getConfigDir());

        String gameVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
        List<String> loaders = FabricLoader.getInstance().isModLoaded("quilt_loader")
                ? List.of("quilt", "fabric")
                : List.of("fabric", "quilt");
        ModrinthClient modrinth = new ModrinthClient(gameVersion, loaders);
        UpdateSources sources = new UpdateSources(gameVersion, loaders, modrinth);
        boolean delegated = modMenuUpdaterInstalled();
        updater = new ModUpdaterService(gameDir, sources, modrinth, modrinth::resolve, delegated);
        if (delegated) {
            CleanMcMod.LOGGER.info("Mod Menu Updater is installed — CleanMCMod lists updates and installs nothing");
        }

        openMenuKey = KeyMappingHelper.registerKeyMapping(MenuKey.create());
        openUpdatesKey = KeyMappingHelper.registerKeyMapping(MenuKey.createUpdates());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                client.setScreenAndShow(new CleanMenuScreen(service, updater));
            }
            while (openUpdatesKey.consumeClick()) {
                client.setScreenAndShow(new ModUpdatesScreen(new CleanMenuScreen(service, updater), updater));
            }
        });

        // A player who never reads the key bindings has to find this button on the title screen.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                Screens.getWidgets(screen).add(Button
                        .builder(Component.translatable("cleanmcmod.button.open"),
                                button -> client.setScreenAndShow(new CleanMenuScreen(service, updater)))
                        .bounds(screen.width - 106, 6, 100, 20)
                        .build());
            }
        });

        CleanMcModCommand.register();
        CleanMcMod.LOGGER.info("CleanMCMod ready — game directory {}", service.gameDir());
    }
}
