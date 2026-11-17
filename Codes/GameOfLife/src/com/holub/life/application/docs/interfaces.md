# 실행 계약

- 제공 B / 사용 D·Life / 변경 2026-11-03, #24
- 변경: 2026-11-17, #71 — A 모델 좌표 계약에 맞춰 legacy LOAD 원점은 CellPosition(0, 0), PersistenceMapper는 같은 불변 좌표를 담는 컬렉션 복사로 맞췄다. 공개 Controller·Mapper 시그니처, 잠금·Clock·알림·실패 처리와 createMemento/transfer 경로는 유지한다. [B 댓글](https://github.com/DesignPattern8/DPTermProject/issues/71#issuecomment-6944309778)에 따른 Board.snapshot()/restore() 전환은 #88의 별도 작업이다.

```java
GameController(Board board, Cell fileCells, Clock clock)  // fileCells: board가 감싼 같은 Neighborhood (파일 경로용, 과도기)
void GameController.toggle(int row, int column)
void GameController.clear()
boolean GameController.tick()
void GameController.load(File file) throws IOException
void GameController.store(File file) throws IOException
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

Load는 같은 보드 잠금 안에서 파일 읽기·닫기와 Memento 변환을 먼저 완료한다. 개방/내용 읽기/닫기/변환 실패 시 기존 일반·암세포와 Clock 실행 상태·속도·예약을 유지하고 EDITED를 보내지 않는다. 잘못된 파일 형식은 IOException으로 보고한다. 성공하면 Clock 정지 → Clear → transfer(LOAD) 후 잠금 밖에서 EDITED를 한 번 보낸다. 유효한 빈 목록은 성공, 0바이트 파일은 실패다. 실패 후 잠금이 풀리면 대기하던 tick/편집은 계속 진행할 수 있다. 적용 단계의 모델·Clock 프로그램 오류나 리스너 오류까지 rollback하지 않는다. 성공한 저장은 createMemento에서 좌표를 한 번만 수집한다. IOException 표시는 D가 담당한다. load/store는 성공·실패와 관계없이 연 파일 스트림을 닫고 반환한다.

- 변경: 2026-11-04, #29. main의 #16·#17 계약 반영. 시그니처 변경 없음.
- 변경: 2026-11-07, #41. 실패 경로에서도 파일 스트림을 닫도록 수정(`try-with-resources`). 시그니처·보드 상태 계약 변경 없음. 테스트: [APP-S01~04](test-cases.md#불러오기저장-후-파일-스트림-해제-41).
- 변경: 2026-11-07, #44. `Clock.instance()`·`Clock(Timer)` 삭제(→ `Clock(TickSource)`), `TickSource`·`UtilTimerTickSource`·`Speed`·`GameListener`·`Clock.start(Speed)`·`GameController.addGameListener` 추가. 영향: D(`Life`, `GameMenu`, `BoardPanel` 연결부). 근거: [003](003-pattern-analysis.md)
- 변경: 2026-11-13, #55. `GameController(Cell, Clock)` → `GameController(Board, Cell, Clock)`. 편집·tick은 A의 `Board`로, Load/Store는 기존 `Cell` Memento 경로. `GameController`의 보드 호출은 잠금 하나로 직렬화하고 알림은 잠금 밖에서 보낸다. 범위 밖 `toggle`은 `IndexOutOfBoundsException`(상태 변경 없음). 영향: D(`Life.java` 조립 1곳). 근거: [004](004-board-connection.md)

- 변경: 2026-11-17, #63. 공개 시그니처 유지, Load 실패 시 Clear/Clock 정지/EDITED를 제거하고 읽기·변환 성공 후 적용하도록 변경. B 주도, C read(Path) 사용, D의 IOException 표시 유지. [005](005-safe-load.md), [#63 구현 전 계약](https://github.com/DesignPattern8/DPTermProject/issues/63)과 PR에서 검토하며 팀원 합의 완료를 주장하지 않는다.
