package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 클래식 모드 셀 단위 특성 테스트 (docs/test-cases.md CL-C). */
class ClassicCellTest {
  private static final Cell D = Cell.DUMMY;

  private static Resident liveResident() {
    Resident r = new Resident();
    r.userClicked(null, null); // Resident.userClicked는 인자를 쓰지 않고 상태만 뒤집는다
    return r;
  }

  @Test
  @DisplayName("CL-C-01 새 Resident는 죽어 있다")
  void newResidentIsDead() {
    assertFalse(new Resident().isAlive());
  }

  @Test
  @DisplayName("CL-C-02 고립된 산 셀은 다음 상태가 바뀐다고 알리고, transition 뒤 죽는다")
  void isolatedLiveResidentDies() {
    // Given
    Resident cell = liveResident();
    // When
    boolean willChange = cell.figureNextState(D, D, D, D, D, D, D, D);
    cell.transition();
    // Then
    assertTrue(willChange);
    assertFalse(cell.isAlive());
  }

  @Test
  @DisplayName("CL-C-03 Resident.transition()은 상태가 바뀐 셀에 true, 안 바뀐 셀에 false를 반환한다")
  void residentTransitionReportsChange() {
    // Given
    Resident changing = liveResident();
    changing.figureNextState(D, D, D, D, D, D, D, D);
    Resident unchanged = new Resident();
    unchanged.figureNextState(D, D, D, D, D, D, D, D);
    // When
    boolean changingResult = changing.transition();
    boolean unchangedResult = unchanged.transition();
    // Then
    assertTrue(changingResult);
    assertFalse(unchangedResult);
  }

  @Test
  @DisplayName("CL-C-04 DUMMY는 항상 죽어 있고 가장자리 요청에 자기 자신을 돌려준다")
  void dummyIsAlwaysDead() {
    assertFalse(D.isAlive());
    assertSame(D, D.edge(0, 0));
    assertSame(D, D.edge(5, 5));
  }

  @Test
  @DisplayName("CL-C-05 [현재 동작] Neighborhood.isAlive()는 빈 보드에서도 true다")
  void neighborhoodIsAliveIsAlwaysTrue() {
    assertTrue(new ClassicBoard().cell().isAlive());
  }

  @Test
  @DisplayName("CL-C-06 Universe와 같은 구성의 보드는 폭이 64칸이다")
  void boardIs64CellsWide() {
    assertEquals(64, new ClassicBoard().cell().widthInCells());
  }

  @Test
  @DisplayName("CL-C-07 Neighborhood.edge()에 가운데 칸을 요청하면 assert가 실패한다")
  void edgeRejectsCentralCell() {
    // Given
    Neighborhood block = new Neighborhood(8, new Resident());
    // When / Then
    assertThrows(AssertionError.class, () -> block.edge(3, 3));
  }

  @Test
  @DisplayName("CL-C-08 glider 하나만 움직일 때는 그 근처의 하위 블록만 다시 계산한다 (tick마다 Neighborhood 5개 이하)")
  void onlyBlocksNearChangesAreRecomputed() {
    // Given: 64×64 보드에 glider 하나, 첫 tick은 클릭으로 생긴 활성 표시를 소화한다
    ClassicBoard board =
        new ClassicBoard().clickAll(new int[][] {{1, 2}, {2, 3}, {3, 1}, {3, 2}, {3, 3}}).tick();
    for (int tick = 2; tick <= 10; tick++) {
      // When
      Neighborhood.recomputedNeighborhoods = 0;
      board.tick();
      // Then: 바깥 블록 1 + glider가 걸친 하위 블록과 그 이웃만 (수정 전에는 64 + 1 = 65)
      assertTrue(
          Neighborhood.recomputedNeighborhoods <= 5,
          "tick " + tick + ": " + Neighborhood.recomputedNeighborhoods + "개 다시 계산");
    }
  }
}
