package piboh.cleanmc.mod.update;

/**
 * Where an update is downloaded from. The order is also the preference order: when several sources
 * offer the same version of a mod, the first one in this enum wins, so Modrinth is preferred over
 * GitHub and GitHub over CurseForge.
 */
public enum ModSource {

    MODRINTH("cleanmcmod.source.modrinth", "MR"),
    GITHUB("cleanmcmod.source.github", "GH"),
    CURSEFORGE("cleanmcmod.source.curseforge", "CF");

    private final String translationKey;
    private final String tag;

    ModSource(String translationKey, String tag) {
        this.translationKey = translationKey;
        this.tag = tag;
    }

    public String translationKey() {
        return translationKey;
    }

    /** Two letters for the update list, so the row stays narrow. */
    public String tag() {
        return tag;
    }

    /** Only Modrinth publishes a hash of every file; the other two are installed unverified. */
    public boolean publishesChecksum() {
        return this == MODRINTH;
    }

    /** The source that wins when two sources publish the same version number. */
    public boolean prefers(ModSource other) {
        return ordinal() < other.ordinal();
    }
}
