package de.btegermany.terraplusminus.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.*;

/**
 * A {@link FileVisitor} that copies the visited file hierarchy to a directory.
 *
 * @author Smyler
 */
public class CopyDirectoryTreeVisitor implements FileVisitor<Path> {

    private final Path sourceDirectory;
    private final Path destinationDirectory;
    private final CopyOption[] options;
    private final Logger logger;
    private final boolean dryRun;

    public CopyDirectoryTreeVisitor(@NotNull Path sourceDirectory, @NotNull Path destinationDirectory, @NotNull Logger logger, boolean dryRun, @NotNull CopyOption... options) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.options = options;
        this.logger = logger;
        this.dryRun = dryRun;
    }

    @Override
    public @NotNull FileVisitResult preVisitDirectory(Path path, @NotNull BasicFileAttributes basicFileAttributes) throws IOException {
        Path destination = this.transposeFromSrcToDst(path);
        if (!exists(destination)) {
            this.copy(path, destination);
        } else if (!isDirectory(destination)){
            throw new IOException("Cannot copy directory '" + path + "' to '" + destination + "': destination exists and is not a directory");
        } else {
            this.logger.finest("Did not copy directory '" + path + "' to '" + destination + "': destination already exists");
        }
        return CONTINUE;
    }

    @Override
    public @NotNull FileVisitResult visitFile(Path path, @NotNull BasicFileAttributes basicFileAttributes) throws IOException {
        Path destination = this.transposeFromSrcToDst(path);
        this.copy(path, destination);
        return CONTINUE;
    }

    @Override
    public @NotNull FileVisitResult visitFileFailed(Path path, @NotNull IOException e) throws IOException {
        throw e;
    }

    @Override
    public @NotNull FileVisitResult postVisitDirectory(Path path, @Nullable IOException e) throws IOException {
        if (e != null) {
            throw e;
        }
        return CONTINUE;
    }

    private Path transposeFromSrcToDst(Path path) {
        return this.destinationDirectory.resolve(path.relativize(this.sourceDirectory));
    }

    private void copy(Path source, Path destination) throws IOException {
        this.logger.finest("Copying '" + source + "' to '" + destination + "'");
        if (this.dryRun) {
            if (exists(destination)) {
                throw new FileAlreadyExistsException(destination.toString());
            } else if (isWritable(destination)) {
                throw new AccessDeniedException(destination.toString());
            } else if (isReadable(source)) {
                throw new AccessDeniedException(source.toString());
            }
        } else {
            Files.copy(source, destination, this.options);
        }
    }

}
