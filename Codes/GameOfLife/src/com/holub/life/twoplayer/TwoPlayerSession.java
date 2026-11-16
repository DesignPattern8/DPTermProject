package com.holub.life.twoplayer;

import java.util.Optional;

/** 2인용 UI 진입점의 모델 쪽 (Facade). docs/007-two-player-ui.md 결정 3. 골격. */
public final class TwoPlayerSession {
  /** 그리기용 스냅숏. cells[row][column]은 소유자, DEAD면 null. */
  public record Snapshot(Player[][] cells, Optional<Coordinate> tentative, Player currentPlayer) {}

  public TwoPlayerSession(GameSettings settings) {}

  public Response click(int row, int column) { throw new UnsupportedOperationException("not implemented (#7)"); }

  public Response confirm() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public Response pass() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public boolean tick() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public void newGame() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public String status() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public Snapshot snapshot() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public PhaseKind phase() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public int generationInRound() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public int rows() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public int columns() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public void addListener(Runnable listener) { throw new UnsupportedOperationException("not implemented (#7)"); }
}
