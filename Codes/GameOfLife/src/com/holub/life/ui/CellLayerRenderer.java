package com.holub.life.ui;

import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CellPosition;
import com.holub.ui.Colors;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** Base layer: draws every cell. Moved from the original Resident.redraw(). */
public final class CellLayerRenderer implements BoardRenderer {
  private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
  private static final Color LIVE_COLOR = Color.RED;
  private static final Color DEAD_COLOR = Colors.LIGHT_YELLOW;

  @Override
  public void render(Graphics g, BoardSnapshot snapshot, Rectangle surface) {
    GridCoordinateMapper mapper = GridCoordinateMapper.forSnapshot(snapshot);
    Graphics g2 = g.create();
    try {
      for (int row = 0; row < snapshot.rows(); ++row)
        for (int column = 0; column < snapshot.columns(); ++column) {
          CellPosition position = new CellPosition(row, column);
          drawCell(g2, mapper.cellBounds(position, surface), snapshot.isAlive(position));
        }
    } finally {
      g2.dispose();
    }
  }

  private static void drawCell(Graphics g, Rectangle here, boolean alive) {
    g.setColor(alive ? LIVE_COLOR : DEAD_COLOR);
    g.fillRect(here.x + 1, here.y + 1, here.width - 1, here.height - 1);

    // Doesn't draw a line on the far right and bottom of the
    // grid, but that's life, so to speak. (Same as the original.)

    g.setColor(BORDER_COLOR);
    g.drawLine(here.x, here.y, here.x, here.y + here.height);
    g.drawLine(here.x, here.y, here.x + here.width, here.y);
  }
}
