package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.awt.Point;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Timer;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameMenuTest {
  private static class RecordingTimer extends Timer {
    TimerTask task;
    long period;

    RecordingTimer() {
      super(true);
    }

    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
      this.task = task;
      this.period = period;
      super.scheduleAtFixedRate(task, 86_400_000, period);
    }
  }

  @Test
  void goCommandsKeepOriginalActions() {
    RecordingTimer timer = new RecordingTimer();
    try {
      Clock clock = new Clock(new UtilTimerTickSource(timer));
      String[] names = {"Agonizing", "Slow", "Medium", "Fast"};
      long[] periods = {500, 150, 70, 30};
      for (int i = 0; i < names.length; i++) {
        GameMenu.go(clock, names[i]);
        assertEquals(periods[i], timer.period);
      }
      GameMenu.go(clock, "Halt");
      assertFalse(timer.task.cancel());
      int[] ticks = {0};
      clock.addClockListener(() -> ticks[0]++);
      GameMenu.go(clock, "Tick (Single Step)");
      assertEquals(1, ticks[0]);
    } finally {
      timer.cancel();
    }
  }

  @Test
  void openMenuSuppressesTick() {
    RecordingTimer timer = new RecordingTimer();
    MenuSelectionManager menus = MenuSelectionManager.defaultManager();
    try {
      Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
      board.toggle(10, 9);
      board.toggle(10, 10);
      board.toggle(10, 11);
      Clock clock = new Clock(new UtilTimerTickSource(timer));
      GameController controller = new GameController(new CompositeBoard(board), board, clock);
      menus.setSelectedPath(new MenuElement[] {new JMenu("Go")});
      GameMenu.onTick(controller);
      assertTrue(board.createMemento().isAlive(new Point(9, 10)));
      menus.clearSelectedPath();
      GameMenu.onTick(controller);
      assertFalse(board.createMemento().isAlive(new Point(9, 10)));
      assertTrue(board.createMemento().isAlive(new Point(10, 9)));
    } finally {
      menus.clearSelectedPath();
      timer.cancel();
    }
  }

  // ---- UI-R01~R09: menu entry table and injected dialogs (ui/docs/test-cases.md) ----

  /** Records requested button texts and shown errors; returns a preset file or throws. */
  private static class RecordingDialogs implements UserDialogs {
    File choice;
    FileNotFoundException failure;
    final List<String> buttons = new ArrayList<>();
    final List<String> errors = new ArrayList<>();

    @Override
    public File chooseFile(String selectButtonText) throws FileNotFoundException {
      buttons.add(selectButtonText);
      if (failure != null) throw failure;
      return choice;
    }

    @Override
    public void showError(String message) {
      errors.add(message);
    }
  }

  /** Records what the clock asked of its tick source instead of scheduling anything. */
  private static class RecordingTickSource implements TickSource {
    final List<String> requests = new ArrayList<>();

    @Override
    public void start(int intervalMillis, Runnable onTick) {
      requests.add("start " + intervalMillis);
    }

    @Override
    public void stop() {
      requests.add("stop");
    }
  }

  @TempDir Path directory;
  private final Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final RecordingTickSource ticks = new RecordingTickSource();
  private final Clock clock = new Clock(ticks);
  private final GameController controller = new GameController(board, clock);
  private final RecordingDialogs dialogs = new RecordingDialogs();
  private final int[] exits = {0};
  private final List<GameMenu.Entry> entries =
      GameMenu.entries(controller, clock, dialogs, () -> exits[0]++);

  private void run(String menu, String label) {
    for (GameMenu.Entry entry : entries)
      if (entry.menu().equals(menu) && entry.label().equals(label)) {
        entry.action().actionPerformed(null);
        return;
      }
    fail("Missing menu entry: " + menu + " / " + label);
  }

  private Set<Point> liveCells() {
    return new HashSet<>(board.createMemento().liveCells());
  }

  @Test
  void menuEntriesKeepOriginalOrder() {
    List<String> actual = new ArrayList<>();
    for (GameMenu.Entry entry : entries) actual.add(entry.menu() + "/" + entry.label());
    assertEquals(
        List.of(
            "Grid/Clear",
            "Grid/Load",
            "Grid/Store",
            "Grid/Exit",
            "Go/Halt",
            "Go/Tick (Single Step)",
            "Go/Agonizing",
            "Go/Slow",
            "Go/Medium",
            "Go/Fast"),
        actual);
  }

  @Test
  void clearEntryClearsBoard() {
    board.toggle(1, 1);
    board.toggle(20, 30);
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    run("Grid", "Clear");
    assertTrue(liveCells().isEmpty());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  void loadEntryLoadsChosenFile() throws Exception {
    board.toggle(10, 9);
    board.toggle(10, 10);
    board.toggle(10, 11);
    Set<Point> blinker = liveCells();
    File file = directory.resolve("blinker.life").toFile();
    controller.store(file);
    board.clear();
    board.toggle(40, 40);
    dialogs.choice = file;
    run("Grid", "Load");
    assertEquals(List.of("Load"), dialogs.buttons);
    assertEquals(blinker, liveCells());
    assertTrue(dialogs.errors.isEmpty());
  }

  @Test
  void storeEntryWritesChosenFile() throws Exception {
    board.toggle(3, 4);
    board.toggle(30, 31);
    board.toggle(60, 2);
    Set<Point> stored = liveCells();
    File file = directory.resolve("out.life").toFile();
    dialogs.choice = file;
    run("Grid", "Store");
    assertEquals(List.of("Write"), dialogs.buttons);
    assertTrue(dialogs.errors.isEmpty());
    board.clear();
    controller.load(file);
    assertEquals(stored, liveCells());
  }

  @Test
  void loadFailureShowsReadFailed() throws Exception {
    Path file = directory.resolve("bad.life");
    Files.write(file, new byte[] {1, 2, 3, 4});
    dialogs.choice = file.toFile();
    run("Grid", "Load");
    assertEquals(List.of("Read Failed!"), dialogs.errors);
  }

  @Test
  void storeFailureShowsWriteFailed() {
    board.toggle(5, 6);
    Set<Point> before = liveCells();
    dialogs.choice = directory.toFile();
    run("Grid", "Store");
    assertEquals(List.of("Write Failed!"), dialogs.errors);
    assertEquals(before, liveCells());
  }

  @Test
  void cancelKeepsOriginalFailureMessage() {
    board.toggle(5, 6);
    Set<Point> before = liveCells();
    dialogs.failure = new FileNotFoundException("No file selected by user");
    run("Grid", "Load");
    run("Grid", "Store");
    assertEquals(before, liveCells());
    assertEquals(List.of("Read Failed!", "Write Failed!"), dialogs.errors);
  }

  @Test
  void exitEntryRunsExitAction() {
    run("Grid", "Exit");
    assertEquals(1, exits[0]);
  }

  @Test
  void goEntriesMapToClockRequests() {
    int[] tickCount = {0};
    clock.addClockListener(() -> tickCount[0]++);
    run("Go", "Agonizing");
    run("Go", "Slow");
    run("Go", "Medium");
    run("Go", "Fast");
    run("Go", "Halt");
    run("Go", "Tick (Single Step)");
    GameMenu.go(clock, "Unknown");
    assertEquals(
        List.of("start 500", "start 150", "start 70", "start 30", "stop", "stop"),
        ticks.requests);
    assertEquals(1, tickCount[0]);
  }
}
