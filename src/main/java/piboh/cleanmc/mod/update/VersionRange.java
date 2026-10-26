package piboh.cleanmc.mod.update;

/**
 * The small subset of Fabric's version predicates that mods actually declare in
 * {@code fabric.mod.json}: {@code >=1.0 <2.0}, {@code <2.0.0}, {@code =1.2}, {@code ~1.2.3},
 * {@code ^1.2.3}, {@code 1.2.x} and {@code *}.
 */
public final class VersionRange {

    private VersionRange() {
    }

    /** @return true/false when the predicate can be evaluated, {@code null} when it cannot */
    public static Boolean satisfies(String version, String predicate) {
        if (predicate == null || predicate.isBlank()) {
            return null;
        }
        for (String token : predicate.trim().split("\s+")) {
            Boolean one = single(version, token);
            if (one == null || !one) {
                return one;
            }
        }
        return true;
    }

    private static Boolean single(String version, String token) {
        if (token.equals("*") || token.equalsIgnoreCase("x")) {
            return true;
        }
        String prefix = operator(token);
        String bound = token.substring(prefix.length()).trim();
        if (bound.isEmpty()) {
            return null;
        }
        if (bound.equalsIgnoreCase("x") || bound.endsWith(".x") || bound.endsWith(".X")) {
            String base = bound.substring(0, bound.length() - 1);
            if (base.endsWith(".")) {
                base = base.substring(0, base.length() - 1);
            }
            return prefix.isEmpty() || prefix.equals("=")
                    ? startsWithSegment(version, base)
                    : null;
        }
        if (!bound.matches("[0-9][0-9A-Za-z.+-]*")) {
            return null;
        }
        return switch (prefix) {
            case ">=" -> VersionComparator.compare(version, bound) >= 0;
            case ">" -> VersionComparator.compare(version, bound) > 0;
            case "<=" -> VersionComparator.compare(version, bound) <= 0;
            case "<" -> VersionComparator.compare(version, bound) < 0;
            case "=", "" -> VersionComparator.compare(version, bound) == 0;
            case "~" -> VersionComparator.compare(version, bound) >= 0
                    && VersionComparator.compare(version, bump(bound, 1)) < 0;
            case "^" -> VersionComparator.compare(version, bound) >= 0
                    && VersionComparator.compare(version, bump(bound, 0)) < 0;
            default -> null;
        };
    }

    private static String operator(String token) {
        for (String candidate : new String[] {">=", "<=", ">", "<", "=", "~", "^"}) {
            if (token.startsWith(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    /** {@code 1} bumps the minor, {@code 0} the major: {@code 1.2.3} becomes {@code 1.3.0} / {@code 2.0.0}. */
    private static String bump(String version, int index) {
        String[] parts = version.split("\\.");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.max(parts.length, index + 1); i++) {
            if (i > 0) {
                out.append('.');
            }
            if (i < index) {
                out.append(parts[i]);
            } else if (i == index) {
                out.append(number(parts[i]) + 1);
            } else {
                out.append('0');
            }
        }
        return out.toString();
    }

    private static boolean startsWithSegment(String version, String base) {
        return version.equals(base) || version.startsWith(base + ".");
    }

    private static int number(String part) {
        StringBuilder digits = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (!Character.isDigit(c)) {
                break;
            }
            digits.append(c);
        }
        return digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
    }
}
