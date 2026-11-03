package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.ui.BoardRenderer;
import com.holub.life.ui.GridCoordinateMapper;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class RenderingExtractionTest {
  private static final int[][] GLIDER = {{1, 2}, {2, 3}, {3, 1}, {3, 2}, {3, 3}};

  private static String render(ClassicBoard board, BufferedImage image, boolean all)
      throws Exception {
    Graphics g = image.getGraphics();
    try {
      BoardRenderer.redraw(board.cell(), g, new Rectangle(0, 0, 512, 512), all);
    } finally {
      g.dispose();
    }
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    for (int y = 0; y < image.getHeight(); y++)
      for (int x = 0; x < image.getWidth(); x++) {
        int pixel = image.getRGB(x, y);
        digest.update((byte) (pixel >>> 24));
        digest.update((byte) (pixel >>> 16));
        digest.update((byte) (pixel >>> 8));
        digest.update((byte) pixel);
      }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static BufferedImage image() {
    return new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
  }

  @Test
  void emptyRenderMatchesOriginal() throws Exception {
    assertEquals(
        "fe3b1391b32590c4dba28941183c3746353a03e7843e009221232e79739ac8f4",
        render(new ClassicBoard(), image(), true));
  }

  @Test
  void fullRenderMatchesOriginal() throws Exception {
    assertEquals(
        "43b86df7e09360564c5bb71d5c289e46ef8cf93d2c4e31df92f52ed7978998f6",
        render(new ClassicBoard().clickAll(GLIDER), image(), true));
  }

  @Test
  void incrementalFramesMatchOriginal() throws Exception {
    ClassicBoard board = new ClassicBoard().clickAll(GLIDER);
    BufferedImage image = image();
    render(board, image, true);
    String[] expected = {
      "031fbf11c4077ff3f66272043103ba5e049a69678fd55f068e9b8e21eea7576c",
      "fee8fc754e54b6237c32fd794b1cf6279849f5b22b61c830ea414dfcc797d106",
      "1f58f9a6c21d24db66853531ad089309ed600e96e2d48abee5c4b60c996eb573",
      "c29e2299d9d903cc5c7ad09163ea4903c0e4d9e6c95d91d440d1d2a18d051b89"
    };
    for (String frame : expected) {
      board.tick();
      assertEquals(frame, render(board, image, false));
    }
  }

  @Test
  void stableBlockGetsItsLastRefresh() throws Exception {
    ClassicBoard board =
        new ClassicBoard().clickAll(new int[][] {{10, 10}, {10, 11}, {11, 10}, {11, 11}});
    BufferedImage image = image();
    render(board, image, true);
    board.tick();
    assertEquals(
        "6a77de1600557b00a79eed9114bbc3c626c5a3927d4685559329f63bcf343998",
        render(board, image, false));
  }

  @Test
  void unevenSurfaceKeepsOriginalClickBehavior() {
    ClassicBoard board = new ClassicBoard();
    Rectangle surface = new Rectangle(0, 0, 511, 511);
    GridCoordinateMapper.userClicked(board.cell(), new Point(63, 63), surface);
    GridCoordinateMapper.userClicked(board.cell(), new Point(0, 0), surface);
    assertEquals(ClassicBoard.cells(new int[][] {{8, 8}, {0, 0}}), board.liveCells());
    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> GridCoordinateMapper.userClicked(board.cell(), new Point(510, 510), surface));
  }
}
