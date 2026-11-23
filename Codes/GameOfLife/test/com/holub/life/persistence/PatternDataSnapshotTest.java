package com.holub.life.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.holub.life.model.BoardSnapshot;
import com.holub.life.model.CellPosition;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PatternDataSnapshotTest {
  @Test
  void preservesCancerAndSourceData() {
    // Given
    var normal = new CellPosition(1, 3);
    var cancer = new CellPosition(2, 4);
    var cells = List.of(cancer, normal, cancer);
    var data = new PatternData(cells, Set.of(cancer));

    // When
    var snapshot = data.toSnapshot(3, 5);

    // Then
    assertEquals(new BoardSnapshot(3, 5, Set.of(normal, cancer), Set.of(cancer)), snapshot);
    assertEquals(cells, data.cells());
    assertEquals(Set.of(cancer), data.cancerCells());
  }

  @Test
  void clipsBothKindsAndDeduplicatesAtBoardBoundary() {
    // Given
    var corner = new CellPosition(3, 4);
    var origin = new CellPosition(0, 0);
    var outside =
        Set.of(
            new CellPosition(-1, 0),
            new CellPosition(0, -1),
            new CellPosition(4, 0),
            new CellPosition(0, 5),
            new CellPosition(Integer.MIN_VALUE, 0),
            new CellPosition(0, Integer.MAX_VALUE));
    var cells = new java.util.ArrayList<>(outside);
    cells.addAll(List.of(corner, corner, origin));
    var cancer = new java.util.HashSet<>(outside);
    cancer.add(corner);

    // When
    var snapshot = new PatternData(cells, cancer).toSnapshot(4, 5);

    // Then
    assertEquals(new BoardSnapshot(4, 5, Set.of(origin, corner), Set.of(corner)), snapshot);
  }

  @ParameterizedTest
  @CsvSource({"1,1", "3,5"})
  void supportsEmptyBoards(int rows, int columns) {
    var data = new PatternData(List.of(), Set.of());
    var snapshot = data.toSnapshot(rows, columns);
    assertEquals(new BoardSnapshot(rows, columns, Set.of(), Set.of()), snapshot);
  }

  @ParameterizedTest
  @CsvSource({"0,1", "1,0", "-1,1", "1,-1"})
  void rejectsNonPositiveDimensions(int rows, int columns) {
    var data = new PatternData(List.of(), Set.of());
    assertThrows(IllegalArgumentException.class, () -> data.toSnapshot(rows, columns));
  }

  @Test
  void rejectsCancerOutsideLiveSet() {
    var cancer = new CellPosition(1, 1);
    var data = new PatternData(List.of(), Set.of(cancer));
    assertThrows(IllegalArgumentException.class, () -> data.toSnapshot(4, 4));
    assertEquals(Set.of(cancer), data.cancerCells());
    assertTrue(data.cells().isEmpty());
  }
}
