package com.holub.life.persistence;

import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CellPosition;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Owns file streams; the caller retains board and clock policy. */
public final class BoardFileStore {
  private final PatternCodec codec;
  private final FileMover mover;
  private final List<Integer> blockWidths;

  public BoardFileStore(PatternCodec codec) {
    this(codec, BoardFileStore::replaceAtomically);
  }

  /** Configures child-block widths from the root down to cells (e.g. [8, 1]). */
  public BoardFileStore(PatternCodec codec, List<Integer> blockWidths) {
    this(codec, BoardFileStore::replaceAtomically, validatedWidths(blockWidths));
  }

  BoardFileStore(PatternCodec codec, FileMover mover) {
    this(codec, mover, List.of());
  }

  private BoardFileStore(PatternCodec codec, FileMover mover, List<Integer> blockWidths) {
    this.codec = Objects.requireNonNull(codec, "codec");
    this.mover = Objects.requireNonNull(mover, "mover");
    this.blockWidths = blockWidths;
  }

  private static void replaceAtomically(Path source, Path target) throws IOException {
    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }

  private static List<Integer> validatedWidths(List<Integer> widths) {
    List<Integer> copy = List.copyOf(widths);
    if (copy.isEmpty() || copy.getLast() != 1) {
      throw new IllegalArgumentException("Block widths must end with cell width 1");
    }
    for (int i = 0; i < copy.size(); i++) {
      int width = copy.get(i);
      if (width <= 0 || (i > 0 && (copy.get(i - 1) <= width || copy.get(i - 1) % width != 0))) {
        throw new IllegalArgumentException(
            "Block widths must decrease and divide each parent width");
      }
    }
    return copy;
  }

  @FunctionalInterface
  interface FileMover {
    void move(Path source, Path target) throws IOException;
  }

  public PatternData read(Path path) throws IOException {
    return read(path, () -> {});
  }

  /** Fully reads and closes the file before projecting it onto the target board. */
  public BoardSnapshot read(Path path, int rows, int columns) throws IOException {
    if (rows <= 0 || columns <= 0) {
      throw new IllegalArgumentException("Board dimensions must be positive");
    }
    PatternData data = read(path);
    try {
      return data.toSnapshot(rows, columns);
    } catch (IllegalArgumentException invalidPattern) {
      throw new IOException("Life file cannot be represented by a board snapshot", invalidPattern);
    }
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

  /** Captures once after staging opens, orders the snapshot, then uses the atomic write path. */
  public void writeSnapshot(Path path, Supplier<BoardSnapshot> capture) throws IOException {
    if (blockWidths.isEmpty()) {
      throw new IllegalStateException("Snapshot writes require an explicit block layout");
    }
    Objects.requireNonNull(capture, "capture");
    write(path, () -> orderedPattern(capture.get()));
  }

  private PatternData orderedPattern(BoardSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    int outerWidth = blockWidths.getFirst();
    if (snapshot.rows() % outerWidth != 0 || snapshot.columns() % outerWidth != 0) {
      throw new IllegalArgumentException(
          "Snapshot dimensions must be divisible by the outer block width");
    }
    List<CellPosition> ordered = new ArrayList<>(snapshot.liveCells());
    ordered.sort(this::comparePositions);
    return new PatternData(ordered, snapshot.cancerCells());
  }

  private int comparePositions(CellPosition first, CellPosition second) {
    for (int width : blockWidths) {
      int row = Integer.compare(first.row() / width, second.row() / width);
      if (row != 0) return row;
      int column = Integer.compare(first.column() / width, second.column() / width);
      if (column != 0) return column;
    }
    return 0;
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
