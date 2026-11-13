package com.holub.life.application;

import com.holub.life.model.Neighborhood;
import java.util.Random;

/**
 * Single-player cancer rules that used to run inside Universe's clock listener: cancer outbreaks
 * whose probability rises every {@value #GENERATIONS_PER_PHASE} generations, spread to adjacent
 * cells, a limited number of treatments, and the win/lose check. Callers must serialize access, as
 * with {@link com.holub.life.model.Board}.
 */
public final class CancerGame {
  public static final int GENERATIONS_PER_PHASE = 100;
  public static final int INITIAL_TREATMENT_CHARGES = 3;
  /** 25% of the 64x64 board. */
  public static final int LOSING_CANCER_COUNT = 1024;

  private static final double INVASION_PROBABILITY = 0.03;

  private final Neighborhood board;
  private final Random random;

  private int treatmentCharges = INITIAL_TREATMENT_CHARGES;
  private int generation = 0;
  private boolean cancerHasAppeared = false;
  private boolean gameOver = false;
  private String resultMessage = null;

  public CancerGame(Neighborhood board, Random random) {
    this.board = board;
    this.random = random;
  }

  /**
   * Apply the cancer rules after one generation.
   *
   * @return true if the game ended on this call.
   */
  public boolean afterGeneration() {
    if (gameOver) return false;

    generation++;
    int phase = Math.min(4, (generation - 1) / GENERATIONS_PER_PHASE + 1);
    board.tryNaturalCancer(cancerProbabilityForPhase(phase), random);
    board.applyInvasion(INVASION_PROBABILITY, random);

    int tumors = board.cancerCount();
    if (tumors > 0) cancerHasAppeared = true;

    if (tumors >= LOSING_CANCER_COUNT) {
      finish("암세포가 보드의 25% 이상을 차지하였습니다. 패배입니다.");
      return true;
    }
    if (cancerHasAppeared && tumors == 0) {
      finish("암세포를 모두 제거하였습니다. 승리입니다.");
      return true;
    }

    board.tryCancerOutbreak(cancerProbabilityForPhase(phase), random);
    return false;
  }

  /**
   * Cure cancer cells at (row, column) and its eight neighbors, using one treatment charge.
   *
   * @return false if no charges were left, in which case nothing changes.
   */
  public boolean treat(int row, int column) {
    if (treatmentCharges <= 0) return false;
    board.treatAt(row, column);
    treatmentCharges--;
    return true;
  }

  public boolean isOver() {
    return gameOver;
  }

  /** The win/lose message, or null while the game is running. */
  public String resultMessage() {
    return resultMessage;
  }

  public int treatmentCharges() {
    return treatmentCharges;
  }

  static double cancerProbabilityForPhase(int phase) {
    switch (phase) {
      case 1:
        return 0.001;
      case 2:
        return 0.005;
      case 3:
        return 0.02;
      default:
        return 0.05;
    }
  }

  private void finish(String message) {
    gameOver = true;
    resultMessage = message;
  }
}
