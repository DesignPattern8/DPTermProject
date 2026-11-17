# 화면 테스트

구현 전 정의: [EXT-R01~05, EXT-U01~02](../../docs/domain-extraction-tests.md).

RenderingExtractionTest는 e38ce7a 원본에서 생성한 ARGB SHA-256 기대값을 사용한다. 빈 보드·glider·4세대 부분 갱신·block 안정화의 픽셀을 고정한다. 511px 표면의 정수 나눗셈과 범위 밖 예외도 유지한다.

GameMenuTest는 기존 속도 매핑·메뉴 선택 중 tick 차단을 검증한다. NativeUiSmoke의 EXT-N01 결과와 실행 방법은 [검증 기록](../../application/docs/verification.md)을 참고한다.

## 메뉴 항목 표·대화상자 분리 (#9, 결정 [005](005-menu-binding.md)·[006](006-dialog-injection.md))

구현 전에 정의한다. 모두 Swing 창 없이 `GameMenu.entries(controller, clock, dialogs, exit)`의 항목을 직접 실행한다. `dialogs`는 테스트용 기록 대역(`RecordingDialogs`: 고를 파일 또는 던질 예외를 미리 정하고, 표시한 오류 메시지를 기록), `exit`는 호출 횟수를 세는 `Runnable`이다. 파일은 `@TempDir`에 만든다.

기존 EXT-U01(`goCommandsKeepOriginalActions`)·EXT-U02(`openMenuSuppressesTick`)는 수정하지 않고 그대로 통과해야 한다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-R01 | 정상(특성) | 메뉴 구성(메뉴·라벨·순서)이 원본과 같은지 확인 | `entries(...)` | Grid: Clear, Load, Store, Exit → Go: Halt, Tick (Single Step), Agonizing, Slow, Medium, Fast (10개, 순서 포함) | `GameMenuTest#menuEntriesKeepOriginalOrder` |
| UI-R02 | 정상 | Clear 항목이 보드를 비우고 화면 갱신 알림을 보내는지 확인 | 셀 2개가 살아 있는 보드, `GameListener` 기록 | 살아 있는 셀 0, 알림 `EDITED` 1회 | `GameMenuTest#clearEntryClearsBoard` |
| UI-R03 | 정상 | Load 항목이 대화상자에서 고른 파일로 불러오는지 확인 | blinker를 저장한 파일, 대역이 그 파일을 고름, 보드에는 다른 셀 1개 | 버튼 문구 `"Load"`로 요청, 보드 = blinker 3칸, 오류 표시 없음 | `GameMenuTest#loadEntryLoadsChosenFile` |
| UI-R04 | 정상 | Store 항목이 고른 파일에 저장하는지 확인 (왕복) | 셀 3개, 대역이 새 파일을 고름 | 버튼 문구 `"Write"`로 요청, 파일을 다시 불러오면 같은 3칸, 오류 표시 없음 | `GameMenuTest#storeEntryWritesChosenFile` |
| UI-R05 | 예외 | 형식이 잘못된 파일을 불러오면 원본 문구로 실패를 알리는지 확인 | 바이트 `{1,2,3,4}` 파일을 고름 | 오류 표시 `["Read Failed!"]` (1회) | `GameMenuTest#loadFailureShowsReadFailed` |
| UI-R06 | 예외 | 저장할 수 없는 경로면 원본 문구로 실패를 알리는지 확인 | 디렉터리를 고름 | 오류 표시 `["Write Failed!"]` (1회), 보드 그대로 | `GameMenuTest#storeFailureShowsWriteFailed` |
| UI-R07 | 예외(특성) | 파일 선택을 취소했을 때 원본처럼 동작하는지 확인 | 대역이 `FileNotFoundException`을 던짐(원본 `FileDialogs`의 취소), Load 후 Store | 보드 그대로, 오류 표시 `["Read Failed!", "Write Failed!"]` (원본 동작. 버그 후보, 006 참고) | `GameMenuTest#cancelKeepsOriginalFailureMessage` |
| UI-R08 | 정상 | Exit 항목이 종료 동작을 부르는지 확인 (실제 종료 대신 주입) | Exit 실행 | 주입한 `exit` 1회 호출 | `GameMenuTest#exitEntryRunsExitAction` |
| UI-R09 | 경계 | Go 항목 각각이 맞는 `Clock` 요청을 하고, 표에 없는 이름은 원본처럼 정지하는지 확인 | 기록용 `TickSource` 대역, Go 항목 6개 실행 후 `go(clock, "Unknown")` | Agonizing/Slow/Medium/Fast → `start(500/150/70/30)`, Halt → `stop`, Tick → 리스너 1회, `"Unknown"` → `stop` | `GameMenuTest#goEntriesMapToClockRequests` |

## 클릭 → `CellPosition` (#9, 결정 [008](008-board-api-boundary.md))

