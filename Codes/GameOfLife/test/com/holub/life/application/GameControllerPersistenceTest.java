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

class GameControllerPersistenceTest {
  @TempDir Path directory;
  private final Neighborhood root = new Neighborhood(8, new Neighborhood(8, new Resident()));

  private final class ManualTicks implements TickSource {
    int stops;

    public void start(int millis, Runnable tick) {}

    public void stop() {
      stops++;
    }
  }

  private final ManualTicks ticks = new ManualTicks();
  private final GameController controller =
      new GameController(new CompositeBoard(root), new Clock(ticks));

  @Test
  void preservesTraversalAndCancer() throws Exception {
    root.toggle(1, 9);
    root.toggle(9, 1);
    root.toggle(7, 7);
    var expected =
        root.createMemento().liveCells().stream()
            .map(cell -> new Point(cell.column(), cell.row()))
            .toList();
    Path file = directory.resolve("board.life");
    controller.store(file.toFile());
    try (var in = new ObjectInputStream(Files.newInputStream(file))) {
      assertEquals(expected, in.readObject());
    }
    Resident cancer = (Resident) ((Neighborhood) root.childAt(2, 3)).childAt(1, 2);
    cancer.setCancer();
    controller.store(file.toFile());
    controller.clear();
    controller.load(file.toFile());
    assertTrue(cancer.isCancer());
    assertEquals(4, root.createMemento().liveCells().size());
  }

  @Test
  void failedStorePreservesFileAndBoard() throws Exception {
    // Given
    root.toggle(1, 2);
    Resident cancer = (Resident) ((Neighborhood) root.childAt(0, 0)).childAt(3, 4);
    cancer.setCancer();
    var before = PersistenceMapper.capture(root.createMemento());
    var original = new byte[] {10, 20, 30};
    Path target = Files.write(directory.resolve("board.life"), original);
    var failure = new IOException("partial write failed");
    var codec =
        new PatternCodec() {
          public PatternData read(InputStream in) {
            throw new UnsupportedOperationException();
          }

          public void write(PatternData data, OutputStream out) throws IOException {
            out.write(1);
            throw failure;
          }
        };
    var controller =
        new GameController(
            new CompositeBoard(root),
            new Clock(ticks),
            null,
            new BoardFileStore(codec, List.of(8, 1)));
    var changes = new ArrayList<GameListener.Change>();
    controller.addGameListener(changes::add);

    // When
    assertSame(failure, assertThrows(IOException.class, () -> controller.store(target.toFile())));

    // Then
    assertArrayEquals(original, Files.readAllBytes(target));
    assertEquals(before, PersistenceMapper.capture(root.createMemento()));
    assertEquals(1, ticks.stops);
    assertTrue(changes.isEmpty());
    try (var files = Files.list(directory)) {
      assertEquals(List.of(target), files.toList());
    }
  }

  @Test
  void preservesOpenAndDecodeFailureOrder() throws Exception {
    root.toggle(2, 3);
    var changes = new ArrayList<GameListener.Change>();
    controller.addGameListener(changes::add);
    assertThrows(
        FileNotFoundException.class, () -> controller.load(directory.resolve("missing").toFile()));
    assertEquals(0, ticks.stops);
    assertTrue(root.createMemento().isAlive(new CellPosition(2, 3)));
    assertThrows(IOException.class, () -> controller.store(directory.toFile()));
    assertEquals(0, ticks.stops);
    Path bad = directory.resolve("bad");
    Files.write(bad, new byte[] {1, 2, 3});
    assertThrows(IOException.class, () -> controller.load(bad.toFile()));
    assertEquals(0, ticks.stops);
    assertEquals(List.of(new CellPosition(2, 3)), root.createMemento().liveCells());
    assertTrue(changes.isEmpty());
  }
}
