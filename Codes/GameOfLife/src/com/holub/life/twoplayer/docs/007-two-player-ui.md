# [007] 2인용 UI 통합 — 모드 전환, 진입점, 렌더러, 상태 표시줄

- 상태: 채택 (구현 완료 2026-11-16. 오버헤드 표 D 열은 실측)
- 작성자: Son2kwon / 2026-11-16 / 관련 이슈: #7 (관련: #69, #72, #74)
- 근거 명세: [rules.md](rules.md) 4장(단계), 5장(배치·확정·불가능한 클릭), 8장(Store 비활성, 상태 표시줄 한 줄 + 진영 경계선)
- 코드 근거: `feature/base` `1889b16`. 파일 경로는 `Codes/GameOfLife/src/com/holub/life/` 아래 기준
- 측정 코드: `Codes/GameOfLife/test/com/holub/life/twoplayer/ui/StaticLockCheck.java` (아래 "#74 확인")

## 문제

#5·#6으로 2인용 모델(`TwoPlayerGame`, `NeighborhoodTwoPlayerBoard`)은 끝났지만 화면에 연결되어 있지 않다. 이슈 #7 본문은 `Universe`·`MenuSite` 기준으로 쓰였는데, 지금은 `Universe`가 없고 그 역할이 `ui/LifeFrame`, `ui/BoardPanel`, `ui/GameMenu`, `application/GameController`, `application/Clock`으로 나뉘어 있다. 지금 구조에서 연결을 막는 사실:

