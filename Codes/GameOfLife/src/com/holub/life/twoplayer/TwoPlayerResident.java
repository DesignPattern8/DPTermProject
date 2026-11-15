package com.holub.life.twoplayer;

import com.holub.life.model.Cell;
import com.holub.life.model.Direction;
import com.holub.life.model.NeighborhoodState;
import java.awt.Point;

/**
 * 2인용 셀 하나 (rules.md 3장, docs/006-two-player-cell.md 후보 F). 상태는 소유자 {@link Player}이고 null이면 DEAD다.
 * {@code Neighborhood}의 프로토타입으로 쓰며, 한 보드의 모든 칸이 이 클래스다.
 *
 * <p>전이 규칙: 생존·사망은 색을 무시한 이웃 수(B3/S23)로 정하고, 탄생하는 셀만 이웃 3개 중 다수인 색을 따른다. 살아 있는 셀의 색은
 * 죽을 때까지 바뀌지 않는다. 기존 2단계 구조({@link #figureNextState} → {@link #transition})를 그대로 따른다.
 *
 * <p>2인용 모드는 저장을 지원하지 않고(rules.md 8장) 암 규칙도 없으므로, 그 메서드들은 아무것도 하지 않거나 예외를 던진다.
 */
final class TwoPlayerResident implements Cell {
  /** 지금 상태. DEAD면 null. */
  private Player owner;

  /** {@link #figureNextState}가 계산한 다음 상태. DEAD면 null. */
  private Player next;

  /** 다음 {@link #toggle}이 DEAD 칸을 살릴 때 쓸 소유자. {@link #prepareOwner}로만 정한다. */
  private Player prepared;

  /** 지금 살아 있는 소유자. DEAD면 null. */
  Player owner() {
    return owner;
  }

  /**
   * 다음 {@link #toggle}이 이 DEAD 칸을 owner의 산 셀로 만들도록 정해 둔다. 소유자를 지정한 뒤 반드시
   * {@code Neighborhood.toggle}로 살려야 그 블록이 활성으로 표시되어 다음 세대에 계산된다.
   * {@link NeighborhoodTwoPlayerBoard#place}만 부른다.
   */
  void prepareOwner(Player owner) {
    prepared = owner;
  }

  @Override
  public boolean figureNextState(
      Cell north,
      Cell south,
      Cell east,
      Cell west,
      Cell northeast,
      Cell northwest,
      Cell southeast,
      Cell southwest) {
    int a = 0;
    int b = 0;
    for (Cell neighbor :
        new Cell[] {north, south, east, west, northeast, northwest, southeast, southwest}) {
      Player neighborOwner = ownerOf(neighbor);
      if (neighborOwner == Player.A) {
        a++;
      } else if (neighborOwner == Player.B) {
        b++;
      }
    }
    int live = a + b;
    if (owner != null) {
      next = (live == 2 || live == 3) ? owner : null;
    } else if (live == 3) {
      next = a >= 2 ? Player.A : Player.B; // n = 3이라 a와 b 중 하나는 반드시 2 이상
    } else {
      next = null;
    }
    return next != owner;
  }

  /** 이웃은 같은 보드의 2인용 셀이거나 보드 밖({@code Cell.DUMMY}, 항상 DEAD)이다. */
  private static Player ownerOf(Cell neighbor) {
    return neighbor instanceof TwoPlayerResident cell ? cell.owner : null;
  }

  /** 셀 하나는 자기 자신의 가장자리다. 폭이 1이라 (0,0)만 요청할 수 있다. */
  @Override
  public Cell edge(int row, int column) {
    assert row == 0 && column == 0;
    return this;
  }

  @Override
  public boolean transition() {
    boolean changed = next != owner;
    owner = next;
    return changed;
  }

  /**
   * 살아 있으면 DEAD로, DEAD면 {@link #prepareOwner}로 정해 둔 소유자의 산 셀로 바꾼다. 정해 둔 소유자는 한 번 쓰면 지운다.
   * 좌표는 쓰지 않는다(셀 하나라서).
   */
  @Override
  public void toggle(int row, int column) {
    owner = (owner == null) ? prepared : null;
    next = owner;
    prepared = null;
  }

  @Override
  public boolean isAlive() {
    return owner != null;
  }

  @Override
  public int widthInCells() {
    return 1;
  }

  @Override
  public Cell create() {
    return new TwoPlayerResident();
  }

  @Override
  public Direction isDisruptiveTo() {
    return next == owner ? Direction.NONE : Direction.ALL;
  }

  @Override
  public void clear() {
    owner = null;
    next = null;
    prepared = null;
  }

  /** 2인용 모드는 저장·불러오기를 지원하지 않는다. 아무것도 하지 않는다. */
  @Override
  public boolean transfer(Memento memento, Point upperLeftCorner, boolean doLoad) {
    return false;
  }

  /** {@code Resident}와 같이, 셀 하나의 memento는 만들 수 없다. */
  @Override
  public NeighborhoodState createMemento() {
    throw new UnsupportedOperationException("May not create memento of a unitary cell");
  }

  /** 2인용 모드에는 암세포가 없다. */
  @Override
  public boolean isCancer() {
    return false;
  }

  /** 2인용 모드에는 암세포가 없다. 아무것도 하지 않는다. */
  @Override
  public void setCancer() {}

  /** 2인용 모드에는 암세포가 없다. 아무것도 하지 않는다. */
  @Override
  public void cureCancer() {}
}
