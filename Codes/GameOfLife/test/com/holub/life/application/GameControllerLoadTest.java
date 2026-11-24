package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import com.holub.life.persistence.*;
import java.awt.Point;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameControllerLoadTest {
  @TempDir Path directory;
  private final Neighborhood root = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final CompositeBoard board = new CompositeBoard(root);
  private final ManualTicks ticks = new ManualTicks();
  private final Clock clock = new Clock(ticks);
  private final List<GameListener.Change> changes = new ArrayList<>();
  private final GameController controller = controller(new LegacyLifeCodec());

  private GameController controller(PatternCodec codec) {
    var result = new GameController(board, clock, null, new BoardFileStore(codec));
    result.addGameListener(changes::add);
    return result;
  }

  private static final class ManualTicks implements TickSource {
    int starts;
    int stops;
    int interval;
    Runnable scheduled;

    @Override
    public void start(int intervalMillis, Runnable tick) {
      starts++;
      interval = intervalMillis;
      scheduled = tick;
    }

    @Override
    public void stop() {
      stops++;
      scheduled = null;
    }

    void fire() {
      assertNotNull(scheduled, "running clock lost its scheduled callback");
      scheduled.run();
    }
  }

  @Test
  void malformedInputsPreserveCellsAndCancer() throws Exception {
    // Given: a valid serialized prefix followed by an invalid entry, and broken streams.
    seedMixedBoard();
    var before = board.snapshot();
    byte[] valid = serialized(new LinkedList<>(List.of(new Point(6, 5))));
    List<byte[]> inputs =
        List.of(
            new byte[0],
            new byte[] {1, 2, 3, 4},
            Arrays.copyOf(valid, valid.length / 2),
            serialized(new LinkedList<>(List.of(new Point(6, 5), "not a cell"))));
    for (byte[] bytes : inputs) {
      Path file = Files.write(directory.resolve("bad.life"), bytes);
      // When
      assertThrows(IOException.class, () -> controller.load(file.toFile()));
      // Then: snapshot() observes without the legacy STORE traversal's activation side effects.
      assertEquals(before, board.snapshot());
      assertTrue(cell(1, 1).isCancer());
      assertFalse(cell(2, 2).isCancer());
    }
  }

  @Test
  void failedLoadKeepsRunningClockAndNextGeneration() throws Exception {
    // Given: blinker crossing a Neighborhood boundary, driven only by a manual tick.
    seedBlinker();
    clock.addClockListener(controller::tick);
    clock.start(Speed.FAST);
    Runnable original = ticks.scheduled;
    Path file = Files.write(directory.resolve("bad.life"), new byte[] {1});
    // When
    assertThrows(IOException.class, () -> controller.load(file.toFile()));
    // Then
    assertSame(original, ticks.scheduled);
    assertEquals(1, ticks.starts);
    assertEquals(0, ticks.stops);
    assertEquals(Speed.FAST.millis(), ticks.interval);
    assertTrue(changes.isEmpty());
    ticks.fire();
    assertEquals(verticalBlinker(), board.snapshot().liveCells());
    assertEquals(List.of(GameListener.Change.GENERATION), changes);
  }

  @Test
  void failedLoadKeepsStoppedClock() throws Exception {
    // Given
    Path file = Files.write(directory.resolve("bad.life"), new byte[] {1});
    // When
    assertThrows(IOException.class, () -> controller.load(file.toFile()));
    // Then
    assertNull(ticks.scheduled);
    assertEquals(0, ticks.starts);
    assertEquals(0, ticks.stops);
  }

  @Test
  void missingFilePreservesStateWithoutNotification() {
    // Given
    seedMixedBoard();
    var before = board.snapshot();
    clock.start(Speed.SLOW);
    Runnable original = ticks.scheduled;
    // When
    assertThrows(
        FileNotFoundException.class,
        () -> controller.load(directory.resolve("missing.life").toFile()));
    // Then
    assertEquals(before, board.snapshot());
    assertTrue(cell(1, 1).isCancer());
    assertSame(original, ticks.scheduled);
    assertEquals(0, ticks.stops);
    assertTrue(changes.isEmpty());
  }

  @Test
  void decodeFailureClosesInputAndPropagatesOriginalException() throws Exception {
    // Given
    seedMixedBoard();
    var before = board.snapshot();
    var failure = new IOException("read failed after a prefix");
    InputStream[] opened = new InputStream[1];
    var failing =
        controller(
            new ReadOnlyCodec() {
              @Override
              public PatternData read(InputStream in) throws IOException {
                opened[0] = in;
                assertEquals(7, in.read());
                throw failure;
              }
            });
    Path file = Files.write(directory.resolve("partial.life"), new byte[] {7, 8});
    // When
    assertSame(failure, assertThrows(IOException.class, () -> failing.load(file.toFile())));
    // Then
    assertThrows(IOException.class, () -> opened[0].read());
    assertEquals(before, board.snapshot());
    assertTrue(cell(1, 1).isCancer());
  }

  @Test
  void successfulLoadAppliesOnlyAfterDecodingAndClosingInput() throws Exception {
    // Given
    seedMixedBoard();
    var before = board.snapshot();
    clock.start(Speed.MEDIUM);
    var incoming = new PatternData(List.of(new CellPosition(5, 6)), Set.of());
    InputStream[] opened = new InputStream[1];
    BoardSnapshot[] duringRead = new BoardSnapshot[1];
    boolean[] runningDuringRead = new boolean[1];
    boolean[] cancerDuringRead = new boolean[1];
    var loading =
        controller(
            new ReadOnlyCodec() {
              @Override
              public PatternData read(InputStream in) {
                opened[0] = in;
                duringRead[0] = board.snapshot();
                runningDuringRead[0] = ticks.scheduled != null;
                cancerDuringRead[0] = cell(1, 1).isCancer();
                return incoming;
              }
            });
    loading.addGameListener(
        change -> {
          assertThrows(IOException.class, () -> opened[0].read());
          assertEquals(Set.copyOf(incoming.cells()), board.snapshot().liveCells());
          assertEquals(0, root.cancerCount());
          assertNull(ticks.scheduled);
        });
    Path file = Files.write(directory.resolve("valid.life"), new byte[] {7});
    // When
    loading.load(file.toFile());
    // Then
    assertEquals(before, duringRead[0], "board changed before decoding completed");
    assertTrue(runningDuringRead[0], "clock stopped before decoding completed");
    assertTrue(cancerDuringRead[0]);
    assertEquals(1, ticks.stops);
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  void emptyPatternClearsBoardSuccessfully() throws Exception {
    // Given
    seedMixedBoard();
    clock.start(Speed.FAST);
    Path file = patternFile(new PatternData(List.of(), Set.of()));
    // When
    controller.load(file.toFile());
    // Then
    assertTrue(board.snapshot().liveCells().isEmpty());
    assertEquals(0, root.cancerCount());
    assertEquals(1, ticks.stops);
    assertNull(ticks.scheduled);
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  void cancerPatternReplacesExistingCellKinds() throws Exception {
    // Given
    seedMixedBoard();
    var first = new CellPosition(1, 1);
    var second = new CellPosition(2, 2);
    Path file = patternFile(new PatternData(List.of(first, second), Set.of(second)));
    // When
    controller.load(file.toFile());
    // Then
    assertEquals(Set.of(first, second), board.snapshot().liveCells());
    assertFalse(cell(1, 1).isCancer());
    assertTrue(cell(2, 2).isCancer());
    assertEquals(1, root.cancerCount());
  }

  @Test
  void loadedBoundaryPatternKeepsLegacyCoordinatePolicy() throws Exception {
    // Given
    seedMixedBoard();
    Path file =
        patternFile(
            new PatternData(
                List.of(
                    new CellPosition(-1, 0),
                    new CellPosition(64, 0),
                    new CellPosition(7, 7),
                    new CellPosition(7, 8),
                    new CellPosition(7, 8),
                    new CellPosition(7, 9)),
                Set.of()));
    // When
    controller.load(file.toFile());
    // Then
    assertEquals(
        Set.of(new CellPosition(7, 7), new CellPosition(7, 8), new CellPosition(7, 9)),
        board.snapshot().liveCells());
  }

  private void seedMixedBoard() {
    cell(1, 1).setCancer();
    root.toggle(2, 2);
  }

  private void seedBlinker() {
    root.toggle(7, 7);
    root.toggle(7, 8);
    root.toggle(7, 9);
  }

  private Set<CellPosition> verticalBlinker() {
    return Set.of(new CellPosition(6, 8), new CellPosition(7, 8), new CellPosition(8, 8));
  }

  private Resident cell(int row, int column) {
    Neighborhood block = (Neighborhood) root.childAt(row / 8, column / 8);
    return (Resident) block.childAt(row % 8, column % 8);
  }

  private Path patternFile(PatternData data) throws IOException {
    Path file = directory.resolve("valid.life");
    new BoardFileStore(new LegacyLifeCodec()).write(file, data);
    return file;
  }

  private static byte[] serialized(Object data) throws IOException {
    var bytes = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bytes)) {
      out.writeObject(data);
    }
    return bytes.toByteArray();
  }

  private abstract static class ReadOnlyCodec implements PatternCodec {
    @Override
    public void write(PatternData data, OutputStream out) {
      throw new AssertionError("Load must not write");
    }
  }
}
