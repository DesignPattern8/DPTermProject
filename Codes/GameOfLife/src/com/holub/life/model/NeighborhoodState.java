package com.holub.life.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

/** Mutable memento collections of immutable model coordinates, separate from file encoding. */
public final class NeighborhoodState implements Cell.Memento {
  private Collection<CellPosition> liveCells = new LinkedList<>();
  private Set<CellPosition> cancerCells = new HashSet<>();

  public Collection<CellPosition> liveCells() {
    return liveCells;
  }

  /** Cancer cells are also contained in {@link #liveCells()}. */
  public Set<CellPosition> cancerCells() {
    return cancerCells;
  }

  public void replaceCells(Collection<CellPosition> cells) {
    liveCells = cells;
    cancerCells = new HashSet<>();
  }

  public void replaceCells(Collection<CellPosition> cells, Set<CellPosition> cancer) {
    liveCells = cells;
    cancerCells = cancer;
  }

  public void markAsAlive(CellPosition location) {
    liveCells.add(Objects.requireNonNull(location));
  }

  public void markAsCancer(CellPosition location) {
    liveCells.add(Objects.requireNonNull(location));
    cancerCells.add(location);
  }

  public boolean isAlive(CellPosition location) {
    return liveCells.contains(location);
  }

  public boolean isCancer(CellPosition location) {
    return cancerCells.contains(location);
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("NeighborhoodState:\n");
    for (CellPosition location : liveCells) b.append(location.toString()).append('\n');
    return b.toString();
  }
}
