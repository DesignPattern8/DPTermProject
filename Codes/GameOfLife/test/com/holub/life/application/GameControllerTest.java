package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.awt.Point;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class GameControllerTest {
  @TempDir Path directory;
  private Timer timer;
  private Cell board;
  private GameController controller;

  @BeforeEach
  void setUp() {
    timer = new Timer(true);
    board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    controller = new GameController(board, new Clock(timer));
  }

  @AfterEach
  void cleanUp() {
    timer.cancel();
  }

  @Test
  void tickAdvancesOneGeneration() {
    board.toggle(10, 9);
    board.toggle(10, 10);
    board.toggle(10, 11);
    controller.tick();
    assertEquals(
        Set.of(new Point(10, 9), new Point(10, 10), new Point(10, 11)),
        new HashSet<>(board.createMemento().liveCells()));
  }

  @Test
  void missingFileKeepsBoard() {
    board.toggle(5, 5);
    assertThrows(
        FileNotFoundException.class,
        () -> controller.load(directory.resolve("missing.life").toFile()));
    assertTrue(board.createMemento().isAlive(new Point(5, 5)));
  }

  @Test
  void invalidFileClearsBoard() throws Exception {
    board.toggle(5, 5);
    Path file = directory.resolve("bad.life");
    Files.write(file, new byte[] {1, 2, 3, 4});
    assertThrows(IOException.class, () -> controller.load(file.toFile()));
    assertTrue(board.createMemento().liveCells().isEmpty());
  }

  @Test
  void storeWritesEachCoordinateOnceAndRoundTrip() throws Exception {
    board.toggle(5, 6);
    Path file = directory.resolve("board.life");
    controller.store(file.toFile());
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
      assertEquals(List.of(new Point(6, 5)), in.readObject());
    }
    controller.clear();
    controller.load(file.toFile());
    assertEquals(List.of(new Point(6, 5)), board.createMemento().liveCells());
  }

  @Test
  void invalidOutputDoesNotClearBoard() {
    board.toggle(5, 5);
    assertThrows(IOException.class, () -> controller.store(directory.toFile()));
    assertTrue(board.createMemento().isAlive(new Point(5, 5)));
  }
}
