# 모델 테스트

[기존 CL-G/E/C 케이스](../../docs/test-cases.md)의 입력·기대값을 유지한다. ClassicCellTest의 초기 셀 토글 호출은 userClicked(null,null)에서 추출된 GridCoordinateMapper.userClicked(r,null,null)로만 바꾼다. 기존 테스트 묶음은 package-private 도우미와 함께 유지한다. [EXT-M01~03](../../docs/domain-extraction-tests.md)도 상태 복원 경계를 검증한다.

## main 병합 후 CI 복구 (#29)

CL-C-08은 모델 내부 계측 필드를 사용하는 `model.NeighborhoodRecomputationTest#onlyBlocksNearChangesAreRecomputed`로 이동한다. 보드 크기, glider 좌표, tick 2~10의 재계산 5개 이하 검증은 유지한다. 나머지 정상·경계·예외 CL-C 케이스는 기존 정의 그대로다.

## 클래식 보드 API (#49)

[클래식 보드 계약](interfaces.md#클래식-보드-계약)을 기존 클래식 특성 테스트와 함께 Swing 없이 검증한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| BOARD-01 | 정상 | 세대 진행과 변경 결과 | 블링커와 안정 블록에 각각 `step()` | 블링커는 다음 배치와 `true`, 안정 블록은 같은 배치와 `false` | `CompositeBoardTest#stepReportsWhetherCellStatesChanged` |
| BOARD-02 | 정상 | 전체 배치 복원 | 스냅샷 캡처 → `clear()` → 다른 칸 편집 → `restore()` | 복원한 배치가 캡처 시점과 같고 새로 편집한 칸은 사라진다. | `CompositeBoardTest#restoreReplacesTheEntireBoard` |
| BOARD-03 | 경계 | 좌표 순서와 캡처 값의 독립성 | 오른쪽 위 모서리를 편집하고 캡처한 뒤 같은 칸을 다시 편집 | 스냅샷에 `(0, columns - 1)`이 남는다. | `CompositeBoardTest#snapshotKeepsTopRightPositionAfterEditingBoard` |
| BOARD-04 | 경계 | 복원 후 블록 경계 계산 | 하위 블록 마지막 행의 수평 블링커를 직접 배치한 보드와 복원한 보드에서 각각 한 세대 진행 | 두 결과가 같고 인접 블록에도 세로 블링커의 셀이 탄생한다. | `CompositeBoardTest#restoredBlinkerCrossesNeighborhoodBoundaryOnFirstStep` |
| BOARD-05 | 경계 | 스냅샷 컬렉션의 불변성 | 가변 집합으로 스냅샷을 생성한 뒤 원본 집합을 비우고 반환된 집합의 변경을 시도 | 원래 생존 좌표가 남고 반환된 집합의 변경은 거부된다. | `CompositeBoardTest#snapshotDefensivelyCopiesLiveCells` |
| BOARD-06 | 예외 | 범위 밖 편집 거부 | 음수 또는 보드 크기와 같은 좌표를 `toggle`에 전달 | `IndexOutOfBoundsException`이 발생하고 기존 배치를 유지한다. | `CompositeBoardTest#toggleRejectsOutOfBoundsPositionsWithoutChangingBoard` |
| BOARD-07 | 예외 | 크기 불일치 복원 거부 | 다른 크기의 유효한 스냅샷을 `restore`에 전달 | `IllegalArgumentException`이 발생하고 기존 배치를 유지한다. | `CompositeBoardTest#restoreRejectsSizeMismatchWithoutChangingBoard` |
| BOARD-08 | 경계 | 캡처의 부수효과 방지 | 보드 내부 하위 블록의 남쪽 경계 (23, 20)를 편집하고 캡처 | 루트 방향은 NONE, 하위 블록 방향은 SOUTH로 유지된다. | `CompositeBoardTest#snapshotDoesNotChangeActiveEdges` |
| BOARD-09 | 예외 | 스냅샷 생성 시 크기·좌표 검사 | 0인 크기 또는 크기 밖 생존 좌표를 전달 | `IllegalArgumentException`이 발생한다. | `CompositeBoardTest#snapshotRejectsInvalidDimensionsAndCoordinates` |

## 클래식 생사 판정 분리 (#52)

`Resident.figureNextState()`와 `transition()`의 공개 경로로 판정과 반영 시점을 확인한다. 다음 케이스를 함수 추출 전에 작성·실행하고 추출 후 같은 기대값으로 실행한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| RULE-01 | 정상·경계 | Conway 전이표와 반영 시점 | 현재 생사 2가지 × 살아 있는 이웃 수 0~8의 18가지 조합 | 죽은 셀은 이웃 3에서 탄생하고 산 셀은 2·3에서 생존한다. 계산 직후 현재 생사는 그대로이며 `transition()` 후 결과와 변경 여부가 일치한다. | `ResidentRuleTest#conwayTransitionsKeepCalculationSeparateFromCommit` |
| RULE-02 | 예외 | 잘못된 이웃 타입 검증 | 이웃 하나로 `Neighborhood` 전달, 나머지는 `Cell.DUMMY` | assert가 켜진 기존 테스트 환경에서 `AssertionError`가 발생한다. | `ResidentRuleTest#rejectsNeighborhoodAsResidentNeighbor` |

Block·Blinker·Glider, 보드 모서리·하위 블록 경계, 고정 시드의 참조 모델 비교는 기존 `ClassicGenerationTest`로 회귀 검증한다. 보드 API의 변경 반환값과 복원 경계는 `CompositeBoardTest`를 사용한다.

## 암세포 모델 (PR #60)

암세포 규칙과 발생·전이·치료 연산을 Swing 없이 검증한다. 무작위 값은 항상 같은 값을 돌려주는 `Random` 대역(`FixedRandom`)으로 고정한다. `nextInt`가 0이면 행 우선 순서로 첫 후보를 고른다. 일반 세포 규칙(RULE-01)과 기존 CL 케이스는 그대로 통과해야 한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CAN-M01 | 정상·경계 | 암세포는 살아 있는 이웃이 1~5개일 때만 암세포로 남는다 | 암세포 1개 + 살아 있는 이웃 0~8개 | 1~5개면 살아 있는 암세포로 남고, 0개이거나 6개 이상이면 죽는다 | `CancerModelTest#cancerCellSurvivesWithOneToFiveLiveNeighbors` |
| CAN-M02 | 정상·경계 | 빈칸 주변에 암세포가 3~4개면 암세포가 태어난다 | 죽은 칸 + 암세포 이웃 2~5개 | 3~4개면 살아 있는 암세포로 태어나고, 2개나 5개면 빈칸으로 남는다 | `CancerModelTest#cancerIsBornNextToThreeOrFourCancerCells` |
| CAN-M03 | 정상 | 암 발생은 시작 세포와 8방향으로 이어진 살아 있는 일반 세포만 바꾼다 | (10,10)·(10,11)과 대각선으로 붙은 (11,12)의 3칸 묶음, 멀리 떨어진 (30,30), 확률 1 | 묶음 3칸만 암세포가 되고 떨어진 칸은 일반 세포로 남는다 | `CancerModelTest#outbreakConvertsOnlyTheConnectedGroup` |
| CAN-M04 | 경계 | 확률 판정에서 떨어지면 암이 발생하지 않는다 | 살아 있는 세포 1칸, 확률 0.1, 무작위 값 0.5 | 암세포 0개 | `CancerModelTest#outbreakDoesNothingWhenProbabilityCheckFails` |
| CAN-M05 | 경계 | 빈 보드에서는 암 발생·자연 발생이 아무것도 바꾸지 않는다 | 빈 보드, 확률 1 | 예외 없이 암세포 0개, 살아 있는 칸 0개 | `CancerModelTest#outbreakOnEmptyBoardDoesNothing` |
| CAN-M06 | 정상 | 자연 발생은 살아 있는 일반 세포 하나만 암세포로 바꾼다 | 살아 있는 세포 3칸, 확률 1 | 암세포 정확히 1개 | `CancerModelTest#naturalCancerConvertsExactlyOneCell` |
| CAN-M07 | 정상 | 전이는 암세포에 붙어 있는 일반 세포만, 한 세대에 한 칸씩만 감염시킨다 | 한 줄로 놓인 암세포 1칸 + 일반 세포 2칸, 확률 1 | 붙어 있는 칸만 암세포가 되고 그 다음 칸은 일반 세포로 남는다 | `CancerModelTest#invasionInfectsOnlyDirectNeighborsInOneGeneration` |
| CAN-M08 | 경계 | 전이 확률이 0이면 감염되지 않는다 | 암세포 옆 일반 세포, 확률 0 | 일반 세포가 그대로 남는다 | `CancerModelTest#invasionWithZeroProbabilityInfectsNothing` |
| CAN-M09 | 정상 | 치료는 클릭한 칸 중심 3×3 안의 암세포만 없애고 일반 세포는 남긴다 | 3×3 안의 암세포 2칸·일반 세포 1칸, 3×3 밖 암세포 1칸 | 안쪽 암세포는 죽고, 일반 세포와 바깥 암세포는 남는다 | `CancerModelTest#treatmentCuresOnlyCancerInThreeByThreeArea` |
| CAN-M10 | 경계 | 보드 모서리 치료는 보드 안쪽 칸만 처리한다 | (0,0)과 (1,1)의 암세포, (0,0) 치료 | 예외 없이 두 칸 모두 치료된다 | `CancerModelTest#treatmentAtCornerStaysInsideBoard` |
| CAN-M11 | 예외 | 보드 밖 좌표 치료는 아무것도 바꾸지 않는다 | (0,0) 암세포, (-5,-5)와 (100,100) 치료 | 예외 없이 암세포 1개 유지 | `CancerModelTest#treatmentOutsideBoardChangesNothing` |
| CAN-M12 | 경계 | 암세포 수는 하위 블록 경계를 넘어 모두 센다 | 하위 블록 경계 (7,7)·(7,8)·(8,7)·(8,8)과 (63,63)의 암세포 | `cancerCount()` = 5 | `CancerModelTest#cancerCountSpansSubBlocks` |
| CAN-M13 | 정상 | Clear는 암세포 상태도 지운다 | 암세포 2칸, `clear()` | 암세포 0개, 살아 있는 칸 0개 | `CancerModelTest#clearRemovesCancer` |
| CAN-M14 | 경계 | 복합 셀과 DUMMY는 암세포가 되지 않는다 | `Neighborhood`와 `Cell.DUMMY`에 `setCancer()` | 둘 다 `isCancer()` = false | `CancerModelTest#compositeAndDummyCellsAreNeverCancer` |
