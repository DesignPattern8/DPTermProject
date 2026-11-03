package com.holub.life.twoplayer;

import java.util.Optional;

/**
 * 시뮬레이션: K세대를 진행한다. 배치 입력은 받지 않는다. 보드가 정체돼도 K세대를 모두 진행한다 (rules.md 4장 2, 6장).
 * 매 세대 전멸 검사는 #6 2차에서 추가한다.
 */
final class SimulationPhase implements Phase {
  private final TwoPlayerGame game;
  private final int round;

  SimulationPhase(TwoPlayerGame game, int round) {
    this.game = game;
    this.round = round;
  }

  @Override
  public PhaseKind kind() {
    return PhaseKind.SIMULATION;
  }

  @Override
  public boolean tick() {
    game.board().advanceGeneration();
    game.countGeneration();
    if (game.generationInRound() == game.settings().generationsPerRound()) game.endSimulation(round);
    return true;
  }

  @Override
  public Response select(Player who, int row, int column) {
    return Response.WRONG_PHASE;
  }

  @Override
  public Response confirm(Player who) {
    return Response.WRONG_PHASE;
  }

  @Override
  public Response pass(Player who) {
    return Response.WRONG_PHASE;
  }

  @Override
  public Player currentPlayer() {
    return null;
  }

  @Override
  public int remaining(Player player) {
    return 0;
  }

  @Override
  public Optional<Coordinate> tentative() {
    return Optional.empty();
  }
}
