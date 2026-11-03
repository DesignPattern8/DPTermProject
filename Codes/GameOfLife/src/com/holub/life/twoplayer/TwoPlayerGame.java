package com.holub.life.twoplayer;

import java.util.Optional;

/**
 * 2인용 게임 진행 (rules.md 4~5장). 입력은 메서드를 직접 호출하고 결과를 Response로 돌려받는다(docs/004-player-input.md).
 * 단계별 규칙은 Phase 구현 클래스에 있다(docs/003-game-phase.md). 셀 상태는 TwoPlayerBoard 뒤에 숨긴다.
 *
 * <p>한 스레드에서만 호출한다. 동기화하지 않는다.
 */
public final class TwoPlayerGame {
  private final TwoPlayerBoard board;
  private final GameSettings settings;
  private Phase phase;
  private int round = 0;
  private int generationInRound = 0;

  public TwoPlayerGame(TwoPlayerBoard board, GameSettings settings) {
    this.board = board;
    this.settings = settings;
    this.phase = new InitialPlacementPhase(this);
  }

  // ---- 입력 ----

  /** 임시 배치(또는 이동). 보드에는 아직 놓지 않는다. */
  public Response select(Player who, int row, int column) {
    return phase.select(who, row, column);
  }

  /** 임시 배치를 확정한다. 확정한 셀은 취소할 수 없다. */
  public Response confirm(Player who) {
    return phase.confirm(who);
  }

  /** 개입 단계에서만 받는다. */
  public Response pass(Player who) {
    return phase.pass(who);
  }

  /** 시뮬레이션 단계에서 1세대 진행하고 true. 다른 단계에서는 무시하고 false. */
  public boolean tick() {
    return phase.tick();
  }

  // ---- 상태 조회 ----

  public PhaseKind phase() {
    return phase.kind();
  }

  public Player currentPlayer() {
    return phase.currentPlayer();
  }

  public int remaining(Player player) {
    return phase.remaining(player);
  }

  public int round() {
    return round;
  }

  public int generationInRound() {
    return generationInRound;
  }

  public Optional<Coordinate> tentative() {
    return phase.tentative();
  }

  // ---- 단계 클래스가 쓰는 package-private 메서드 ----

  TwoPlayerBoard board() {
    return board;
  }

  GameSettings settings() {
    return settings;
  }

  void countGeneration() {
    generationInRound++;
  }

  void startSimulation(int newRound) {
    round = newRound;
    generationInRound = 0;
    phase = new SimulationPhase(this, newRound);
  }

  void endSimulation(int finishedRound) {
    if (finishedRound == settings.rounds()) {
      phase = new GameOverPhase();
      return;
    }
    InterventionPhase intervention = new InterventionPhase(this, finishedRound);
    phase = intervention;
    intervention.begin(); // 자동 패스로 곧바로 끝날 수 있어서 phase를 먼저 바꾼다
  }

  void endIntervention(int finishedRound) {
    startSimulation(finishedRound + 1);
  }
}
