package com.holub.life.twoplayer;

/** 초기 배치: A부터 1개씩 번갈아 각자 N개, 자기 진영만, 패스 없음 (rules.md 4장 1, 5장). */
final class InitialPlacementPhase extends PlacingPhase {
  InitialPlacementPhase(TwoPlayerGame game) {
    super(game, Player.A, game.settings().placements());
  }

  @Override
  public PhaseKind kind() {
    return PhaseKind.PLACEMENT;
  }

  /** A = 왼쪽 절반(0 ~ columns/2 - 1열), B = 오른쪽 절반. */
  @Override
  protected boolean allowsColumn(Player who, int column) {
    boolean leftHalf = column < game.board().columns() / 2;
    return who == Player.A ? leftHalf : !leftHalf;
  }

  @Override
  protected void afterConfirm() {
    if (remaining(Player.A) == 0 && remaining(Player.B) == 0) game.startSimulation(1);
    else current = current.opponent();
  }

  @Override
  public Response pass(Player who) {
    return Response.PASS_NOT_ALLOWED;
  }
}
