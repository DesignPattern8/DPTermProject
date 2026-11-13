package com.holub.life.model;

import java.awt.Point;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

public final class Resident implements Cell {
  private boolean amAlive = false;
  private boolean willBeAlive = false;
  private boolean isCancer = false;
  private boolean willBeCancer = false;

  private boolean isStable() {
    return amAlive == willBeAlive;
  }

  /**
   * figure the next state.
   *
   * @return true if the cell is not stable (will change state on the next transition().
   */
  public boolean figureNextState(
      Cell north,
      Cell south,
      Cell east,
      Cell west,
      Cell northeast,
      Cell northwest,
      Cell southeast,
      Cell southwest) {
    verify(north, "north");
    verify(south, "south");
    verify(east, "east");
    verify(west, "west");
    verify(northeast, "northeast");
    verify(northwest, "northwest");
    verify(southeast, "southeast");
    verify(southwest, "southwest");

    int neighbors = 0;
    int cancerNeighbors = 0;
    Cell[] adjacent = {north, south, east, west, northeast, northwest, southeast, southwest};

    for (Cell cell : adjacent) {
      if (cell.isAlive()) {
        neighbors++;
        if (cell.isCancer()) cancerNeighbors++;
      }
    }

    if (isCancer) {
      willBeAlive = neighbors >= 1 && neighbors <= 5;
      willBeCancer = willBeAlive;
    } else {
      willBeAlive = nextAlive(amAlive, neighbors);
      willBeCancer =
          !amAlive && cancerNeighbors >= 3 && cancerNeighbors <= 4; // 암세포가 3~4개 인접한 빈칸에는 암세포가 태어남
      if (willBeCancer) willBeAlive = true; // 암세포 출생이면 생존 상태도 함께 켬
    }

    return !isStable();
  }

  private static boolean nextAlive(boolean alive, int liveNeighbors) {
    return liveNeighbors == 3 || (alive && liveNeighbors == 2);
  }

  private void verify(Cell c, String direction) {
    assert (c instanceof Resident) || (c == Cell.DUMMY)
        : "incorrect type for " + direction + ": " + c.getClass().getName();
  }

  /**
   * This cell is monetary, so it's at every edge of itself. It's an internal error for any position
   * except for (0,0) to be requsted since the width is 1.
   */
  public Cell edge(int row, int column) {
    assert row == 0 && column == 0;
    return this;
  }

  public boolean transition() {
    boolean changed = !isStable() || isCancer != willBeCancer;
    amAlive = willBeAlive;
    isCancer = willBeCancer;
    return changed;
  }

  public void toggle(int row, int column) {
    amAlive = !amAlive;
  }

  public void clear() {
    amAlive = willBeAlive = false;
    isCancer = willBeCancer = false;
  }

  public boolean isAlive() {
    return amAlive;
  }

  public Cell create() {
    return new Resident();
  }

  public int widthInCells() {
    return 1;
  }

  public Direction isDisruptiveTo() {
    return isStable() ? Direction.NONE : Direction.ALL;
  }

  public boolean transfer(Memento memento, Point upperLeft, boolean doLoad) {
    if (doLoad) {
      boolean wasAlive = amAlive;
      boolean wasCancer = isCancer;
      amAlive = willBeAlive = memento.isAlive(upperLeft);
      isCancer = willBeCancer = memento.isCancer(upperLeft);

      return wasAlive != amAlive || wasCancer != isCancer;
    }
    if (amAlive) { // store only live cells
      if (isCancer) {
        memento.markAsCancer(upperLeft);
      } else {
        memento.markAsAlive(upperLeft);
      }
    }

    return false;
  }

  /**
   * Mementos must be created by Neighborhood objects. Throw an exception if anybody tries to do it
   * here.
   */
  public NeighborhoodState createMemento() {
    throw new UnsupportedOperationException("May not create memento of a unitary cell");
  }

  public boolean isCancer() {
    return isCancer;
  }

  public void setCancer() {
    amAlive = willBeAlive = true;
    isCancer = willBeCancer = true;
  }

  public void cureCancer() {
    if (isCancer) {
      amAlive = willBeAlive = false;
      isCancer = willBeCancer = false;
    }
  }
}
