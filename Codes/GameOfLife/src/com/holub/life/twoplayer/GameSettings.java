package com.holub.life.twoplayer;

/**
 * 2인용 게임 설정값 (rules.md 4장). 테스트에서는 작은 값을 주입한다.
 *
 * <ul>
 *   <li>placements (N): 초기 배치에서 각자 두는 개수
 *   <li>generationsPerRound (K): 라운드마다 시뮬레이션하는 세대 수
 *   <li>interventions (M): 개입 단계마다 각자 받는 배치 개수
 *   <li>rounds (R): 라운드 수
 * </ul>
 */
public final class GameSettings {
  private final int placements;
  private final int generationsPerRound;
  private final int interventions;
  private final int rounds;

  public GameSettings(int placements, int generationsPerRound, int interventions, int rounds) {
    if (placements < 1) throw new IllegalArgumentException("placements (N) must be >= 1");
    if (generationsPerRound < 1)
      throw new IllegalArgumentException("generationsPerRound (K) must be >= 1");
    if (interventions < 0) throw new IllegalArgumentException("interventions (M) must be >= 0");
    if (rounds < 1) throw new IllegalArgumentException("rounds (R) must be >= 1");
    this.placements = placements;
    this.generationsPerRound = generationsPerRound;
    this.interventions = interventions;
    this.rounds = rounds;
  }

  /** 기본값 N = 20, K = 20, M = 3, R = 5 (rules.md 4장). */
  public static GameSettings defaults() {
    return new GameSettings(20, 20, 3, 5);
  }

  public int placements() {
    return placements;
  }

  public int generationsPerRound() {
    return generationsPerRound;
  }

  public int interventions() {
    return interventions;
  }

  public int rounds() {
    return rounds;
  }
}
