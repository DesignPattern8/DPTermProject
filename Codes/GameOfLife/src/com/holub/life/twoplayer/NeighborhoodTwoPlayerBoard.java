package com.holub.life.twoplayer;

import java.util.Optional;

/**
 * {@link TwoPlayerBoard}의 실제 구현. 64×64 {@code Neighborhood(8, Neighborhood(8, TwoPlayerResident))}로 만든다
 * (docs/006-two-player-cell.md 후보 F, {@code CompositeBoard}·{@code Board}는 쓰지 않음). 계약은 docs/interfaces.md,
 * 테스트 케이스는 docs/test-cases.md 1장. 구현 전 골격.
 *
 * <p>한 스레드에서만 호출한다.
 */
public final class NeighborhoodTwoPlayerBoard implements TwoPlayerBoard {

  /** 빈 64×64 보드. */
  public NeighborhoodTwoPlayerBoard() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int rows() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int columns() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** 범위 밖이면 {@link IndexOutOfBoundsException}. */
  @Override
  public boolean isEmpty(int row, int column) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public boolean hasEmptyCell() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * 범위 밖이면 {@link IndexOutOfBoundsException}, 살아 있는 칸이면 {@link IllegalArgumentException}, owner가 null이면
   * {@link NullPointerException}. 예외를 던지면 보드는 바뀌지 않는다.
   */
  @Override
  public void place(int row, int column, Player owner) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void advanceGeneration() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** owner가 null이면 {@link NullPointerException}. */
  @Override
  public int count(Player owner) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** 그 칸의 소유자. DEAD면 비어 있다. 범위 밖이면 {@link IndexOutOfBoundsException}. (테스트·#7 렌더러용 조회) */
  public Optional<Player> owner(int row, int column) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
