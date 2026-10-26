package piboh.cleanmc.mod.clean;

import java.util.List;

/** Result of measuring every configured cache target. */
public record CacheScan(List<TargetState> targets, int files, long bytes) {

    public CacheScan {
        targets = List.copyOf(targets);
    }

    public static CacheScan of(List<TargetState> targets) {
        int files = 0;
        long bytes = 0L;
        for (TargetState state : targets) {
            files += state.files();
            bytes += state.bytes();
        }
        return new CacheScan(targets, files, bytes);
    }

    /** Targets that actually exist on disk — the ones a clean would touch. */
    public List<TargetState> presentTargets() {
        return targets.stream().filter(TargetState::present).toList();
    }

    public int presentCount() {
        return presentTargets().size();
    }

    public boolean empty() {
        return presentCount() == 0;
    }
}
