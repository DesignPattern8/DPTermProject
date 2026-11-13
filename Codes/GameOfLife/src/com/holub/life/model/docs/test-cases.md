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
