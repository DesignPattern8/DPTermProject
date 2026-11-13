package com.holub.life.model;

import java.awt.Point;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/** Existing Memento data, separated from its file encoding. */
public final class NeighborhoodState implements Cell.Memento {
  private Collection liveCells = new LinkedList();
  private Set<Point> cancerCells = new HashSet<Point>();

  public Collection liveCells() {
    return liveCells;
  }

  /** Cancer cells are also contained in {@link #liveCells()}. */
  public Set<Point> cancerCells() {
    return cancerCells;
  }

  public void replaceCells(Collection cells) {
    liveCells = cells;
    cancerCells = new HashSet<Point>();
  }

  public void replaceCells(Collection cells, Set<Point> cancer) {
    liveCells = cells;
    cancerCells = cancer;
  }

  public void markAsAlive(Point location) {
    liveCells.add(new Point(location));
  }

  public void markAsCancer(Point location) {
    liveCells.add(new Point(location));
    cancerCells.add(new Point(location));
  }

  public boolean isAlive(Point location) {
    return liveCells.contains(location);
  }

  public boolean isCancer(Point location) {
    return cancerCells.contains(location);
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("NeighborhoodState:\n");
    for (Iterator i = liveCells.iterator(); i.hasNext(); )
      b.append(((Point) i.next()).toString() + "\n");
    return b.toString();
  }
}
