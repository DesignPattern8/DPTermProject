package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

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

  static Resident cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return (Resident) block.childAt(row % 8, column % 8);
  }

  /** Fill rows/columns [0, size) with cancer, optionally leaving out the last cell. */
  private void cancerSquare(int size, boolean leaveOutLast) {
    for (int r = 0; r < size; r++)
      for (int c = 0; c < size; c++)
        if (!(leaveOutLast && r == size - 1 && c == size - 1)) cellAt(board, r, c).setCancer();
  }

  /**
   * 3×4 cancer block (12 cells, more than {@link CancerGame#MIN_CANCER_COUNT_FOR_TREATMENT}) that
   * lies entirely inside the 5×5 treatment area centered on (row, column).
   *
   * @return the number of cancer cells placed.
   */
  static int cancerBlock(Neighborhood board, int row, int column) {
    for (int r = row - 1; r <= row + 1; r++)
      for (int c = column - 1; c <= column + 2; c++) cellAt(board, r, c).setCancer();
    return 12;
  }

  /** Cancer block far from every treatment spot in these tests; keeps the count above 10. */
  static int distantCancerBlock(Neighborhood board) {
    return cancerBlock(board, 56, 56);
  }

  @Test
  @DisplayName("CAN-A01 치료는 5번까지만 되고 그 뒤에는 보드를 바꾸지 않는다")
  void treatmentIsLimitedToFiveCharges() {
    // Given: six lone cancer cells far apart, plus a distant block that keeps the count above 10
    int distant = distantCancerBlock(board);
    int[][] spots = {{5, 5}, {5, 20}, {5, 40}, {20, 5}, {20, 20}, {40, 5}};
    for (int[] s : spots) cellAt(board, s[0], s[1]).setCancer();
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    // When / Then
    for (int i = 0; i < CancerGame.INITIAL_TREATMENT_CHARGES; i++) {
      assertTrue(game.treat(spots[i][0], spots[i][1]));
      assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES - 1 - i, game.treatmentCharges());
    }
    assertFalse(game.treat(40, 5));
    assertTrue(cellAt(board, 40, 5).isCancer());
    assertEquals(distant + 1, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-A02 치료로 마지막 암세포를 없애면 그 자리에서 승리로 끝난다")
  void curingAllCancerWins() {
    // Given
    cancerBlock(board, 10, 10);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    // When
    boolean treated = game.treat(10, 10);

    // Then
    assertTrue(treated);
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
    cancerBlock(board, 10, 10);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);
    game.treat(10, 10);
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

  @Test
  @DisplayName("CAN-A09 암세포가 10개 이하면 치료를 거부하고 횟수를 쓰지 않는다")
  void treatmentRefusedAtOrBelowMinimumCancerCount() {
    // Given: exactly 10 cancer cells inside one treatment area
    for (int c = 8; c <= 12; c++) {
      cellAt(board, 9, c).setCancer();
      cellAt(board, 10, c).setCancer();
    }
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    // When
    boolean treated = game.treat(10, 10);

    // Then
    assertFalse(treated);
    assertEquals(CancerGame.MIN_CANCER_COUNT_FOR_TREATMENT, board.cancerCount());
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES, game.treatmentCharges());
  }

  @Test
  @DisplayName("CAN-A10 치료 범위에 암세포가 없으면 횟수를 쓰지 않는다")
  void treatmentOfAreaWithoutCancerUsesNoCharge() {
    // Given
    int distant = distantCancerBlock(board);
    board.toggle(30, 30);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);

    // When
    boolean treated = game.treat(30, 30);

    // Then
    assertFalse(treated);
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES, game.treatmentCharges());
    assertEquals(distant, board.cancerCount());
  }

  @Test
  @DisplayName("CAN-A11 치료 없이 암세포가 모두 사라지면 승리가 아니다")
  void cancerVanishingWithoutTreatmentIsNotAWin() {
    // Given: cancer appeared once
    cellAt(board, 10, 10).setCancer();
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);
    game.afterGeneration();

    // When: it disappears without a treatment
    cellAt(board, 10, 10).cureCancer();
    boolean ended = game.afterGeneration();

    // Then
    assertFalse(ended);
    assertFalse(game.isOver());
    assertNull(game.resultMessage());
  }

  @Test
  @DisplayName("CAN-A12 끝난 게임에서는 치료하지 않는다")
  void finishedGameRefusesTreatment() {
    // Given: a lost game
    cancerSquare(32, false);
    CancerGame game = new CancerGame(board, NO_NEW_CANCER);
    game.afterGeneration();

    // When
    boolean treated = game.treat(10, 10);

    // Then
    assertFalse(treated);
    assertEquals(CancerGame.INITIAL_TREATMENT_CHARGES, game.treatmentCharges());
    assertEquals(CancerGame.LOSING_CANCER_COUNT, board.cancerCount());
  }
}
