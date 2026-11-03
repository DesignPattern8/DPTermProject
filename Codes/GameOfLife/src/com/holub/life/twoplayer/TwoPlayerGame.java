package com.holub.life.twoplayer;

import java.awt.Point;
import java.util.Optional;

/** 2인용 게임 진행 (rules.md 4~5장). 구현 전 골격. */
public final class TwoPlayerGame {
  public TwoPlayerGame(TwoPlayerBoard board, GameSettings settings) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public Response select(Player who, int row, int column) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public Response confirm(Player who) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public Response pass(Player who) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public boolean tick() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public PhaseKind phase() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public Player currentPlayer() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public int remaining(Player player) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public int round() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public int generationInRound() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  public Optional<Point> tentative() {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
