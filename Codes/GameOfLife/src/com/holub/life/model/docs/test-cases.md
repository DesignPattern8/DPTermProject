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
| CAN-M09 | 정상·경계 | 치료는 클릭한 칸 중심 5×5 안의 암세포만 없애고 일반 세포는 남긴다 (#96에서 3×3→5×5) | (30,30) 치료. 5×5 안의 암세포 (29,29)·가장자리 (32,32), 일반 세포 (30,31), 5×5 밖 암세포 (33,33) | 안쪽 암세포는 죽고, 일반 세포와 바깥 암세포는 남는다 | `CancerModelTest#treatmentCuresOnlyCancerInFiveByFiveArea` |
| CAN-M10 | 경계 | 보드 모서리 치료는 보드 안쪽 칸만 처리한다 | (0,0)과 (1,1)의 암세포, (0,0) 치료 | 예외 없이 두 칸 모두 치료된다 | `CancerModelTest#treatmentAtCornerStaysInsideBoard` |
| CAN-M11 | 예외 | 보드 밖 좌표 치료는 아무것도 바꾸지 않는다 | (0,0) 암세포, (-5,-5)와 (100,100) 치료 | 예외 없이 암세포 1개 유지 | `CancerModelTest#treatmentOutsideBoardChangesNothing` |
| CAN-M12 | 경계 | 암세포 수는 하위 블록 경계를 넘어 모두 센다 | 하위 블록 경계 (7,7)·(7,8)·(8,7)·(8,8)과 (63,63)의 암세포 | `cancerCount()` = 5 | `CancerModelTest#cancerCountSpansSubBlocks` |
| CAN-M13 | 정상 | Clear는 암세포 상태도 지운다 | 암세포 2칸, `clear()` | 암세포 0개, 살아 있는 칸 0개 | `CancerModelTest#clearRemovesCancer` |
| CAN-M16 | 예외 | 치료 범위에 암세포가 없으면 `treatAt`은 false를 돌려주고 아무것도 바꾸지 않는다 (#96) | 5×5 안에 일반 세포만, 밖에 암세포 1칸 | false, 일반 세포 생존, 암세포 1개 유지 | `CancerModelTest#treatmentWithoutCancerInAreaReportsNothingCured` |
| CAN-M17 | 예외 | 암세포는 토글로 상태를 바꿀 수 없다 (#96) | 암세포 (10,10)에 `toggle` | 여전히 살아 있는 암세포 | `CancerModelTest#toggleIgnoresCancerCell` |

## 암세포 셀 계약 분리 (#70)

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CAN-M15 | 경계 | 보드 가장자리의 암세포 출생 유지 | `(1,5)`, `(1,6)`, `(1,7)`에 암세포를 복원하고 한 세대 진행 | 바로 위 `(0,6)`에 암세포가 태어난다. | `CancerModelTest#cancerIsBornAtBoardEdge` |
| CAN-M14 | 예외·계약 | 암세포 미지원 타입에 암세포 연산이 노출되지 않는다 | `Neighborhood`, `Cell.DUMMY`, `TwoPlayerResident`의 타입 확인 | 셋 모두 `CancerCell` 계약을 구현하지 않는다. | `CancerModelTest#compositeAndDummyCellsDoNotSupportCancer`, `TwoPlayerRuleTest#twoPlayerCellsDoNotSupportCancer` |

정상 케이스는 CAN-M01~13의 기존 계산·발생·치료를 사용한다. SNAP-C, CAN-P, CAN-A, CAN-C 및 화면의 암세포 색과 클래식·2인용 규칙도 회귀 검증한다.

## 암세포 스냅샷 (#73)

`BoardSnapshot`의 `cancerCells`는 `liveCells`의 부분집합이며 두 집합 모두 불변이다. 기존 3인자 생성자는 암세포가 없는 스냅샷을 만든다. 캡처는 보드 상태를 바꾸지 않고, 복원은 생사·암 여부와 다음 세대의 경계 계산을 함께 보존한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| SNAP-C01 | 정상 | 생사·암 상태의 전체 복원 | 일반·암세포 혼합 배치를 캡처하고 다른 배치로 바꾼 뒤 복원 | 캡처한 생사·암 여부로 돌아오고 추가한 셀은 제거된다. | `CancerSnapshotTest#restoresLiveAndCancerCellsTogether` |
| SNAP-C02 | 경계 | 복원 직후 블록 경계의 암세포 탄생 | 하위 블록 마지막 행의 암세포 3개를 캡처·복원하고 한 세대 진행 | 직접 편집한 보드와 같고 인접 블록에도 암세포가 태어난다. | `CancerSnapshotTest#restoredCancerKeepsBoundaryBirthOnFirstStep` |
| SNAP-C03 | 경계 | 캡처의 읽기 전용 동작 | 경계의 일반·암세포 혼합 배치를 캡처 | 생사·암 여부와 루트·하위 블록의 활성 정보가 유지된다. | `CancerSnapshotTest#captureKeepsMixedCellStatesAndActiveEdges` |
| SNAP-C04 | 경계 | 두 좌표 집합의 방어 복사 | 가변 집합으로 스냅샷을 만든 뒤 원본 및 반환 집합 변경 시도 | 원본 변경에 영향받지 않고 반환 집합 변경은 거부된다. | `CancerSnapshotTest#snapshotDefensivelyCopiesBothCellSets` |
| SNAP-C05 | 예외 | 잘못된 암세포 좌표 거부 | 음수·크기 밖·생존 집합에 없는 암세포 좌표 | `IllegalArgumentException`, 기존 보드 유지 | `CancerSnapshotTest#rejectsCancerOutsideLiveCellsBeforeRestore` |
| SNAP-C06 | 예외 | 크기 불일치 복원 거부 | 기존 암세포 보드에 다른 크기의 유효 스냅샷 복원 요청 | `IllegalArgumentException`, 생사·암 상태 유지 | `CancerSnapshotTest#sizeMismatchKeepsExistingCancerCells` |
| SNAP-C07 | 정상 | 3인자 생성자 호환·이전 암 상태 제거 | 기존 암세포 보드에 3인자 생성자로 만든 클래식 스냅샷 복원 | 지정된 일반 세포만 남고 암세포 집합은 비어 있다. | `CancerSnapshotTest#classicSnapshotClearsPreviousCancerState` |
| SNAP-C08 | 경계 | 빈 보드 캡처·복원 | 빈 보드 캡처 후 암세포를 배치하고 복원 | 생존·암세포 집합 모두 비어 있다. | `CancerSnapshotTest#emptySnapshotRemovesAllCellStates` |
| SNAP-C09 | 예외 | null 암세포 집합·원소 거부 | null 집합 또는 null 원소가 든 암세포 집합 | `NullPointerException` | `CancerSnapshotTest#rejectsNullCancerCollectionsAndElements` |

## legacy Load 경계 활성화 (#81)

`GameController.load()`의 실제 파일 경로와 `Neighborhood.transfer(LOAD)`를 통해 복원한 뒤, 읽기 전용 `Board.snapshot()`으로 관찰한다. 세대 계산 전에 `createMemento()`를 호출하면 활성 정보가 바뀔 수 있어 관찰에 사용하지 않는다. 일반·암세포 규칙과 직접 편집 경로의 결과는 유지한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| LDB-01 | 정상·경계 | 정상 파일 Load 후 첫 세대·연속 세대 계산 | 64×64 블링커: 블록 내부, 하단·우측·모서리 경계, 보드 네 가장자리. 8×8 3중 구조의 상위 경계. 총 11개 배치 | 첫 세대는 명시한 회전·범위 절단 결과, 이후 세대는 직접 편집한 보드와 같다. | `LegacyLoadBoundaryTest#loadedBlinkerMatchesDirectEditing` |
| LDB-02 | 경계 | 암세포 파일의 경계 너머 탄생 보존 | 하단·우측·모서리 경계의 암세포 3개, 총 3개 배치 | 첫 세대에 기존 3개와 수직/수평 양쪽 새 암세포가 남고 이후에도 직접 편집 경로와 같다. | `LegacyLoadBoundaryTest#loadedCancerMatchesDirectEditing` |
| LDB-03 | 경계 | 유효한 빈 파일 복원 후 이전 세포 제거 | 경계 일반·암세포 배치 → 빈 패턴 파일 Load → tick | 생존·암세포가 모두 비고 다음 세대에도 되살아나지 않는다. | `LegacyLoadBoundaryTest#emptyFileClearsBoundaryCells` |
| LDB-04 | 정상·경계 | Controller 외의 기존 Memento 복원 경로 | 일반·암세포가 있는 보드에 clear 없이 경계 블링커 Memento 적용 | 기존 배치를 대체하고 첫 세대 경계 출생도 직접 편집한 보드와 같다. | `LegacyLoadBoundaryTest#directMementoLoadReplacesCellsAndActivatesEdges` |
| LDB-05 | 경계 | 빈 LOAD의 불필요한 활성화 방지 | clear한 중첩 보드에 빈 Memento LOAD | 반환 false, 모든 블록이 비활성·방향 NONE이며 빈 상태 유지 | `LegacyLoadBoundaryTest#emptyMementoLeavesClearedBlocksInactive` |
| LDB-06 | 예외 | #63의 실패 시 상태 보존 계약 유지 | 잘못된 파일 또는 없는 파일 Load, 실행 중/정지 상태 | 일반·암세포 배치와 Clock 예약·속도 보존, 변경 알림 없음 | 기존 `GameControllerLoadTest#malformedInputsPreserveCellsAndCancer`, `#failedLoadKeepsRunningClockAndNextGeneration`, `#missingFilePreservesStateWithoutNotification` |
| LDB-07 | 정상·경계 | 기존 저장 형식·복원 호환 | 기존 클래식 Point Collection, 암세포 형식, 중복·빈 데이터 | 기존 형식·좌표·중복 해석 및 STORE 동작 유지 | 기존 `ClassicMementoTest`, `CancerCodecTest`, `LegacyLifeCodecTest` |

## 보드별 전이 읽기 허가 (#74)

같은 루트 아래의 모든 중첩 블록은 하나의 읽기 허가 상태를 공유하고, 서로 다른 보드는 독립적이어야 한다. 순서·중첩 관찰에는 일회성 전이 콜백을, 두 스레드의 실제 겹침에는 CountDownLatch를 사용하며 sleep·무작위 재현에 의존하지 않는다. 같은 보드의 변경 직렬화는 기존 호출자 계약을 유지한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| GATE-01 | 정상·경계 | 같은 트리 전체의 읽기 허가 공유 | 2중·3중 보드의 leaf 전이 중 루트와 모든 중첩 블록 조회 | 전이 중 모두 false, 종료 후 모두 true | `NeighborhoodReadGateTest#sameBoardBlocksShareReadGate` |
| GATE-02 | 경계 | 보드·프로토타입·복제 간 독립성 | 별도 생성한 루트, 같은 Neighborhood 프로토타입으로 만든 두 루트, 공개 create()로 복제한 루트 | 한쪽 전이 중 다른 보드와 원본 프로토타입은 읽기 허용 | `NeighborhoodReadGateTest#independentBoardsKeepReadGatesSeparate` |
| GATE-03 | 경계 | 서로 다른 보드 전이가 중첩되어도 독립 종료 | A의 전이 콜백에서 B 전이를 실행하고 각 단계의 읽기 허가 관찰 | B가 먼저 끝나면 B만 열리고 A는 계속 닫힘, 최종 둘 다 열림 | `NeighborhoodReadGateTest#nestedBoardTransitionsKeepGatesIndependent` |
| GATE-04 | 경계 | 두 스레드의 보드별 독립 종료 | A·B 각각 전이 콜백에서 래치 대기, A 먼저 해제 | 두 보드 전이 중 각각 닫힘, A 종료 후 B 대기 중에도 A는 열림 | `NeighborhoodReadGateTest#concurrentBoardTransitionsFinishIndependently` |
| GATE-05 | 경계 | 중첩 블록의 전이가 끝나도 루트는 닫힘 유지 | 루트의 전이 콜백에서 같은 트리의 다른 하위 블록 전이 호출 | 하위 호출 전후 루트와 모든 자식의 읽기 허가가 false | `NeighborhoodReadGateTest#nestedSubtreeTransitionKeepsOuterGateClosed` |
| GATE-06 | 예외 | 예외·Error 후 허가와 깊이 복구 | 중첩 leaf의 전이에서 RuntimeException 또는 Error를 한 번 발생 | 원래 오류 전파, 전체 트리 허가 복구, 다음 전이도 정상적으로 닫히고 열림 | `NeighborhoodReadGateTest#transitionFailureReleasesWholeBoard` |
| GATE-07 | 정상·경계 | 기존 게임·렌더링·복원 동작 유지 | 기존 클래식·암세포·2인용·안정 블록 마지막 그리기·legacy Load 경계 테스트 | 기존 반환값·규칙·부분 그리기·복원 결과 유지 | 기존 `RenderingExtractionTest`, `ClassicGenerationTest`, `CancerModelTest`, `TwoPlayerRuleTest`, `LegacyLoadBoundaryTest` |
| GATE-08 | 경계 | 기존 수동 재현 도구의 현재 구현 호환 | `StaticLockCheck`의 순차·두 보드·두 Clock 실행 후 public beginRedraw(true) 관찰 | 종료 후 건너뜀 0, private static 필드 반사 없이 실행 | `com.holub.life.twoplayer.ui.StaticLockCheck#main` (JUnit 외 수동 확인) |

## 모델 Memento 좌표 분리 (#71)

구현 전에 현재 `Point(x=열, y=행)` API로 특성을 고정한다. 출력은 기존 `PersistenceMapper.capture()`가 반환하는 `CellPosition(row, column)` 값으로 비교한다. 구현 후에는 입력 좌표 생성 도우미와 입력 컬렉션 타입만 `CellPosition`으로 바꾸고, 입력 좌표 값·순서·기대값·예외·상태 검증은 유지한다. `TwoPlayerResident`의 지원하지 않는 연산도 직접 검증하도록 테스트 패키지는 `com.holub.life.twoplayer`로 둔다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| COORD-01 | 정상·경계 | 재귀 블록 순서와 비영원점 행·열 보존 | 2×2 하위 격자의 2중·3중 보드, 비대칭 일반·암 배치, 원점 `(11, 23)` | 전체 행 정렬이 아닌 기존 재귀 블록 순서, 원점과 셀 오프셋을 더한 행·열, 암 부분집합을 보존하고 전달한 원점을 바꾸지 않는다. | `MementoCoordinateTest#storeKeepsNestedBlockOrderAndOffset` |
| COORD-02 | 정상·경계 | 비영원점 LOAD의 교체·종류 변경·범위 해석 | 3중 보드의 기존 일반·암 배치, 원점 `(11, 23)` 기준 새 일반·암 위치와 영역 밖 네 방향 좌표 | 원점 안 새 배치로 교체하고 일반↔암 변경·이전 셀 제거를 반영한다. 영역 밖 좌표는 무시하며 다음 상태도 현재 상태와 같고 Memento는 바뀌지 않는다. | `MementoCoordinateTest#loadUsesOffsetAndReplacesLiveAndCancerState` |
| COORD-03 | 정상·경계 | State의 순서·중복과 암 표시 계약 | 일반·암 좌표를 같은 순서로 각각 두 번 기록 | 생존 목록은 입력 순서와 중복을 유지하고 암 집합은 중복 없이 생존 목록에 포함된다. 조회는 각 좌표의 종류에 맞는다. | `MementoCoordinateTest#stateKeepsInsertionOrderDuplicatesAndCancerSubset` |
| COORD-04 | 정상·경계 | 가변 컬렉션 별칭과 단일 overload의 암 초기화 | 양쪽 컬렉션으로 `replaceCells` 후 양방향 변경, 이후 생존 컬렉션만 교체 | getter는 전달한 컬렉션과 같은 참조다. 양쪽 변경이 서로 보이며 단일 overload는 생존 순서·중복을 보존하고 이전 암 집합을 새 빈 집합으로 교체한다. | `MementoCoordinateTest#replacementKeepsMutableAliasesAndResetsCancer` |
| COORD-05 | 정상·예외 | 지원하지 않는 저장 연산의 기존 처리 | DUMMY와 2인용 셀에 STORE·LOAD, 단위 셀에 `createMemento()` | transfer는 false이고 셀·Memento를 바꾸지 않는다. Resident·DUMMY·2인용 단위 셀의 Memento 생성은 `UnsupportedOperationException`이다. | `MementoCoordinateTest#unsupportedCellsKeepTransferNoOpAndMementoFailure` |
| COORD-06 | 예외 | null 기록의 기존 실패 보존 | 새 State에 `markAsAlive(null)`·`markAsCancer(null)` | 두 호출 모두 `NullPointerException`이고 생존·암 컬렉션은 비어 있다. | `MementoCoordinateTest#nullMarksFailWithoutAddingCells` |
| COORD-07 | 정상·경계 | 모델과 파일 왕복의 음수·중복·암 좌표 보존 | 음수 행의 일반 좌표·음수 열의 암 좌표를 중복 기록하고 Mapper→Codec→Mapper 왕복 | 행·열, 생존 입력 순서와 중복, 암 좌표가 그대로다. 모델 컬렉션과 파일 타입 변환이 음수 좌표를 보정하거나 제거하지 않는다. | `MementoCoordinateTest#modelFileRoundTripKeepsNegativeDuplicateAndCancerCoordinates` |

기존 `LegacyLoadBoundaryTest`의 경계 출생·빈 LOAD 비활성 케이스, `CancerSnapshotTest`, `ClassicMementoTest`, `CancerCodecTest`와 `LegacyLifeCodecTest`는 기존 입력과 기대 동작을 유지한다. 신규 테스트는 읽은 코드의 현재 특성만 검증하며, #71이 끝난 뒤 테스트 수나 검증 성공 여부는 실제 실행 결과로 기록한다.
