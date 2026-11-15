package com.holub.life.twoplayer;

import com.holub.life.model.Cell;
import com.holub.life.model.CellPosition;
import com.holub.life.model.CompositeBoard;
import com.holub.life.model.Direction;
import com.holub.life.model.Neighborhood;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.model.Resident;
import java.awt.Point;
import java.util.Random;

/**
 * 006 설계 결정의 측정 코드. JUnit 테스트가 아니다({@code @Test} 없음, {@code ./gradlew test}에서 실행되지 않음).
 *
 * <p>후보 F(model 무수정, {@code Neighborhood} 재사용 + 2인용 셀)와 후보 G(twoplayer 전용 격자)를 프로토타입으로 만들어
 * 아래를 확인하고 측정한다.
 *
 * <ol>
 *   <li>동등성: 무작위 보드에서 F와 G의 칸별 색이 같고, 살아 있는 칸이 클래식 {@code Resident}와 같다.
 *   <li>명세 예시 4의 세대별 셀 수, Neighborhood 경계를 넘는 탄생(TP-R-10), 정체 뒤 배치(TP-R-11).
 *   <li>{@code CompositeBoard}로 F의 루트를 감쌌을 때 되는 것과 깨지는 것.
 *   <li>tick당 시간(클래식, F, G)과 {@code count(owner)} 1회 시간.
 * </ol>
 *
 * <p>실행 방법은 {@code twoplayer/docs/006-two-player-cell.md}의 "측정 방법". 프로토타입 셀은 측정용이며 실제 구현이 아니다.
 */
final class CellCandidateBenchmark {
  private static final int SIZE = 64;
  private static final long SEED = 42;
  private static final double DENSITY = 0.3;
  private static final int TICKS = 200;
  private static final int REPEATS = 10;

  private CellCandidateBenchmark() {}

  /** 후보 F의 프로토타입 셀. owner가 null이면 DEAD. */
  private static final class PrototypeCell implements Cell {
    private Player owner;
    private Player next;
    /** place 직전에 보드가 정한다. toggle이 DEAD 칸을 이 소유자로 살린다. */
    private Player armed;

    @Override
    public boolean figureNextState(
        Cell north,
        Cell south,
        Cell east,
        Cell west,
        Cell northeast,
        Cell northwest,
        Cell southeast,
        Cell southwest) {
      Cell[] adjacent = {north, south, east, west, northeast, northwest, southeast, southwest};
      int a = 0;
      int b = 0;
      for (Cell cell : adjacent) {
        if (cell instanceof PrototypeCell neighbor && neighbor.owner != null) {
          if (neighbor.owner == Player.A) {
            a++;
          } else {
            b++;
          }
        }
      }
      int live = a + b;
      if (owner != null) {
        next = (live == 2 || live == 3) ? owner : null;
      } else {
        next = live == 3 ? (a >= 2 ? Player.A : Player.B) : null;
      }
      return next != owner;
    }

    @Override
    public Cell edge(int row, int column) {
      return this;
    }

    @Override
    public boolean transition() {
      boolean changed = next != owner;
      owner = next;
      return changed;
    }

    @Override
    public void toggle(int row, int column) {
      owner = (owner == null) ? armed : null;
      next = owner;
      armed = null;
    }

    @Override
    public boolean isAlive() {
      return owner != null;
    }

    @Override
    public int widthInCells() {
      return 1;
    }

    @Override
    public Cell create() {
      return new PrototypeCell();
    }

    @Override
    public Direction isDisruptiveTo() {
      return next == owner ? Direction.NONE : Direction.ALL;
    }

    @Override
    public void clear() {
      owner = null;
      next = null;
    }

    @Override
    public boolean transfer(Memento memento, Point upperLeft, boolean doLoad) {
      return false;
    }

    @Override
    public NeighborhoodState createMemento() {
      throw new UnsupportedOperationException("two-player store is not supported");
    }

    @Override
    public boolean isCancer() {
      return false;
    }

    @Override
    public void setCancer() {}

    @Override
    public void cureCancer() {}
  }

