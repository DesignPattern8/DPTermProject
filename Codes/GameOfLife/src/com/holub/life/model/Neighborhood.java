package com.holub.life.model;

import com.holub.asynch.ConditionVariable;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/***
 * A group of {@link Cell} objects. Cells are grouped into neighborhoods
 * to make board updates more efficient. When a neighborhood is
 * quiescent (none of the Cells it contains are active), then it
 * ignores any requests to update itself.
 *
 * <h3>History</h3>
 * <p>11-29-04
 * Alexy Marinichev fixed the disapearing-glider problem by clearing
 * the active edges in transistion() rather then figureNextState().
 * The original call is commented out and the new line is marked
 * with "(1)"
 *
 * @include /etc/license.txt
 */

public final class Neighborhood implements Cell {
  /**
   * Block if reading is not permitted because the grid is transitioning to the next state. Only one
   * lock is used (for the outermost neighborhood) since all updates must be requested through the
   * outermost neighborhood.
   */
  private static final ConditionVariable readingPermitted = new ConditionVariable(true);

  /**
   * Returns true only if none of the cells in the Neighborhood changed state during the last
   * transition.
   */
  private boolean amActive = false;

  /** The actual grid of Cells contained within this neighborhood. */
  private final Cell[][] grid;

  /** The neighborhood is square, so gridSize is both the horizontal and vertical size. */
  private final int gridSize;

  /**
   * Create a new Neigborhood containing gridSize-by-gridSize clones of the prototype. The Protype
   * is deliberately not put into the grid, so you can reuse it if you like.
   */
  public Neighborhood(int gridSize, Cell prototype) {
    this.gridSize = gridSize;
    this.grid = new Cell[gridSize][gridSize];

    for (int row = 0; row < gridSize; ++row)
      for (int column = 0; column < gridSize; ++column) grid[row][column] = prototype.create();
  }

  /**
   * The "clone" method used to create copies of the current neighborhood. This method is called
   * from the containing neighborhood's constructor. (The current neighborhood is passed into the
   * containing-neighborhood constructor as the "prototype" argument.
   */
  public Cell create() {
    return new Neighborhood(gridSize, grid[0][0]);
  }

  /** Became stable on the last clock tick. One more refresh is required. */
  private boolean oneLastRefreshRequired = false;

  /**
   * Shows the direction of the cells along the edge of the block that will change state in the next
   * transition. For example, if the upper-left corner has changed, then the current Cell is
   * disruptive in the NORTH, WEST, and NORTHWEST directions. If this is the case, the neigboring
   * cells may need to be updated, even if they were previously stable.
   */
  public Direction isDisruptiveTo() {
    return activeEdges;
  }

  private Direction activeEdges = new Direction(Direction.NONE);

  /**
   * Figures the next state of the current neighborhood and the contained neigborhoods (or cells).
   * Does not transition to the next state, however. Note that the neighboring cells are passed in
   * as arguments rather than being stored internally---an example of the Flyweight pattern.
   *
   * @see #transition
   * @param north The neighbor to our north
   * @param south The neighbor to our south
   * @param east The neighbor to our east
   * @param west The neighbor to our west
   * @param northeast The neighbor to our northeast
   * @param northwest The neighbor to our northwest
   * @param southeast The neighbor to our southeast
   * @param southwest The neighbor to our southwest
   * @return true if this neighborhood (i.e. any of it's cells) will change state in the next
   *     transition.
   */
  /**
   * Instrumentation for tests only (#12): the number of neighborhoods, at every nesting level, that
   * recomputed their cells in figureNextState() since the counter was last reset. Package-private
   * on purpose; not part of the public API.
   */
  static int recomputedNeighborhoods = 0;

