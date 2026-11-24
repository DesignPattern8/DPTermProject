package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.awt.Point;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class SnapshotFileStoreTest {
  @TempDir Path directory;
  private static final byte[] ORIGINAL = {10, 20, 30};
  private static final BoardSnapshot EMPTY = new BoardSnapshot(8, 8, Set.of());
  private static final PatternData EMPTY_DATA = new PatternData(List.of(), Set.of());

  @Test
  void readsFilteredCancerSnapshotIntoBoard() throws Exception {
    // Given
    var normal = new CellPosition(0, 0);
    var cancer = new CellPosition(7, 7);
    var outside = new CellPosition(8, 0);
    var data = new PatternData(List.of(normal, cancer, cancer, outside), Set.of(cancer, outside));
    var store = new BoardFileStore(new LegacyLifeCodec());
    Path file = directory.resolve("mixed.life");
    store.write(file, data);
    var board = new CompositeBoard(new Neighborhood(8, new Resident()));

    // When
    board.restore(store.read(file, board.rows(), board.columns()));

    // Then
    assertEquals(new BoardSnapshot(8, 8, Set.of(normal, cancer), Set.of(cancer)), board.snapshot());
  }

  @Test
  void readsExistingClassicFile() throws Exception {
    Path file = directory.resolve("classic.life");
    try (var out = new ObjectOutputStream(Files.newOutputStream(file))) {
      out.writeObject(
          new LinkedList<>(List.of(new Point(4, 2), new Point(4, 2), new Point(-1, 0))));
    }
    var loaded = new BoardFileStore(new LegacyLifeCodec()).read(file, 3, 5);
    assertEquals(new BoardSnapshot(3, 5, Set.of(new CellPosition(2, 4))), loaded);
  }

  @ParameterizedTest
  @CsvSource({"0,8", "8,0", "-1,8", "8,-1"})
  void rejectsInvalidDimensionsBeforeOpening(int rows, int columns) {
    var store = new BoardFileStore(new LegacyLifeCodec());
    assertThrows(
        IllegalArgumentException.class,
        () -> store.read(directory.resolve("missing"), rows, columns));
  }

  @Test
  void reportsUnrepresentableCancerAsIoFailure() throws Exception {
    var store = new BoardFileStore(new LegacyLifeCodec());
    Path file = directory.resolve("invalid.life");
    var data = new PatternData(List.of(), Set.of(new CellPosition(1, 1)));
    store.write(file, data);
    var failure = assertThrows(IOException.class, () -> store.read(file, 8, 8));
    assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    assertEquals(data, store.read(file));
  }

  @ParameterizedTest
  @ValueSource(strings = {"success", "decode", "convert"})
  void closesInputOnEveryReadOutcome(String outcome) throws Exception {
    var codec =
        new PatternCodec() {
          InputStream input;

          public PatternData read(InputStream in) throws IOException {
            input = in;
            if (outcome.equals("decode")) throw new IOException("decode failed");
            return outcome.equals("convert")
                ? new PatternData(List.of(), Set.of(new CellPosition(1, 1)))
                : EMPTY_DATA;
          }

          public void write(PatternData data, OutputStream out) {
            fail("unexpected write");
          }
        };
    Path file = Files.write(directory.resolve("read.life"), new byte[] {1});
    var store = new BoardFileStore(codec);
    if (outcome.equals("success")) assertEquals(EMPTY, store.read(file, 8, 8));
    else assertThrows(IOException.class, () -> store.read(file, 8, 8));
    assertThrows(IOException.class, () -> codec.input.read());
  }

  @Test
  void preservesCodecFailures() throws Exception {
    for (Exception failure :
        List.of(new IOException("decode"), new IllegalArgumentException("codec"))) {
      var codec =
          new PatternCodec() {
            public PatternData read(InputStream in) throws IOException {
              if (failure instanceof IOException io) throw io;
              throw (RuntimeException) failure;
            }

            public void write(PatternData data, OutputStream out) {
              fail("unexpected write");
            }
          };
      Path file = Files.write(directory.resolve("read.life"), new byte[] {1});
      assertSame(
          failure,
          assertThrows(failure.getClass(), () -> new BoardFileStore(codec).read(file, 8, 8)));
    }
  }

  static Stream<Arguments> layouts() {
    return Stream.of(
        Arguments.of(new Neighborhood(8, new Neighborhood(8, new Resident())), List.of(8, 1)),
        Arguments.of(new Neighborhood(2, new Neighborhood(4, new Resident())), List.of(4, 1)),
        Arguments.of(new Neighborhood(8, new Resident()), List.of(1)),
        Arguments.of(
            new Neighborhood(2, new Neighborhood(3, new Neighborhood(2, new Resident()))),
            List.of(6, 2, 1)),
        Arguments.of(new Neighborhood(1, new Resident()), List.of(1)));
  }

  @ParameterizedTest
  @MethodSource("layouts")
  void matchesLegacyTraversalForEachLayout(Neighborhood root, List<Integer> widths)
      throws Exception {
    // Given: compare every position with the original tree traversal, not a second comparator.
    for (int row = 0; row < root.widthInCells(); row++) {
      for (int column = 0; column < root.widthInCells(); column++) root.toggle(row, column);
    }
    var expected =
        root.createMemento().liveCells().stream()
            .map(cell -> new Point(cell.column(), cell.row()))
            .toList();
    var board = new CompositeBoard(root);
    var store = new BoardFileStore(new LegacyLifeCodec(), widths);
    Path file = directory.resolve("ordered.life");

    // When
    store.writeSnapshot(file, board::snapshot);

    // Then
    try (var in = new ObjectInputStream(Files.newInputStream(file))) {
      assertEquals(expected, in.readObject());
    }
  }

  @Test
  void roundTripsCancerWithoutChangingBoard() throws Exception {
    var root = new Neighborhood(8, new Neighborhood(8, new Resident()));
    root.toggle(1, 9);
    root.toggle(9, 1);
    var block = (Neighborhood) root.childAt(0, 0);
    var cancer = (Resident) block.childAt(7, 7);
    cancer.setCancer();
    var expectedOrder = new ArrayList<>(root.createMemento().liveCells());
    var board = new CompositeBoard(root);
    var expected = board.snapshot();
    var store = new BoardFileStore(new LegacyLifeCodec(), List.of(8, 1));
    Path file = directory.resolve("cancer.life");

    store.writeSnapshot(file, board::snapshot);
    var restored = new CompositeBoard(new Neighborhood(8, new Neighborhood(8, new Resident())));
    restored.restore(store.read(file, 64, 64));

    assertEquals(expected, restored.snapshot());
    assertEquals(expected, board.snapshot());
    assertEquals(expectedOrder, store.read(file).cells());
  }

  @Test
  void roundTripsEmptySnapshot() throws Exception {
    var store = configuredStore();
    Path file = directory.resolve("empty.life");
    store.writeSnapshot(file, () -> EMPTY);
    assertEquals(EMPTY, store.read(file, 8, 8));
    try (var in = new ObjectInputStream(Files.newInputStream(file))) {
      assertEquals(List.of(), in.readObject());
    }
  }

  @Test
  void copiesLayoutBeforeCallerMutation() throws Exception {
    var widths = new ArrayList<>(List.of(4, 1));
    var store = new BoardFileStore(new LegacyLifeCodec(), widths);
    widths.clear();
    widths.add(1);
    var first = new CellPosition(3, 3);
    var second = new CellPosition(0, 4);
    Path file = directory.resolve("copied.life");
    store.writeSnapshot(file, () -> new BoardSnapshot(8, 8, Set.of(first, second)));
    assertEquals(List.of(first, second), store.read(file).cells());
  }

  static Stream<List<Integer>> invalidLayouts() {
    return Stream.of(
        List.of(),
        List.of(0, 1),
        List.of(-2, 1),
        List.of(1, 2, 1),
        List.of(4, 4, 1),
        List.of(6, 4, 1),
        List.of(4, 2),
        List.of(Integer.MAX_VALUE, 2, 1));
  }

  @ParameterizedTest
  @MethodSource("invalidLayouts")
  void rejectsInvalidLayouts(List<Integer> widths) {
    assertThrows(
        IllegalArgumentException.class, () -> new BoardFileStore(new LegacyLifeCodec(), widths));
  }

  @Test
  void rejectsNullLayouts() {
    assertThrows(
        NullPointerException.class,
        () -> new BoardFileStore(new LegacyLifeCodec(), (List<Integer>) null));
    assertThrows(
        NullPointerException.class,
        () -> new BoardFileStore(new LegacyLifeCodec(), Arrays.asList(4, null, 1)));
  }

  @Test
  void requiresExplicitLayoutForSnapshotWrites() throws Exception {
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    assertThrows(
        IllegalStateException.class,
        () ->
            new BoardFileStore(new LegacyLifeCodec())
                .writeSnapshot(
                    target,
                    () -> {
                      fail("capture must not run without layout");
                      return EMPTY;
                    }));
    assertPreservedAndClean(target);
  }

  @ParameterizedTest
  @CsvSource({"6,8", "8,6", "1,1"})
  void rejectsIncompatibleDimensionsWithoutReplacingTarget(int rows, int columns) throws Exception {
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            configuredStore()
                .writeSnapshot(target, () -> new BoardSnapshot(rows, columns, Set.of())));
    assertPreservedAndClean(target);
  }

  @Test
  void capturesOnceAfterStagingBeforeEncoding() throws Exception {
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var events = new ArrayList<String>();
    var captures = new AtomicInteger();
    var codec =
        new PatternCodec() {
          public PatternData read(InputStream in) {
            return EMPTY_DATA;
          }

          public void write(PatternData data, OutputStream out) throws IOException {
            events.add("encode");
            assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
            out.write(42);
          }
        };
    var store = new BoardFileStore(codec, List.of(4, 1));

    store.writeSnapshot(
        target,
        () -> {
          captures.incrementAndGet();
          events.add("capture");
          try (var files = Files.list(directory)) {
            assertEquals(
                1, files.filter(p -> p.getFileName().toString().startsWith(".life-")).count());
            assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
          } catch (IOException failure) {
            throw new UncheckedIOException(failure);
          }
          return EMPTY;
        });

    assertEquals(1, captures.get());
    assertEquals(List.of("capture", "encode"), events);
    assertArrayEquals(new byte[] {42}, Files.readAllBytes(target));
    assertOnlyTarget(target);
  }

  @Test
  void doesNotCaptureWhenPreparationFails() {
    var store = configuredStore();
    for (Path invalid : List.of(directory, directory.resolve("missing/board.life"))) {
      assertThrows(
          IOException.class,
          () ->
              store.writeSnapshot(
                  invalid,
                  () -> {
                    fail("capture must follow preparation");
                    return EMPTY;
                  }));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void captureFailurePreservesTargetAndCleansStage(boolean exists) throws Exception {
    Path target = directory.resolve("board.life");
    if (exists) Files.write(target, ORIGINAL);
    var failure = new IllegalStateException("capture failed");
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () ->
                configuredStore()
                    .writeSnapshot(
                        target,
                        () -> {
                          throw failure;
                        })));
    if (exists) assertPreservedAndClean(target);
    else
      try (var files = Files.list(directory)) {
        assertEquals(List.of(), files.toList());
      }
  }

  @Test
  void encodingFailurePreservesTargetAndClosesOutput() throws Exception {
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var failure = new IOException("encode failed");
    var codec =
        new PatternCodec() {
          OutputStream output;

          public PatternData read(InputStream in) {
            return EMPTY_DATA;
          }

          public void write(PatternData data, OutputStream out) throws IOException {
            output = out;
            out.write(1);
            throw failure;
          }
        };
    var store = new BoardFileStore(codec, List.of(4, 1));
    assertSame(
        failure, assertThrows(IOException.class, () -> store.writeSnapshot(target, () -> EMPTY)));
    assertPreservedAndClean(target);
    assertThrows(IOException.class, () -> codec.output.write(2));
  }

  @Test
  void rejectsNullCaptureWithoutReplacingTarget() throws Exception {
    Path target = Files.write(directory.resolve("board.life"), ORIGINAL);
    var store = configuredStore();
    assertThrows(NullPointerException.class, () -> store.writeSnapshot(target, null));
    assertPreservedAndClean(target);
    assertThrows(NullPointerException.class, () -> store.writeSnapshot(target, () -> null));
    assertPreservedAndClean(target);
  }

  @Test
  void keepsRawApiIndependentOfLayout() throws Exception {
    var outside = new CellPosition(-1, 20);
    var data = new PatternData(List.of(outside, new CellPosition(0, 4), outside), Set.of());
    var store = configuredStore();
    Path file = directory.resolve("raw.life");
    store.write(file, data);
    assertEquals(data, store.read(file));
  }

  private BoardFileStore configuredStore() {
    return new BoardFileStore(new LegacyLifeCodec(), List.of(4, 1));
  }

  private void assertPreservedAndClean(Path target) throws IOException {
    assertArrayEquals(ORIGINAL, Files.readAllBytes(target));
    assertOnlyTarget(target);
  }

  private void assertOnlyTarget(Path target) throws IOException {
    try (var files = Files.list(directory)) {
      assertEquals(List.of(target), files.toList());
    }
  }
}
