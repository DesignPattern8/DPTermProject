package com.holub.life;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/** 비교 기준용 단순 B3/S23. 보드 밖은 죽은 셀이다. 원본 코드와 독립적으로 작성했다. */
final class ReferenceLife {
  private boolean[][] alive;
  private final int size;

  ReferenceLife(int size, Set<Point> liveCells) {
    this.size = size;
    this.alive = new boolean[size][size];
    for (Point p : liveCells) alive[p.y][p.x] = true;
  }

  void tick() {
    boolean[][] next = new boolean[size][size];
    for (int r = 0; r < size; r++) {
      for (int c = 0; c < size; c++) {
        int n = 0;
        for (int dr = -1; dr <= 1; dr++)
          for (int dc = -1; dc <= 1; dc++) {
            if (dr == 0 && dc == 0) continue;
            int rr = r + dr, cc = c + dc;
            if (rr >= 0 && rr < size && cc >= 0 && cc < size && alive[rr][cc]) n++;
          }
        next[r][c] = n == 3 || (alive[r][c] && n == 2);
      }
    }
    alive = next;
  }

  Set<Point> liveCells() {
    Set<Point> s = new HashSet<>();
    for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) if (alive[r][c]) s.add(new Point(c, r));
    return s;
  }
}
