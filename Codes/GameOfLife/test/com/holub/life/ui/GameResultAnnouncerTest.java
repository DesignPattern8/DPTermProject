package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.io.File;
import java.util.*;
import org.junit.jupiter.api.Test;

/** UI-R17~R19 (#67, decision 009): the cancer game's result message. See ui/docs/test-cases.md. */
class GameResultAnnouncerTest {
  private static class IdleTickSource implements TickSource {
    @Override
    public void start(int intervalMillis, Runnable onTick) {}

    @Override
    public void stop() {}
  }

  private static class NoNewCancer extends Random {
    @Override
    public double nextDouble() {
      return 0.99;
    }
  }

  /** Records shown messages; the menu-only methods are not used here. */
  private static class RecordingDialogs implements UserDialogs {
    final List<String> messages = new ArrayList<>();

    @Override
    public File chooseFile(String selectButtonText) {
      throw new AssertionError("not used");
    }

    @Override
    public void showError(String message) {
      throw new AssertionError("not used");
    }

    @Override
    public void showMessage(String message) {
      messages.add(message);
    }
  }

  private static Cell cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return block.childAt(row % 8, column % 8);
  }

  private final Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final GameController controller =
      new GameController(
          new CompositeBoard(board),
          board,
          new Clock(new IdleTickSource()),
          new CancerGame(board, new NoNewCancer()));
  private final RecordingDialogs dialogs = new RecordingDialogs();

  /** Same steps as #60's CAN-C03: two cancer cells, treat both, the next tick wins. */
  private void winTheGame() {
    cellAt(board, 10, 10).setCancer();
    cellAt(board, 10, 11).setCancer();
    controller.tick();
    controller.treat(10, 10);
    controller.tick();
    assertNotNull(controller.gameResultMessage());
  }

  @Test
  void gameOverShowsResultOnce() {
    controller.addGameListener(new GameResultAnnouncer(controller, dialogs, Runnable::run));

    winTheGame();

    assertEquals(List.of(controller.gameResultMessage()), dialogs.messages);
    assertTrue(dialogs.messages.get(0).contains("승리"));
  }

  @Test
  void otherChangesShowNothing() {
    List<Runnable> queued = new ArrayList<>();
    GameResultAnnouncer announcer = new GameResultAnnouncer(controller, dialogs, queued::add);

    announcer.boardChanged(GameListener.Change.EDITED);
    announcer.boardChanged(GameListener.Change.GENERATION);

    assertTrue(queued.isEmpty());
    assertTrue(dialogs.messages.isEmpty());
  }

  @Test
  void messageWaitsForExecutor() {
    List<Runnable> queued = new ArrayList<>();
    controller.addGameListener(new GameResultAnnouncer(controller, dialogs, queued::add));

    winTheGame();

    assertTrue(dialogs.messages.isEmpty());
    assertEquals(1, queued.size());
    queued.get(0).run();
    assertEquals(List.of(controller.gameResultMessage()), dialogs.messages);
  }
}