  /** 후보 F: Neighborhood(8, Neighborhood(8, PrototypeCell)) + 칸 색 조회용 평면 색인. */
  private static final class CandidateF {
    final Neighborhood root = new Neighborhood(8, new Neighborhood(8, new PrototypeCell()));
    final PrototypeCell[][] cells = new PrototypeCell[SIZE][SIZE];

    CandidateF() {
      for (int r = 0; r < 8; r++) {
        for (int c = 0; c < 8; c++) {
          Neighborhood inner = (Neighborhood) root.childAt(r, c);
          for (int rr = 0; rr < 8; rr++) {
            for (int cc = 0; cc < 8; cc++) {
              cells[r * 8 + rr][c * 8 + cc] = (PrototypeCell) inner.childAt(rr, cc);
            }
          }
        }
      }
    }

    void place(int row, int column, Player owner) {
      cells[row][column].armed = owner;
      root.toggle(row, column);
    }

    void step() {
      Cell outside = Cell.DUMMY;
      if (root.figureNextState(
          outside, outside, outside, outside, outside, outside, outside, outside)) {
        root.transition();
      }
    }

    Player owner(int row, int column) {
      return cells[row][column].owner;
    }

    int count(Player owner) {
      int n = 0;
      for (PrototypeCell[] row : cells) {
        for (PrototypeCell cell : row) {
          if (cell.owner == owner) {
            n++;
          }
        }
      }
      return n;
    }
  }

  /** 후보 G: twoplayer 전용 격자. 0 = DEAD, 1 = A, 2 = B. 보드 밖은 DEAD. */
  private static final class CandidateG {
    int[][] grid = new int[SIZE][SIZE];
    int[][] nextGrid = new int[SIZE][SIZE];

    void place(int row, int column, Player owner) {
      grid[row][column] = code(owner);
    }

