package com.holub.life.model;

import java.util.Set;

/** Immutable live-cell and cancer-cell positions of a board. Cancer cells are also live. */
public record BoardSnapshot(
    int rows, int columns, Set<CellPosition> liveCells, Set<CellPosition> cancerCells) {
  /** Creates a classic snapshot without cancer cells. */
  public BoardSnapshot(int rows, int columns, Set<CellPosition> liveCells) {
    this(rows, columns, liveCells, Set.of());
  }

  public BoardSnapshot {
    if (rows <= 0 || columns <= 0) {
      throw new IllegalArgumentException("Board dimensions must be positive");
    }
    liveCells = Set.copyOf(liveCells);
    cancerCells = Set.copyOf(cancerCells);
    for (CellPosition position : liveCells) {
      if (position.row() < 0
          || position.row() >= rows
          || position.column() < 0
          || position.column() >= columns) {
        throw new IllegalArgumentException("Live cell outside board: " + position);
      }
    }
    if (!liveCells.containsAll(cancerCells)) {
      throw new IllegalArgumentException("Cancer cells must also be live cells");
    }
  }
}
