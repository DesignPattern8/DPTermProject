package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.CellPosition;
import java.util.*;
import org.junit.jupiter.api.Test;

class PatternDataTest {
  @Test
  void copiesInputCells() {
    var cells = new ArrayList<>(List.of(new CellPosition(1, 2)));
    var cancer = new HashSet<>(cells);
    var data = new PatternData(cells, cancer);
    cells.clear();
    cancer.clear();
    assertEquals(List.of(new CellPosition(1, 2)), data.cells());
    assertEquals(Set.of(new CellPosition(1, 2)), data.cancerCells());
    assertThrows(UnsupportedOperationException.class, () -> data.cells().clear());
    assertThrows(UnsupportedOperationException.class, () -> data.cancerCells().clear());
  }

  @Test
  void rejectsNullValues() {
    assertThrows(NullPointerException.class, () -> new PatternData(null, Set.of()));
    assertThrows(
        NullPointerException.class,
        () -> new PatternData(Arrays.asList((CellPosition) null), Set.of()));
    assertThrows(NullPointerException.class, () -> new PatternData(List.of(), null));
  }

  @Test
  void clipsAndDeduplicatesOnlyWhenCreatingSnapshot() {
    var cells =
        List.of(
            new CellPosition(2, 3),
            new CellPosition(2, 3),
            new CellPosition(-1, 0),
            new CellPosition(4, 0),
            new CellPosition(0, 5));
    var data = new PatternData(cells, Set.of());
    var snapshot = data.toClassicSnapshot(4, 5);
    assertEquals(Set.of(new CellPosition(2, 3)), snapshot.liveCells());
    assertEquals(cells, data.cells());
    assertEquals(4, snapshot.rows());
    assertEquals(5, snapshot.columns());
    assertThrows(IllegalArgumentException.class, () -> data.toClassicSnapshot(0, 5));
  }

  @Test
  void rejectsLossyCancerSnapshot() {
    var cell = new CellPosition(1, 2);
    assertThrows(
        IllegalStateException.class,
        () -> new PatternData(List.of(cell), Set.of(cell)).toClassicSnapshot(8, 8));
  }
}