| 위치 | 사실 | 영향 |
| --- | --- | --- |
| `ui/LifeFrame.java:20-22` | 창에 `BoardPanel` 하나만 붙는다 | 2인용 화면을 붙일 자리가 없다 |
| `ui/BoardPanel.java:52-53` | 클릭은 `controller.toggle`·`treat`(클래식 `GameController`) | 2인용 클릭(현재 플레이어 임시 배치)은 다른 대상이 받아야 한다 |
| `ui/BoardRenderer.java:39` | `Resident`만 칠한다 (#69) | 2인용 셀은 그려지지 않는다 |
| 006 W2·W3, #72 | 2인용 보드는 `Board`를 구현하지 않고 `GameController`를 거치지 않는다 | 2인용 진입점이 따로 필요하다 |
| `ui/GameMenu.java:25-32` | Go 메뉴는 `Clock` 하나에 묶이고, `install`이 그 시계에 클래식 tick 리스너를 단다 | 2인용 모드에서도 시계가 돌면 **클래식 보드도 계속 진행한다** |
| `application/Clock.java:43-45` | 리스너를 더하기만 하고 뗄 수 없다 | 모드를 바꿔도 클래식 리스너를 뗄 수 없다 |
| `model/Neighborhood.java:32`, `258` (#74) | 읽기 잠금·중첩 깊이가 `static` | 두 보드가 동시에 세대를 진행하면 문제 (아래 "#74 확인") |
| `ui/GameMenu.java:20-22` | Clear·Load·Store가 클래식 `GameController`로 간다 | 2인용 모드에서 누르면 보이지 않는 클래식 보드를 바꾼다. Store는 rules.md 8장에서 비활성 |

합의된 제약: model·application은 고치지 않고 `Clock`만 가져다 쓴다(#5와 같음). 2인용 진입점과 렌더러는 `twoplayer` 안에 만든다. ui(`LifeFrame`, `GameMenu`)는 모드 메뉴를 붙이는 부분만 고친다(리팩토링이 아니라 기능 추가).

## #74 확인 — 모드 전환에서 실제로 문제가 되는가

scratch에서 실행(2026-11-16, 코드 무수정). 클래식 64×64 보드(시드 7, 무작위 1,500번 toggle)와 2인용 보드(시드 11, 1,500번 배치)를 한 프로세스에 만든다. 환경: Apple M4 Pro 12코어, macOS, JDK 26.0.2.1.

| 경우 | 진행 방법 | 결과 (진행을 멈춘 뒤) |
| --- | --- | --- |
| S1. 한 번에 한 보드만 | 한 스레드에서 클래식 500세대 → 2인용 500세대 | `nestingLevel` = -1, 잠금 열림, 클래식 `beginRedraw(true)` 200,000번 중 false **0번** |
| S2. 두 보드를 동시에 | 두 스레드에서 1초 동안 계속 진행 (5회) | 5회 모두 `nestingLevel` = -21,035 ~ -23,139, 잠금 **닫힌 채 남음**, 클래식 `beginRedraw(true)` 10,000번 중 false **10,000번** |
| S3. 모드마다 시계 (Fast 30 ms 두 개) | `Clock` + `UtilTimerTickSource` 두 개를 동시에 Fast로 시작, `nestingLevel`이 -1이 아니게 되면 멈춤 (6회) | 6회 모두 **1초 안에** `nestingLevel`이 -3 ~ -8로 깨짐. 1회는 잠금이 닫힌 채 남아 클래식 `beginRedraw(true)` 10,000번 중 false 10,000번, 5회는 잠금이 열린 채 남음(false 0번) |

- #74 본문의 "`nestingLevel`의 `++`/`--`가 섞여 잠금이 풀리지 않거나 잘못 풀릴 수 있다(추정)"가 재현됐다. 한 번 깨지면 **두 보드를 멈춘 뒤에도** 되돌아오지 않는다.
  - 잠금이 닫힌 채 남으면 클래식 화면은 다시는 그려지지 않는다(`ui/BoardRenderer.java:16`에서 바로 return).
  - 잠금이 열린 채 남아도 `nestingLevel`이 -1이 아니므로 이후 `transition()`의 `++nestingLevel == 0`(`model/Neighborhood.java:233`)이 다시는 참이 되지 않는다. 잠금이 영영 닫히지 않아, 세대 진행 중에도 그리기를 막지 못한다(코드로 추론, 화면 깨짐은 재현하지 않음).
- S3의 6회 중 1회는 이 문서의 측정 클래스를 만들기 전 scratch 실행이고, 5회는 `StaticLockCheck`로 돌린 값이다.
- 그러므로 모드 전환에서 **문제가 된다.** 단, 두 보드가 동시에 세대를 진행할 때만이다(S1은 0번). 클래식 리스너를 뗄 수 없는 지금 구조(`Clock.java:43-45`, `GameMenu.java:32`)에서 2인용을 그냥 붙이면 S2·S3이 된다.
- 재현 결과는 #74에 댓글로 남겼다. `Neighborhood`는 고치지 않는다(강령 2). 2인용 쪽에서 피해 가는 방법은 아래 결정 1·3.

## 결정 1. 모드 전환 구조

### 후보

#### A. Abstract Factory — `GameMode`가 셀·규칙·입력 처리·판정을 함께 만든다 (이슈 #7 원래 후보)

하나의 `BoardPanel`·컨트롤러가 팩토리에서 받은 부품(셀 프로토타입, 클릭 처리, 렌더러, tick)을 쓴다.

- 장점: 모드 하나 = 팩토리 하나. 부품 조합이 어긋나지 않는다.
- 단점: 부품을 받을 쪽이 없다. `BoardPanel`은 `Cell`과 `GameController`를 받고(`BoardPanel.java:15`), `GameController`는 클래식 `Board`를 받는다. 2인용은 둘 다 쓰지 않는다(006 W3). 팩토리를 쓰려면 `BoardPanel`·`BoardRenderer`·`GameController`를 모드 중립으로 바꿔야 하므로 ui·application 리팩토링이 된다.

#### B. Strategy — 기존 `BoardPanel` 안에서 클릭·그리기·tick 전략만 바꾼다 (006 후보 B)

- 장점: 패널이 하나라 크기·메뉴 처리가 한 곳이다.
- 단점: `BoardPanel`의 마우스 처리(`:44-56`)와 그리기(`:73`, `:88`), `BoardRenderer`(#69)를 고친다. 전략 인터페이스가 클래식(`toggle`·`treat`)과 2인용(`select`·`confirm`·`pass`)의 입력을 모두 담아야 해서 커진다. ui 담당 영역의 리팩토링이다.

#### C. 패턴 없음 — 모드마다 화면과 시계를 따로, `CardLayout`으로 교체

`twoplayer`가 자기 `Clock`과 패널을 만들고, `LifeFrame`은 Mode 메뉴에서 카드만 바꾼다.

- 장점: ui 변경이 가장 적다(Mode 메뉴와 카드).
- 단점: Go 메뉴가 클래식 시계에 묶여 있어(`GameMenu.java:25-31`) 2인용용 Go 메뉴를 하나 더 만들거나 메뉴를 다시 묶어야 한다. 두 시계가 함께 돌 수 있어 **S3이 된다**(1초 안에 클래식 화면이 멈춤). 시계를 서로 멈추게 해도 진행 중인 tick은 멈추지 않는다(`java.util.Timer`의 실행 중 작업은 `cancel`로 끝나지 않음).

#### D. 화면 교체 + `GameMode` 인터페이스 + 시계 하나를 `ModeSwitch`가 잠금 하나로 나눠 준다 (Strategy, 채택)

```java
// ui
public interface GameMode {          // 모드마다 다른 것만 (Strategy)
  String menuName();                 // Mode 메뉴 항목 이름
  JComponent view();                 // CardLayout 카드
  void tick();                       // 시계 tick 1번 (ModeSwitch의 잠금 안에서 불림)
  boolean supportsFileCommands();    // Clear·Load·Store 사용 가능 여부
}
public final class ModeSwitch {      // 시계 리스너 1개, 잠금 1개
  ModeSwitch(Clock clock, GameMode first, Consumer<Boolean> fileCommandsEnabled);
  void add(GameMode mode);
  void select(GameMode mode);        // 시계를 멈추고, 진행 중 tick이 끝나길 기다린 뒤 바꾼다
  JComponent view();
}
public final class ClassicMode implements GameMode { ... }   // BoardPanel + GameController.tick
// twoplayer.ui
public final class TwoPlayerMode implements GameMode { ... } // 2인용 진입점
```

- 시계는 하나만 쓴다. Go 메뉴(`GameMenu.go`)는 그대로다. 시계 리스너는 `ModeSwitch`가 하나만 달고, 지금 모드의 `tick()`만 부른다. 클래식 리스너를 따로 달지 않으므로 뗄 필요가 없다.
- **모든 모드의 tick이 같은 잠금 안에서 돈다.** 모드 전환도 그 잠금을 잡으므로, 전환이 끝난 뒤에는 이전 모드의 tick이 진행 중일 수 없다. 그래서 두 보드가 동시에 세대를 진행하는 일(S2·S3)이 구조적으로 없다.
- 장점: model·application 무수정. ui 변경은 Mode 메뉴와 tick 경로뿐이고 `BoardPanel`·`BoardRenderer`·`GameController`는 그대로다. 모드를 하나 더 넣을 때 `GameMode` 구현 1개 + 조립 1줄.
- 단점: ui에 타입 3개(`GameMode`, `ModeSwitch`, `ClassicMode`)가 생긴다. 2인용 tick이 시계 스레드에서 돌아 2인용 진입점이 클릭(EDT)과 tick(시계 스레드)을 직렬화해야 한다(결정 3).
- State가 아니라 Strategy로 본 이유: 모드는 스스로 다음 모드로 바뀌지 않고, 사용자가 메뉴로 고른다.

### 오버헤드 비교표

- 실측: 구현 뒤 저장소 코드에서 센 값(D 열). LOC는 빈 줄·주석 제외. 추정: 구현하지 않은 후보(A~C).

| 항목 | A. Abstract Factory | B. Strategy (패널 안) | C. 화면·시계 따로 | **D. `GameMode` + `ModeSwitch`** |
| --- | --- | --- | --- | --- |
| 구조 — 고칠 기존 파일 | `BoardPanel`, `BoardRenderer`, `GameController`, `LifeFrame`, `GameMenu` 이상 (추정) | `BoardPanel`, `BoardRenderer`, `LifeFrame`, `GameMenu` (추정) | `LifeFrame` + Go 메뉴 재연결 (추정) | `LifeFrame` +22/−2, `GameMenu` +18/−2, `Life` +3/−1 (`git diff --numstat`, 실측). `BoardPanel`·`BoardRenderer`·`GameController`·`Clock`·model 0 |
| 구조 — 새 타입 | 팩토리 인터페이스 1 + 부품 인터페이스 3~4 + 모드별 구현 (추정) | 전략 인터페이스 2~3 + 구현 (추정) | 2인용 쪽만 (추정) | ui 3: `GameMode` 8줄, `ModeSwitch` 74줄, `ClassicMode` 27줄. 2인용 4: `TwoPlayerSession` 153줄, `TwoPlayerMode` 40줄, `TwoPlayerPanel` 117줄, `TwoPlayerRenderer` 46줄 (실측) |
| 간접성 — tick 1번 | 팩토리 부품 호출 +1 (추정) | 전략 호출 +1 (추정) | 0 | `ModeSwitch` → `GameMode.tick()` +1, 잠금 1번 |
| 결합도 | ui·application이 모드 부품 인터페이스에 의존 | ui가 2인용 입력 개념을 가짐 | `twoplayer` → `application.Clock` | ui는 `twoplayer`를 모름. `twoplayer.ui` → `ui.GameMode`, 조립은 `Life` |
| #74 (두 보드 동시 진행) | 보드가 하나면 없음 (추정) | 없음 (추정) | **있음 (S3 실측, 1초 안)** | 없음 (잠금으로 직렬화, TP-U-13으로 확인) |
| 테스트 용이성 | 팩토리 대역 | 전략 대역 | 시계 두 개를 함께 다뤄야 함 | headless에서 대역 모드·대역 `TickSource`로 TP-U 17개 (실측) |
| 확장 — 다음 모드 | 팩토리 + 부품 전부 | 전략 구현 전부 | 화면 + 시계 + 메뉴 | `GameMode` 구현 1 + 조립 1줄 |
| ui 담당 영역과 충돌 | 큼 | 큼 | 작음 | 작음 (Mode 메뉴와 tick 경로) |

### 결정

**D를 채택한다.** C는 ui 변경이 가장 적지만 S3(1초 안에 클래식 화면 정지)을 피할 수 없다. A·B는 ui·application 리팩토링이라 이번 범위(기능 추가)가 아니다. B는 #69 후보 B와 같은 방향이라, ui 담당이 #69를 B로 정하면 그때 다시 본다.

## 결정 2. 상태 표시줄 갱신

| 항목 | A. Observer (2인용 진입점의 리스너 목록) | B. 매 tick 폴링 |
| --- | --- | --- |
| 갱신 시점 | 입력(클릭·확정·패스·새 게임)과 진행한 tick 직후 | tick마다 |
| 정확성 | 배치 단계(시계가 멈춰 있음)에도 갱신 | **배치 단계에서 시계가 멈춰 있으면 클릭해도 문구가 바뀌지 않는다** |
| 구조 비용 | 리스너 목록 1개(`CopyOnWriteArrayList<Runnable>`, `GameController.java:28`과 같은 방식) | 0 |
| 테스트 | 대역 리스너로 갱신 확인 (TP-U-03) | 시계를 돌려야 확인 |

**A를 채택한다.** 배치·개입 단계는 시계가 멈춘 채 진행되므로 B는 맞지 않는다. 기존 `com.holub.tools.Publisher`는 `Distributor`로 한 번 더 감싸야 하고 형변환이 필요해서, `GameController`처럼 리스너 목록을 쓴다.

## 결정 3. 2인용 진입점과 렌더러

### 진입점 (`twoplayer`)

- `TwoPlayerSession`(Swing 없음, Facade): `NeighborhoodTwoPlayerBoard` + `TwoPlayerGame`을 만들고, UI 입력을 "현재 플레이어"로 바꿔 넘긴다(`click(row, column)` → `select(currentPlayer(), …)`). 상태 문구(`status()`)를 만든다. 리스너(결정 2).
  - 모든 공개 메서드를 `synchronized`로 묶는다. 클릭은 EDT, tick은 시계 스레드에서 오지만 `TwoPlayerGame`에는 한 번에 하나씩만 들어간다(`TwoPlayerGame`의 "한 스레드에서만" 계약을 직렬화로 지킨다).
  - 다른 후보: tick을 `SwingUtilities.invokeLater`로 EDT에 넘기기. 그러면 2인용 세대 진행이 `ModeSwitch`의 잠금 밖(EDT)에서 돌아 결정 1의 직렬화가 깨진다. 기각.
- `twoplayer.ui.TwoPlayerMode`(`GameMode` 구현), `TwoPlayerPanel`(보드 + 상태 표시줄 + 확정·패스·새 게임 버튼, Enter 키 = 확정), `TwoPlayerRenderer`. Swing 코드는 `twoplayer.ui`에만 두어 `twoplayer` 패키지의 "Swing 없음"을 유지한다.

### 렌더러 (#69)

| 항목 | A. `twoplayer` 안에 따로 (`owner` 기반 스냅숏) | A'. `twoplayer` 안에 따로 (`Neighborhood`의 `beginRedraw`·`childAt` 사용, #69 후보 A 원문) | B·C. `BoardRenderer` 일반화 (#69 후보 B·C) |
| --- | --- | --- | --- |
| ui 변경 | 0 | 0 | `BoardRenderer` |
| #74 | **잠금을 쓰지 않는다** | `beginRedraw`가 `static` 잠금을 읽는다 | 잠금을 읽는다 |
| 부분 갱신 | 없음, 매번 64×64 전체 | 활성 블록만 | 활성 블록만 |
| 필요한 API | `NeighborhoodTwoPlayerBoard.owner`(공개, #5) | 루트 `Neighborhood` 노출 필요 (지금 `private`) | 셀 쪽 공개 메서드 |

**A를 채택한다.** 세션이 칸 소유자를 잠금 안에서 64×64 배열로 복사해 주고(`cells()`), 렌더러는 그 배열만 그린다. `Neighborhood`의 잠금을 읽지 않으므로 클래식 보드가 어떤 상태든 2인용 화면은 영향받지 않는다. 부분 갱신이 없어 매번 4,096칸을 칠하지만, 클래식 `paint`도 `drawAll = true`로 전체를 칠한다(`BoardPanel.java:73`).

- `owner()`는 `TwoPlayerBoard` 인터페이스에 올리지 않는다(006 후속 작업). 렌더러는 세션의 `cells()`만 읽고, 세션은 실제 보드 클래스를 안다. `StubBoard`는 바뀌지 않는다.
- 색: A = 파랑, B = 빨강 계열, 빈 칸·격자선은 클래식과 같은 `Colors.LIGHT_YELLOW`·`DARK_YELLOW`. 31·32열 사이에 진영 경계선(검정 2px). 임시 배치 칸은 현재 플레이어 색 테두리.

## 그 밖의 결정

- **모드를 바꾸면 시계를 멈춘다.** 클래식에서 Fast로 돌다가 2인용으로 바꿨을 때 2인용 시뮬레이션이 저절로 돌거나, 돌아왔을 때 클래식이 예전 속도로 다시 도는 일을 막는다. 2인용 게임 상태는 모드를 바꿔도 유지한다(새 게임은 버튼).
- **2인용 모드에서 Grid의 Clear·Load·Store를 끈다.** Store는 rules.md 8장. Load·Clear는 보이지 않는 클래식 보드를 바꾸므로 함께 끈다. Exit는 그대로. 2인용 보드 비우기는 "새 게임" 버튼이 한다. 클래식으로 돌아오면 다시 켠다.
- **Go 메뉴의 Tick·속도는 2인용에서도 같은 시계를 움직인다.** 시뮬레이션 단계에서만 세대가 진행되고 K에 들어간다(`TwoPlayerGame.tick()`). 다른 단계의 tick은 아무것도 하지 않고 문구도 바꾸지 않는다(빠른 속도로 돌 때 문구가 깜빡이지 않도록).
- **메뉴가 열려 있으면 tick을 건너뛴다**(기존 `GameMenu.onTick`, `GameMenu.java:48-50`). `ModeSwitch`가 모든 모드에 같은 검사를 한다.
- `GameMenu.install`(Grid·Go 메뉴 + 클래식 tick 리스너)은 그대로 두고, 메뉴만 다는 `installMenus`를 나눈다. `LifeFrame`은 `installMenus` + `ModeSwitch`를 쓴다. 기존 `GameMenuTest`는 `install`·`go`·`onTick`을 그대로 쓴다.

## 전/후 비교

```text
전: Life → LifeFrame(BoardPanel) ; GameMenu.install: Grid·Go 메뉴 + clock → GameController.tick
후: Life → LifeFrame(ModeSwitch.view = CardLayout[ClassicMode.view = BoardPanel, TwoPlayerMode.view = TwoPlayerPanel])
          GameMenu.installMenus: Grid·Go 메뉴 ; Mode 메뉴: Classic / 2 Players
          clock → ModeSwitch.tick ─(잠금)→ 지금 모드.tick()
                                        ├ ClassicMode → GameController.tick (전과 같음)
                                        └ TwoPlayerMode → TwoPlayerSession.tick → TwoPlayerGame.tick
```

클래스 다이어그램: 전 [twoplayer-ui-before](diagrams/twoplayer-ui-before.puml) ([png](diagrams/twoplayer-ui-before.png)) / 후 [twoplayer-ui-class](diagrams/twoplayer-ui-class.puml) ([png](diagrams/twoplayer-ui-class.png))

## 구현하면서 바뀐 것

- **상태 표시줄에 임시 배치 칸을 넣었다**(`Selected (r,c)`). TP-U-03(클릭하면 문구가 바뀐다)이 실패해서 알았다. 임시 배치만 하면 문구가 그대로라 상태 표시줄만 보고는 클릭이 받아들여졌는지 알 수 없었다.
- **카드 크기를 지금 모드 화면에 맞추고, 전환할 때 창을 `pack()`한다.** `CardLayout`은 가장 큰 카드에 맞추므로 클래식 화면이 2인용 화면(상태 표시줄 포함) 높이로 늘어났다. 기존 `NativeUiSmoke`의 "panel dimensions"(512×512) 검사가 실패해서 알았다. 2인용으로 바꾸면 창이 상태 표시줄 높이만큼 커지고, 돌아오면 원래 크기(클래식 512×512)가 된다.
- **상태 표시줄을 한 줄 + 버튼 줄로 나누고 너비를 보드에 맞췄다.** 한 줄에 버튼까지 두니 창이 599px로 넓어지고 문구가 잘렸다(실제 창 실행으로 확인). 문구도 줄였다.
- `NativeUiSmoke`(수동 실행, `@Test` 없음)가 `BoardPanel`을 "내용 패널의 0번째 컴포넌트"로 찾았는데 이제 카드 안에 있어서, 타입으로 찾도록 바꿨다. 검사 항목은 그대로이고 통과한다.
- TP-U-09·10·11·16은 셀 1개씩 두면 1세대 뒤 전멸로 게임이 끝나서, 테스트를 구현 전에 블록(2×2)으로 고쳤다.

## 실제 창 확인 (2026-11-16)

scratch 드라이버로 `Life.main`을 띄우고(headless 아님) 확인했다. 드라이버는 저장소에 넣지 않았다.

- Classic: 보드 512×512. Mode → 2 Players: 2인용 화면이 보이고 클래식 화면은 숨음. Grid의 Clear·Load·Store 꺼짐, Exit 켜짐.
- A·B가 20개씩 배치 → Go → Fast: 상태 표시줄이 `Simulation 5/20` → `10/20` → `16/20` → `Round 1 Intervention | Turn: B`로 바뀌고, 개입 단계에서는 시계가 돌아도 그대로.
- Mode → Classic: 보드 512×512, Store 다시 켜짐. 기존 `NativeUiSmoke`(마우스 토글, Tick, Store, Clear, Load, 취소·읽기 실패) 통과.

## 후속 작업

- ~~TP-U 테스트 케이스를 이 문서 기준으로 다시 정의한다.~~ 완료([test-cases.md](test-cases.md) 5장, 17개).
- ~~구현 후 오버헤드 표 D 열을 실측으로 바꾼다.~~ 완료.
- 2인용 저장·불러오기, 배치 가능 칸 강조는 범위 밖(rules.md 8장).
- Grid 메뉴 항목을 켜고 끄는 `GameMenu.setFileCommandsEnabled`는 `MenuSite`가 `JFrame`을 요구해 headless 테스트가 없다. 실제 창 확인만 했다.
- #74: 재현 결과를 댓글로 남겼다(2026-11-16). `Neighborhood` 쪽 수정은 리팩토링 팀. 고쳐지면 `ModeSwitch`의 잠금은 "tick 하나씩"이라는 의미만 남는다.
- #69: ui 담당이 B·C로 렌더러를 일반화하면 `TwoPlayerRenderer`를 그쪽으로 옮길 수 있다.
