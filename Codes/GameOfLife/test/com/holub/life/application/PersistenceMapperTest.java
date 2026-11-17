package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import com.holub.life.persistence.PatternData;
import java.awt.Point;
import java.util.*;
import org.junit.jupiter.api.Test;

class PersistenceMapperTest {
  @Test
  void copiesMutableMemento() {
    var point = new Point(6, 5);
    var state = new NeighborhoodState();
    state.replaceCells(new ArrayList<>(List.of(point)), new HashSet<>(Set.of(new Point(point))));
    var data = PersistenceMapper.capture(state);
    point.move(0, 0);
    state.cancerCells().clear();
    assertEquals(List.of(new CellPosition(5, 6)), data.cells());
    var restored = PersistenceMapper.restore(data);
    ((Point) restored.liveCells().iterator().next()).move(1, 1);
    assertEquals(List.of(new CellPosition(5, 6)), data.cells());
    assertEquals(Set.of(new Point(6, 5)), restored.cancerCells());
  }

  @Test
  void restoresClassicSnapshot() {
    var data =
        new PatternData(
            List.of(
                new CellPosition(1, 2),
                new CellPosition(1, 2),
                new CellPosition(-1, 2),
                new CellPosition(10, 10)),
            Set.of());
    Board board = new CompositeBoard(new Neighborhood(4, new Resident()));
    board.restore(data.toClassicSnapshot(board.rows(), board.columns()));
    assertEquals(Set.of(new CellPosition(1, 2)), board.snapshot().liveCells());
  }
}
