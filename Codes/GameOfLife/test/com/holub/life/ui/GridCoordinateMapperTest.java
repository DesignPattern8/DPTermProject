package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import java.awt.Point;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

/** UI-R10~R12 (ui/docs/test-cases.md): pixel to CellPosition, decision 008. */
class GridCoordinateMapperTest {
  private final Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
  private final Rectangle surface = new Rectangle(0, 0, 512, 512);

  @Test
  void cellAtMatchesLegacyPosition() {
    for (int y = 0; y < 512; y += 7)
      for (int x = 0; x < 512; x += 7) {
        Point legacy = GridCoordinateMapper.position(board, new Point(x, y), surface);
        assertEquals(
            new CellPosition(legacy.y, legacy.x),
            GridCoordinateMapper.cellAt(board, new Point(x, y), surface),
            "pixel " + x + "," + y);
      }
  }

  @Test
  void cellAtMapsEdgesToRowAndColumn() {
    assertEquals(new CellPosition(0, 0), cellAt(0, 0));
    assertEquals(new CellPosition(7, 7), cellAt(63, 63));
    assertEquals(new CellPosition(8, 8), cellAt(64, 64));
    assertEquals(new CellPosition(63, 63), cellAt(511, 511));
    assertEquals(new CellPosition(25, 12), cellAt(100, 200));
  }

  @Test
  void cellAtKeepsOriginalOutOfRangeException() {
    Rectangle uneven = new Rectangle(0, 0, 511, 511);
    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> GridCoordinateMapper.cellAt(board, new Point(510, 510), uneven));
  }

  private CellPosition cellAt(int x, int y) {
    return GridCoordinateMapper.cellAt(board, new Point(x, y), surface);
  }
}
