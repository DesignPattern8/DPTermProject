package com.holub.life.ui;

import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import java.awt.Point;
import java.awt.Rectangle;

/** Preserves the original per-neighborhood integer division, including edge exceptions. */
public final class GridCoordinateMapper {
  private GridCoordinateMapper() {}

  public static Point position(Cell cell, Point here, Rectangle surface) {
    if (!(cell instanceof Neighborhood block)) return new Point(0, 0);
    int pixelsPerCell = surface.width / block.gridSize();
    int row = here.y / pixelsPerCell;
    int column = here.x / pixelsPerCell;
    Point offset = new Point(here.x % pixelsPerCell, here.y % pixelsPerCell);
    Rectangle subcell = new Rectangle(0, 0, pixelsPerCell, pixelsPerCell);
    Cell child = block.childAt(row, column);
    Point position = position(child, offset, subcell);
    position.translate(column * child.widthInCells(), row * child.widthInCells());
    return position;
  }

  public static void userClicked(Cell cell, Point here, Rectangle surface) {
    Point position = position(cell, here, surface);
    cell.toggle(position.y, position.x);
  }
}
