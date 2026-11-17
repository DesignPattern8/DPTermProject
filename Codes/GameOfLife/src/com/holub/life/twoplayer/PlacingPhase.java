package com.holub.life.twoplayer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** 칸을 고르고 확정하는 단계(초기 배치, 개입)의 공통 동작 (rules.md 5장). */
abstract class PlacingPhase implements Phase {
  protected final TwoPlayerGame game;
  protected Player current;
  private final Map<Player, Integer> remaining = new EnumMap<>(Player.class);
  private Coordinate tentative; // 확정 전에는 보드에 놓지 않는다.

  PlacingPhase(TwoPlayerGame game, Player first, int placementsEach) {
    this.game = game;
    this.current = first;
    remaining.put(Player.A, placementsEach);
    remaining.put(Player.B, placementsEach);
  }

  /** 이 단계에서 who가 column 열에 둘 수 있는가 (진영 제한). */
  protected abstract boolean allowsColumn(Player who, int column);

  /** 확정이 끝난 뒤 차례를 넘기고 단계 종료를 판단한다. */
  protected abstract void afterConfirm();

  // 거부 판단 순서: 차례 → 보드 범위 → 진영 → 빈 칸 (단계 검사는 단계 클래스 자체가 한다)
  @Override
  public Response select(Player who, int row, int column) {
    if (who != current) return Response.NOT_YOUR_TURN;
    TwoPlayerBoard board = game.board();
    if (row < 0 || row >= board.rows() || column < 0 || column >= board.columns())
      return Response.OUT_OF_BOARD;
    if (!allowsColumn(who, column)) return Response.OPPONENT_HALF;
    if (!board.isEmpty(row, column)) return Response.CELL_NOT_EMPTY;
    tentative = new Coordinate(row, column);
    return Response.ACCEPTED;
  }

  @Override
  public Response confirm(Player who) {
    if (who != current) return Response.NOT_YOUR_TURN;
    if (tentative == null) return Response.NO_TENTATIVE_PLACEMENT;
    game.board().place(tentative.row(), tentative.column(), who);
    tentative = null;
    remaining.put(who, remaining.get(who) - 1);
    afterConfirm();
    return Response.ACCEPTED;
  }

  @Override
  public boolean tick() {
    return false;
  }

  @Override
  public Player currentPlayer() {
    return current;
  }

  @Override
  public int remaining(Player player) {
    return remaining.get(player);
  }

  @Override
  public Optional<Coordinate> tentative() {
    return Optional.ofNullable(tentative);
  }

  protected void clearTentative() {
    tentative = null;
  }
}
