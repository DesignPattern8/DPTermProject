package com.holub.life.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import com.holub.ui.Colors;
import java.awt.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/** UI-R16 (#67, cancer feature #60): cell colors. See ui/docs/test-cases.md. */
class BoardRendererTest {
  private static Cell cellAt(Neighborhood board, int row, int column) {
    Neighborhood block = (Neighborhood) board.childAt(row / 8, column / 8);
    return block.childAt(row % 8, column % 8);
  }

  @Test
  void cancerCellsAreMagenta() {
    Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));
    cellAt(board, 0, 0).setCancer();
    board.toggle(0, 1);
    BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    Graphics g = image.getGraphics();
    try {
      BoardRenderer.redraw(board, g, new Rectangle(0, 0, 512, 512), true);
    } finally {
      g.dispose();
    }

    assertEquals(Color.MAGENTA.getRGB(), image.getRGB(4, 4));
    assertEquals(Color.RED.getRGB(), image.getRGB(12, 4));
    assertEquals(Colors.LIGHT_YELLOW.getRGB(), image.getRGB(20, 4));
  }
}
