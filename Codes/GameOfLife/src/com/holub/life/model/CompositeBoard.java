package com.holub.life.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Board facade over the existing classic cell hierarchy. */
public final class CompositeBoard implements Board {
  private final Neighborhood neighborhood;

  /** Wraps the root of a nonempty classic square board built from Resident cells. */
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
    // The legacy memento traversal can update active edges.
    collectLiveCells(neighborhood, 0, 0, liveCells);
    return new BoardSnapshot(rows(), columns(), liveCells);
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
  }

  private static void collectLiveCells(Cell cell, int row, int column, Set<CellPosition> liveCells) {
    if (cell instanceof Neighborhood block) {
      int childWidth = block.childAt(0, 0).widthInCells();
      for (int r = 0; r < block.gridSize(); r++) {
        for (int c = 0; c < block.gridSize(); c++) {
          collectLiveCells(
              block.childAt(r, c), row + r * childWidth, column + c * childWidth, liveCells);
        }
      }
    } else if (cell.isAlive()) {
      liveCells.add(new CellPosition(row, column));
    }
  }
}
