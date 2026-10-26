package piboh.cleanmc.mod.clean;

import piboh.cleanmc.mod.CleanMcMod;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes cache targets.
 *
 * <p>Safety rules, in order:</p>
 * <ol>
 *   <li>a target must live inside the game directory and must not be the game directory itself;</li>
 *   <li>symbolic links are deleted, never followed;</li>
 *   <li>files locked by a running game are skipped and reported, not treated as fatal;</li>
 *   <li>directories are only removed when empty, and the target root itself is kept.</li>
 * </ol>
 */
public final class CacheCleaner {

    private final Path gameDir;

    public CacheCleaner(Path gameDir) {
        this.gameDir = gameDir.toAbsolutePath().normalize();
    }

    public CleanReport clean(List<CacheTarget> targets, boolean dryRun) {
        long started = System.nanoTime();
        List<TargetResult> results = new ArrayList<>(targets.size());
        for (CacheTarget target : targets) {
            results.add(cleanOne(target, dryRun));
        }
        long duration = (System.nanoTime() - started) / 1_000_000L;
        return new CleanReport(dryRun, duration, results);
    }

    TargetResult cleanOne(CacheTarget target, boolean dryRun) {
        Path path = target.path();
        if (path.equals(gameDir) || !path.startsWith(gameDir)) {
            CleanMcMod.LOGGER.warn("Refusing to clean {} (outside the game directory)", path);
            return TargetResult.failed(target.id(), "outside the game directory");
        }
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return TargetResult.missing(target.id());
        }
        if (dryRun) {
            TargetState state = new CacheScanner().scan(target);
            return TargetResult.wouldRemove(target.id(), state.files(), state.bytes());
        }

        DeletingVisitor visitor = new DeletingVisitor(path);
        try {
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            CleanMcMod.LOGGER.warn("Clean of {} aborted: {}", path, e.toString());
            if (visitor.deleted == 0) {
                return TargetResult.failed(target.id(), e.toString());
            }
        }

        if (visitor.deleted == 0 && visitor.failed > 0) {
            return TargetResult.failed(target.id(), "no file could be deleted");
        }
        if (visitor.failed > 0 || visitor.skipped > 0) {
            String reason = visitor.skipped > 0 ? "locked by the running game" : "some entries could not be deleted";
            return TargetResult.partial(target.id(), visitor.deleted, visitor.bytes, visitor.skipped, reason);
        }
        return TargetResult.cleaned(target.id(), visitor.deleted, visitor.bytes);
    }

    private static final class DeletingVisitor extends SimpleFileVisitor<Path> {
        private final Path root;
        private int deleted;
        private long bytes;
        private int skipped;
        private int failed;

        private DeletingVisitor(Path root) {
            this.root = root;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            long size = attrs.isRegularFile() ? attrs.size() : 0L;
            try {
                Files.delete(file);
                deleted++;
                bytes += size;
            } catch (NoSuchFileException e) {
                // already gone: nothing to do
            } catch (AccessDeniedException e) {
                skipped++;
            } catch (IOException e) {
                failed++;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (exc instanceof NoSuchFileException) {
                return FileVisitResult.CONTINUE;
            }
            if (exc instanceof AccessDeniedException) {
                skipped++;
            } else {
                failed++;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (exc != null) {
                failed++;
                return FileVisitResult.CONTINUE;
            }
            if (dir.equals(root)) {
                // keep the target folder itself; the game expects e.g. logs/ to exist
                return FileVisitResult.CONTINUE;
            }
            try {
                Files.deleteIfExists(dir);
            } catch (DirectoryNotEmptyException e) {
                // something inside could not be removed — already counted
            } catch (IOException e) {
                skipped++;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
