package piboh.cleanmc.mod.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import piboh.cleanmc.mod.CleanMcMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The language CleanMCMod itself speaks.
 *
 * <p>Minecraft translates a mod only through the game language, so the one way to let a player read
 * this mod in Italian while the game stays in English is to read our own JSON: the table of the
 * chosen locale is loaded from this jar, and every key it contains is resolved here.
 * {@code Component.translatable} stays as the fallback for the keys our files do not carry, so vanilla
 * strings such as "Done" still come from the game.</p>
 */
public final class Lang {

    /** The locales the mod ships, each named in its own language. */
    public static final Map<String, String> SHIPPED = new LinkedHashMap<>();

    static {
        SHIPPED.put("en_us", "English");
        SHIPPED.put("de_de", "Deutsch");
        SHIPPED.put("es_es", "Español");
        SHIPPED.put("fr_fr", "Français");
        SHIPPED.put("it_it", "Italiano");
        SHIPPED.put("ja_jp", "日本語");
        SHIPPED.put("ko_kr", "한국어");
        SHIPPED.put("nl_nl", "Nederlands");
        SHIPPED.put("pl_pl", "Polski");
        SHIPPED.put("pt_br", "Português");
        SHIPPED.put("ru_ru", "Русский");
        SHIPPED.put("tr_tr", "Türkçe");
        SHIPPED.put("uk_ua", "Українська");
        SHIPPED.put("zh_cn", "简体中文");
        SHIPPED.put("zh_tw", "繁體中文");
    }

    private static final Gson GSON = new Gson();
    private static final Type TABLE_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private static Map<String, String> table = Map.of();
    private static String selected = "";

    private Lang() {
    }

    /** The code in use, or an empty string when the mod follows the game language. */
    public static String selected() {
        return selected;
    }

    /** Loads a locale, or returns to the game language when the code is empty. */
    public static void select(String code) {
        selected = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        if (selected.isEmpty()) {
            table = Map.of();
            return;
        }
        String resource = "/assets/cleanmcmod/lang/" + selected + ".json";
        try (InputStream in = Lang.class.getResourceAsStream(resource)) {
            if (in == null) {
                CleanMcMod.LOGGER.warn("No translation shipped for {}", selected);
                table = Map.of();
                return;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> parsed = GSON.fromJson(reader, TABLE_TYPE);
                table = parsed == null ? Map.of() : Map.copyOf(parsed);
            }
        } catch (Exception e) {
            CleanMcMod.LOGGER.warn("Could not read the {} translation: {}", selected, e.toString());
            table = Map.of();
        }
    }

    /** The name to show for a code, or the game language label for an empty one. */
    public static String name(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return SHIPPED.getOrDefault(code, code);
    }

    /** The next code in the shipped list, wrapping from the last one back to the game language. */
    public static String next(String code) {
        List<String> codes = List.copyOf(SHIPPED.keySet());
        int index = code == null || code.isBlank() ? -1 : codes.indexOf(code);
        return index + 1 >= codes.size() ? "" : codes.get(index + 1);
    }

    /** Our own translation when the chosen table has the key, the game's otherwise. */
    public static Component text(String key, Object... args) {
        String value = table.get(key);
        if (value == null) {
            return Component.translatable(key, args);
        }
        return Component.literal(args.length == 0 ? value : String.format(Locale.ROOT, value, args));
    }
}
