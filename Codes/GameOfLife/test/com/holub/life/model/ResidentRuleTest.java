package com.holub.life.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ResidentRuleTest {
  @ParameterizedTest(name = "alive={0}, liveNeighbors={1}, nextAlive={2}")
  @CsvSource({
    "false, 0, false",
    "false, 1, false",
    "false, 2, false",
    "false, 3, true",
    "false, 4, false",
    "false, 5, false",
    "false, 6, false",
    "false, 7, false",
    "false, 8, false",
    "true, 0, false",
    "true, 1, false",
    "true, 2, true",
    "true, 3, true",
    "true, 4, false",
    "true, 5, false",
    "true, 6, false",
    "true, 7, false",
    "true, 8, false"
  })
  @DisplayName("RULE-01 Conway 전이표를 계산하고 transition에서 반영한다")
  void conwayTransitionsKeepCalculationSeparateFromCommit(
      boolean alive, int liveNeighbors, boolean expectedAlive) {
    // Given
    Resident cell = new Resident();
    if (alive) cell.toggle(0, 0);
    Cell[] neighbors = new Cell[8];
    Arrays.fill(neighbors, Cell.DUMMY);
    for (int index = 0; index < liveNeighbors; ++index) {
      Resident neighbor = new Resident();
      neighbor.toggle(0, 0);
      neighbors[index] = neighbor;
    }

    // When
    boolean willChange =
        cell.figureNextState(
            neighbors[0], neighbors[1], neighbors[2], neighbors[3],
            neighbors[4], neighbors[5], neighbors[6], neighbors[7]);

    // Then
    assertEquals(alive, cell.isAlive());
    assertEquals(alive != expectedAlive, willChange);
    assertEquals(alive != expectedAlive, cell.transition());
    assertEquals(expectedAlive, cell.isAlive());
  }

  @Test
  @DisplayName("RULE-02 Resident 이웃에 Neighborhood를 전달하면 assert가 실패한다")
  void rejectsNeighborhoodAsResidentNeighbor() {
    // Given
    Resident cell = new Resident();
    Cell invalidNeighbor = new Neighborhood(1, new Resident());
    Cell dead = Cell.DUMMY;

    // When / Then
    assertThrows(
        AssertionError.class,
        () -> cell.figureNextState(invalidNeighbor, dead, dead, dead, dead, dead, dead, dead));
  }
}
