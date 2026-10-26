package piboh.cleanmc.mod.clean;

/** What a target currently occupies on disk. */
public record TargetState(CacheTarget target, boolean exists, int files, long bytes) {

    public static TargetState missing(CacheTarget target) {
        return new TargetState(target, false, 0, 0L);
    }

    public boolean present() {
        return exists;
    }
}
