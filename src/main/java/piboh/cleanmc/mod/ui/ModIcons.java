package piboh.cleanmc.mod.ui;

import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import piboh.cleanmc.mod.CleanMcMod;
import piboh.cleanmc.mod.update.LocalMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The mod icon, read from the mod's own jar and registered as a texture.
 *
 * <p>The jar already carries {@code assets/<modid>/icon.png}, so nothing has to be downloaded: the
 * file is opened once per jar, the first time the update list asks for it, and the identifier is
 * cached. A jar without a usable icon simply returns {@code null} and the row falls back to its
 * initial tile.</p>
 */
public final class ModIcons {

    private static final int ICON_PIXELS = 12;

    /** The size the settings card draws the mod's own icon at. */
    private static final int CARD_PIXELS = 32;

    private static final Map<Path, Identifier> CACHE = new HashMap<>();
    private static final Map<Path, Boolean> TRIED = new HashMap<>();

    private static final Identifier OWN = Identifier.fromNamespaceAndPath("cleanmcmod", "own_icon");
    private static boolean ownTried;
    private static Identifier ownId;

    private ModIcons() {
    }

    /** The texture of this jar's icon, or {@code null} when the jar has none that can be decoded. */
    public static Identifier get(LocalMod mod) {
        Path path = mod.path();
        if (TRIED.containsKey(path)) {
            return CACHE.get(path);
        }
        TRIED.put(path, Boolean.TRUE);
        Identifier id = load(mod);
        if (id != null) {
            CACHE.put(path, id);
        }
        return id;
    }

    /** The size a row draws the icon at, so the loader and the screen cannot disagree. */
    public static int size() {
        return ICON_PIXELS;
    }

    /** The size the card draws the icon at, so the screen and the loader cannot disagree. */
    public static int cardSize() {
        return CARD_PIXELS;
    }

    /**
     * The mod's own icon, read from {@code assets/cleanmcmod/icon.png} inside this jar: the card shows
     * the image the game already shows for the mod instead of a second icon drawn by hand. Returns
     * {@code null} when it cannot be read, and the card keeps its empty frame.
     */
    public static Identifier own() {
        if (ownTried) {
            return ownId;
        }
        ownTried = true;
        try {
            Path path = FabricLoader.getInstance().getModContainer("cleanmcmod")
                    .flatMap(container -> container.findPath("assets/cleanmcmod/icon.png"))
                    .orElse(null);
            if (path == null) {
                return null;
            }
            try (InputStream in = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(in);
                Minecraft.getInstance().getTextureManager()
                        .register(OWN, new DynamicTexture(() -> "cleanmcmod icon",
                                shrink(image, CARD_PIXELS)));
            }
            ownId = OWN;
        } catch (Exception exception) {
            CleanMcMod.LOGGER.warn("Could not read the mod icon: {}", exception.toString());
        }
        return ownId;
    }

    private static Identifier load(LocalMod mod) {
        try (ZipFile zip = new ZipFile(mod.path().toFile())) {
            ZipEntry entry = findIcon(zip, mod.modId());
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                NativeImage image = NativeImage.read(in);
                Identifier id = Identifier.fromNamespaceAndPath("cleanmcmod", "mod_icon/" + slug(mod, entry.getName()));
                Minecraft.getInstance().getTextureManager()
                        .register(id, new DynamicTexture(() -> "cleanmcmod mod icon", shrink(image, ICON_PIXELS)));
                return id;
            }
        } catch (Exception exception) {
            // A jar whose icon cannot be decoded keeps its initial tile, and the reason is in the log
            // instead of being swallowed.
            CleanMcMod.LOGGER.warn("Could not read the icon of {}: {}", mod.fileName(), exception.toString());
            return null;
        }
    }

    /** {@code assets/<modid>/icon.png} first, then the shortest {@code icon.png} the jar carries. */
    /**
     * The icon at the size a row draws it. Mod icons are 64 to 512 pixels wide, and a texture drawn
     * with a 16 pixel size samples its top left corner only, which is why every row looked blank.
     */
    private static NativeImage shrink(NativeImage source, int size) {
        if (source.getWidth() == size && source.getHeight() == size) {
            return source;
        }
        NativeImage target = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            int fromY = y * source.getHeight() / size;
            int toY = Math.max(fromY + 1, (y + 1) * source.getHeight() / size);
            for (int x = 0; x < size; x++) {
                int fromX = x * source.getWidth() / size;
                int toX = Math.max(fromX + 1, (x + 1) * source.getWidth() / size);
                int alpha = 0;
                int first = 0;
                int second = 0;
                int third = 0;
                for (int sourceY = fromY; sourceY < toY; sourceY++) {
                    for (int sourceX = fromX; sourceX < toX; sourceX++) {
                        int pixel = source.getPixel(sourceX, sourceY);
                        alpha += pixel >>> 24 & 0xFF;
                        first += pixel >> 16 & 0xFF;
                        second += pixel >> 8 & 0xFF;
                        third += pixel & 0xFF;
                    }
                }
                int count = (toY - fromY) * (toX - fromX);
                target.setPixel(x, y, (alpha / count) << 24 | (first / count) << 16
                        | (second / count) << 8 | third / count);
            }
        }
        source.close();
        return target;
    }

    private static ZipEntry findIcon(ZipFile zip, String modId) {
        if (modId != null && !modId.isBlank()) {
            ZipEntry exact = zip.getEntry("assets/" + modId + "/icon.png");
            if (exact != null) {
                return exact;
            }
        }
        ZipEntry root = zip.getEntry("icon.png");
        if (root != null) {
            return root;
        }
        ZipEntry best = null;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith("assets/") || !name.endsWith("icon.png")) {
                continue;
            }
            if (best == null || name.length() < best.getName().length()) {
                best = entry;
            }
        }
        return best;
    }

    private static String slug(LocalMod mod, String entryName) {
        String base = mod.modId() != null && !mod.modId().isBlank()
                ? mod.modId()
                : mod.fileName().replace(".jar", "");
        return base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_")
                + "_" + Integer.toHexString((entryName + mod.path()).hashCode());
    }
}
