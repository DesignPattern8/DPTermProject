package com.holub.life.model;

import java.awt.Point;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/** Existing Memento data, separated from its file encoding. */
public final class NeighborhoodState implements Cell.Memento {
  private Collection liveCells = new LinkedList();

  public Collection liveCells() {
    return liveCells;
  }

  public void replaceCells(Collection cells) {
    liveCells = cells;
  }

  public void markAsAlive(Point location) {
    liveCells.add(new Point(location));
  }

  public boolean isAlive(Point location) {
    return liveCells.contains(location);
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("NeighborhoodState:\n");
    for (Iterator i = liveCells.iterator(); i.hasNext(); )
      b.append(((Point) i.next()).toString() + "\n");
    return b.toString();
  }
}
