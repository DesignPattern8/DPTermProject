package com.holub.life.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Board facade over the existing classic and cancer cell hierarchy. */
public final class CompositeBoard implements Board {
  private final Neighborhood neighborhood;

  /** Wraps the root of a nonempty square board built from Resident cells. */
  public CompositeBoard(Neighborhood neighborhood) {
    this.neighborhood = Objects.requireNonNull(neighborhood, "neighborhood");
  }

  @Override
  public int rows() {
    return neighborhood.widthInCells();
  }

  @Override
  public int columns() {
    return neighborhood.widthInCells();
  }

  @Override
  public boolean step() {
    Cell outside = Cell.DUMMY;
    return neighborhood.figureNextState(
            outside, outside, outside, outside, outside, outside, outside, outside)
        && neighborhood.transition();
  }

  @Override
  public void toggle(CellPosition position) {
    int row = Objects.checkIndex(position.row(), rows());
    int column = Objects.checkIndex(position.column(), columns());
    neighborhood.toggle(row, column);
  }

  @Override
  public void clear() {
    neighborhood.clear();
  }

  @Override
  public BoardSnapshot snapshot() {
    Set<CellPosition> liveCells = new HashSet<>();
    Set<CellPosition> cancerCells = new HashSet<>();
    // The legacy memento traversal can update active edges.
    collectCells(neighborhood, 0, 0, liveCells, cancerCells);
    return new BoardSnapshot(rows(), columns(), liveCells, cancerCells);
  }

  @Override
  public void restore(BoardSnapshot snapshot) {
    if (snapshot.rows() != rows() || snapshot.columns() != columns()) {
      throw new IllegalArgumentException("Snapshot dimensions must match the board");
    }
    clear();
    // Toggle marks active edges so neighboring blocks take part in the next generation.
    for (CellPosition position : snapshot.liveCells()) {
      neighborhood.toggle(position.row(), position.column());
    }
    for (CellPosition position : snapshot.cancerCells()) {
      cellAt(position.row(), position.column()).setCancer();
    }
  }

  private Cell cellAt(int row, int column) {
    Cell cell = neighborhood;
    while (cell instanceof Neighborhood block) {
      int childWidth = block.childAt(0, 0).widthInCells();
      cell = block.childAt(row / childWidth, column / childWidth);
      row %= childWidth;
      column %= childWidth;
    }
    return cell;
  }

  private static void collectCells(
      Cell cell,
      int row,
      int column,
      Set<CellPosition> liveCells,
      Set<CellPosition> cancerCells) {
    if (cell instanceof Neighborhood block) {
      int childWidth = block.childAt(0, 0).widthInCells();
      for (int r = 0; r < block.gridSize(); r++) {
        for (int c = 0; c < block.gridSize(); c++) {
          collectCells(
              block.childAt(r, c),
              row + r * childWidth,
              column + c * childWidth,
              liveCells,
              cancerCells);
        }
      }
    } else if (cell.isAlive()) {
      CellPosition position = new CellPosition(row, column);
      liveCells.add(position);
      if (cell.isCancer()) cancerCells.add(position);
    }
  }
}
