package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicCommitTest {
  @TempDir Path directory;
  private static final byte[] ORIGINAL = {10, 20, 30};
  private static final PatternData EMPTY = new PatternData(List.of(), Set.of());

  @Test
  void moveFailurePreservesTargetAndCleansTemporaryFile() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var failure = new IOException("rename failed");
    var store =
        new BoardFileStore(
            new LegacyLifeCodec(),
            (source, destination) -> {
              throw failure;
            });

    // When / Then: the same failure must also leave a previously missing target absent.
    assertSame(failure, assertThrows(IOException.class, () -> store.write(target, EMPTY)));
    assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
    Files.delete(target);
    assertSame(failure, assertThrows(IOException.class, () -> store.write(target, EMPTY)));
    assertFalse(Files.exists(target));
    assertDirectoryEmpty();
  }

  @Test
  void unsupportedAtomicMoveDoesNotRetry() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var calls = new AtomicInteger();
    var failure = new AtomicMoveNotSupportedException("staged", target.toString(), "unsupported");
    var store =
        new BoardFileStore(
            new LegacyLifeCodec(),
            (source, destination) -> {
              calls.incrementAndGet();
              throw failure;
            });

    // When
    assertSame(
        failure,
        assertThrows(AtomicMoveNotSupportedException.class, () -> store.write(target, EMPTY)));

    // Then
    assertEquals(1, calls.get());
    assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
    Files.delete(target);
    assertDirectoryEmpty();
  }

  @Test
  void commitsCompletedSiblingAfterStreamClosed() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var output = new AtomicReference<OutputStream>();
    byte[] encoded = {1, 2, 3, 4};
    var codec =
        new PatternCodec() {
          public PatternData read(InputStream in) {
            throw new UnsupportedOperationException();
          }

          public void write(PatternData data, OutputStream out) throws IOException {
            output.set(out);
            out.write(encoded);
          }
        };
    var moves = new AtomicInteger();
    var store =
        new BoardFileStore(
            codec,
            (source, destination) -> {
              assertEquals(directory.toRealPath(), source.getParent());
              assertEquals(target.toRealPath(), destination);
              assertNotEquals(source, destination);
              assertArrayEquals(encoded, Files.readAllBytes(source));
              assertArrayEquals(ORIGINAL, Files.readAllBytes(destination));
              assertThrows(IOException.class, () -> output.get().write(9));
              moves.incrementAndGet();
              Files.move(
                  source,
                  destination,
                  StandardCopyOption.ATOMIC_MOVE,
                  StandardCopyOption.REPLACE_EXISTING);
            });

    // When
    store.write(target, EMPTY);

    // Then
    assertEquals(1, moves.get());
    assertArrayEquals(encoded, Files.readAllBytes(target));
    Files.delete(target);
    assertDirectoryEmpty();
  }

  @Test
  void cleanupFailureIsSuppressedOnOriginalFailure() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var staged = new AtomicReference<Path>();
    var failure = new IOException("move failed");
    var store =
        new BoardFileStore(
            new LegacyLifeCodec(),
            (source, destination) -> {
              staged.set(source);
              Files.delete(source);
              Files.createDirectory(source);
              Files.write(source.resolve("prevents-deletion"), new byte[] {1});
              throw failure;
            });
    try {
      // When
      assertSame(failure, assertThrows(IOException.class, () -> store.write(target, EMPTY)));

      // Then
      assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
      assertEquals(1, failure.getSuppressed().length);
      var cleanup = assertInstanceOf(DirectoryNotEmptyException.class, failure.getSuppressed()[0]);
      assertEquals(staged.get().toString(), cleanup.getFile());
    } finally {
      if (staged.get() != null) {
        Files.deleteIfExists(staged.get().resolve("prevents-deletion"));
        Files.deleteIfExists(staged.get());
      }
    }
  }

  private void assertDirectoryEmpty() throws IOException {
    try (var files = Files.list(directory)) {
      assertEquals(List.of(), files.toList());
    }
  }
}
