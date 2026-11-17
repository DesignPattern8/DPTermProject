# 실행 테스트

구현 전 정의: [EXT-A01~06](../../docs/domain-extraction-tests.md).

GameControllerTest는 정상 세대, 실제 파일 왕복과 좌표당 1회 기록, 파일 열기/해석/출력 실패 경로를 확인한다. ClockTest는 실제 시간을 기다리지 않는 Timer 대역으로 즉시 시작(0), 주기, 교체·정지 시 취소, 수동 tick을 확인한다. 테스트가 소유한 Timer는 finally/AfterEach에서 cancel한다.

## 불러오기·저장 후 파일 스트림 해제 (#41)

`GameController.load/store`가 성공·실패와 관계없이 연 파일 스트림을 닫는지 확인한다. 기존 EXT-A02/A03/A05/A06의 보드 상태 기대값은 바꾸지 않는다.

스트림이 열려 있는지는 두 가지로 판정한다.

- 파일 삭제: Windows는 열린 파일을 지울 수 없으므로 삭제가 실패한다.
- 열린 파일 디스크립터 수: Linux·macOS(CI 포함)는 열린 파일도 지워지므로, `UnixOperatingSystemMXBean.getOpenFileDescriptorCount()`가 호출 전보다 늘지 않았는지 본다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| APP-S01 | 정상 | 정상 Load 후 입력 스트림 해제 | 셀 하나 저장한 파일 → Load | 셀 복원, 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#successfulLoadReleasesFile |
| APP-S02 | 예외 | 형식이 잘못된 파일 Load 실패 후 입력 스트림 해제 | garbage 4바이트 파일 | IOException, 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#invalidFileLoadReleasesFile |
| APP-S03 | 경계 | 빈 파일(0바이트) Load 실패 후 입력 스트림 해제 | 0바이트 파일 | IOException, 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#emptyFileLoadReleasesFile |
| APP-S04 | 정상 | 정상 Store 후 출력 스트림 해제 | 셀 하나 → Store | 파일 삭제 가능·열린 파일 수 그대로 | GameControllerTest#successfulStoreReleasesFile |

한계: 출력 파일을 연 뒤 직렬화(`LegacyLifeCodec.flush`)가 실패하는 경우는 `GameController`가 스트림을 직접 열어 대역을 넣을 수 없어 테스트로 재현하지 못했다. `store`는 `load`와 같은 `try-with-resources` 구조로 고쳤다.

## B 리팩토링: TickSource·Speed·GameListener (#44)

[003](003-pattern-analysis.md) D1~D4 적용 후에도 기존 동작이 같고, 새 계약이 동작하는지 확인한다.

