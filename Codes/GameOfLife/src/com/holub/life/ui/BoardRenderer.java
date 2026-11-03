package com.holub.life.ui;

import com.holub.life.model.Cell;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.Resident;
import com.holub.ui.Colors;
import java.awt.*;

/** Drawing code extracted from Resident and Neighborhood without changing paint order. */
public final class BoardRenderer {
  private BoardRenderer() {}

  public static void redraw(Cell cell, Graphics g, Rectangle here, boolean drawAll) {
    if (cell instanceof Neighborhood block) {
      try {
        if (!block.beginRedraw(drawAll)) return;
        int compoundWidth = here.width;
        Rectangle subcell = new Rectangle(here.x, here.y,
            here.width / block.gridSize(), here.height / block.gridSize());
        for (int row = 0; row < block.gridSize(); ++row) {
          for (int column = 0; column < block.gridSize(); ++column) {
            redraw(block.childAt(row, column), g, subcell, drawAll);
            subcell.translate(subcell.width, 0);
          }
          subcell.translate(-compoundWidth, subcell.height);
        }
        g = g.create();
        g.setColor(Colors.LIGHT_ORANGE);
        g.drawRect(here.x, here.y, here.width, here.height);
        if (block.isActive()) {
          g.setColor(Color.BLUE);
          g.drawRect(here.x + 1, here.y + 1, here.width - 2, here.height - 2);
        }
        g.dispose();
      } catch (InterruptedException e) {
        // Match the original redraw: skip painting after interruption.
      }
    } else if (cell instanceof Resident) {
      g = g.create();
      g.setColor(cell.isAlive() ? Color.RED : Colors.LIGHT_YELLOW);
      g.fillRect(here.x + 1, here.y + 1, here.width - 1, here.height - 1);
      g.setColor(Colors.DARK_YELLOW);
      g.drawLine(here.x, here.y, here.x, here.y + here.height);
      g.drawLine(here.x, here.y, here.x + here.width, here.y);
      g.dispose();
    }
  }
}
