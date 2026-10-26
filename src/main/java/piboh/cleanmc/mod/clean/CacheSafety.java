package piboh.cleanmc.mod.clean;

/** How disruptive deleting a target is. */
public enum CacheSafety {
    /** The game or mod regenerates the data on the next launch; nothing is downloaded again. */
    SAFE("cleanmcmod.safety.safe"),
    /** Deleting forces a re-download from Mojang's servers; only offered by the deep-clean option. */
    REQUIRES_REDOWNLOAD("cleanmcmod.safety.requires_redownload");

    private final String translationKey;

    CacheSafety(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
