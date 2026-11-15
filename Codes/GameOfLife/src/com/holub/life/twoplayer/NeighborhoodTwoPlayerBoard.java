package com.holub.life.twoplayer;

import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link TwoPlayerBoard}의 실제 구현. 64×64 {@code Neighborhood(8, Neighborhood(8, TwoPlayerResident))}
 * 로 만든다(docs/006-two-player-cell.md 후보 F). {@code CompositeBoard}·{@code Board}는 쓰지 않는다(006 W3).
 * 계약은 docs/interfaces.md, 테스트 케이스는 docs/test-cases.md 1장.
 *
 * <p>세대 진행은 {@code Neighborhood}의 활성 블록 계산을 그대로 쓴다. 칸 조회·집계는 생성할 때 만든 64×64 색인으로 한다(셀 객체는
 * 생성 뒤 바뀌지 않는다).
 *
 * <p>한 스레드에서만 호출한다.
 */
public final class NeighborhoodTwoPlayerBoard implements TwoPlayerBoard {
  private static final int BLOCKS = 8;
  private static final int BLOCK_SIZE = 8;
  private static final int SIZE = BLOCKS * BLOCK_SIZE;

  private final Neighborhood root =
      new Neighborhood(BLOCKS, new Neighborhood(BLOCK_SIZE, new TwoPlayerResident()));

  /** cells[row][column]: root 아래의 셀을 보드 좌표로 펼친 색인. */
  private final TwoPlayerResident[][] cells = new TwoPlayerResident[SIZE][SIZE];

  /** 빈 64×64 보드. */
  public NeighborhoodTwoPlayerBoard() {
    for (int blockRow = 0; blockRow < BLOCKS; blockRow++) {
      for (int blockColumn = 0; blockColumn < BLOCKS; blockColumn++) {
        Neighborhood block = (Neighborhood) root.childAt(blockRow, blockColumn);
        for (int r = 0; r < BLOCK_SIZE; r++) {
          for (int c = 0; c < BLOCK_SIZE; c++) {
            cells[blockRow * BLOCK_SIZE + r][blockColumn * BLOCK_SIZE + c] =
                (TwoPlayerResident) block.childAt(r, c);
          }
        }
      }
    }
  }

  @Override
  public int rows() {
    return SIZE;
  }

  @Override
  public int columns() {
    return SIZE;
  }

  /** 범위 밖이면 {@link IndexOutOfBoundsException}. */
  @Override
  public boolean isEmpty(int row, int column) {
    return cellAt(row, column).owner() == null;
  }

  @Override
  public boolean hasEmptyCell() {
    for (TwoPlayerResident[] row : cells) {
      for (TwoPlayerResident cell : row) {
        if (cell.owner() == null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 범위 밖이면 {@link IndexOutOfBoundsException}, owner가 null이면 {@link NullPointerException}, 살아 있는 칸이면
   * {@link IllegalArgumentException}. 예외를 던지면 보드는 바뀌지 않는다.
   *
   * <p>소유자를 셀에 정해 둔 뒤 {@code Neighborhood.toggle}로 살린다. toggle이 블록을 활성으로 표시해야 정체한 블록에 놓은 셀도 다음
   * 세대에 계산된다.
   */
  @Override
  public void place(int row, int column, Player owner) {
    TwoPlayerResident cell = cellAt(row, column);
    Objects.requireNonNull(owner, "owner");
    if (cell.owner() != null) {
      throw new IllegalArgumentException(
          "cell (" + row + "," + column + ") is not empty: " + cell.owner());
    }
    cell.prepareOwner(owner);
    root.toggle(row, column);
  }

  /** rules.md 3장 규칙으로 1세대 진행한다. 보드 밖은 DEAD({@code Cell.DUMMY})다. */
  @Override
  public void advanceGeneration() {
    Cell outside = Cell.DUMMY;
    if (root.figureNextState(
        outside, outside, outside, outside, outside, outside, outside, outside)) {
      root.transition();
    }
  }

  /** owner가 null이면 {@link NullPointerException}. */
  @Override
  public int count(Player owner) {
    Objects.requireNonNull(owner, "owner");
    int count = 0;
    for (TwoPlayerResident[] row : cells) {
      for (TwoPlayerResident cell : row) {
        if (cell.owner() == owner) {
          count++;
        }
      }
    }
    return count;
  }

  /** 그 칸의 소유자. DEAD면 비어 있다. 범위 밖이면 {@link IndexOutOfBoundsException}. (테스트·#7 렌더러용 조회) */
  public Optional<Player> owner(int row, int column) {
    return Optional.ofNullable(cellAt(row, column).owner());
  }

  private TwoPlayerResident cellAt(int row, int column) {
    return cells[Objects.checkIndex(row, SIZE)][Objects.checkIndex(column, SIZE)];
  }
}
