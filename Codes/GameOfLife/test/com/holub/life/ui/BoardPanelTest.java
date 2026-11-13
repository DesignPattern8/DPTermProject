package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.application.*;
import com.holub.life.model.*;
import java.awt.event.MouseEvent;
import java.util.*;
import org.junit.jupiter.api.Test;

/** UI-R13 (ui/docs/test-cases.md): a click toggles the cell under the pointer, decision 008. */
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
}
