package com.holub.life.model;

import java.util.Set;

/** Immutable dimensions and live-cell positions of a classic board. */
public record BoardSnapshot(int rows, int columns, Set<CellPosition> liveCells) {
  public BoardSnapshot {
    if (rows <= 0 || columns <= 0) {
      throw new IllegalArgumentException("Board dimensions must be positive");
    }
    liveCells = Set.copyOf(liveCells);
    for (CellPosition position : liveCells) {
      if (position.row() < 0
          || position.row() >= rows
          || position.column() < 0
          || position.column() >= columns) {
        throw new IllegalArgumentException("Live cell outside board: " + position);
      }
    }
  }
}
