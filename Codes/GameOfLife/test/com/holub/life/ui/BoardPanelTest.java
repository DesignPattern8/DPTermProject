package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * UI-R13 (decision 008) and UI-R14~R15 (#67, cancer feature #60): clicks on the board panel. See
 * ui/docs/test-cases.md.
 */
class BoardPanelTest {
  private static class IdleTickSource implements TickSource {
    @Override
    public void start(int intervalMillis, Runnable onTick) {}

    @Override
    public void stop() {}
  }

  @Test
  void clickTogglesCellUnderPointer() {
    Neighborhood cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
    CompositeBoard board = new CompositeBoard(cells);
    GameController controller =
        new GameController(board, cells, new Clock(new IdleTickSource()));
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    BoardPanel panel = new BoardPanel(cells, controller);
    panel.setSize(512, 512);

    panel.dispatchEvent(
        new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0L, 0, 100, 200, 1, false));

    assertEquals(Set.of(new CellPosition(25, 12)), board.snapshot().liveCells());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  /** Never starts new cancer: every probability check fails (same idea as #60's FixedRandom). */
  private static class NoNewCancer extends Random {
    @Override
    public double nextDouble() {
      return 0.99;
    }
  }

  private static Cell cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return block.childAt(row % 8, column % 8);
  }

  private static MouseEvent press(BoardPanel panel, int x, int y, boolean right) {
    return new MouseEvent(
        panel,
        MouseEvent.MOUSE_PRESSED,
        0L,
        right ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK,
        x,
        y,
        1,
        false,
        right ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
  }

  @Test
  void rightClickTreatsCellUnderPointer() {
    Neighborhood cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
    cellAt(cells, 25, 12).setCancer();
    CancerGame cancer = new CancerGame(cells, new NoNewCancer());
    GameController controller =
        new GameController(
            new CompositeBoard(cells), cells, new Clock(new IdleTickSource()), cancer);
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    BoardPanel panel = new BoardPanel(cells, controller);
    panel.setSize(512, 512);

    panel.dispatchEvent(press(panel, 100, 200, true));

    assertFalse(cellAt(cells, 25, 12).isCancer());
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES - 1, cancer.treatmentCharges());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  @Test
  void leftClickTogglesEvenWithCancerRules() {
    Neighborhood cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
    CancerGame cancer = new CancerGame(cells, new NoNewCancer());
    GameController controller =
        new GameController(
            new CompositeBoard(cells), cells, new Clock(new IdleTickSource()), cancer);
    BoardPanel panel = new BoardPanel(cells, controller);
    panel.setSize(512, 512);

    panel.dispatchEvent(press(panel, 100, 200, false));

    assertTrue(cellAt(cells, 25, 12).isAlive());
    assertFalse(cellAt(cells, 25, 12).isCancer());
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES, cancer.treatmentCharges());
  }
}
