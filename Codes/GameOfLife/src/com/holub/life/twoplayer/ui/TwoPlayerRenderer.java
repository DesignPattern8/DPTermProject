package com.holub.life.twoplayer.ui;

import com.holub.life.twoplayer.Coordinate;
import com.holub.life.twoplayer.Player;
import com.holub.life.twoplayer.TwoPlayerSession;
import com.holub.ui.Colors;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * 2인용 보드 그리기 (docs/007-two-player-ui.md 결정 3 A, #69). 세션의 스냅숏만 그리고 {@code Neighborhood}의 읽기 잠금은 쓰지
 * 않는다(#74). 칸 모양(채운 칸 + 위·왼쪽 격자선)은 클래식 {@code BoardRenderer}와 같다.
 */
final class TwoPlayerRenderer {
  static final Color A_COLOR = new Color(0x1f, 0x5f, 0xd0);
  static final Color B_COLOR = new Color(0xd0, 0x30, 0x20);
  static final Color EMPTY_COLOR = Colors.LIGHT_YELLOW;
  static final Color GRID_COLOR = Colors.DARK_YELLOW;
  static final Color BORDER_COLOR = Color.BLACK;

  private TwoPlayerRenderer() {}

  /** {@code here}를 칸 수로 나눠 모든 칸을 칠하고, 임시 배치 칸 테두리와 진영 경계선(가운데 열 왼쪽, 2px)을 그린다. */
  static void render(Graphics g, Rectangle here, TwoPlayerSession.Snapshot snapshot) {
    Player[][] cells = snapshot.cells();
    int rows = cells.length;
    int columns = cells[0].length;
    int width = here.width / columns;
    int height = here.height / rows;
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        int x = here.x + column * width;
        int y = here.y + row * height;
        g.setColor(colorOf(cells[row][column]));
        g.fillRect(x + 1, y + 1, width - 1, height - 1);
        g.setColor(GRID_COLOR);
        g.drawLine(x, y, x, y + height);
        g.drawLine(x, y, x + width, y);
      }
    }
    if (snapshot.tentative().isPresent() && snapshot.currentPlayer() != null) {
      Coordinate at = snapshot.tentative().get();
      g.setColor(colorOf(snapshot.currentPlayer()));
      g.drawRect(
          here.x + at.column() * width + 1, here.y + at.row() * height + 1, width - 3, height - 3);
    }
    g.setColor(BORDER_COLOR);
    g.fillRect(here.x + (columns / 2) * width - 1, here.y, 2, rows * height);
  }

  private static Color colorOf(Player owner) {
    if (owner == null) return EMPTY_COLOR;
    return owner == Player.A ? A_COLOR : B_COLOR;
  }
}
