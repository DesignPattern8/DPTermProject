package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Timer;
import javax.swing.*;
import org.junit.jupiter.api.DisplayName;
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
      GameController controller = new GameController(new CompositeBoard(board), clock);
      menus.setSelectedPath(new MenuElement[] {new JMenu("Go")});
      GameMenu.onTick(controller);
      assertTrue(board.createMemento().isAlive(new CellPosition(10, 9)));
      menus.clearSelectedPath();
      GameMenu.onTick(controller);
      assertFalse(board.createMemento().isAlive(new CellPosition(10, 9)));
      assertTrue(board.createMemento().isAlive(new CellPosition(9, 10)));
    } finally {
      menus.clearSelectedPath();
      timer.cancel();
    }
  }

  // ---- UI-R30: the clock is wired only through ModeSwitch (decision 011, #102) ----

  @Test
  void gameMenuHasNoDirectClockWiring() {
    // Given the menu class; When looking up the old entry point that also added a clock listener
    // Then it is gone, so nobody can attach a second tick path next to ModeSwitch (006 M2).
    assertThrows(
        NoSuchMethodException.class,
        () ->
            GameMenu.class.getMethod(
                "install", BoardPanel.class, GameController.class, Clock.class));
  }

  // ---- UI-R01~R09: menu entry table and injected dialogs (ui/docs/test-cases.md) ----

  /** Records requested button texts and shown errors; returns a preset file, or nothing when cancelled. */
  private static class RecordingDialogs implements UserDialogs {
    File choice;
    boolean cancelled;
    final List<String> buttons = new ArrayList<>();
    final List<String> errors = new ArrayList<>();

    @Override
    public Optional<File> chooseFile(String selectButtonText) {
      buttons.add(selectButtonText);
      return cancelled ? Optional.empty() : Optional.of(choice);
    }

    @Override
    public void showError(String message) {
      errors.add(message);
    }

    @Override
    public void showMessage(String message) {
      fail("Grid and Go menus never show an information message: " + message);
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
  private final Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final RecordingTickSource ticks = new RecordingTickSource();
  private final Clock clock = new Clock(ticks);
  private final GameController controller = new GameController(new CompositeBoard(board), clock);
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

  private Set<CellPosition> liveCells() {
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
    Set<CellPosition> blinker = liveCells();
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
    Set<CellPosition> stored = liveCells();
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
    board.toggle(5, 6);
    Set<CellPosition> before = liveCells();
    Path file = directory.resolve("bad.life");
    Files.write(file, new byte[] {1, 2, 3, 4});
    dialogs.choice = file.toFile();
    run("Grid", "Load");
    assertEquals(List.of("Read Failed!"), dialogs.errors);
    assertEquals(before, liveCells());
  }

  @Test
  void storeFailureShowsWriteFailed() {
    board.toggle(5, 6);
    Set<CellPosition> before = liveCells();
    dialogs.choice = directory.toFile();
    run("Grid", "Store");
    assertEquals(List.of("Write Failed!"), dialogs.errors);
    assertEquals(before, liveCells());
  }

  @Test
  void cancelDoesNothing() {
    board.toggle(5, 6);
    Set<CellPosition> before = liveCells();
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    dialogs.cancelled = true;
    run("Grid", "Load");
    run("Grid", "Store");
    assertEquals(before, liveCells());
    assertTrue(changes.isEmpty());
    assertEquals(List.of(), dialogs.errors);
  }

  @Test
  void missingChosenFileStillShowsReadFailed() {
    board.toggle(5, 6);
    Set<CellPosition> before = liveCells();
    dialogs.choice = directory.resolve("missing.life").toFile();
    run("Grid", "Load");
    assertEquals(List.of("Read Failed!"), dialogs.errors);
    assertEquals(before, liveCells());
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

  @Test
  @DisplayName("UI-R36 Grid 메뉴의 Clear는 지금 모드의 컨트롤러에 간다")
  void gridEntriesFollowActiveController() {
    Neighborhood singleBoard = new Neighborhood(8, new Neighborhood(8, new Resident()));
    GameController single =
        new GameController(new CompositeBoard(singleBoard), clock);
    board.toggle(1, 1);
    singleBoard.toggle(2, 2);
    GameController[] active = {single};
    List<GameMenu.Entry> routed =
        GameMenu.entries(() -> active[0], clock, dialogs, () -> exits[0]++);
    GameMenu.Entry clear =
        routed.stream().filter(e -> e.label().equals("Clear")).findFirst().orElseThrow();

    clear.action().actionPerformed(null);
    assertFalse(singleBoard.createMemento().isAlive(new CellPosition(2, 2)), "1인용 보드가 빈다");
    assertTrue(board.createMemento().isAlive(new CellPosition(1, 1)), "클래식 보드는 그대로");

    active[0] = controller;
    clear.action().actionPerformed(null);
    assertFalse(board.createMemento().isAlive(new CellPosition(1, 1)), "이제 클래식 보드가 빈다");
  }
}
