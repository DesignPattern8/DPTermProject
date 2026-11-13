package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancerGameTest {
  /** Random stand-in: every nextDouble/nextInt call returns the same value. */
  static final class FixedRandom extends Random {
    private final double nextDouble;

    FixedRandom(double nextDouble) {
      this.nextDouble = nextDouble;
    }

    @Override
    public double nextDouble() {
      return nextDouble;
    }

    @Override
    public int nextInt(int bound) {
      return 0;
    }
  }

  /** Never passes a probability check, so no new cancer appears. */
  static final Random NO_NEW_CANCER = new FixedRandom(0.99);

  private Neighborhood board;

  @BeforeEach
  void setUp() {
    board = new Neighborhood(8, new Neighborhood(8, new Resident()));
  }

  static Cell cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return block.childAt(row % 8, column % 8);
  }

  /** Fill rows/columns [0, size) with cancer, optionally leaving out the last cell. */
  private void cancerSquare(int size, boolean leaveOutLast) {
    for (int r = 0; r < size; r++)
      for (int c = 0; c < size; c++)
        if (!(leaveOutLast && r == size - 1 && c == size - 1)) cellAt(board, r, c).setCancer();
  }

  @Test
  @DisplayName("CAN-A01 치료는 3번까지만 되고 그 뒤에는 보드를 바꾸지 않는다")
  void treatmentIsLimitedToThreeCharges() {
    // Given
    int[][] spots = {{5, 5}, {5, 20}, {5, 40}, {40, 5}};
    for (int[] s : spots) cellAt(board, s[0], s[1]).setCancer();
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    // When / Then
    for (int i = 0; i < 3; i++) {
      assertTrue(game.treat(spots[i][0], spots[i][1]));
      assertEquals(2 - i, game.treatmentCharges());
    }
    assertFalse(game.treat(40, 5));
    assertTrue(cellAt(board, 40, 5).isCancer());
    assertEquals(1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-A02 암세포가 나타난 뒤 모두 없어지면 승리로 끝난다")
  void curingAllCancerWins() {
    // Given
    cellAt(board, 10, 10).setCancer();
    cellAt(board, 10, 11).setCancer();
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);
    assertFalse(game.afterGeneration());

    // When
    game.treat(10, 10);
    boolean ended = game.afterGeneration();

    // Then
    assertTrue(ended);
    assertTrue(game.isOver());
    assertTrue(game.resultMessage().contains("승리"));
  }

  @Test
  @DisplayName("CAN-A03 암세포가 한 번도 나타나지 않았으면 0개여도 승리가 아니다")
  void noCancerYetIsNotAWin() {
    board.toggle(5, 5);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    assertFalse(game.afterGeneration());
    assertFalse(game.isOver());
    assertNull(game.resultMessage());
  }

  @Test
  @DisplayName("CAN-A04 암세포가 1024개(보드의 25%)가 되면 패배한다")
  void cancerCoveringQuarterOfBoardLoses() {
    cancerSquare(32, false);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    assertTrue(game.afterGeneration());
    assertTrue(game.resultMessage().contains("패배"));
  }

  @Test
  @DisplayName("CAN-A05 암세포가 1023개면 아직 패배가 아니다")
  void oneCellBelowQuarterDoesNotLose() {
    cancerSquare(32, true);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    assertFalse(game.afterGeneration());
    assertEquals(CancerGame.LOSING_CANCER_COUNT - 1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-A06 끝난 게임은 더 이상 진행하지 않는다")
  void finishedGameDoesNotContinue() {
    cellAt(board, 10, 10).setCancer();
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);
    game.afterGeneration();
    game.treat(10, 10);
    game.afterGeneration();
    String message = game.resultMessage();

    assertFalse(game.afterGeneration());
    assertEquals(message, game.resultMessage());
  }

  @Test
  @DisplayName("CAN-A07 발생 확률은 100세대마다 올라간다")
  void outbreakProbabilityRisesAfterHundredGenerations() {
    // Given: 0.003 fails phase 1 (0.001) and passes phase 2 (0.005)
    board.toggle(5, 5);
    CancerGame game = new CancerGame(board, new FixedRandom(0.003));

    // When
    for (int i = 0; i < CancerGame.GENERATIONS_PER_PHASE; i++) game.afterGeneration();
    int afterPhaseOne = board.cancerCount();
    game.afterGeneration();

    // Then
    assertEquals(0, afterPhaseOne);
    assertEquals(1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-A08 단계별 발생 확률은 0.001, 0.005, 0.02, 0.05이고 4단계 이후는 같다")
  void phaseProbabilities() {
    assertEquals(0.001, CancerGame.cancerProbabilityForPhase(1));
    assertEquals(0.005, CancerGame.cancerProbabilityForPhase(2));
    assertEquals(0.02, CancerGame.cancerProbabilityForPhase(3));
    assertEquals(0.05, CancerGame.cancerProbabilityForPhase(4));
    assertEquals(0.05, CancerGame.cancerProbabilityForPhase(5));
  }
}
