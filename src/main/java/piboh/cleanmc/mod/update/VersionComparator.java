package piboh.cleanmc.mod.update;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Compares the version numbers Modrinth and mod metadata use ({@code 1.2.3}, {@code 1.2.3-rc1},
 * {@code 0.136.1+1.21.8}).
 *
 * <p>Rules: build metadata after {@code +} is ignored, numeric segments compare as numbers, and at
 * the same position a number is newer than a word, so {@code 1.0.0} beats {@code 1.0.0-beta}.
 * Missing segments count as zero.</p>
 */
public final class VersionComparator {

    private VersionComparator() {
    }

    public static int compare(String left, String right) {
        String[] a = tokens(left);
        String[] b = tokens(right);
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            String x = i < a.length ? a[i] : "0";
            String y = i < b.length ? b[i] : "0";
            boolean numericX = isNumeric(x);
            boolean numericY = isNumeric(y);
            int result;
            if (numericX && numericY) {
                result = compareNumeric(x, y);
            } else if (numericX != numericY) {
                result = numericX ? 1 : -1;
            } else {
                result = x.compareToIgnoreCase(y);
            }
            if (result != 0) {
                return Integer.signum(result);
            }
        }
        return 0;
    }

    /**
     * True when two spellings name the same build: the same file published as {@code 3.0.10+mc26.3}
     * on one source and {@code 26.3-3.0.10} on another. The build metadata, the running game version
     * and the loader name are removed before comparing, so a source that renames a version is not
     * mistaken for a newer one. Offering such a row means asking the installer to replace the jar the
     * game has open, which Windows refuses.
     */
    public static boolean sameBuild(String left, String right, String gameVersion) {
        return compare(core(left, gameVersion), core(right, gameVersion)) == 0;
    }

    /** True when the text carries a number: a release label without one is not a version. */
    public static boolean hasNumber(String version) {
        if (version == null) {
            return false;
        }
        for (int i = 0; i < version.length(); i++) {
            if (Character.isDigit(version.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** The version without build metadata, game version and loader tokens. */
    static String core(String version, String gameVersion) {
        if (version == null) {
            return "";
        }
        String cleaned = version.trim().toLowerCase(Locale.ROOT);
        int buildMetadata = cleaned.indexOf('+');
        if (buildMetadata >= 0) {
            cleaned = cleaned.substring(0, buildMetadata);
        }
        for (String loader : List.of("neoforge", "fabric", "quilt", "forge")) {
            cleaned = cleaned.replace(loader, "-");
        }
        if (gameVersion != null && !gameVersion.isBlank()) {
            // Only a standalone game version is dropped: stripping it inside a longer number would
            // turn a real version (0.26.3 shipped as mc26.3-0.26.3-fabric) into something else, and
            // leaving the ".0" of 26.3.0 behind would keep the same build looking newer.
            String game = java.util.regex.Pattern.quote(gameVersion.trim().toLowerCase(Locale.ROOT));
            String standalone = "(?<![0-9.])(?:mc)?" + game + "([.]0)?(?![0-9])";
            cleaned = cleaned.replaceAll(standalone, "-");
        }
        return cleaned;
    }

    private static String[] tokens(String version) {
        if (version == null) {
            return new String[0];
        }
        String cleaned = version.trim().toLowerCase(Locale.ROOT);
        int buildMetadata = cleaned.indexOf('+');
        if (buildMetadata >= 0) {
            cleaned = cleaned.substring(0, buildMetadata);
        }
        if (cleaned.startsWith("v") && cleaned.length() > 1 && Character.isDigit(cleaned.charAt(1))) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.isEmpty()) {
            return new String[0];
        }
        // A leading separator (the game version stripped from 26.3-3.0.10 leaves one) must not
        // become an empty token, which would compare as a word against the first number.
        return Arrays.stream(cleaned.split("[^a-z0-9]+"))
                .filter(token -> !token.isEmpty())
                .toArray(String[]::new);
    }

    private static int compareNumeric(String left, String right) {
        String a = trimZeros(left);
        String b = trimZeros(right);
        if (a.length() != b.length()) {
            return Integer.compare(a.length(), b.length());
        }
        return a.compareTo(b);
    }

    private static String trimZeros(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    private static boolean isNumeric(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return !token.isEmpty();
    }
}
