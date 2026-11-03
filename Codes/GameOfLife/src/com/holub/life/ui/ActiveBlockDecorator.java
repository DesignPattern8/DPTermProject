package com.holub.life.ui;

import com.holub.life.model.BoardSnapshot;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Highlights the blocks that changed on the last generation (and the whole board, if anything
 * changed). Moved from the "amActive" drawing in the original Neighborhood.redraw().
 */
public final class ActiveBlockDecorator implements BoardRenderer {
  private static final Color ACTIVE_COLOR = Color.BLUE;
  private final BoardRenderer inner;

  public ActiveBlockDecorator(BoardRenderer inner) {
    this.inner = inner;
  }

  @Override
  public void render(Graphics g, BoardSnapshot snapshot, Rectangle surface) {
    inner.render(g, snapshot, surface);

    GridCoordinateMapper mapper = GridCoordinateMapper.forSnapshot(snapshot);
    Graphics g2 = g.create();
    try {
      g2.setColor(ACTIVE_COLOR);
      for (int blockRow = 0; blockRow < mapper.blockRows(); ++blockRow)
        for (int blockColumn = 0; blockColumn < mapper.blockColumns(); ++blockColumn)
          if (snapshot.isBlockActive(blockRow, blockColumn))
            drawInset(g2, mapper.blockBounds(blockRow, blockColumn, surface));
      if (snapshot.isBoardActive()) drawInset(g2, surface);
    } finally {
      g2.dispose();
    }
  }

  private static void drawInset(Graphics g, Rectangle here) {
    g.drawRect(here.x + 1, here.y + 1, here.width - 2, here.height - 2);
  }
}
