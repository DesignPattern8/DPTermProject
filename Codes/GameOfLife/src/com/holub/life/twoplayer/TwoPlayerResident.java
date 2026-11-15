package com.holub.life.twoplayer;

import com.holub.life.model.Cell;
import com.holub.life.model.Direction;
import com.holub.life.model.NeighborhoodState;
import java.awt.Point;

/**
 * 2인용 셀 하나 (rules.md 3장, docs/006-two-player-cell.md 후보 F). 상태는 소유자 {@link Player}이고 null이면 DEAD다.
 * {@code Neighborhood}의 프로토타입으로 쓴다. 구현 전 골격.
 */
final class TwoPlayerResident implements Cell {

  /** 지금 살아 있는 소유자. DEAD면 null. */
  Player owner() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /** 다음 {@link #toggle}이 이 DEAD 칸을 owner의 산 셀로 만들도록 정해 둔다. 보드의 place만 부른다. */
  void prepareOwner(Player owner) {
    throw new UnsupportedOperationException("not implemented yet");
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
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Cell edge(int row, int column) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public boolean transition() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void toggle(int row, int column) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public boolean isAlive() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int widthInCells() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Cell create() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Direction isDisruptiveTo() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public boolean transfer(Memento memento, Point upperLeftCorner, boolean doLoad) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public NeighborhoodState createMemento() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public boolean isCancer() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void setCancer() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void cureCancer() {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
