package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
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
      new GameController(new CompositeBoard(root), root, new Clock(ticks));

  @Test
  void preservesTraversalAndCancer() throws Exception {
    root.toggle(1, 9);
    root.toggle(9, 1);
    root.toggle(7, 7);
    var expected = new ArrayList<>(root.createMemento().liveCells());
    Path file = directory.resolve("board.life");
    controller.store(file.toFile());
    try (var in = new ObjectInputStream(Files.newInputStream(file))) {
      assertEquals(expected, in.readObject());
    }
    var cancer = ((Neighborhood) root.childAt(2, 3)).childAt(1, 2);
    cancer.setCancer();
    controller.store(file.toFile());
    controller.clear();
    controller.load(file.toFile());
    assertTrue(cancer.isCancer());
    assertEquals(4, root.createMemento().liveCells().size());
  }

  @Test
  void preservesOpenAndDecodeFailureOrder() throws Exception {
    root.toggle(2, 3);
    var changes = new ArrayList<GameListener.Change>();
    controller.addGameListener(changes::add);
    assertThrows(
        FileNotFoundException.class, () -> controller.load(directory.resolve("missing").toFile()));
    assertEquals(0, ticks.stops);
    assertTrue(root.createMemento().isAlive(new Point(3, 2)));
    assertThrows(IOException.class, () -> controller.store(directory.toFile()));
    assertEquals(0, ticks.stops);
    Path bad = directory.resolve("bad");
    Files.write(bad, new byte[] {1, 2, 3});
    assertThrows(IOException.class, () -> controller.load(bad.toFile()));
    assertEquals(1, ticks.stops);
    assertTrue(root.createMemento().liveCells().isEmpty());
    assertEquals(List.of(GameListener.Change.EDITED, GameListener.Change.EDITED), changes);
  }
}