  public boolean figureNextState(
      Cell north,
      Cell south,
      Cell east,
      Cell west,
      Cell northeast,
      Cell northwest,
      Cell southeast,
      Cell southwest) {
    boolean nothingHappened = true;

    // Is some ajacent neigborhood active on the edge
    // that ajoins me?

    if (amActive
        || north.isDisruptiveTo().the(Direction.SOUTH)
        || south.isDisruptiveTo().the(Direction.NORTH)
        || east.isDisruptiveTo().the(Direction.WEST)
        || west.isDisruptiveTo().the(Direction.EAST)
        || northeast.isDisruptiveTo().the(Direction.SOUTHWEST)
        || northwest.isDisruptiveTo().the(Direction.SOUTHEAST)
        || southeast.isDisruptiveTo().the(Direction.NORTHWEST)
        || southwest.isDisruptiveTo().the(Direction.NORTHEAST)) {
      ++recomputedNeighborhoods;
      Cell northCell,
          southCell,
          eastCell,
          westCell,
          northeastCell,
          northwestCell,
          southeastCell,
          southwestCell;

      // activeEdges.clear();

      for (int row = 0; row < gridSize; ++row) {
        for (int column = 0; column < gridSize; ++column) {
          // Get the current cell's eight neighbors

          if (row == 0) // {=Neighborhood.get.neighbors}
          {
            northwestCell =
                (column == 0)
                    ? northwest.edge(gridSize - 1, gridSize - 1)
                    : north.edge(gridSize - 1, column - 1);

            northCell = north.edge(gridSize - 1, column);

            northeastCell =
                (column == gridSize - 1)
                    ? northeast.edge(gridSize - 1, 0)
                    : north.edge(gridSize - 1, column + 1);
          } else {
            northwestCell =
                (column == 0) ? west.edge(row - 1, gridSize - 1) : grid[row - 1][column - 1];

            northCell = grid[row - 1][column];

            northeastCell =
                (column == gridSize - 1) ? east.edge(row - 1, 0) : grid[row - 1][column + 1];
          }

          westCell = (column == 0) ? west.edge(row, gridSize - 1) : grid[row][column - 1];

          eastCell = (column == gridSize - 1) ? east.edge(row, 0) : grid[row][column + 1];

          if (row == gridSize - 1) {
            southwestCell =
                (column == 0) ? southwest.edge(0, gridSize - 1) : south.edge(0, column - 1);

            southCell = south.edge(0, column);

            southeastCell =
                (column == gridSize - 1) ? southeast.edge(0, 0) : south.edge(0, column + 1);
          } else {
            southwestCell =
                (column == 0) ? west.edge(row + 1, gridSize - 1) : grid[row + 1][column - 1];

            southCell = grid[row + 1][column];

            southeastCell =
                (column == gridSize - 1) ? east.edge(row + 1, 0) : grid[row + 1][column + 1];
          }

          // Tell the cell to change its state. If
          // the cell changed (the figureNextState request
          // returned false), then mark the current block as
          // unstable. Also, if the unstable cell is on the
          // edge of the block modify activeEdges to
          //  indicate which edge or edges changed.

          if (grid[row][column].figureNextState(
              northCell, southCell,
              eastCell, westCell,
              northeastCell, northwestCell,
              southeastCell, southwestCell)) {
            nothingHappened = false;
          }
        }
      }
    }

    if (amActive && nothingHappened) oneLastRefreshRequired = true;

    amActive = !nothingHappened;
    return amActive;
  }

  /**
   * Transition the neighborhood to the previously-computed state.
   *
   * @return true if the transition actually changed anything.
   * @see #figureNextState
   */
  public boolean transition() {
    // The condition variable is set and reset only by the
    // outermost neighborhood. It's actually incorrect
    // for an inner block to touch it because the whole
    // board has to be locked for edge cells in a subblock
    // to compute their next state correctly. There's no
    // race condition since the only place that transition()
    // is called is from the clock tick, and recursively
    // from here. As long as the recompute time is less
    // than the tick interval, everything's copacetic.

    boolean someSubcellChangedState = false;

    if (++nestingLevel == 0) readingPermitted.set(false);

    activeEdges.clear(); /*(1)*/

    for (int row = 0; row < gridSize; ++row) // {=transition.start}
    for (int column = 0; column < gridSize; ++column) {
        if (grid[row][column].transition()) {
          rememberThatCellAtEdgeChangedState(row, column);
          someSubcellChangedState = true;
        } // {=transition.end}
      }

    if (nestingLevel-- == 0) readingPermitted.set(true);

    return someSubcellChangedState;
  }

  // The following variable is used only by the transition()
  // method. Since Java doesn't support static local variables,
  // I am forced to declare it in class scope, but I deliberately
  // don't put it up at the top of the class defintion because
  // it's not really an attribute of the class---it's just
  // an implemenation detail of the immediately preceding
  // method.
  //
  private static int nestingLevel = -1;

