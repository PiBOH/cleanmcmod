package piboh.cleanmc.mod.clean;

/** Where a cache target comes from, used for grouping and colouring in the UI. */
public enum CacheCategory {
    VANILLA("cleanmcmod.category.vanilla"),
    LOADER("cleanmcmod.category.loader"),
    RESOURCE_PACK("cleanmcmod.category.resource_pack"),
    MOD("cleanmcmod.category.mod"),
    CUSTOM("cleanmcmod.category.custom");

    private final String translationKey;

    CacheCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
