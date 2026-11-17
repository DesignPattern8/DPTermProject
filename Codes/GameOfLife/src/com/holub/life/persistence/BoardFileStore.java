package com.holub.life.persistence;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Owns file streams; the caller retains board and clock policy. */
public final class BoardFileStore {
  private final PatternCodec codec;
  private final FileMover mover;

  public BoardFileStore(PatternCodec codec) {
    this(
        codec,
        (source, target) ->
            Files.move(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING));
  }

  BoardFileStore(PatternCodec codec, FileMover mover) {
    this.codec = Objects.requireNonNull(codec, "codec");
    this.mover = Objects.requireNonNull(mover, "mover");
  }

  @FunctionalInterface
  interface FileMover {
    void move(Path source, Path target) throws IOException;
  }

  public PatternData read(Path path) throws IOException {
    return read(path, () -> {});
  }

  /** Runs preparation only after opening succeeds, before decoding begins. */
  public PatternData read(Path path, Runnable afterOpen) throws IOException {
    Objects.requireNonNull(afterOpen, "afterOpen");
    try (InputStream in = new FileInputStream(path.toFile())) {
      afterOpen.run();
      return codec.read(in);
    }
  }

  public void write(Path path, PatternData data) throws IOException {
    Objects.requireNonNull(data, "data");
    write(path, () -> data);
  }

  /** Stages a complete file before atomically replacing the target; no non-atomic fallback. */
  public void write(Path path, Supplier<PatternData> capture) throws IOException {
    Objects.requireNonNull(capture, "capture");
    Path absolute = path.toAbsolutePath();
    if (absolute.getParent() == null) {
      throw new FileSystemException(path.toString(), null, "Save target must be a regular file");
    }
    Path target = absolute.getParent().toRealPath().resolve(absolute.getFileName());
    Optional<Set<PosixFilePermission>> permissions = validateTarget(target);
    Path staged = Files.createTempFile(target.getParent(), ".life-", ".tmp");
    try {
      try (OutputStream out = Files.newOutputStream(staged)) {
        codec.write(capture.get(), out);
      }
      if (permissions.isPresent()) Files.setPosixFilePermissions(staged, permissions.get());
      mover.move(staged, target);
    } catch (IOException | RuntimeException | Error failure) {
      try {
        Files.deleteIfExists(staged);
      } catch (IOException | RuntimeException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
  }

  private static Optional<Set<PosixFilePermission>> validateTarget(Path target) throws IOException {
    BasicFileAttributes attributes;
    try {
      attributes =
          Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException missing) {
      return Optional.empty();
    }
    if (!attributes.isRegularFile()) {
      throw new FileSystemException(
          target.toString(), null, "Save target must be a regular file, not a link");
    }
    if (!Files.isWritable(target)) throw new AccessDeniedException(target.toString());
    PosixFileAttributeView posix =
        Files.getFileAttributeView(target, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
    if (posix == null) return Optional.empty();
    Set<PosixFilePermission> permissions = posix.readAttributes().permissions();
    if (!permissions.contains(PosixFilePermission.OWNER_WRITE)
        && !permissions.contains(PosixFilePermission.GROUP_WRITE)
        && !permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
      throw new AccessDeniedException(target.toString());
    }
    return Optional.of(permissions);
  }
}