구현 전에 정의한다. 계산은 기존 `GridCoordinateMapper.position`을 그대로 쓰므로, 기대값은 `position` 결과(x = 열, y = 행)와 같아야 한다. `BoardPanel`은 headless에서 만들 수 있는 `JPanel`이라 창 없이 `MouseEvent`를 보낸다.

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-R10 | 정상(특성) | `cellAt`이 기존 `position`과 같은 셀을 고르는지 확인 | 64×64 보드, 512px 표면, 0~511 픽셀을 7px 간격으로 모든 (x, y) | 모든 지점에서 `cellAt` = `CellPosition(position.y, position.x)` | `GridCoordinateMapperTest#cellAtMatchesLegacyPosition` |
| UI-R11 | 경계 | 블록 경계·보드 끝 픽셀의 행·열 | 512px, 픽셀 (x, y) = (0,0), (63,63), (64,64), (511,511), (100,200) | (행, 열) = (0,0), (7,7), (8,8), (63,63), (25,12) | `GridCoordinateMapperTest#cellAtMapsEdgesToRowAndColumn` |
| UI-R12 | 예외(특성) | 나누어떨어지지 않는 표면에서 범위 밖 예외를 원본과 같게 유지 | 511px 표면, 픽셀 (510, 510) | `ArrayIndexOutOfBoundsException` (EXT-R04와 같음) | `GridCoordinateMapperTest#cellAtKeepsOriginalOutOfRangeException` |
| UI-R13 | 정상 | 패널 클릭이 행·열을 뒤집지 않고 그 셀을 토글하는지 확인 | 512×512 `BoardPanel`, 픽셀 (x=100, y=200) `MOUSE_PRESSED` | 살아 있는 셀 = {(행 25, 열 12)}, 알림 `EDITED` 1회 | `BoardPanelTest#clickTogglesCellUnderPointer` |

## 암세포 기능(#60)의 화면 변경 (#67, 결정 [009](009-game-result-dialog.md))

구현 전에 정의한다. #60이 바꾼 D 코드(우클릭 치료, 암세포 색, 결과 메시지)는 지금까지 D 테스트가 없었다. 모두 창 없이 실행한다. 암 규칙은 새 암세포가 생기지 않도록 `nextDouble()`이 항상 0.99인 `Random` 대역을 넣은 `CancerGame`으로 만든다(#60 `CancerGameTest.FixedRandom`과 같은 방식).

| ID | 분류 | 목적 | 입력/초기 상태 | 기대 결과 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- |
| UI-R14 | 정상(특성) | 오른쪽 클릭이 포인터 아래 셀의 치료를 요청하는지 확인 | 암 규칙 컨트롤러, (행 25, 열 12)가 암세포, 512px 패널에서 픽셀 (x=100, y=200) 오른쪽 버튼 `MOUSE_PRESSED` | 그 셀은 더 이상 암세포가 아님, 남은 치료 횟수 3 → 2, 알림 `EDITED` 1회 | `BoardPanelTest#rightClickTreatsCellUnderPointer` |
| UI-R15 | 경계(특성) | 암 규칙이 있어도 왼쪽 클릭은 치료하지 않고 토글만 하는지 확인 | UI-R14와 같은 컨트롤러, 빈 셀 (25,12)를 왼쪽 클릭 | (25,12)가 살아남(암세포 아님), 남은 치료 횟수 3 그대로 | `BoardPanelTest#leftClickTogglesEvenWithCancerRules` |
| UI-R16 | 정상(특성) | 암세포는 마젠타, 일반 생존 셀은 빨강, 빈 셀은 연노랑으로 그리는지 확인 | (0,0) 암세포, (0,1) 생존, (0,2) 빈 셀. 512px `BufferedImage`에 전체 그리기 | 각 셀 중앙 픽셀(4,4), (12,4), (20,4)이 `MAGENTA`, `RED`, `Colors.LIGHT_YELLOW` | `BoardRendererTest#cancerCellsAreMagenta` |
| UI-R17 | 정상 | 게임 종료 알림에 결과 메시지를 한 번 띄우는지 확인 | 이긴 상태의 컨트롤러(#60 CAN-C03과 같은 절차), 기록용 대화상자, 즉시 실행기 | `showMessage` 1회, 내용 = `controller.gameResultMessage()` ("승리" 포함) | `GameResultAnnouncerTest#gameOverShowsResultOnce` |
| UI-R18 | 예외 | 게임 종료가 아닌 알림에는 메시지를 띄우지 않는지 확인 | 같은 구독자에 `EDITED`, `GENERATION` 전달 | `showMessage` 0회, 실행기에도 작업 0개 | `GameResultAnnouncerTest#otherChangesShowNothing` |
| UI-R19 | 경계 | 대화상자는 실행기(제품에서는 이벤트 스레드)에서 띄우는지 확인 | `GAME_OVER` 전달, 작업을 모아 두기만 하는 실행기 | 전달 직후 `showMessage` 0회, 모아 둔 작업 실행 후 1회 | `GameResultAnnouncerTest#messageWaitsForExecutor` |
