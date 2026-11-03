package com.holub.life.twoplayer;

import java.util.Optional;

/** 종료: 어떤 입력도 받지 않고, 결과를 보관한다 (rules.md 4장 표, 6장). */
final class GameOverPhase implements Phase {
  private final GameResult result;

  GameOverPhase(GameResult result) {
    this.result = result;
  }

  @Override
  public PhaseKind kind() {
    return PhaseKind.GAME_OVER;
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
  public boolean tick() {
    return false;
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

  @Override
  public Optional<GameResult> result() {
    return Optional.of(result);
  }
}
