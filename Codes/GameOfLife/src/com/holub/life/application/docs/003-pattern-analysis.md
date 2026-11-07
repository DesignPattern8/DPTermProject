# [003] B 도메인 기존 패턴 분석과 GoF 리팩토링 방향

- 상태: 제안 (초안, 팀 검토 전)
- 작성: 시형 / 2026-11-07 / [#43](https://github.com/DesignPattern8/DPTermProject/issues/43)
- 기준 코드: `main` `4afc828` (#25 도메인 추출 이후). 줄 번호는 이 커밋 기준이다.
- 범위: `application/Clock`, `application/GameController`, 그리고 B의 실행 정책이 아직 남아 있는 `ui/GameMenu`의 Go·tick 부분. 코드는 바꾸지 않는다. 기능 추가(Undo/Redo 등)는 다루지 않는다.

## 1. 현재 구조와 호출 관계

| B 공개 API | 호출 위치 |
| --- | --- |
| `Clock.instance()` | `Life.java:16` (제품 코드에서 유일) |
| `new Clock(Timer)` | `ClockTest.java:30`, `GameControllerTest.java:23`, `GameMenuTest.java:34,61` (테스트만) |
| `Clock.startTicking(int)` | `GameMenu.java:42` |
| `Clock.stop()` | `GameController.java:45,55` |
| `Clock.tick()` | `GameMenu.java:40` (Tick 메뉴) |
| `Clock.addClockListener(Listener)` | `GameMenu.java:35` (구독자 1개) |
| `GameController.toggle(row, column)` | `BoardPanel.java:51` |
| `GameController.clear()` | `GameMenu.java:20` |
| `GameController.tick()` | `GameMenu.java:47` (Clock 알림을 받아 호출) |
| `GameController.load/store(File)` | `GameMenu.java:57,67` |

크기(물리 행 수): `Clock.java` 98, `GameController.java` 60, `Publisher.java` 221(내부 테스트 `main` 포함), `GameMenu.java` 73.

한 세대가 진행되는 경로: `java.util.Timer` 스레드 → `TimerTask.run` (`Clock.java:45`) → `Clock.tick` → `Publisher.publish` → `Distributor.deliverTo` (`Clock.java:93`) → `GameMenu.onTick` (`GameMenu.java:46`) → `GameController.tick` → `Cell.figureNextState/transition`. 간접 호출은 Clock 안에서 3단계(TimerTask, Publisher, Distributor)다.

## 2. 이미 쓰이고 있는 패턴

원본 UML(`diagrams/class diagram/holub-life-class-core.puml:37,152,167,173,179`)은 `Universe <<Singleton, Mediator>>`, `Clock <<Singleton, Observer: Subject>>`, `Clock.Listener <<Observer>>`, `Publisher <<Observer 구현 / Strategy>>`, `Publisher.Distributor <<Visitor>>`로 표시한다. #25 이후 B 코드에서 확인한 내용은 다음과 같다.

| 패턴 | 위치 | 현재 코드에서의 모습 |
| --- | --- | --- |
| Singleton | `Clock.java:19-28` | `instance()`가 지연 생성한다. 그런데 `public Clock(Timer)`(`Clock.java:15`)도 있어서 인스턴스가 하나라는 보장은 이미 없다. 원본은 생성자에서 메뉴를 만들어야 해서 Singleton이었는데(원본 `Clock.java` 주석 "can't be an everything-is-static singleton because it creates a menu"), 메뉴 생성은 #25에서 `GameMenu`로 옮겨졌다. |
| Observer | `Clock.java:58-97`, `GameMenu.java:35` | Subject는 `Clock`, Observer는 `Clock.Listener`. 알리는 내용은 "시간이 흘렀다"이고 "보드가 바뀌었다"가 아니다. 구독자는 1개이며 구독 해제 API는 없다. |
| Visitor 성격 (Publisher 내부) | `Publisher.java:65-67,97-100` | `Distributor.deliverTo`가 구독자마다 호출된다. 구독자 타입이 `Object`라서 `Clock.java:94`에서 형변환한다. |
| Command 성격 | `Clock.java:43-48`, `GameMenu.java:27` | `TimerTask`와 `ActionListener`가 동작을 객체로 감싼다. 실행 취소·기록은 없다. |
| Facade 성격 | `GameController.java:11-60` | UI는 보드 편집·진행·파일 작업을 이 클래스 하나로 요청한다. `toggle`/`clear`는 그대로 위임만 한다. |
| Memento Caretaker | `GameController.java:47-49,56-57` | `createMemento()`로 받은 `NeighborhoodState`를 C(`LegacyLifeCodec`)에 넘기고 A(`transfer`)로 되돌린다. 내부는 보지 않는다. |
| Null Object (A 소유, B가 사용) | `GameController.java:29-37` | 바깥 셀의 이웃 8칸을 `Cell.DUMMY`로 넘긴다. |

## 3. 문제점

| ID | 문제 | 근거 |
| --- | --- | --- |
| P1 | Singleton이 남아 있지만 이유가 사라졌고, 공개 생성자 때문에 강제되지도 않는다. 생성 방법이 두 가지다. | `Clock.java:15,25`, `Life.java:16` |
| P2 | 타이머 구현(`java.util.Timer`)에 직접 의존한다. 테스트는 `Timer`를 상속해 `scheduleAtFixedRate`를 가로채는 대역으로 우회한다(실제 예약은 하루 뒤로 미룸). | `Clock.java:8,49`, `ClockTest.java:9-24` |
| P3 | tick은 Timer 스레드에서, 클릭 편집은 Swing 이벤트 스레드에서 같은 보드를 바꾼다. 원본과 같은 동작이며, A의 `Neighborhood`가 `ConditionVariable`로 읽기만 막는다. | `Clock.java:45`, `BoardPanel.java:51`, `Neighborhood.java:28` |
| P4 | 실행 속도 정책이 UI에 있다. 메뉴 이름 첫 글자로 주기를 고르고, 정지는 `startTicking(0)`으로 표현한다. "실행 중인가"를 물을 방법이 없다. | `GameMenu.java:38-44`, `Clock.java:36-56` |
| P5 | 화면 갱신 책임이 호출하는 쪽마다 흩어져 있다. 편집 후 `repaint()`, Clear·Load 후 `repaint()`, tick 후 `refreshNow()`를 각 UI 코드가 직접 부른다. 새 편집 경로(예: 2인용 배치)를 추가하면 갱신 호출도 매번 함께 넣어야 한다. | `BoardPanel.java:52`, `GameMenu.java:21,34-35,47,62` |
| P6 | `Publisher`는 구독자 타입이 `Object`라서 컴파일 시점 검사가 없다. 제품 코드의 사용처는 `Clock` 하나다. | `Publisher.java:117`, `Clock.java:94` |
| P7 | `GameController`가 구체 클래스(`Clock`)와 정적 함수(`BoardFileStore`, `LegacyLifeCodec`)를 직접 호출해서 대역으로 바꿀 수 없다. #41에서 "저장 중 직렬화 실패" 테스트를 못 쓴 이유다. | `GameController.java:44-48,54-57` |
| P8 | 한 세대 진행에 A의 내부 규약(이웃 8개를 `DUMMY`로 넘기고 `figureNextState` → `transition` 순서로 호출)을 B가 알아야 한다. | `GameController.java:28-41` |

P8은 A의 보드 API(`Board.step()` 같은 것)가 생기면 사라지는 문제라서 A에 요청할 항목으로만 남긴다.

## 4. 문제별 후보 비교

오버헤드 항목은 AGENTS.md 1-1을 따른다. 아직 구현하지 않았으므로 **추가 타입 수·변경 파일 수는 설계 기준 추정**이다. LOC와 런타임은 구현 PR에서 실측한다.

### D1. Clock을 어떻게 만들고 전달할 것인가 (P1)

| 후보 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. Singleton 유지 | 지금 그대로 | 변경 없음 | 공개 생성자와 공존해서 Singleton의 의미가 없다. 전역 접근이 남는다. |
| B. 생성자 주입 (패턴 없이) | `Life`가 하나 만들어 `GameController`·UI에 넘긴다. `instance()` 삭제 | 생성 방법이 하나가 된다. 테스트가 이미 이 방식이다 | `Life.java`(D 관리) 1줄 수정 |
| C. Monostate | 상태를 static으로 두고 인스턴스는 여러 개 허용 | 호출부 변경 없음 | 테스트끼리 상태를 공유해 격리가 깨진다 |

| 오버헤드 | A | B | C |
| --- | --- | --- | --- |
| 구조 비용 (추정) | 0 | 타입 0, 파일 2(`Clock`, `Life`) | 타입 0, 파일 1 |
| 간접성 | 0 | 0 | 0 |
| 결합도 | 전역 접근 유지 | 전역 접근 제거 | 전역 상태 유지 |
| 테스트 용이성 | 생성자로 이미 가능 | 같음 | 나빠짐 (static 상태) |
| 확장 비용 | 2인용에서 시계가 2개 필요하면 불가 | 필요한 만큼 생성 | 불가 |

### D2. 시간 흐름(타이머)을 어떻게 분리할 것인가 (P2, P3)

| 후보 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. `java.util.Timer` 직접 사용 유지 | 지금 그대로 | 변경 없음 | 상속 대역 우회, 스레드 문제 그대로 |
| B. Strategy: `TickSource` 인터페이스 | `start(interval, Runnable)`/`stop()`. 구현은 기존 동작용 `UtilTimerTickSource`(B)와 D가 이미 만든 `SwingTickSource`(`refactor/D-ui`) | 테스트에서 수동 tick 대역을 쓸 수 있다. D의 요청안과 같다. Swing 구현을 고르면 tick이 이벤트 스레드로 옮겨가 P3가 사라진다 | 타입 +2(인터페이스, 기본 구현). Swing 구현으로 바꾸면 스레드가 바뀌므로 "동작 유지"인지 특성 테스트로 확인해야 한다 |
| C. Bridge | Clock 계층과 타이머 계층을 분리 | B와 같은 효과 | Clock 쪽 계층(여러 종류의 Clock)이 없어서 실제로는 B와 같은 구조가 된다. 이름만 무거워진다 |
| D. Template Method | 추상 `Clock`에 `schedule()`을 추상 메서드로 두고 타이머별 하위 클래스 | 인터페이스 없이 가능 | 시간 정책과 타이머가 상속으로 묶인다. D가 Swing 하위 클래스를 만들려면 B의 클래스를 상속해야 해서 패키지 간 결합이 커진다 |

| 오버헤드 | A | B | C | D |
| --- | --- | --- | --- | --- |
| 구조 비용 (추정) | 0 | 타입 +2, 파일 3(`Clock`, 새 2) + 테스트 | B와 같음 | 타입 +1~2, 파일 2~3 |
| 간접성 | 0 | tick당 +1 (`TickSource` → `Runnable`). 대신 `TimerTask` 1단계가 구현 안으로 들어감 | B와 같음 | tick당 +1 (가상 호출) |
| 결합도 | B → `java.util.Timer` | B → 자기 인터페이스만 | B와 같음 | D → B 상속 |
| 테스트 용이성 | `Timer` 상속 대역 필요 | 수동 tick 대역(람다)로 충분 | B와 같음 | 하위 클래스 대역 필요 |
| 확장 비용 | 새 타이머마다 `Clock` 수정 | 구현 클래스 1개 추가 | B와 같음 | 하위 클래스 1개 추가 |

### D3. 실행 상태와 속도를 어떻게 표현할 것인가 (P4)

| 후보 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. 지금 그대로 | 메뉴 이름 첫 글자 분기 + `0` = 정지 | 변경 없음 | 메뉴 이름을 바꾸면 동작이 바뀐다. 실행 중 여부를 물을 수 없다 |
| B. enum `Speed` (패턴 없이) | `HALT(0), AGONIZING(500), SLOW(150), MEDIUM(70), FAST(30)`를 B가 소유하고 메뉴는 enum 값만 넘긴다 | 정책이 B로 온다. 주기 값 한 곳 | 타입 +1 |
| C. State | `Stopped`/`Running` 상태 객체가 `start`/`stop`/`tick` 요청을 다르게 처리 | 상태별 규칙이 늘면 한 곳에 모인다 | 지금 상태별로 다른 동작은 "이미 실행 중이면 이전 예약 취소" 하나뿐이다(`Clock.java:37-40`). 타입 +3 이상에 비해 얻는 것이 적다 |
| D. Command (메뉴 항목마다 명령 객체) | `HaltCommand`, `SpeedCommand(ms)`, `StepCommand` | 메뉴·단축키·버튼이 같은 객체를 쓴다 | 실행 취소·기록이 필요 없어서 람다/메서드 참조와 차이가 없다. 2인용 #6에서도 같은 이유로 Command를 기각했다(`feature/two-player-mode` 브랜치 `twoplayer/docs/004-player-input.md`) |

| 오버헤드 | A | B | C | D |
| --- | --- | --- | --- | --- |
| 구조 비용 (추정) | 0 | 타입 +1, 파일 2(`GameMenu`, 새 1) | 타입 +3, 파일 3~4 | 타입 +3~4 |
| 간접성 | 0 | 0 | 요청당 +1 (상태 위임) | 메뉴 동작당 +1 |
| 결합도 | 정책이 D에 있음 | 정책이 B로 이동 | B 안에 머묾 | B 또는 D |
| 테스트 용이성 | 메뉴 이름 문자열로 테스트 (`GameMenuTest`) | enum 값으로 테스트 | 상태별 테스트 | 명령별 테스트 |
| 확장 비용 | 속도 추가 시 분기 수정 | enum 상수 1개 추가 | 상태 추가 시 클래스 추가 | 명령 클래스 추가 |

### D4. 보드가 바뀌었다는 사실을 어떻게 알릴 것인가 (P5, P6)

| 후보 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. 지금 그대로 | 호출한 UI가 직접 `repaint`/`refreshNow` | 변경 없음 | 새 편집 경로마다 갱신 호출을 잊을 수 있다 |
| B. Observer: `GameListener` | `GameController`가 편집·tick·불러오기 후 `boardChanged(...)`를 알린다. D의 `BoardPanel`이 구독 | 갱신 책임이 한 곳. D의 요청안과 같다. 통계·2인용 상태 표시줄도 같은 알림을 받을 수 있다 | 타입 +1. 알림 인자(지금은 갱신 범위 boolean, D 요청은 `BoardSnapshot`)를 A와 합의해야 한다 |
| C. Mediator | `GameController`가 화면 객체를 직접 알고 갱신을 지시 | 호출 단계가 가장 적다 | B → UI 의존이 생겨 패키지 규칙("application은 UI를 직접 호출하지 않음")을 어긴다 |

B를 고를 경우 구현 방식도 나뉜다: (B-1) 기존 `Publisher` 재사용(형변환 남음, 수정 중에도 안전한 순회 제공) / (B-2) `CopyOnWriteArrayList<GameListener>` 단순 구현(타입 검사, 행 수가 적음). `Clock`이 `Publisher`의 유일한 사용처라서 B-2로 바꾸면 `Publisher`를 쓰는 곳이 사라진다.

| 오버헤드 | A | B | C |
| --- | --- | --- | --- |
| 구조 비용 (추정) | 0 | 타입 +1, 파일 4(`GameController`, 새 1, `BoardPanel`, `GameMenu`) | 타입 0, 파일 3 |
| 간접성 | 0 | 변경 1번당 +1 (구독자 순회) | 0 |
| 결합도 | UI가 갱신 시점을 앎 | B → 자기 인터페이스만 | B → `java.awt`/Swing |
| 테스트 용이성 | Swing 없이 "갱신되었는가"를 확인할 수 없음 | 기록용 리스너 대역으로 확인 가능 | Swing 필요 |
| 확장 비용 | 새 편집 경로마다 UI 수정 | 리스너 1개 추가 | B 수정 |

### D5. GameController의 역할 이름: Facade인가 Mediator인가

원본 UML은 `Universe`를 Mediator로 표시했다. #25 이후 `GameController`를 보면:

- 하위 객체(`Clock`, `Cell`, `BoardFileStore`, `LegacyLifeCodec`)끼리 서로 알지 않고, `GameController`에 콜백하지도 않는다. 호출은 UI → `GameController` → 하위 객체로 한 방향이다.
- Mediator가 되려면 동료 객체가 중재자에게 사건을 알리고 중재자가 다른 동료를 조정해야 한다. 지금 그런 흐름은 load/store 안에서 `Clock.stop()`을 먼저 부르는 순서 조정 정도다.

그래서 현재 역할은 **Facade**로 설명하는 것이 코드와 맞다. D2·D4를 적용하면 `TickSource`가 `GameController`에 tick을 알리고 `GameController`가 보드 진행과 `GameListener` 알림을 조정하게 되는데, 그때 Mediator로 볼지 다시 판단한다.

### D6. Memento 보관 (Caretaker)

지금처럼 B는 받아서 넘기기만 하고 내부를 보지 않는다. 변경할 이유가 없어 유지한다. A가 불변 `BoardSnapshot`을 만들면 그것을 받는 쪽으로 바꾼다.

## 5. B와 관련된 나머지 GoF 패턴 검토

AGENTS.md 1-1("리팩토링은 GoF 패턴을 최대한 많이 비교")에 따라 위에서 다루지 않은 패턴이 B 코드에 맞는지 확인했다.

| 패턴 | 판단 | 이유 (이 코드 기준) |
| --- | --- | --- |
| Abstract Factory / Factory Method | 해당 없음 | B가 만드는 객체는 `TimerTask` 1종뿐이다. 객체 생성 가족이 없다 |
| Builder | 해당 없음 | `GameController` 생성 인자는 2개다 |
| Prototype | 해당 없음 (A) | 셀 구조 복제는 A의 `Neighborhood` 생성 방식이다 |
| Adapter | 조건부 | `TickSource`를 도입하면 기존 `java.util.Timer`를 감싸는 기본 구현이 Adapter 역할을 한다. D2-B의 구현 세부로 본다 |
| Composite | 해당 없음 (A) | 보드 구조 |
| Decorator | 해당 없음 | 실행 제어에 덧붙일 선택 기능이 없다. 렌더링 옵션은 D 영역 |
| Flyweight | 해당 없음 | B에 공유할 대량 객체가 없다 |
| Proxy | 검토 후 기각 | 메뉴가 열려 있으면 tick을 막는 필터(`GameMenu.java:46-53`)를 `GameController` 앞의 보호 Proxy로 만들 수 있다. 하지만 이 필터는 Swing 메뉴 상태(`MenuSelectionManager`)에 의존해서 D에 있어야 하고, 조건 하나에 클래스를 두는 비용이 크다 |
| Chain of Responsibility | 해당 없음 | tick을 처리할 후보가 여러 개 연결된 구조가 아니다 |
| Interpreter | 해당 없음 | 해석할 언어가 없다 |
| Iterator | 해당 없음 (A) | 살아 있는 칸 순회는 A의 문제 |
| Visitor | 유지 여부만 판단 | `Publisher.Distributor`가 Visitor 형태다. D4-B-2를 고르면 `Publisher`와 함께 B에서 사라진다 |
| Strategy / State / Command / Observer / Facade / Mediator / Memento / Singleton / Template Method / Bridge | 4장 | D1~D6에서 비교 |

## 6. 제안 (팀 검토 전)

| 결정 | 제안 | 근거 요약 |
| --- | --- | --- |
| D1 | B. 생성자 주입, `Clock.instance()` 제거 | Singleton을 만든 이유(생성자에서 메뉴 생성)가 사라졌고 공개 생성자로 이미 깨져 있다 |
| D2 | B. Strategy `TickSource` | 대역 테스트가 쉬워지고 D의 `SwingTickSource`를 그대로 받을 수 있다. Bridge·Template Method는 지금 구조에서 얻는 것이 없다 |
| D3 | B. enum `Speed` | 상태별로 다른 규칙이 하나뿐이라 State는 과하다. 2인용 등 새 기능이 실행 상태 규칙을 늘리면 2차 리팩토링에서 State를 다시 검토한다 |
| D4 | B. Observer `GameListener`, 구현은 B-2 | 갱신 책임을 한 곳에 모으고 B → UI 의존을 만들지 않는다 |
| D5 | Facade로 기술 | 현재 호출 방향이 한 방향이다 |
| D6 | 유지 | — |

### D의 요청안(`refactor/D-ui` `ui/docs/interfaces.md`) 검토

| 요청 | 의견 |
| --- | --- |
| `TickSource.start(int, Runnable)` / `stop()` | 수락 (D2-B와 같음) |
| `GameListener.boardChanged(BoardSnapshot)` | 방향은 수락. `BoardSnapshot`은 A가 아직 만들지 않았으므로, 그 전까지는 기존 부분 갱신 조건(`tick()`의 boolean)을 담는 인자로 시작할지 A·D와 정해야 한다 |
| `GameController`를 인터페이스로 | 보류. 구현이 하나뿐이라 지금은 구체 클래스로 두고, 대역이 필요한 쪽(UI 테스트)이 생기면 추출한다 |
| `start(intervalMillis)` / `stop()` / `step()` | D3 결과(`Speed` enum)에 맞춰 `start(Speed)`를 제안. 정지는 `stop()`으로 분리 (요청과 같음) |
| `toggleCell(CellPosition)` | A의 `CellPosition` 도입 후 반영 |

## 7. 적용 순서 (구현 PR 후보)

모두 기존 동작을 유지하는 리팩토링이다. 각 단계는 test-cases 정의 → 테스트 → 구현 순서로 별도 PR로 낸다.

1. D1: `Clock.instance()` 제거, `Life`에서 주입 (D와 협의, `Life.java`는 D 관리)
2. D3: `Speed` enum, `GameMenu.go`의 문자 분기 대체 (`GameMenuTest#goCommandsKeepOriginalActions` 유지)
3. D2: `TickSource` + 기존 `java.util.Timer` 기반 구현. 스레드는 아직 바꾸지 않는다
4. D4: `GameListener`, UI의 직접 `repaint` 호출 정리 (D와 함께)
5. `SwingTickSource`로 교체할지 결정 (스레드가 바뀌므로 특성 테스트·수동 시연으로 확인)

## 8. 전/후 다이어그램

TODO: `diagrams/003-before.puml`, `003-after.puml` (Clock·GameController·GameMenu·Publisher 범위) 작성 후 `./gradlew renderDiagrams -Pdir=src/com/holub/life/application`.

## 9. 후속 작업 / 열린 질문

- A에 요청: 한 세대 진행 API(P8), `BoardSnapshot` 도입 시점
- D와 합의: `GameListener` 알림 인자, `Life.java` 수정 담당, `SwingTickSource` 도입 시점
- 기능 담당자의 새 기능(2인용 등)이 들어온 뒤 2차 리팩토링에서 D3(State)·D5(Mediator) 판단을 다시 한다
- 불러오기 실패 시 보드가 비워지는 동작(`GameController.java:45-48`)은 동작 변경이라 이 분석 범위 밖이다
