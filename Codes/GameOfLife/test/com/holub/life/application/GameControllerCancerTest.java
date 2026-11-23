package com.holub.life.application;

import static com.holub.life.application.CancerGameTest.NO_NEW_CANCER;
import static com.holub.life.application.CancerGameTest.cancerBlock;
import static com.holub.life.application.CancerGameTest.cellAt;
import static com.holub.life.application.CancerGameTest.distantCancerBlock;
import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import java.awt.Point;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameControllerCancerTest {
  /** Manual TickSource that only records whether the clock is running. */
  private static final class ManualTickSource implements TickSource {
    boolean running;

    @Override
    public void start(int intervalMillis, Runnable onTick) {
      running = true;
    }

    @Override
    public void stop() {
      running = false;
    }
  }

  private Neighborhood board;
  private ManualTickSource ticks;
  private Clock clock;
  private final List<GameListener.Change> changes = new ArrayList<>();

  @BeforeEach
  void setUp() {
    board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    ticks = new ManualTickSource();
    clock = new Clock(ticks);
  }

  private GameController cancerController() {
    GameController controller =
        new GameController(
            new CompositeBoard(board), board, clock, new CancerGame(board, NO_NEW_CANCER));
    controller.addGameListener(changes::add);
    return controller;
  }

  @Test
  @DisplayName("CAN-C01 암 규칙 없이 만든 컨트롤러는 치료하지 않는다")
  void classicControllerIgnoresTreatment() {
    // Given
    cellAt(board, 3, 3).setCancer();
    GameController controller = new GameController(new CompositeBoard(board), board, clock);
    controller.addGameListener(changes::add);

    // When
    boolean treated = controller.treat(3, 3);

    // Then
    assertFalse(treated);
    assertTrue(cellAt(board, 3, 3).isCancer());
    assertTrue(changes.isEmpty());
    assertNull(controller.gameResultMessage());
  }

  @Test
  @DisplayName("CAN-C02 치료하면 전체 다시 그리기를 알리고, 횟수가 없으면 알리지 않는다")
  void treatmentNotifiesOnlyWhenUsed() {
    // Given: one lone cancer cell per treatment, plus a distant block so the game never ends
    distantCancerBlock(board);
    int attempts = CancerGame.INITIAL_TREATMENT_CHARGES + 1;
    for (int i = 0; i < attempts; i++) cellAt(board, 8 * i, 8 * i).setCancer();
    GameController controller = cancerController();

    // When
    for (int i = 0; i < attempts; i++) controller.treat(8 * i, 8 * i);

    // Then
    assertEquals(
        Collections.nCopies(CancerGame.INITIAL_TREATMENT_CHARGES, GameListener.Change.EDITED),
        changes);
  }

  @Test
  @DisplayName("CAN-C03 마지막 암세포를 없애는 치료는 시계를 멈추고 GAME_OVER를 알린다")
  void winningTreatmentStopsClockAndReportsGameOver() {
    // Given
    cancerBlock(board, 10, 10);
    GameController controller = cancerController();
    clock.startTicking(100);

    // When
    controller.treat(10, 10);

    // Then
    assertFalse(ticks.running);
    assertEquals(List.of(GameListener.Change.EDITED, GameListener.Change.GAME_OVER), changes);
    assertTrue(controller.gameResultMessage().contains("승리"));
  }

  @Test
  @DisplayName("CAN-C04 끝난 뒤의 tick은 보드를 진행하지 않고 알리지 않는다")
  void tickAfterGameOverDoesNothing() {
    // Given: a finished game
    cancerBlock(board, 10, 10);
    GameController controller = cancerController();
    controller.treat(10, 10);
    assertNotNull(controller.gameResultMessage());
    controller.toggle(30, 29);
    controller.toggle(30, 30);
    controller.toggle(30, 31);
    changes.clear();

    // When
    boolean changed = controller.tick();

    // Then
    assertFalse(changed);
    assertEquals(
        Set.of(new Point(29, 30), new Point(30, 30), new Point(31, 30)),
        new HashSet<>(board.createMemento().liveCells()));
    assertTrue(changes.isEmpty());
  }

  @Test
  @DisplayName("CAN-C05 치료가 거부되면 시계를 멈추지 않고 알리지 않는다")
  void refusedTreatmentLeavesClockRunningAndNotifiesNothing() {
    // Given: only one cancer cell, below the treatment minimum
    cellAt(board, 10, 10).setCancer();
    GameController controller = cancerController();
    clock.startTicking(100);

    // When
    boolean treated = controller.treat(10, 10);

    // Then
    assertFalse(treated);
    assertTrue(ticks.running);
    assertTrue(changes.isEmpty());
    assertNull(controller.gameResultMessage());
  }
}
