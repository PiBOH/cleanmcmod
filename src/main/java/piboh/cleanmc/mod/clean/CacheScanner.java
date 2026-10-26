package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/** Measures cache targets without touching them. */
public final class CacheScanner {

    public TargetState scan(CacheTarget target) {
        Path path = target.path();
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return TargetState.missing(target);
        }
        Counter counter = new Counter();
        try {
            Files.walkFileTree(path, counter);
        } catch (IOException e) {
            CleanMcMod.LOGGER.warn("Could not measure {}: {}", path, e.toString());
        }
        return new TargetState(target, true, counter.files, counter.bytes);
    }

    public CacheScan scanAll(List<CacheTarget> targets) {
        List<TargetState> states = new ArrayList<>(targets.size());
        for (CacheTarget target : targets) {
            states.add(scan(target));
        }
        return CacheScan.of(states);
    }

    /** Counts regular files and their size. Symbolic links are counted but never followed. */
    private static final class Counter extends SimpleFileVisitor<Path> {
        private int files;
        private long bytes;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            files++;
            if (attrs.isRegularFile()) {
                bytes += attrs.size();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
