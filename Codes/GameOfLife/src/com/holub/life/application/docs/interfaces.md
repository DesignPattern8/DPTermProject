# 실행 계약

- 제공 B / 사용 D·Life / 변경 2026-11-03, #24

```java
GameController(Board board, Clock clock)                    // 파일: LegacyLifeCodec, 블록 폭 [8, 1]
GameController(Board board, Clock clock, CancerGame cancerGame)
GameController(Board board, Clock clock, CancerGame cancerGame, BoardFileStore files)  // files: board 구조의 블록 폭을 설정한 저장소
GameController(Board board, Clock clock, CancerGame cancerGame, BoardFileStore files, PresetCatalog presets)  // 나머지 생성자는 new PresetCatalog()
void GameController.toggle(int row, int column)
void GameController.clear()
boolean GameController.tick()
void GameController.load(File file) throws IOException
void GameController.store(File file) throws IOException
List<PresetChoice> GameController.presets()                  // PresetCatalog.list() 순서
PresetResult GameController.applyPreset(String id, CellPosition origin)

record PresetChoice(String id, String displayName)
enum PresetResult { APPLIED, OUT_OF_BOUNDS, UNKNOWN_ID }
void GameController.addGameListener(GameListener listener)

Clock(TickSource ticks)
void Clock.start(Speed speed)
void Clock.startTicking(int millisecondsBetweenTicks)
void Clock.stop()
void Clock.tick()
void Clock.addClockListener(Clock.Listener listener)

interface TickSource { void start(int intervalMillis, Runnable onTick); void stop(); }
UtilTimerTickSource(Timer timer)            // 기존 java.util.Timer 동작
enum Speed { AGONIZING(500), SLOW(150), MEDIUM(70), FAST(30); int millis(); }
interface GameListener { enum Change { EDITED, GENERATION } void boardChanged(Change change); }
```

GameController.tick의 boolean은 원본 Universe의 refreshNow 조건이며, true일 때만 `GENERATION`을 알린다. toggle·clear·성공한 load는 `EDITED`를 알리고 store는 알리지 않는다. 알림은 호출한 스레드에서 보낸다(tick은 Timer 스레드). TickSource.start는 이전 예약을 먼저 취소하고 즉시(지연 0) 시작한다. Clock은 주기 0 이하를 정지로 처리한다. Clock은 기존 Timer 스레드에서 알림을 보내며 메뉴 tick은 호출 스레드에서 동작한다. UI는 GameMenu에서 메뉴 선택 중 tick을 차단한다. 기존 호출 스레드와 잠금 구조를 유지한다. 외부에서 전달한 Timer의 최종 cancel은 소유자가 담당한다. stop은 원본대로 현재 TimerTask만 취소한다.

Load는 같은 보드 잠금 안에서 파일 읽기·닫기와 `BoardSnapshot` 변환(C의 `read(path, rows, columns)`)을 먼저 완료한다. 개방/내용 읽기/닫기/변환 실패 시 기존 일반·암세포와 Clock 실행 상태·속도·예약을 유지하고 EDITED를 보내지 않는다. 잘못된 파일 형식은 IOException으로 보고한다. 성공하면 Clock 정지 → `Board.restore(snapshot)` 후 잠금 밖에서 EDITED를 한 번 보낸다. 생존 목록에 없는 암세포 좌표가 든 파일은 C가 IOException으로 거부한다(#88 SNAP-09). 유효한 빈 목록은 성공, 0바이트 파일은 실패다. 실패 후 잠금이 풀리면 대기하던 tick/편집은 계속 진행할 수 있다. 적용 단계의 모델·Clock 프로그램 오류나 리스너 오류까지 rollback하지 않는다. 성공한 저장은 `Board.snapshot()`을 한 번만 캡처하고, 파일 좌표 순서는 C가 블록 폭으로 정한다. IOException 표시는 D가 담당한다. load/store는 성공·실패와 관계없이 연 파일 스트림을 닫고 반환한다.

applyPreset은 프리셋 좌상단 (0,0)을 `origin`에 맞춘다. 없는 id는 `UNKNOWN_ID`, 한 칸이라도 보드 밖이면 `OUT_OF_BOUNDS`이고 두 경우 모두 보드를 바꾸지 않고 알리지 않는다. 성공하면 잠금 하나 안에서 `Board.snapshot()`을 한 번 읽고 생존 목록(암세포 포함)에 없는 칸만 `Board.toggle`한 뒤 잠금 밖에서 `EDITED`를 한 번 보낸다. 시계는 멈추지 않는다. `origin == null`은 `NullPointerException`. 모드별 허용은 D가 판단하고 B는 검사하지 않는다.

- 변경: 2026-11-04, #29. main의 #16·#17 계약 반영. 시그니처 변경 없음.
- 변경: 2026-11-07, #41. 실패 경로에서도 파일 스트림을 닫도록 수정(`try-with-resources`). 시그니처·보드 상태 계약 변경 없음. 테스트: [APP-S01~04](test-cases.md#불러오기저장-후-파일-스트림-해제-41).
- 변경: 2026-11-07, #44. `Clock.instance()`·`Clock(Timer)` 삭제(→ `Clock(TickSource)`), `TickSource`·`UtilTimerTickSource`·`Speed`·`GameListener`·`Clock.start(Speed)`·`GameController.addGameListener` 추가. 영향: D(`Life`, `GameMenu`, `BoardPanel` 연결부). 근거: [003](003-pattern-analysis.md)
- 변경: 2026-11-13, #55. `GameController(Cell, Clock)` → `GameController(Board, Cell, Clock)`. 편집·tick은 A의 `Board`로, Load/Store는 기존 `Cell` Memento 경로. `GameController`의 보드 호출은 잠금 하나로 직렬화하고 알림은 잠금 밖에서 보낸다. 범위 밖 `toggle`은 `IndexOutOfBoundsException`(상태 변경 없음). 영향: D(`Life.java` 조립 1곳). 근거: [004](004-board-connection.md)

- 변경: 2026-11-17, #63. 공개 시그니처 유지, Load 실패 시 Clear/Clock 정지/EDITED를 제거하고 읽기·변환 성공 후 적용하도록 변경. B 주도, C read(Path) 사용, D의 IOException 표시 유지. [005](005-safe-load.md), [#63 구현 전 계약](https://github.com/DesignPattern8/DPTermProject/issues/63)과 PR에서 검토하며 팀원 합의 완료를 주장하지 않는다.
- 변경: 2026-11-23, #88. `GameController(Board, Cell, Clock[, CancerGame[, BoardFileStore]])` → `GameController(Board, Clock[, CancerGame[, BoardFileStore]])`. Load/Store가 파일용 `Cell` Memento 대신 `Board.snapshot()/restore()`와 C의 스냅샷 파일 API(#93)를 쓴다. 기본 저장소는 블록 폭 `[8, 1]`. 영향: `Life` 조립 1곳, A·D·2인용 테스트의 생성자 호출(인자 삭제만). 근거: [007](007-snapshot-file-connection.md)
- 변경: 2026-11-24, #62. `presets()`, `applyPreset(String, CellPosition)`, `PresetChoice`, `PresetResult`, `PresetCatalog`를 받는 생성자 추가. 기존 생성자·메서드 변경 없음. 영향: D(`Preset` 메뉴에서 사용). 합의: #62 11/17 댓글. 근거: [008](008-preset-placement.md)