  /**
   * Modifies activeEdges to indicate whether the addition of the cell at (row,column) makes an edge
   * active.
   */
  private void rememberThatCellAtEdgeChangedState(int row, int column) {
    if (row == 0) {
      activeEdges.add(Direction.NORTH);

      if (column == 0) activeEdges.add(Direction.NORTHWEST);
      else if (column == gridSize - 1) activeEdges.add(Direction.NORTHEAST);
    } else if (row == gridSize - 1) {
      activeEdges.add(Direction.SOUTH);

      if (column == 0) activeEdges.add(Direction.SOUTHWEST);
      else if (column == gridSize - 1) activeEdges.add(Direction.SOUTHEAST);
    }

    if (column == 0) {
      activeEdges.add(Direction.WEST);
    } else if (column == gridSize - 1) {
      activeEdges.add(Direction.EAST);
    }
    // else it's an internal cell. Do nothing.
  }

  public int gridSize() {
    return gridSize;
  }

  public Cell childAt(int row, int column) {
    return grid[row][column];
  }

  public boolean isActive() {
    return amActive;
  }

  public boolean beginRedraw(boolean drawAll) throws InterruptedException {
    if (!amActive && !oneLastRefreshRequired && !drawAll) return false;
    oneLastRefreshRequired = false;
    if (!readingPermitted.isTrue()) return false;
    readingPermitted.waitForTrue();
    return true;
  }

  /** Return the edge cell in the indicated row and column. */
  public Cell edge(int row, int column) {
    assert (row == 0 || row == gridSize - 1) || (column == 0 || column == gridSize - 1)
        : "central cell requested from edge()";

    return grid[row][column];
  }

  public void toggle(int row, int column) {
    int subcellWidth = grid[0][0].widthInCells();
    int childRow = row / subcellWidth;
    int childColumn = column / subcellWidth;
    grid[childRow][childColumn].toggle(row % subcellWidth, column % subcellWidth);
    amActive = true;
    rememberThatCellAtEdgeChangedState(childRow, childColumn);
  }

  public boolean isAlive() {
    return true;
  }

  public int widthInCells() {
    return gridSize * grid[0][0].widthInCells();
  }

  public void clear() {
    activeEdges.clear();

    for (int row = 0; row < gridSize; ++row)
      for (int column = 0; column < gridSize; ++column) grid[row][column].clear();

    amActive = false;
  }

  /** Cause subcells to add an annotation to the indicated memento if they happen to be alive. */
  public boolean transfer(Memento memento, Point corner, boolean load) {
    int subcellWidth = grid[0][0].widthInCells();
    int myWidth = widthInCells();
    Point upperLeft = new Point(corner);

    for (int row = 0; row < gridSize; ++row) {
      for (int column = 0; column < gridSize; ++column) {
        if (grid[row][column].transfer(memento, upperLeft, load)) amActive = true;

        Direction d = grid[row][column].isDisruptiveTo();

        if (!d.equals(Direction.NONE)) activeEdges.add(d);

        upperLeft.translate(subcellWidth, 0);
      }
      upperLeft.translate(-myWidth, subcellWidth);
    }
    return amActive;
  }

  public NeighborhoodState createMemento() {
    NeighborhoodState m = new NeighborhoodState();
    transfer(m, new Point(0, 0), Cell.STORE);
    return m;
  }

  public boolean isCancer() {
    return false; // Neighborhood 자체는 개별 세포가 아니므로 암세포가 아님
  }

  public void setCancer() {
    // 암세포 전환은 개별 Resident에만 적용
  }

  public void cureCancer() {
    // 복합 셀 자체는 치료 대상이 아님
  }

  /** 중첩된 Neighborhood를 실제 보드 좌표에 맞춰 펼친다. */
  private void fillResidents(Resident[][] board, int top, int left) {
    int subcellWidth = grid[0][0].widthInCells();

    for (int row = 0; row < gridSize; row++) {
      for (int column = 0; column < gridSize; column++) {
        Cell cell = grid[row][column];
        int cellTop = top + row * subcellWidth;
        int cellLeft = left + column * subcellWidth;

        if (cell instanceof Resident) {
          board[cellTop][cellLeft] = (Resident) cell;
        } else if (cell instanceof Neighborhood) {
          ((Neighborhood) cell).fillResidents(board, cellTop, cellLeft);
        }
      }
    }
  }

  private Resident[][] residentBoard() {
    int size = widthInCells();
    Resident[][] board = new Resident[size][size];
    fillResidents(board, 0, 0);

    return board;
  }

  public void tryNaturalCancer(double probability, Random random) {
    if (random.nextDouble() >= probability) return;

    Resident[][] board = residentBoard();
    List<Resident> candidates = new ArrayList<Resident>();

    for (Resident[] row : board) {
      for (Resident cell : row) {
        if (cell != null && cell.isAlive() && !cell.isCancer()) {
          candidates.add(cell);
        }
      }
    }

    if (!candidates.isEmpty()) {
      candidates.get(random.nextInt(candidates.size())).setCancer();
    }
  }

