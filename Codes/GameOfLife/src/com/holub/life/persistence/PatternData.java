package com.holub.life.persistence;

import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CellPosition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** File coordinates preserve order, duplicates and out-of-board values until placement. */
public record PatternData(List<CellPosition> cells, Set<CellPosition> cancerCells) {
  public PatternData {
    cells = List.copyOf(cells);
    cancerCells = Set.copyOf(cancerCells);
  }

  /** Projects classic file data onto the target board, never silently discarding cancer state. */
  public BoardSnapshot toClassicSnapshot(int rows, int columns) {
    if (!cancerCells.isEmpty()) {
      throw new IllegalStateException("A classic snapshot cannot represent cancer cells");
    }
    Set<CellPosition> live = new HashSet<>();
    for (CellPosition cell : cells) {
      if (cell.row() >= 0 && cell.row() < rows && cell.column() >= 0 && cell.column() < columns) {
        live.add(cell);
      }
    }
    return new BoardSnapshot(rows, columns, live);
  }
}
