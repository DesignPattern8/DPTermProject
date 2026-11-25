package com.holub.life.application;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.*;
import com.holub.life.persistence.PatternData;
import java.util.*;
import org.junit.jupiter.api.Test;

class PersistenceMapperTest {
  @Test
  void copiesMutableMemento() {
    var position = new CellPosition(5, 6);
    var state = new NeighborhoodState();
    state.replaceCells(new ArrayList<>(List.of(position)), new HashSet<>(Set.of(position)));
    var data = PersistenceMapper.capture(state);
    state.liveCells().clear();
    state.cancerCells().clear();
    assertEquals(List.of(new CellPosition(5, 6)), data.cells());
    var restored = PersistenceMapper.restore(data);
    restored.liveCells().clear();
    restored.liveCells().add(new CellPosition(1, 1));
    assertEquals(List.of(new CellPosition(5, 6)), data.cells());
    assertEquals(Set.of(new CellPosition(5, 6)), restored.cancerCells());
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