  public void applyInvasion(double probability, Random random) {
    Resident[][] board = residentBoard();
    int size = board.length;
    List<Resident> infected = new ArrayList<Resident>();

    // 먼저 감염 대상을 모아야 같은 세대에 감염이 연쇄적으로 무한히 번지는 일을 막을 수 있음
    for (int row = 0; row < size; row++) {
      for (int column = 0; column < size; column++) {
        Resident cell = board[row][column];
        if (cell == null || !cell.isAlive() || cell.isCancer()) continue;

        int cancerNeighbors = 0;

        for (int dr = -1; dr <= 1; dr++) {
          for (int dc = -1; dc <= 1; dc++) {
            if (dr == 0 && dc == 0) continue;

            int r = row + dr;
            int c = column + dc;
            if (r >= 0 && r < size && c >= 0 && c < size) {
              Resident neighbor = board[r][c];
              if (neighbor != null && neighbor.isAlive() && neighbor.isCancer()) {
                cancerNeighbors++;
              }
            }
          }
        }

        double combinedProbability =
            1.0 - Math.pow(1.0 - probability, cancerNeighbors); // 인접한 암세포가 많을수록 감염 확률 증가

        if (cancerNeighbors > 0 && random.nextDouble() < combinedProbability) {
          infected.add(cell);
        }
      }
    }

    for (Resident cell : infected) {
      cell.setCancer();
    }
  }

  public int cancerCount() {
    Resident[][] board = residentBoard();
    int count = 0;

    for (Resident[] row : board) {
      for (Resident cell : row) {
        if (cell != null && cell.isAlive() && cell.isCancer()) count++;
      }
    }

    return count;
  }

  public void treatAt(int row, int column) {
    Resident[][] board = residentBoard();
    int size = board.length;

    // 클릭한 칸과 주변 한 칸(대각선 포함)을 치료
    for (int dr = -1; dr <= 1; dr++) {
      for (int dc = -1; dc <= 1; dc++) {
        int r = row + dr;
        int c = column + dc;

        if (r >= 0 && r < size && c >= 0 && c < size) {
          Resident cell = board[r][c];
          if (cell != null) cell.cureCancer();
        }
      }
    }
  }

  /** 살아 있는 일반 세포 하나를 무작위로 골라, 대각선을 포함해 연결된 살아 있는 일반 세포만 암세포로 바꾼다. */
  public void tryCancerOutbreak(double probability, Random random) {
    if (random.nextDouble() >= probability) return;

    int boardWidth = widthInCells();
    Resident[][] board = residentBoard();
    List<CellPosition> candidates =
        new ArrayList<CellPosition>(); // 암 발생 시작점은 살아 있는 일반 세포 중에서 무작위로 고름

    for (int row = 0; row < boardWidth; row++) {
      for (int column = 0; column < boardWidth; column++) {
        Resident cell = board[row][column];
        if (cell != null && cell.isAlive() && !cell.isCancer()) {
          candidates.add(new CellPosition(row, column));
        }
      }
    }

    if (candidates.isEmpty()) return;

    CellPosition start = candidates.get(random.nextInt(candidates.size()));
    boolean[][] visited = new boolean[boardWidth][boardWidth];
    ArrayDeque<CellPosition> queue = new ArrayDeque<CellPosition>();

    queue.add(start);
    visited[start.row()][start.column()] = true;

    while (!queue.isEmpty()) {
      CellPosition position = queue.removeFirst();
      Resident cell = board[position.row()][position.column()];

      cell.setCancer(); // Neighborhood 전체가 아니라, 연결 탐색으로 찾은 Resident 하나만 변경

      for (int dr = -1; dr <= 1; ++dr) {
        for (int dc = -1; dc <= 1; ++dc) {
          if (dr == 0 && dc == 0) continue;

          int nextRow = position.row() + dr;
          int nextColumn = position.column() + dc;

          if (nextRow < 0
              || nextRow >= boardWidth
              || nextColumn < 0
              || nextColumn >= boardWidth
              || visited[nextRow][nextColumn]) {
            continue;
          }

          Resident neighbor = board[nextRow][nextColumn];

          if (neighbor != null && neighbor.isAlive() && !neighbor.isCancer()) {
            visited[nextRow][nextColumn] = true;
            queue.addLast(new CellPosition(nextRow, nextColumn));
          }
        }
      }
    }
  }
}
