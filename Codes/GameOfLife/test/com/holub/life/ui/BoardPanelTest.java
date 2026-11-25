package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
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
    GameController controller = new GameController(board, new Clock(new IdleTickSource()));
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

  private static Resident cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return (Resident) block.childAt(row % 8, column % 8);
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
    // More than CancerGame.MIN_CANCER_COUNT_FOR_TREATMENT cells far from (25, 12), so the
    // treatment is allowed and does not end the game.
    for (int r = 54; r <= 56; r++) for (int c = 54; c <= 57; c++) cellAt(cells, r, c).setCancer();
    CancerGame cancer = new CancerGame(cells, new NoNewCancer());
    GameController controller =
        new GameController(new CompositeBoard(cells), new Clock(new IdleTickSource()), cancer);
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
        new GameController(new CompositeBoard(cells), new Clock(new IdleTickSource()), cancer);
    BoardPanel panel = new BoardPanel(cells, controller);
    panel.setSize(512, 512);

    panel.dispatchEvent(press(panel, 100, 200, false));

    assertTrue(cellAt(cells, 25, 12).isAlive());
    assertFalse(cellAt(cells, 25, 12).isCancer());
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES, cancer.treatmentCharges());
  }

  @Test
  @DisplayName("UI-R31 클래식 모드에서는 원본처럼 오른쪽 클릭도 셀을 토글한다")
  void rightClickTogglesInClassicMode() {
    Neighborhood cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
    CompositeBoard board = new CompositeBoard(cells);
    GameController controller =
        new GameController(board, new Clock(new IdleTickSource()));
    List<GameListener.Change> changes = new ArrayList<>();
    controller.addGameListener(changes::add);
    BoardPanel panel = new BoardPanel(cells, controller);
    panel.setSize(512, 512);

    panel.dispatchEvent(press(panel, 100, 200, true));

    assertEquals(Set.of(new CellPosition(25, 12)), board.snapshot().liveCells());
    assertEquals(List.of(GameListener.Change.EDITED), changes);
  }

  /** UI-R29 (#62, decision 010). */
  @Test
  void panelClickPlacesPresetAndRestoresCursor() {
    Neighborhood cells = new Neighborhood(8, new Neighborhood(8, new Resident()));
    CompositeBoard board = new CompositeBoard(cells);
    GameController controller = new GameController(board, new Clock(new IdleTickSource()));
    UserDialogs dialogs =
        new UserDialogs() {
          @Override
          public Optional<File> chooseFile(String selectButtonText) {
            throw new AssertionError("not used");
          }

          @Override
          public void showError(String message) {
            fail("unexpected error: " + message);
          }

          @Override
          public void showMessage(String message) {
            throw new AssertionError("not used");
          }
        };
    PresetPlacement placement = new PresetPlacement(controller, dialogs);
    BoardPanel panel = new BoardPanel(cells, controller, placement);
    panel.setSize(512, 512);

    placement.select("glider");
    assertEquals(Cursor.CROSSHAIR_CURSOR, panel.getCursor().getType());
    panel.dispatchEvent(press(panel, 84, 84, false));

    assertEquals(Cursor.DEFAULT_CURSOR, panel.getCursor().getType());
    assertEquals(
        Set.of(
            new CellPosition(10, 11),
            new CellPosition(11, 12),
            new CellPosition(12, 10),
            new CellPosition(12, 11),
            new CellPosition(12, 12)),
        board.snapshot().liveCells());
  }
}
