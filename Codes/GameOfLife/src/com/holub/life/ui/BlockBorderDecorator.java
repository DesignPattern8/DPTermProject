package com.holub.life.ui;

import com.holub.life.model.BoardSnapshot;
import com.holub.ui.Colors;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Outlines every block and the whole board. Moved from the border drawing in the original
 * Neighborhood.redraw().
 */
public final class BlockBorderDecorator implements BoardRenderer {
  private final BoardRenderer inner;

  public BlockBorderDecorator(BoardRenderer inner) {
    this.inner = inner;
  }

  @Override
  public void render(Graphics g, BoardSnapshot snapshot, Rectangle surface) {
    inner.render(g, snapshot, surface);

    GridCoordinateMapper mapper = GridCoordinateMapper.forSnapshot(snapshot);
    Graphics g2 = g.create();
    try {
      g2.setColor(Colors.LIGHT_ORANGE);
      for (int blockRow = 0; blockRow < mapper.blockRows(); ++blockRow)
        for (int blockColumn = 0; blockColumn < mapper.blockColumns(); ++blockColumn) {
          Rectangle block = mapper.blockBounds(blockRow, blockColumn, surface);
          g2.drawRect(block.x, block.y, block.width, block.height);
        }
      g2.drawRect(surface.x, surface.y, surface.width, surface.height);
    } finally {
      g2.dispose();
    }
  }
}
