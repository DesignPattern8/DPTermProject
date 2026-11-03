package com.holub.life.ui;

import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CellPosition;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * Translates between screen pixels and board coordinates. The board is drawn as blocks of
 * <code>blockSize</code> by <code>blockSize</code> cells, and pixel sizes are computed the same way
 * the original nested Neighborhood objects did: first divide the surface among the blocks, then
 * divide each block among its cells (integer division at each level).
 */
public final class GridCoordinateMapper {
  private final int rows;
  private final int columns;
  private final int blockSize;

  public GridCoordinateMapper(int rows, int columns, int blockSize) {
    if (rows <= 0 || columns <= 0 || blockSize <= 0)
      throw new IllegalArgumentException("board dimensions must be positive");
    if (rows % blockSize != 0 || columns % blockSize != 0)
      throw new IllegalArgumentException("board must be a whole number of blocks");
    this.rows = rows;
    this.columns = columns;
    this.blockSize = blockSize;
  }

  public static GridCoordinateMapper forSnapshot(BoardSnapshot snapshot) {
    return new GridCoordinateMapper(snapshot.rows(), snapshot.columns(), snapshot.blockSize());
  }

  public int blockRows() {
    return rows / blockSize;
  }

  public int blockColumns() {
    return columns / blockSize;
  }

  public int blockSize() {
    return blockSize;
  }

  /**
   * Returns the cell under the given point, or an empty result if the point is outside the board.
   * (The original code threw ArrayIndexOutOfBoundsException in that case.)
   */
  public Optional<CellPosition> toCell(Point here, Rectangle surface) {
    int blockWidth = surface.width / blockColumns();
    int blockHeight = surface.height / blockRows();
    int cellWidth = blockWidth / blockSize;
    int cellHeight = blockHeight / blockSize;
    if (cellWidth == 0 || cellHeight == 0 || here.x < 0 || here.y < 0) return Optional.empty();

    int blockColumn = here.x / blockWidth;
    int blockRow = here.y / blockHeight;
    int innerColumn = (here.x % blockWidth) / cellWidth;
    int innerRow = (here.y % blockHeight) / cellHeight;

    if (blockRow >= blockRows() || blockColumn >= blockColumns()) return Optional.empty();
    if (innerRow >= blockSize || innerColumn >= blockSize) return Optional.empty();

    return Optional.of(
        new CellPosition(blockRow * blockSize + innerRow, blockColumn * blockSize + innerColumn));
  }

  /** Bounds of one block, relative to the surface. */
  public Rectangle blockBounds(int blockRow, int blockColumn, Rectangle surface) {
    int blockWidth = surface.width / blockColumns();
    int blockHeight = surface.height / blockRows();
    return new Rectangle(
        surface.x + blockColumn * blockWidth,
        surface.y + blockRow * blockHeight,
        blockWidth,
        blockHeight);
  }

  /** Bounds of one cell, relative to the surface. */
  public Rectangle cellBounds(CellPosition position, Rectangle surface) {
    Rectangle block =
        blockBounds(position.row() / blockSize, position.column() / blockSize, surface);
    int cellWidth = block.width / blockSize;
    int cellHeight = block.height / blockSize;
    return new Rectangle(
        block.x + (position.column() % blockSize) * cellWidth,
        block.y + (position.row() % blockSize) * cellHeight,
        cellWidth,
        cellHeight);
  }

  /**
   * Shrinks a side length so that every cell gets the same number of pixels (the original
   * Universe resize rule): the result is the largest multiple of the cell count that fits.
   */
  public static int snapToCells(int pixels, int cellsAcross) {
    return (pixels / cellsAcross) * cellsAcross;
  }
}
