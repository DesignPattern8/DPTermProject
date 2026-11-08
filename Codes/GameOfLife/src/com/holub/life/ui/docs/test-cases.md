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
