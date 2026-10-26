package piboh.cleanmc.mod.clean;

/** Outcome state of one target. */
public enum TargetStatus {
    /** Everything that existed was removed. */
    CLEANED,
    /** Something was removed, but files were locked or refuse to be deleted. */
    PARTIAL,
    /** Nothing was deleted because the run was a dry run. */
    DRY_RUN,
    /** The target does not exist in this game directory. */
    MISSING,
    /** Nothing could be removed, or the target was rejected by the path guard. */
    FAILED
}
