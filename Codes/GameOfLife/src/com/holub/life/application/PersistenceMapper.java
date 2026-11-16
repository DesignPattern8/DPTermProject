package com.holub.life.application;

import com.holub.life.model.CellPosition;
import com.holub.life.model.NeighborhoodState;
import com.holub.life.persistence.PatternData;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Maps model mementos and file values without traversing or changing the board. */
public final class PersistenceMapper {
  private PersistenceMapper() {}

  public static PatternData capture(NeighborhoodState state) {
    List<CellPosition> live = new ArrayList<>();
    for (Object value : state.liveCells()) {
      Point point = (Point) value;
      live.add(new CellPosition(point.y, point.x));
    }
    Set<CellPosition> cancer = new HashSet<>();
    for (Point point : state.cancerCells()) cancer.add(new CellPosition(point.y, point.x));
    return new PatternData(live, cancer);
  }

  public static NeighborhoodState restore(PatternData data) {
    List<Point> live = new LinkedList<>();
    for (CellPosition cell : data.cells()) live.add(new Point(cell.column(), cell.row()));
    Set<Point> cancer = new HashSet<>();
    for (CellPosition cell : data.cancerCells()) cancer.add(new Point(cell.column(), cell.row()));
    NeighborhoodState state = new NeighborhoodState();
    state.replaceCells(live, cancer);
    return state;
  }
}
