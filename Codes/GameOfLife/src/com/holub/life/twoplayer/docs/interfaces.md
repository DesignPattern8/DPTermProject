# 2인용 모드 인터페이스

- 근거: [003-game-phase.md](003-game-phase.md), [004-player-input.md](004-player-input.md), [006-two-player-cell.md](006-two-player-cell.md), [007-two-player-ui.md](007-two-player-ui.md)
- 2인용이 쓰는 ui 쪽 계약(`GameMode`, `ModeSwitch`)은 [../../ui/docs/interfaces.md](../../ui/docs/interfaces.md)
- 공통 `Cell` 인터페이스 변경 제안은 core 쪽 문서에 있다: [../../docs/interfaces.md](../../docs/interfaces.md)

---

# TwoPlayerBoard

- 제공 도메인: 2인용 모드(구현: `NeighborhoodTwoPlayerBoard`, #5) / 사용 도메인: 2인용 게임 진행(`TwoPlayerGame`) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-03, 최초 작성 (#6). 셀 상태 회의(#5) 전이라 게임 진행이 보드에 묻는 것만 모았다.
  - 2026-11-03, `count(Player)` 추가 (#6 2차, 승패 판정)
  - 2026-11-15, 계약 위반 시 동작(예외)을 정함 (#5, [test-cases.md](test-cases.md) 1-2장 TP-B-13~17). 실제 구현 `NeighborhoodTwoPlayerBoard` 추가

## 시그니처

```java
package com.holub.life.twoplayer;

public interface TwoPlayerBoard {
  int rows();
  int columns();
  boolean isEmpty(int row, int column);
  boolean hasEmptyCell();
  void place(int row, int column, Player owner);
  void advanceGeneration();
  int count(Player owner);
}
```

## 계약

- `rows()`, `columns()`: 1 이상, 게임 중 바뀌지 않는다.
- `isEmpty(row, column)`: 그 칸이 `DEAD`이면 true. 사전조건: `0 <= row < rows()`, `0 <= column < columns()` (범위 검사는 호출하는 쪽이 먼저 한다). 어기면 `IndexOutOfBoundsException`.
- `hasEmptyCell()`: `isEmpty`가 true인 칸이 하나라도 있으면 true.
- `place(row, column, owner)`: 그 칸을 `owner`의 산 셀로 만든다. 사전조건: 범위 안, `owner`가 null이 아님, `isEmpty(row, column)`이 true. 어기면 차례로 `IndexOutOfBoundsException`, `NullPointerException`, `IllegalArgumentException`이고, 예외를 던지면 보드는 바뀌지 않는다. 보드는 진영을 검사하지 않는다(진영 제한은 게임 진행의 일). 게임 진행은 확정할 때만 부른다(임시 배치는 보드에 놓지 않는다).
- `advanceGeneration()`: rules.md 3장 규칙으로 1세대 진행한다.
- 스레드: 게임 진행과 같은 스레드에서만 부른다. UI에서는 `TwoPlayerSession`의 잠금 안에서만 불린다(#7).
- `count(owner)`: 지금 살아 있는 `owner` 셀 수. 0 이상. `owner`가 null이면 `NullPointerException`(`DEAD` 칸 수를 돌려주지 않는다). 게임 진행은 시뮬레이션 중 매 세대 직후(판정)와 상태 조회(`score`)에서 부른다. `NeighborhoodTwoPlayerBoard`는 전체 순회(4,096칸)로 센다([006](006-two-player-cell.md) 오버헤드 표).

- 게임 진행(`TwoPlayerGame`)은 범위·빈 칸을 먼저 확인하므로(거부 이유 `OUT_OF_BOARD`, `CELL_NOT_EMPTY`) 정상 진행에서는 위 예외가 나지 않는다.

---

# NeighborhoodTwoPlayerBoard

- 제공 도메인: 2인용 모드 / 사용 도메인: 2인용 게임 진행(`TwoPlayerGame`에 넣음), 2인용 UI(#7, 조회) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-15, 최초 작성 (#5, [006](006-two-player-cell.md) 후보 F)
  - 2026-11-23, 스레드 계약: `Neighborhood`의 읽기 허가·전이 깊이가 보드별로 바뀜 (#74, A 모델). 공개 시그니처 변경 없음
  - 2026-11-24, `TwoPlayerResident`가 `Cell`의 암 메서드 3개를 더는 구현하지 않음 (#70), `transfer` 좌표가 `CellPosition` (#71). 공개 시그니처 변경 없음

## 시그니처

```java
package com.holub.life.twoplayer;

public final class NeighborhoodTwoPlayerBoard implements TwoPlayerBoard {
  public NeighborhoodTwoPlayerBoard();                 // 빈 64×64 보드
  // TwoPlayerBoard 메서드 7개
  public Optional<Player> owner(int row, int column);  // 이 클래스에만 있음
}
```

## 계약

- 구성: `Neighborhood(8, Neighborhood(8, TwoPlayerResident))`. `rows()` = `columns()` = 64. `CompositeBoard`·`Board`는 쓰지 않는다(006 W3).
- `TwoPlayerBoard` 계약을 모두 지킨다(위 절).
- `owner(row, column)`: 그 칸의 소유자. `DEAD`면 `Optional.empty()`. 범위 밖이면 `IndexOutOfBoundsException`. 테스트와 `TwoPlayerSession.snapshot()`이 쓴다. **`TwoPlayerBoard` 인터페이스에는 넣지 않았다**(#7, 007 결정 3: 렌더러는 세션 스냅숏만 읽고, 세션은 실제 보드 클래스를 안다. `StubBoard`는 바뀌지 않음).
- 스레드: 같은 보드의 호출은 한 번에 하나씩만 진행한다. #74에서 `Neighborhood`의 읽기 허가·전이 깊이를 보드별로 분리해 클래식 보드와 공유하지 않는다. 앱에서 두 모드의 tick을 직렬화하는 `ModeSwitch` 정책은 유지한다. [A 모델 계약](../../model/docs/interfaces.md)

## package-private: `TwoPlayerResident`

`NeighborhoodTwoPlayerBoard`만 쓰는 2인용 셀(`Cell` 구현)이다. 다른 도메인은 쓰지 않는다.

```java
final class TwoPlayerResident implements Cell {
  Player owner();                   // 지금 소유자. DEAD면 null
  void prepareOwner(Player owner);  // 다음 toggle이 DEAD 칸을 살릴 때 쓸 소유자
  // Cell 메서드 11개 (#70 이후. 암 메서드는 CancerCell로 분리되어 구현하지 않는다)
}
```

- **"소유자 지정 → toggle" 규칙**: 칸을 살릴 때는 `prepareOwner(owner)`를 부른 뒤 반드시 루트 `Neighborhood.toggle(row, column)`으로 살린다. 셀의 상태만 바꾸면 그 블록이 활성으로 표시되지 않아 다음 세대에 계산되지 않는다(`model/Neighborhood.java:131-139` 활성 검사, `305-312` `toggle`이 `amActive`와 가장자리 표시를 켬). `NeighborhoodTwoPlayerBoard.place`가 이 순서를 지킨다.
- `toggle`: 살아 있으면 `DEAD`로, `DEAD`면 정해 둔 소유자의 산 셀로 바꾸고, 정해 둔 소유자는 지운다. 정해 둔 소유자 없이 `DEAD` 칸을 toggle하면 `DEAD` 그대로다. 살아 있는 칸을 toggle하면 죽이므로, `place`는 살아 있는 칸을 먼저 거부한다.
- 전이: rules.md 3장. 이웃은 같은 보드의 `TwoPlayerResident` 또는 `Cell.DUMMY`(`DEAD`)다.
- 저장 메서드: `transfer(Memento, CellPosition, boolean)`은 false(아무것도 안 함), `createMemento`는 `UnsupportedOperationException`(2인용 저장 미지원, rules.md 8장). #71 이후 좌표가 `CellPosition`이라 `java.awt`에 의존하지 않는다. 테스트: `MementoCoordinateTest` COORD-05(A 모델 정의).
- 암 메서드: 구현하지 않는다. #70에서 `isCancer`·`setCancer`·`cureCancer`가 `Cell`에서 `CancerCell`로 분리되었고 `TwoPlayerResident`는 `CancerCell`이 아니다. 테스트: `TwoPlayerRuleTest` CAN-M14(A 모델 정의).

---

# TwoPlayerGame

- 제공 도메인: 2인용 모드 / 사용 도메인: 2인용 UI(#7) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-03, 최초 작성 (#6 1차: 단계·차례·배치·패스·tick)
  - 2026-11-03, `score(Player)`, `result()`, `GameResult` 추가 (#6 2차: 승패 판정)

## 시그니처

```java
package com.holub.life.twoplayer;

public final class TwoPlayerGame {
  public TwoPlayerGame(TwoPlayerBoard board, GameSettings settings);

  // 입력 (004: 직접 호출 + 결과 값)
  public Response select(Player who, int row, int column);   // 임시 배치·이동
  public Response confirm(Player who);                         // 임시 배치 확정
  public Response pass(Player who);                            // 개입 단계만
  public boolean tick();                                       // 시뮬레이션 1세대, 아니면 무시하고 false

  // 상태 조회
  public PhaseKind phase();
  public Player currentPlayer();          // 시뮬레이션·종료 단계에서는 null
  public int remaining(Player player);    // 이번 배치 단계에서 남은 배치 개수
  public int round();                     // 초기 배치 중 0, 라운드 r의 시뮬레이션·개입 중 r
  public int generationInRound();         // 이번 라운드 시뮬레이션에서 진행한 세대 수
  public Optional<Coordinate> tentative(); // 현재 임시 배치 칸
  public int score(Player player);           // 지금 살아 있는 자기 셀 수 (= board.count)
  public Optional<GameResult> result();      // 종료 단계에서만 값이 있음
}

public enum PhaseKind { PLACEMENT, SIMULATION, INTERVENTION, GAME_OVER }
public enum Player { A, B; public Player opponent(); }
public enum Response { ACCEPTED, WRONG_PHASE, NOT_YOUR_TURN, OUT_OF_BOARD, CELL_NOT_EMPTY,
                       OPPONENT_HALF, NO_TENTATIVE_PLACEMENT, PASS_NOT_ALLOWED }
public final class GameSettings { /* N, K, M, R. 기본값 20, 20, 3, 5 */ }
public record Coordinate(int row, int column) {}
public record GameResult(Outcome outcome, EndReason reason, int scoreA, int scoreB) {}
public enum Outcome { A_WINS, B_WINS, DRAW }
public enum EndReason { WIPE_OUT, ROUNDS_COMPLETED }
```

## 계약

- 입력 메서드는 거부해도 예외를 던지지 않고 거부 이유를 돌려준다. 거부된 입력은 게임 상태를 바꾸지 않는다.
- 거부 이유 판단 순서: 단계 → 차례 → 보드 범위 → 진영(초기 배치만) → 빈 칸. 남은 개수가 0인 플레이어는 자동 패스로 차례가 오지 않으므로 별도 이유가 없다(`NOT_YOUR_TURN`).
- `tick()`은 시뮬레이션 단계에서만 보드를 1세대 진행하고 true를 돌려준다. 다른 단계에서는 아무것도 하지 않고 false.
- 선공·후공: 초기 배치는 A가 먼저 둔다. 이후 배치 단계(라운드 1 개입, 라운드 2 개입, …)마다 선공이 바뀐다(라운드 1 개입 선공 = B).
- 자동 패스: 개입 단계에서 차례가 온 플레이어의 남은 개수가 0이거나 `board.hasEmptyCell()`이 false면 그 차례는 자동으로 패스된다. 연속 패스 수에 들어간다.
- `GameSettings`: N·K·R은 1 이상, M은 0 이상. 어기면 생성자가 `IllegalArgumentException`.
- 스레드: 한 스레드에서만 부른다(또는 호출을 직렬화한다). 동기화하지 않는다. UI에서는 `TwoPlayerSession`이 모든 호출을 `synchronized`로 직렬화한다(#7).
- 판정(rules.md 6장, [005](005-cell-count-and-judge.md)): 시뮬레이션 중 매 세대 직후 한쪽이라도 0이면 즉시 종료(`WIPE_OUT`), 둘 다 0이면 무승부. R번째 라운드의 K번째 세대 뒤 전멸이 아니면 셀 수 비교(`ROUNDS_COMPLETED`), 같으면 무승부. 마지막 라운드가 아니어도 K번째 세대에 전멸하면 개입 없이 종료한다. 배치·개입 단계에서는 판정하지 않는다.
- `result()`는 종료 단계에서만 값이 있고, 이후 바뀌지 않는다.

---

# TwoPlayerSession

- 제공 도메인: 2인용 모드 / 사용 도메인: 2인용 UI(`twoplayer.ui`) / 담당자: Son2kwon
- 변경 이력: 2026-11-16, 최초 작성 (#7, [007](007-two-player-ui.md) 결정 2·3)

## 시그니처

```java
package com.holub.life.twoplayer;

public final class TwoPlayerSession {
  public record Snapshot(Player[][] cells, Optional<Coordinate> tentative, Player currentPlayer) {}

  public TwoPlayerSession(GameSettings settings);   // 빈 NeighborhoodTwoPlayerBoard + TwoPlayerGame
  public void addListener(Runnable listener);

  public Response click(int row, int column);  // 현재 플레이어의 select
  public Response confirm();                   // 현재 플레이어의 confirm
  public Response pass();                      // 현재 플레이어의 pass
  public boolean tick();                       // TwoPlayerGame.tick
  public void newGame();                       // 빈 보드로 새 게임

  public String status();                      // 상태 표시줄 한 줄
  public Snapshot snapshot();
  public PhaseKind phase();
  public int generationInRound();
  public int rows();
  public int columns();
}
```

## 계약

- 입력은 "지금 차례인 플레이어"의 입력으로 넘긴다. 차례가 없는 단계(시뮬레이션·종료)에서는 `WRONG_PHASE`를 돌려주고 게임을 부르지 않는다. 거부 규칙과 순서는 `TwoPlayerGame`과 같다.
- 거부된 입력의 이유는 다음에 받아들인 입력(또는 진행한 tick, 새 게임)까지 `status()` 앞에 `Rejected: <이유> | `로 붙는다.
- `status()`의 조각: 단계(`Placement`, `Round r Simulation g/K`, `Round r Intervention`, `Game over: … (wipe out | rounds completed)`), 차례(`Turn: A`), 남은 개수(`Left A:x B:y`), 임시 배치(`Selected (r,c)`), 셀 수(`A:B = a:b`). 영어 한 줄.
- 리스너: 입력 메서드(거부 포함)와 `newGame()` 뒤 1번, `tick()`은 세대를 진행했을 때만 1번. 잠금 밖에서, 그 메서드를 부른 스레드에서 부른다. 화면 갱신 스레드로 넘기는 것은 리스너의 일이다.
- `snapshot()`: 잠금 안에서 64×64 소유자를 복사한다. 돌려받은 배열을 고쳐도 게임은 바뀌지 않는다.
- 스레드: 모든 공개 메서드가 `synchronized`라 클릭(EDT)과 tick(시계 스레드)이 섞여도 `TwoPlayerGame`에는 하나씩 들어간다. #74에서 모델의 전이 상태는 보드별로 분리됐다. 다른 모드와 동시에 세대를 진행하지 않는 앱 정책은 계속 `ModeSwitch`가 담당한다.

---

# TwoPlayerMode

- 제공 도메인: 2인용 모드(`twoplayer.ui`) / 사용 도메인: 조립(`Life`), ui(`LifeFrame`·`ModeSwitch`, `GameMode`로만) / 담당자: Son2kwon
- 변경 이력:
  - 2026-11-16, 최초 작성 (#7)
  - 2026-11-25, Mode 메뉴가 Classic / Single Player / 2 Players로 바뀜 (#109, [ui 012](../../ui/docs/012-classic-mode-restore.md)). `TwoPlayerMode` 시그니처 변경 없음

## 시그니처

```java
package com.holub.life.twoplayer.ui;

public final class TwoPlayerMode implements com.holub.life.ui.GameMode {
  public TwoPlayerMode();                                  // GameSettings.defaults(), 화면 갱신은 EDT
  TwoPlayerMode(GameSettings settings, Executor ui);       // 테스트용 (package-private)
  public String menuName();                                // "2 Players"
  public JComponent view();                                // TwoPlayerPanel
  public void tick();                                      // TwoPlayerSession.tick()
  public boolean supportsFileCommands();                   // false (rules.md 8장)
}
```

## 계약

- `GameMode` 계약을 따른다([ui interfaces.md](../../ui/docs/interfaces.md)). `tick()`은 시뮬레이션 단계에서만 1세대 진행하고(K에 들어감), 다른 단계에서는 아무것도 하지 않는다.
- 모드를 바꿔도 게임 상태는 유지된다. 처음부터 하려면 화면의 "New Game" 버튼.
- `supportsFileCommands()` = false이므로 2인용에서는 Grid의 Clear·Load·Store가 꺼진다. Preset 메뉴는 Single Player에서만 켜지므로 2인용에서도 꺼져 있다([ui 012](../../ui/docs/012-classic-mode-restore.md)). `GameMode`에 메서드를 더하지 않았다.
- 화면: 클릭 = 현재 플레이어 임시 배치·이동, Enter 또는 "Confirm" = 확정, "Pass" = 패스(개입 단계만), "New Game". 격자 밖 클릭은 `OUT_OF_BOARD`로 거부되어 이유가 표시된다.