기존 테스트(EXT-A01~06, EXT-A04, EXT-U01·U02, APP-S01~04, 클래식 CL-*)는 **기대값을 바꾸지 않는다.** 바뀌는 것은 `Clock` 생성 방법(`new Clock(timer)` → `new Clock(new UtilTimerTickSource(timer))`)과, 갱신 콜백이 `GameListener`로 옮겨간 `GameMenu.onTick`의 인자뿐이다. `NativeUiSmoke`(EXT-N01)는 수정 없이 리팩토링 전후 모두 통과해야 한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| APP-R01 | 정상 | 속도별 시작이 `Speed`의 주기로 TickSource를 시작 | 수동 TickSource 대역, `start(AGONIZING/SLOW/MEDIUM/FAST)` | 주기 500/150/70/30 | ClockTest#startUsesSpeedInterval |
| APP-R02 | 정상 | TickSource의 tick이 Clock 리스너에게 전달 | 리스너 2개, 대역에서 tick 2번 | 각 리스너 2번씩 호출 | ClockTest#tickSourceDrivesEveryListener |
| APP-R03 | 경계 | 주기 0 이하는 정지로 처리하고, 정지 상태에서 다시 정지해도 문제없음 | `startTicking(0)`, `stop()` 2번 | 대역이 정지 상태, 예외 없음 | ClockTest#zeroIntervalAndRepeatedStopLeaveSourceStopped |
| APP-R04 | 정상 | 셀 편집 후 전체 갱신 알림 | `toggle(5,5)` | `EDITED` 1번 | GameControllerTest#toggleNotifiesEdited |
| APP-R05 | 정상 | Clear 후 전체 갱신 알림 | `clear()` | `EDITED` 1번 | GameControllerTest#clearNotifiesEdited |
| APP-R06 | 정상 | 세대가 바뀐 tick 후 부분 갱신 알림 | blinker, `tick()` | `GENERATION` 1번 | GameControllerTest#changingTickNotifiesGeneration |
| APP-R07 | 경계 | 바뀐 칸이 없는 tick은 알리지 않음 (기존 `refreshNow` 조건 유지) | 빈 보드, `tick()` | 알림 없음, 반환값 false | GameControllerTest#unchangedTickDoesNotNotify |
| APP-R08 | 정상 | 불러오기 성공 후 전체 갱신 알림 | 저장한 파일 Load | `EDITED` 1번 | GameControllerTest#loadNotifiesEdited |
| APP-R09 | 예외 | 불러오기 실패는 보드 변경을 알리지 않음 (#63에서 계약 변경) | garbage 파일 Load | IOException, 알림 없음 | GameControllerTest#failedLoadDoesNotNotifyEdited |
| APP-R10 | 경계 | 저장은 보드를 바꾸지 않으므로 알리지 않음 | `store()` | 알림 없음 | GameControllerTest#storeDoesNotNotify |

## Board 연결과 보드 호출 직렬화 (#55)

[004](004-board-connection.md) 적용 후의 계약을 확인한다. `GameController`의 기존 테스트(EXT-A, APP-S, APP-R)는 생성자 호출만 바꾸고 기대값은 그대로 둔다. B만 확인하는 케이스는 `Board` 대역(`RecordingBoard`)을 쓴다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| APP-B01 | 정상 | tick은 `Board.step()` 한 번으로 진행하고, 바뀌었으면 부분 갱신을 알린다 | 대역 `step()` = true | 반환 true, `step` 1번, `GENERATION` 1번 | GameControllerBoardTest#tickStepsBoardOnceAndReportsChange |
| APP-B02 | 경계 | 바뀐 칸이 없는 세대는 알리지 않는다 | 대역 `step()` = false | 반환 false, `step` 1번, 알림 없음 | GameControllerBoardTest#unchangedStepIsNotReported |
| APP-B03 | 정상 | 셀 편집은 행·열 그대로 `Board.toggle`에 전달된다 | `toggle(3, 7)` | `CellPosition(3, 7)` 1번, `EDITED` | GameControllerBoardTest#toggleForwardsRowAndColumn |
| APP-B04 | 정상 | Clear는 `Board.clear`에 전달된다 | `clear()` | `clear` 1번, `EDITED` | GameControllerBoardTest#clearForwardsToBoard |
| APP-B05 | 경계 | 다른 스레드의 tick이 진행 중이면 편집은 끝날 때까지 보드에 들어가지 않는다 | 대역 `step()`을 래치로 멈춤 → 다른 스레드에서 `toggle` | `step`이 끝나기 전 `toggle` 미호출, 끝난 뒤 호출 | GameControllerBoardTest#editWaitsForRunningStep |
| APP-B06 | 예외 | 범위 밖 편집은 보드를 바꾸지 않고 거부하며 알리지 않는다 | 실제 64×64 보드, `toggle(64, 0)` | `IndexOutOfBoundsException`, 살아 있는 칸 없음, 알림 없음 | GameControllerTest#outOfRangeToggleIsRejected |

## 암세포 게임 규칙 (PR #60)

`CancerGame`(단계별 발생 확률, 승패, 치료 횟수)과 이를 붙인 `GameController`를 Swing 없이 검증한다. 무작위 값은 `Random` 대역으로 고정하고, 시계는 수동 `TickSource`로 대신한다. `CancerGame` 없이 만든 `GameController`의 기존 테스트(APP-B, EXT-A 등)는 그대로 통과해야 한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| CAN-A01 | 정상·경계 | 치료는 3번까지만 되고 그 뒤에는 보드를 바꾸지 않는다 | 서로 떨어진 암세포 4칸, 차례로 4번 치료 | 1~3번째는 true이고 횟수가 2→1→0으로 줄어든다. 4번째는 false이고 마지막 암세포가 남는다 | `CancerGameTest#treatmentIsLimitedToThreeCharges` |
| CAN-A02 | 정상 | 암세포가 나타난 뒤 모두 없어지면 승리로 끝난다 | 암세포 2칸에서 한 세대 → 모두 치료 → 한 세대 | 두 번째 호출이 true, 승리 메시지 | `CancerGameTest#curingAllCancerWins` |
| CAN-A03 | 경계 | 암세포가 한 번도 나타나지 않았으면 0개여도 승리가 아니다 | 일반 세포 1칸, 발생 확률에 걸리지 않는 무작위 값 | 끝나지 않고 메시지 없음 | `CancerGameTest#noCancerYetIsNotAWin` |
| CAN-A04 | 경계 | 암세포가 1024개(보드의 25%)가 되면 패배한다 | 32×32 암세포 묶음 | true, 패배 메시지 | `CancerGameTest#cancerCoveringQuarterOfBoardLoses` |
| CAN-A05 | 경계 | 암세포가 1023개면 아직 패배가 아니다 | 32×32에서 한 칸 뺀 암세포 묶음 | 끝나지 않음 | `CancerGameTest#oneCellBelowQuarterDoesNotLose` |
| CAN-A06 | 경계 | 끝난 게임은 더 이상 진행하지 않는다 | 승리 직후 다시 호출 | false, 메시지 유지 | `CancerGameTest#finishedGameDoesNotContinue` |
| CAN-A07 | 경계 | 발생 확률은 100세대마다 올라간다 | 일반 세포 1칸, 무작위 값 0.003 (1단계 0.001보다 크고 2단계 0.005보다 작음) | 100세대째까지 암세포 0개, 101세대째에 암세포 생김 | `CancerGameTest#outbreakProbabilityRisesAfterHundredGenerations` |
| CAN-A08 | 정상 | 단계별 확률표 | 단계 1~5 | 0.001, 0.005, 0.02, 0.05, 0.05 | `CancerGameTest#phaseProbabilities` |
| CAN-C01 | 정상 | 암 규칙 없이 만든 컨트롤러는 치료하지 않는다 | 기존 생성자, 암세포 1칸, `treat` | false, 암세포 유지, 알림 없음, 결과 메시지 null | `GameControllerCancerTest#classicControllerIgnoresTreatment` |
| CAN-C02 | 정상 | 치료하면 전체 다시 그리기를 알리고, 횟수가 없으면 알리지 않는다 | 암 규칙 컨트롤러, 4번 치료 | 1~3번째에만 `EDITED` 알림 | `GameControllerCancerTest#treatmentNotifiesOnlyWhenUsed` |
| CAN-C03 | 정상 | 승리하는 tick은 시계를 멈추고 `GAME_OVER`를 알린다 | 암세포 2칸(서로 붙어 안정) → tick → 치료 → tick | 시계 정지, 마지막 알림 `GAME_OVER`, 승리 메시지 | `GameControllerCancerTest#winningTickStopsClockAndReportsGameOver` |
| CAN-C04 | 경계 | 끝난 뒤의 tick은 보드를 진행하지 않고 알리지 않는다 | CAN-C03 이후 블링커를 놓고 tick | false, 블링커 그대로, 알림 없음 | `GameControllerCancerTest#tickAfterGameOverDoesNothing` |

## #63 안전한 Load — 구현 전 정의 (2026-11-17)

[005 행동 계약](005-safe-load.md)을 기준으로 아래 케이스를 먼저 정의한다. #63은 버그 수정이므로 과거 실패 시 Clear/정지/EDITED를 고정한 기대값을 명시적으로 변경한다. 기존 테스트를 삭제하거나 비활성화하지 않는다. APP-R09 및 `invalidFileClearsBoard`, `preservesOpenAndDecodeFailureOrder`는 상태 보존 계약으로 이름·기대값을 갱신한다. 나머지 기존 기대값은 유지한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| LOAD-01 | 예외 | 잘못된 입력에서 일반·암세포 보존 | 0바이트/garbage/잘린 stream/비Point 원소, 기존 혼합 보드 | IOException, 원래 배치·종류 유지 | `GameControllerLoadTest#malformedInputsPreserveCellsAndCancer` |
| LOAD-02 | 예외 | 실행 중 Clock과 다음 세대 유지 | 블록 경계 blinker, FAST, garbage | 예약·주기·start/stop 횟수 불변, 수동 예약 tick은 정상 다음 세대 | `GameControllerLoadTest#failedLoadKeepsRunningClockAndNextGeneration` |
| LOAD-03 | 경계 | 정지 상태 보존 | 정지 Clock, garbage | start/stop 호출 없음 | `GameControllerLoadTest#failedLoadKeepsStoppedClock` |
| LOAD-04 | 예외 | 개방 실패 상태 보존 | 없는 파일, 실행 중 Clock, 암세포 | 셀·Clock 유지, 변경 알림 없음 | `GameControllerLoadTest#missingFilePreservesStateWithoutNotification` |
| LOAD-05 | 예외 | 부분 읽기 오류 전파·자원 해제 | 실제 파일 입력을 읽고 IOException을 던지는 Codec | 동일 예외 전파, 입력 닫힘, 기존 셀 유지 | `GameControllerLoadTest#decodeFailureClosesInputAndPropagatesOriginalException` |
| LOAD-06 | 정상 | 해석 전 상태 보존과 완료 후 알림 | Codec에서 기존 셀·Clock 검사, 정상 반환 | 해석 중 상태 유지, listener에서 입력 닫힘·새 배치·정지 확인 | `GameControllerLoadTest#successfulLoadAppliesOnlyAfterDecodingAndClosingInput` |
| LOAD-07 | 경계 | 유효한 빈 목록으로 교체 | 혼합 보드 → 정상 빈 legacy 파일 | 보드·암세포 비움, Clock 정지, EDITED 1회 | `GameControllerLoadTest#emptyPatternClearsBoardSuccessfully` |
| LOAD-08 | 정상 | 암세포·일반 세포 종류 교체 | 기존 암 위치는 일반으로, 기존 일반 위치는 암으로 저장된 파일 | 좌표와 종류 정확히 복원 | `GameControllerLoadTest#cancerPatternReplacesExistingCellKinds` |
| LOAD-09 | 경계 | 기존 좌표 정책 유지 | 중복·음수·범위 밖 좌표, 블록 경계 blinker | 유효 좌표만 배치, 중복 제거 | `GameControllerLoadTest#loadedBoundaryPatternKeepsLegacyCoordinatePolicy` |
| LOAD-10 | 예외 | 해석 실패는 화면 변경 아님 | 기존 APP-R09 garbage Load | IOException, EDITED 없음 | `GameControllerTest#failedLoadDoesNotNotifyEdited` |

기존 APP-S01~04의 stream 정리, APP-B05의 보드 호출 직렬화, UI-R05의 Read Failed 표시 및 classic/cancer 왕복 회귀는 유지한다.

LOAD-09의 경계 배치 이후 세대 진행에서 별도 기존 결함 [#81](https://github.com/DesignPattern8/DPTermProject/issues/81)을 발견했다. 성공 Load의 경계 출생 처리는 이 PR에서 수정하지 않으며, LOAD-09는 좌표 배치 정책까지만 검증한다. 실패 Load 뒤 기존 예약 tick의 정상 진행은 LOAD-02로 검증한다.

## #88 저장·불러오기를 `Board.snapshot()/restore()`로 — 구현 전 정의 (2026-11-17)

`GameController`의 저장·불러오기가 파일용 `Cell`(`createMemento`/`transfer(LOAD)`) 대신 `Board.snapshot()/restore()`를 쓰고, 생성자의 `Cell fileCells` 인자를 지운 뒤에도 기존 동작이 같은지 확인한다. 기존 Load·Store 테스트(LOAD-01~10, APP-S01~04, APP-R08~R10, `GameControllerPersistenceTest`, classic CL-M, 암세포 왕복)는 **기대값을 바꾸지 않는다.** 바뀌는 것은 생성자 호출(`Cell` 인자 제거)뿐이다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| SNAP-01 | 정상 | 일반 세포 저장 → 불러오기 왕복 | 블록 경계에 걸친 일반 세포 3칸 → Store → Clear → Load | `board.snapshot()`이 저장 전과 같다, `EDITED` 1번 | `GameControllerSnapshotFileTest#classicRoundTripRestoresSameSnapshot` |
| SNAP-02 | 정상 | 일반·암세포 혼합 왕복 | 일반 2칸 + 암세포 1칸 → Store → Clear → Load | `liveCells`·`cancerCells`가 저장 전과 같다 | `GameControllerSnapshotFileTest#cancerRoundTripRestoresBothCellKinds` |
| SNAP-03 | 정상 | 저장 파일의 좌표 순서가 기존 순회 순서와 같다 (기존 `.life` 호환) | 서로 다른 블록의 3칸 | 파일의 `Collection<Point>`가 `createMemento()` 순서와 같다 | 기존 `GameControllerPersistenceTest#preservesTraversalAndCancer` (변경 없음) |
| SNAP-04 | 경계 | 빈 패턴은 빈 보드로 복원 | 혼합 보드 → 빈 legacy 파일 | 보드·암세포 비움, Clock 정지, `EDITED` 1번 | 기존 LOAD-07 `GameControllerLoadTest#emptyPatternClearsBoardSuccessfully` (변경 없음) |
| SNAP-05 | 경계 | 기존 파일의 중복·음수·범위 밖 좌표는 지금처럼 무시 | 중복·`(-1,0)`·`(64,0)` 포함 파일 | 유효 좌표만 배치, 중복 제거 | 기존 LOAD-09 `GameControllerLoadTest#loadedBoundaryPatternKeepsLegacyCoordinatePolicy` (변경 없음) + **필터링 위치 결정 후 추가 테스트 1개** (아래) |
| SNAP-06 | 경계 | 일반 세포 목록에 없는 암세포 좌표·범위 밖 암세포 좌표의 처리 | `cells`에 없는 `cancerCells` 1칸, 범위 밖 암세포 1칸 | **구현 전에 현재 동작을 특성 테스트로 고정한 뒤 기대값 확정** (현재 `Resident.transfer`는 `isAlive`·`isCancer`를 따로 읽음, `BoardSnapshot`은 암세포 ⊆ 생존을 요구) | `GameControllerSnapshotFileTest#cancerOutsideLiveCellsKeepsLegacyBehavior` |
| SNAP-07 | 예외 | 잘못된 파일이면 보드·암세포·Clock 유지, 알림 없음 (#82 계약) | garbage 파일, 실행 중 Clock, 혼합 보드 | `IOException`, 상태 유지, 알림 없음 | 기존 LOAD-01·02·04·10 (변경 없음) |
| SNAP-08 | 정상 | B가 파일용 `Cell` 없이 `Board`만으로 저장·불러오기 | `Board` 대역(`snapshot`/`restore` 기록) + 메모리 Codec | Store는 `snapshot()` 1번, Load는 `restore()` 1번, `createMemento`/`transfer` 경로 없음 | `GameControllerBoardTest#storeAndLoadUseBoardSnapshotOnly` |

### 결정 대기 (구현 전에 확정)

1. **SNAP-05 좌표 필터링 위치 (C vs B)** — #88 댓글로 도현에게 질문함, 답 대기.
   - C: `PatternData.toClassicSnapshot(rows, columns)`가 이미 범위 밖·중복을 걸러 `BoardSnapshot`을 만든다. 다만 지금은 **암세포가 있으면 `IllegalStateException`**이라 암세포 파일을 처리하지 못한다. C가 암세포까지 받는 변환을 제공하면 추가 테스트는 C의 `test-cases.md`에 둔다.
   - B: `PersistenceMapper`가 `PatternData` → `BoardSnapshot` 변환에서 거른다. 추가 테스트는 `PersistenceMapperTest`에 둔다.
2. **SNAP-03 저장 순서** — `BoardSnapshot.liveCells()`는 `Set`이라 순서가 없다. 기존 파일과 같은 순서(`createMemento()`의 블록 재귀 순회 순서)로 쓰려면 B 또는 C가 정렬해야 한다. A 인계 기준은 "파일 출력은 기존 순회 순서를 따른다"(C 항목). 1번과 같은 답에서 함께 정한다.
3. **#81과의 관계** — `Board.restore()`는 `toggle` 경로로 배치하므로 #81(legacy `transfer(LOAD)` 뒤 블록 경계 첫 세대 출생 누락)의 증상이 사라질 수 있다. 이 작업의 테스트는 **다음 세대 결과를 단정하지 않고**(SNAP-01은 배치까지만), 구현 후 관찰한 결과만 #81에 기록한다(강령 2).
