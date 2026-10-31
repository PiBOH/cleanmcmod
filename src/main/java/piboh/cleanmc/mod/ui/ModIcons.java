package piboh.cleanmc.mod.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import piboh.cleanmc.mod.CleanMcMod;
import piboh.cleanmc.mod.update.LocalMod;

import java.io.InputStream;
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

    private static final Map<Path, Identifier> CACHE = new HashMap<>();
    private static final Map<Path, Boolean> TRIED = new HashMap<>();

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
                        .register(id, new DynamicTexture(() -> "cleanmcmod mod icon", image));
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
