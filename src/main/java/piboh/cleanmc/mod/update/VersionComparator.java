package piboh.cleanmc.mod.update;

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
        return cleaned.isEmpty() ? new String[0] : cleaned.split("[^a-z0-9]+");
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
