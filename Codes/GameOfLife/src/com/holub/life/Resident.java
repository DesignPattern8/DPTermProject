package com.holub.life;

import com.holub.ui.Colors; // Contains constants specifying various
// colors not defined in java.awt.Color.
import java.awt.*;
import javax.swing.*;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

public final class Resident implements Cell {
  private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
  private static final Color LIVE_COLOR = Color.RED;
  private static final Color CANCER_COLOR = Color.magenta;
  private static final Color DEAD_COLOR = Colors.LIGHT_YELLOW;

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
    Cell[] adjacent = {
      north, south, east, west,
      northeast, northwest, southeast, southwest
    };

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
      willBeAlive = neighbors == 3 || (amAlive && neighbors == 2);
      willBeCancer =
          !amAlive && cancerNeighbors >= 3 && cancerNeighbors <= 4; // 암세포가 3~4개 인접한 빈칸에는 암세포가 태어남
      if (willBeCancer) willBeAlive = true; // 암세포 출생이면 생존 상태도 함께 켬
    }

    return !isStable();
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
    boolean changed = isStable();
    amAlive = willBeAlive;
    return changed;
  }

  public void redraw(Graphics g, Rectangle here, boolean drawAll) {
    g = g.create();
    g.setColor(amAlive ? (isCancer ? CANCER_COLOR : LIVE_COLOR) : DEAD_COLOR);
    g.fillRect(here.x + 1, here.y + 1, here.width - 1, here.height - 1);

    // Doesn't draw a line on the far right and bottom of the
    // grid, but that's life, so to speak. It's not worth the
    // code for the special case.

    g.setColor(BORDER_COLOR);
    g.drawLine(here.x, here.y, here.x, here.y + here.height);
    g.drawLine(here.x, here.y, here.x + here.width, here.y);
    g.dispose();
  }

  public void userClicked(Point here, Rectangle surface) {
    amAlive = !amAlive;
  }

  public void clear() {
    amAlive = willBeAlive = false;
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

  public boolean transfer(Storable blob, Point upperLeft, boolean doLoad) {
    Memento memento = (Memento) blob;
    if (doLoad) {
      if (amAlive = willBeAlive = memento.isAlive(upperLeft)) return true;
    } else if (amAlive) // store only live cells
    memento.markAsAlive(upperLeft);

    return false;
  }

  /**
   * Mementos must be created by Neighborhood objects. Throw an exception if anybody tries to do it
   * here.
   */
  public Storable createMemento() {
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