    void step() {
      for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
          int a = 0;
          int b = 0;
          for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
              int rr = r + dr;
              int cc = c + dc;
              if ((dr == 0 && dc == 0) || rr < 0 || rr >= SIZE || cc < 0 || cc >= SIZE) {
                continue;
              }
              if (grid[rr][cc] == 1) {
                a++;
              } else if (grid[rr][cc] == 2) {
                b++;
              }
            }
          }
          int live = a + b;
          int state = grid[r][c];
          if (state != 0) {
            nextGrid[r][c] = (live == 2 || live == 3) ? state : 0;
          } else {
            nextGrid[r][c] = live == 3 ? (a >= 2 ? 1 : 2) : 0;
          }
        }
      }
      int[][] swap = grid;
      grid = nextGrid;
      nextGrid = swap;
    }

    int count(Player owner) {
      int target = code(owner);
      int n = 0;
      for (int[] row : grid) {
        for (int state : row) {
          if (state == target) {
            n++;
          }
        }
      }
      return n;
    }

    static int code(Player owner) {
      return owner == null ? 0 : (owner == Player.A ? 1 : 2);
    }
  }

  /** 기준: 클래식 보드(Life.java와 같은 구성). */
  private static final class Classic {
    final Neighborhood root = new Neighborhood(8, new Neighborhood(8, new Resident()));

    void step() {
      Cell outside = Cell.DUMMY;
      if (root.figureNextState(
          outside, outside, outside, outside, outside, outside, outside, outside)) {
        root.transition();
      }
    }

    boolean isAlive(int row, int column) {
      Neighborhood inner = (Neighborhood) root.childAt(row / 8, column / 8);
      return inner.childAt(row % 8, column % 8).isAlive();
    }
  }

  /** 같은 무작위 보드(시드 고정, 왼쪽 절반 A, 오른쪽 절반 B)를 세 후보에 넣는다. */
  private static final class Boards {
    final CandidateF f = new CandidateF();
    final CandidateG g = new CandidateG();
    final Classic classic = new Classic();

    Boards() {
      Random random = new Random(SEED);
      for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
          if (random.nextDouble() < DENSITY) {
            Player owner = c < SIZE / 2 ? Player.A : Player.B;
            f.place(r, c, owner);
            g.place(r, c, owner);
            classic.root.toggle(r, c);
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    System.out.printf(
        "env: java %s, %s %s, %d processors, assertions %s%n",
        System.getProperty("java.version"),
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        Runtime.getRuntime().availableProcessors(),
        assertionsEnabled() ? "on" : "off");
    checkEquivalence();
    checkExample4();
    checkBirthAcrossNeighborhoodBoundary();
    checkPlacementAfterQuiescence();
    checkCompositeBoardReuse();
    measureTickTime();
    measureCount();
  }

  private static boolean assertionsEnabled() {
    boolean enabled = false;
    assert enabled = true;
    return enabled;
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException("check failed: " + message);
    }
  }

  /** 1. F와 G의 칸별 색이 같고, 살아 있는 칸이 클래식과 같다(TP-R-15 불변조건). */
  private static void checkEquivalence() {
    Boards boards = new Boards();
    for (int generation = 0; generation <= TICKS; generation++) {
      for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
          Player owner = boards.f.owner(r, c);
          check(
              CandidateG.code(owner) == boards.g.grid[r][c],
              "F != G at generation " + generation + " (" + r + "," + c + ")");
          check(
              (owner != null) == boards.classic.isAlive(r, c),
              "F alive != classic at generation " + generation + " (" + r + "," + c + ")");
        }
      }
      boards.f.step();
      boards.g.step();
      boards.classic.step();
    }
    System.out.printf(
        "equivalence: %d generations, F == G (owner per cell), F alive == classic%n", TICKS);
  }

  /** 2-1. 명세 예시 4: glider와 block이 부딪혀 세대 10에 0:0. */
  private static void checkExample4() {
    CandidateF f = new CandidateF();
    int[][] glider = {{0, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}};
    int[][] block = {{5, 5}, {5, 6}, {6, 5}, {6, 6}};
    for (int[] p : glider) {
      f.place(p[0], p[1], Player.A);
    }
    for (int[] p : block) {
      f.place(p[0], p[1], Player.B);
    }
    int[][] expected = {
      {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {5, 4}, {6, 5}, {3, 3}, {1, 2}, {0, 0}
    };
    StringBuilder line = new StringBuilder("example 4 (A:B by generation):");
    for (int generation = 0; generation < expected.length; generation++) {
      int a = f.count(Player.A);
      int b = f.count(Player.B);
      check(
          a == expected[generation][0] && b == expected[generation][1],
          "example 4 generation " + generation);
      line.append(' ').append(a).append(':').append(b);
      f.step();
    }
    System.out.println(line + " (matches rules.md 7)");
  }

  /** 2-2. TP-R-10: (7,7)·(7,8)·(8,7)에 A 2 + B 1이면 블록 경계 건너편 (8,8)이 A로 태어난다. */
  private static void checkBirthAcrossNeighborhoodBoundary() {
    CandidateF f = new CandidateF();
    f.place(7, 7, Player.A);
    f.place(7, 8, Player.A);
    f.place(8, 7, Player.B);
    f.step();
    check(f.owner(8, 8) == Player.A, "TP-R-10");
    System.out.println("TP-R-10: (8,8) born as A across the Neighborhood boundary");
  }

  /** 2-3. TP-R-11: 정체한 보드에 블록 경계 옆으로 배치해도 F와 G가 계속 같다. */
  private static void checkPlacementAfterQuiescence() {
    Boards boards = new Boards();
    int quietGenerations = 400;
    for (int i = 0; i < quietGenerations; i++) {
      boards.f.step();
      boards.g.step();
    }
    int[][] placements = {{15, 15}, {15, 16}, {16, 15}, {40, 39}, {40, 40}, {39, 40}};
    Player[] owners = {Player.A, Player.A, Player.B, Player.B, Player.B, Player.A};
    int placed = 0;
    for (int i = 0; i < placements.length; i++) {
      int r = placements[i][0];
      int c = placements[i][1];
      if (boards.f.owner(r, c) == null) {
        boards.f.place(r, c, owners[i]);
        boards.g.place(r, c, owners[i]);
        placed++;
      }
    }
    for (int generation = 0; generation < TICKS; generation++) {
      for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
          check(
              CandidateG.code(boards.f.owner(r, c)) == boards.g.grid[r][c],
              "after quiescence, generation " + generation);
        }
      }
      boards.f.step();
      boards.g.step();
    }
    System.out.printf(
        "TP-R-11: %d generations, %d cells placed at block edges, then %d generations F == G%n",
        quietGenerations, placed, TICKS);
  }

  /** 3. F의 루트를 CompositeBoard로 감쌌을 때. */
  private static void checkCompositeBoardReuse() {
    CandidateF direct = new CandidateF();
    CandidateF wrapped = new CandidateF();
    CompositeBoard board = new CompositeBoard(wrapped.root);
    int[][] glider = {{0, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}};
    for (int[] p : glider) {
      direct.place(p[0], p[1], Player.A);
      wrapped.place(p[0], p[1], Player.A);
    }
    direct.place(30, 40, Player.B);
    wrapped.place(30, 40, Player.B);
    for (int i = 0; i < 20; i++) {
      direct.step();
      board.step();
    }
    boolean sameAfterStep = true;
    for (int r = 0; r < SIZE; r++) {
      for (int c = 0; c < SIZE; c++) {
        sameAfterStep &= direct.owner(r, c) == wrapped.owner(r, c);
      }
    }
    check(sameAfterStep, "CompositeBoard.step() == direct step");
    System.out.printf(
        "CompositeBoard.step(): 20 generations same as direct (rows=%d, columns=%d)%n",
        board.rows(), board.columns());

    board.toggle(new CellPosition(10, 10));
    System.out.printf(
        "CompositeBoard.toggle() on DEAD without owner: (10,10) = %s (no owner parameter)%n",
        wrapped.owner(10, 10));

    int liveBefore = wrapped.count(Player.A) + wrapped.count(Player.B);
    var snapshot = board.snapshot();
    System.out.printf(
        "CompositeBoard.snapshot(): %d positions for %d live cells (A=%d, B=%d), owner not kept%n",
        snapshot.liveCells().size(),
        liveBefore,
        wrapped.count(Player.A),
        wrapped.count(Player.B));
    board.restore(snapshot);
    System.out.printf(
        "CompositeBoard.restore(snapshot): A=%d, B=%d after restore (was %d live)%n",
        wrapped.count(Player.A), wrapped.count(Player.B), liveBefore);

    board.clear();
    System.out.printf(
        "CompositeBoard.clear(): A=%d, B=%d%n", wrapped.count(Player.A), wrapped.count(Player.B));
  }

  /** 4-1. tick당 시간. 같은 무작위 보드, TICKS번 진행, REPEATS번 중 최소. */
  private static void measureTickTime() {
    for (int warmup = 0; warmup < 3; warmup++) {
      timeTicks(0);
      timeTicks(1);
      timeTicks(2);
    }
    System.out.printf(
        "tick ms (64x64, density %.1f, seed %d, %d ticks, best of %d): "
            + "classic=%.4f F=%.4f G=%.4f%n",
        DENSITY,
        SEED,
        TICKS,
        REPEATS,
        timeTicks(0),
        timeTicks(1),
        timeTicks(2));
  }

  private static double timeTicks(int candidate) {
    double best = Double.MAX_VALUE;
    for (int repeat = 0; repeat < REPEATS; repeat++) {
      Boards boards = new Boards();
      long start = System.nanoTime();
      for (int i = 0; i < TICKS; i++) {
        switch (candidate) {
          case 0 -> boards.classic.step();
          case 1 -> boards.f.step();
          default -> boards.g.step();
        }
      }
      best = Math.min(best, (System.nanoTime() - start) / 1e6 / TICKS);
    }
    return best;
  }

  /** 4-2. count(owner) 1회 시간 (전체 순회 4,096칸). */
  private static void measureCount() {
    Boards boards = new Boards();
    int calls = 10_000;
    long sink = 0;
    for (int i = 0; i < calls; i++) {
      sink += boards.f.count(Player.A) + boards.g.count(Player.A);
    }
    long start = System.nanoTime();
    for (int i = 0; i < calls; i++) {
      sink += boards.f.count(Player.A);
    }
    double f = (System.nanoTime() - start) / 1e6 / calls;
    start = System.nanoTime();
    for (int i = 0; i < calls; i++) {
      sink += boards.g.count(Player.A);
    }
    double g = (System.nanoTime() - start) / 1e6 / calls;
    System.out.printf(
        "count(owner) ms per call (%d calls): F=%.5f G=%.5f (checksum %d)%n", calls, f, g, sink);
  }
}
