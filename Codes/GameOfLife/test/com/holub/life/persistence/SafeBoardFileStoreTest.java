package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.holub.life.model.CellPosition;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafeBoardFileStoreTest {
  @TempDir Path directory;
  private static final byte[] ORIGINAL = {10, 20, 30, 40};
  private static final PatternData DATA =
      new PatternData(List.of(new CellPosition(2, 3)), Set.of());

  @Test
  void captureFailurePreservesExistingFile() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var failure = new IllegalStateException("capture failed");
    var store = new BoardFileStore(new LegacyLifeCodec());

    // When
    var thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                store.write(
                    target,
                    () -> {
                      throw failure;
                    }));

    // Then
    assertSame(failure, thrown);
    assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
    assertOnlyTarget(target);
  }

  @Test
  void partialWriteFailurePreservesExistingFile() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var failure = new IOException("disk write failed");
    var store = new BoardFileStore(failingCodec(failure));

    // When
    assertSame(failure, assertThrows(IOException.class, () -> store.write(target, DATA)));

    // Then
    assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
    assertOnlyTarget(target);
  }

  @Test
  void failedNewSaveLeavesNoFiles() throws Exception {
    // Given
    Path target = directory.resolve("new.life");
    var store = new BoardFileStore(failingCodec(new IOException("disk full")));

    // When
    assertThrows(IOException.class, () -> store.write(target, DATA));

    // Then
    try (var paths = Files.list(directory)) {
      assertEquals(List.of(), paths.toList());
    }
  }

  @Test
  void targetRemainsReadableWhileEncoding() throws Exception {
    // Given
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    PatternCodec codec =
        new PatternCodec() {
          public PatternData read(InputStream in) {
            throw new UnsupportedOperationException();
          }

          public void write(PatternData data, OutputStream out) throws IOException {
            out.write(new byte[] {1, 2, 3});
            assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
          }
        };

    // When
    new BoardFileStore(codec).write(target, DATA);

    // Then
    assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(target));
    assertOnlyTarget(target);
  }

  @Test
  void successfulSaveReplacesOrCreatesOnlyTarget() throws Exception {
    // Given
    var store = new BoardFileStore(new LegacyLifeCodec());
    Path target = directory.resolve("board.life");

    // When: first create, then replace a longer existing file.
    store.write(target, DATA);
    assertEquals(DATA, store.read(target));
    Files.write(target, new byte[4096]);
    store.write(target, DATA);

    // Then
    assertEquals(DATA, store.read(target));
    assertOnlyTarget(target);
  }

  @Test
  void preservesExistingPosixPermissions() throws Exception {
    // Given
    assumeTrue(Files.getFileAttributeView(directory, PosixFileAttributeView.class) != null);
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var permissions = PosixFilePermissions.fromString("rw-r-----");
    Files.setPosixFilePermissions(target, permissions);

    // When
    new BoardFileStore(new LegacyLifeCodec()).write(target, DATA);

    // Then
    assertEquals(permissions, Files.getPosixFilePermissions(target));
    assertOnlyTarget(target);
  }

  @Test
  void rejectsSymbolicLinksBeforeCapture() throws Exception {
    // Given
    Path real = Files.write(directory.resolve("real.life"), ORIGINAL);
    Path link = directory.resolve("link.life");
    try {
      Files.createSymbolicLink(link, real.getFileName());
    } catch (UnsupportedOperationException | FileSystemException e) {
      assumeTrue(false, "Symbolic links unavailable: " + e);
    }
    var store = new BoardFileStore(new LegacyLifeCodec());

    // When / Then: both a valid link and the same link after its referent disappears.
    assertThrows(
        IOException.class,
        () ->
            store.write(
                link,
                () -> {
                  fail("capture on symbolic link");
                  return DATA;
                }));
    assertTrue(Files.isSymbolicLink(link));
    assertArrayEquals(ORIGINAL, Files.readAllBytes(real));
    Files.delete(real);
    assertThrows(
        IOException.class,
        () ->
            store.write(
                link,
                () -> {
                  fail("capture on dangling link");
                  return DATA;
                }));
    assertTrue(Files.isSymbolicLink(link));
    assertFalse(Files.exists(real));
    assertOnlyTarget(link);
  }

  @Test
  void rejectsInvalidTargetsBeforeCapture() throws Exception {
    // Given
    Path parentFile = Files.write(directory.resolve("parent-file"), ORIGINAL);
    var store = new BoardFileStore(new LegacyLifeCodec());

    // When / Then
    for (Path target :
        List.of(
            directory, directory.resolve("missing/board.life"), parentFile.resolve("board.life"))) {
      assertThrows(
          IOException.class,
          () ->
              store.write(
                  target,
                  () -> {
                    fail("capture before validating path");
                    return DATA;
                  }));
    }
    assertArrayEquals(ORIGINAL, Files.readAllBytes(parentFile));
    assertOnlyTarget(parentFile);
  }

  @Test
  void rejectsReadOnlyTargetBeforeCapture() throws Exception {
    // Given
    assumeTrue(Files.getFileAttributeView(directory, PosixFileAttributeView.class) != null);
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var permissions = PosixFilePermissions.fromString("r--r-----");
    Files.setPosixFilePermissions(target, permissions);
    try {
      // When
      assertThrows(
          IOException.class,
          () ->
              new BoardFileStore(new LegacyLifeCodec())
                  .write(
                      target,
                      () -> {
                        fail("capture on read-only file");
                        return DATA;
                      }));
      // Then
      assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
      assertEquals(permissions, Files.getPosixFilePermissions(target));
      assertOnlyTarget(target);
    } finally {
      Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
    }
  }

  private static PatternCodec failingCodec(IOException failure) {
    return new PatternCodec() {
      public PatternData read(InputStream in) {
        throw new UnsupportedOperationException();
      }

      public void write(PatternData data, OutputStream out) throws IOException {
        out.write(new byte[] {1, 2, 3});
        throw failure;
      }
    };
  }

  private void assertOnlyTarget(Path target) throws IOException {
    try (var paths = Files.list(directory)) {
      assertEquals(List.of(target), paths.toList());
    }
  }
}
