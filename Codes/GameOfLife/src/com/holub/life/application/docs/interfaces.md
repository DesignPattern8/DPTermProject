# 실행 계약

- 제공 B / 사용 D·Life / 변경 2026-11-03, #24

```java
GameController(Cell board, Clock clock)
void GameController.toggle(int row, int column)
void GameController.clear()
boolean GameController.tick()
void GameController.load(File file) throws IOException
void GameController.store(File file) throws IOException
Clock Clock.instance()
Clock(Timer timer)
void Clock.startTicking(int millisecondsBetweenTicks)
void Clock.stop()
void Clock.tick()
void Clock.addClockListener(Clock.Listener listener)
```

GameController.tick의 boolean은 원본 Universe의 refreshNow 조건이다. Clock은 기존 Timer 스레드에서 알림을 보내며 메뉴 tick은 호출 스레드에서 동작한다. UI는 GameMenu에서 메뉴 선택 중 tick을 차단한다. 동시성 개선이나 알림 체계 변경은 하지 않는다. 외부에서 전달한 Timer의 최종 cancel은 소유자가 담당한다. stop은 원본대로 현재 TimerTask만 취소한다.

파일 열기 실패는 보드를 유지하지만 파일 내용 읽기 실패는 기존 Clear 이후 발생한다. 잘못된 파일 형식은 IOException으로 보고한다. 성공한 저장은 createMemento에서 좌표를 한 번만 수집한다. IOException 표시는 D가 담당한다. load/store는 성공·실패와 관계없이 연 파일 스트림을 닫고 반환한다.

- 변경: 2026-11-04, #29. main의 #16·#17 계약 반영. 시그니처 변경 없음.
- 변경: 2026-11-07, #41. 실패 경로에서도 파일 스트림을 닫도록 수정(`try-with-resources`). 시그니처·보드 상태 계약 변경 없음. 테스트: [APP-S01~04](test-cases.md#불러오기저장-후-파일-스트림-해제-41).
