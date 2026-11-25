# 006 모드별 실행 제어 — 현행 유지와 계약 문서화

- 상태: 채택 (A. 현행 유지 + 계약 문서화). B는 후속 작업. G는 11/24에 불필요로 판단했다가 11/25 결정이 바뀌어 #109·PR #110(2인용 담당)으로 구현됨 (아래 "M5 확인")
- 작성자: 이시형 / 2026-11-24
- 관련: 이슈 #87, #74(닫힘, `c2d09a9` "Merge pull request #90 … fix/74-board-transition"), 2인용 [007 결정 1](../../twoplayer/docs/007-two-player-ui.md#결정-1-모드-전환-구조), [003 D3](003-pattern-analysis.md#d3-실행-상태와-속도를-어떻게-표현할-것인가-p4), #62·PR #100(`supportsFileCommands` 재사용)
- 기준 코드: `origin/refactor/base` `ff54fe0`. 아래 줄 번호는 모두 이 커밋에서 확인했다. (`main`의 #96 암세포 규칙 변경은 아직 `refactor/base`에 없다)

## 문제

v1.0 이후 모드가 클래식·암세포·2인용 셋이 되면서 "어느 모드를 언제 진행하는가"가 여러 패키지에 나뉘었다. tick 경로는 다음과 같다.

```
Timer 스레드 → UtilTimerTickSource(:26) → Clock.tick(:60-62)
  → ModeSwitch.tick(:103-108)   메뉴 열림 검사(:104), tickLock(:105)
    → ClassicMode.tick(:28-30) → GameController.tick(:86-111)  lock(:89 / :98)  → Board.step
    → TwoPlayerMode.tick(:40-42) → TwoPlayerSession.tick(:76-84)  this 모니터(:78)
```

| ID | 문제 | 근거 (`ff54fe0`) |
| --- | --- | --- |
| M1 | **실행 정책이 ui에 있다.** "현재 모드에만 tick"(`ModeSwitch.java:103-108`), "전환 전 시계 정지 + 진행 중 tick 대기"(`:54-60`), "한 번에 한 모드만 진행"(`tickLock`, `:26`)이 모두 `ui/ModeSwitch`의 책임이다. 이 보장은 테스트되어 있지만 B가 아니라 2인용 UI 테스트(`TwoPlayerUiTest` TP-U-11·13·14, `JPanel`을 쓰는 headless 테스트)에 있다. B 테스트(`test/.../application/`)에는 0개 | `ModeSwitch.java:26,40,54-66,103-108` |
| M2 | **tick 연결 경로가 코드상 2개 남아 있다.** v2.0 분석 때와 달리 지금 `LifeFrame`은 모드 목록이 비어도 항상 `ModeSwitch`를 만든다(`LifeFrame.java:28-32`). 그래서 실행 경로는 하나다. 다만 시계에 직접 리스너를 다는 `GameMenu.install`(`GameMenu.java:56-59`)이 공개 메서드로 남아 있고, `src`·`test`에 호출처가 없다(grep 확인). 누가 다시 부르면 `ModeSwitch`와 함께 클래식 보드가 tick을 두 번 받는다 | `GameMenu.java:56-59`, `LifeFrame.java:28-32` |
| M3 | **잠금 3종, 순서는 문서에 없다.** `ModeSwitch.tickLock`(`ModeSwitch.java:26`) → `GameController.lock`(`GameController.java:34`, 사용 8곳) 또는 `TwoPlayerSession` 모니터(`synchronized` 12곳). 편집(EDT 클릭)은 안쪽 잠금만 잡는다. 지금 역순 경로가 없어 교착은 없다(아래 "계약" 절에서 경로별로 확인). 하지만 이 순서는 어느 `interfaces.md`에도 적혀 있지 않다 | `ModeSwitch.java:26,58,105`, `GameController.java:34,89,98,179`, `TwoPlayerSession.java:39,51,63,78,88` |
| M4 | **암세포가 null 분기로 붙어 있다.** `tick`은 두 구현이 한 메서드에 있다(클래식 `:87-94`, 암 `:96-110`). `treat`(`:119`), `gameResultMessage`(`:172`)도 `cancerGame == null` 분기. 모드가 늘면 분기도 는다 | `GameController.java:33,87,119,172` |
| M5 | **"Classic" 모드가 항상 암세포 규칙을 켠다.** `Life`가 언제나 `CancerGame`을 넣어 조립한다(`Life.java:25,30`). `CancerGame.afterGeneration`은 매 세대 자연 발생·전이를 시도한다(`CancerGame.java:45-46`). 메뉴 이름은 "Classic"(`ClassicMode.java:17-19`)이지만 원본 Life(암 없음)를 고를 수 없다. 특성 테스트는 `GameController(Board, Clock)`(`cancerGame` 없음)으로 만들어 통과하므로 드러나지 않는다. **→ 11/24 의도된 동작으로 닫았다가, 11/25 교수님 확인("기본 클래식 모드는 있어야 한다")으로 결정 변경: 원본 Classic 복원 + Single Player(암세포) 분리, #109·PR #110 (아래 "M5 확인")** | `Life.java:25-31`, `CancerGame.java:39-61`, `ClassicMode.java:17-19` |
| M6 | **시계 정지 호출이 흩어져 있다.** `GameController`(암 게임 종료 `:107`, load `:179`, store `:189`), `ModeSwitch`(`:57`), `GameMenu`(Halt `:44`, 모르는 이름 `:88`). "실행 중인가"를 물을 API는 여전히 없다(`Clock`에 상태 조회 메서드 없음, 003 P4) | 위 줄, `Clock.java:19-37` |
| M7 | **모드 능력 플래그.** `GameMode`의 능력 플래그는 지금 `supportsFileCommands()` 하나(`GameMode.java:22-23`). #62의 D 화면(PR #100, 열림)은 `supportsPresets()`를 추가하지 않고 같은 신호로 Preset 메뉴도 켜고 끄기로 했다(PR #100의 `ui/docs/010-preset-placement-ui.md`). 인터페이스가 커지는 문제는 미뤄졌지만, 그 대신 플래그 이름("파일 명령")과 실제 의미(파일 명령 + 프리셋)가 어긋난다 | `GameMode.java:22-23`, `ModeSwitch.java:39,65` |
| M8 | **알림 종류에 모드 전용 값.** `GameListener.Change.GAME_OVER`는 암 전용(`GameListener.java:15-16`). 암 모드는 매 tick `EDITED`(전체 다시 그리기)도 보낸다(`GameController.java:104-105`) | `GameListener.java:10-17`, `GameController.java:103-109` |

### #74 해결(#90)로 달라진 것

#87을 만들 때(이슈 본문 문제 4) "한 번에 한 보드만 tick"은 #74의 **유일한 방어선**이었다. 2인용 007의 측정에서 두 보드를 동시에 진행하면 `Neighborhood`의 `static` 읽기 잠금·중첩 깊이가 깨져 1초 안에 클래식 화면이 멈췄다(S3, 6/6).

`c2d09a9`(#90, 11/17 `refactor/base` 머지)에서 A가 전이 상태를 보드(루트)별로 분리했다. 공개 생성자·`create()`로 만든 루트는 독립 상태를 갖고, 같은 프로토타입으로 만든 보드끼리도 공유하지 않는다([model interfaces.md](../../model/docs/interfaces.md) #74 항목, `NeighborhoodReadGateTest` GATE-01~06). #74는 11/24(KST) 닫혔다.

그래서 `ModeSwitch.tickLock`은 이제 **모델 안전장치가 아니라 앱 실행 정책**이다(`ModeSwitch.java:18-19` 주석도 그렇게 고쳐졌다). 이 보장이 깨져도 클래식 화면이 영구히 멈추는 일은 없고, 남는 영향은 "보이지 않는 모드의 보드가 진행된다"는 동작 차이다. M1·M3을 지금 구조 변경으로 풀어야 할 긴급도가 낮아졌다.

## 후보

### A. 패턴 없음 — 현행 유지 + 잠금 순서·tick 보장 계약 문서화 (채택)

코드는 그대로 둔다. 이 문서의 "계약" 절에 잠금 순서와 "현재 모드만, 한 번에 하나만 tick" 보장을 적고, 그 보장을 지키는 기존 테스트(TP-U-11·13·14)를 근거로 연결한다.

- 장점: 다른 도메인 변경 0. 동결일(11/24)에 위험 없이 끝난다.
- 단점: M1(정책이 ui에 있음)·M4·M7·M8은 그대로다(M5는 PR #110으로 해소). 보장 테스트가 B가 아닌 2인용 UI 테스트에 있다.

### B. Strategy를 application으로 — 실행기(`ModeRunner`, 가칭) + 규칙 인터페이스(`GameRules`, 가칭)

B에 시계 리스너 1개·현재 규칙 객체·잠금 1개·전환 API를 갖는 실행기를 둔다. `GameRules`(`tick()`, 필요 시 편집)는 application 타입이고 `ClassicRules`/암 규칙/2인용 세션이 구현한다. `ModeSwitch`에는 카드 교체와 메뉴 켜고 끄기만 남는다.

- 장점: M1·M2·M3 해소. 전환·tick 보장을 Swing 없이 `TickSource` 대역으로 B 테스트에서 검증(#87 To Do의 "대역 TickSource로 재현").
- 단점: 인터페이스 변경 — `GameMode.tick()`의 위치가 바뀌어 D(`ModeSwitch`, `ClassicMode`, `LifeFrame`)와 2인용(`TwoPlayerMode`)이 함께 고쳐야 한다. 2인용 007 결정 1(Strategy를 ui에)을 뒤집는 셈이라 2인용 담당 합의가 먼저다.

### C. State — 모드를 상태 객체로

B와 같은 구조에서 상태 객체가 다음 상태를 스스로 정한다.

- 모드는 스스로 다음 모드로 바뀌지 않고 사용자가 Mode 메뉴로 고른다(`LifeFrame.java:34-37`). 전이 주체가 밖에 있으므로 State가 아니라 Strategy다. 2인용 007 결정 1의 판단("State가 아니라 Strategy로 본 이유")과 같다.
- **기각.** 다만 "실행 상태"(정지/실행/게임 종료)는 State 후보로 따로 본다 → 아래 "003 D3 재판단".

### D. Mediator — 시계·모드·화면·메뉴 활성화를 한 조정자에

지금 `ModeSwitch`가 이미 이 모양이다(시계 `:40,57`, 카드 `:61-64`, 메뉴 `:65`). 이것을 application으로 옮기면 조정자가 Swing 화면(`CardLayout`, `Window.pack`)까지 알아야 해 B → UI 의존이 생긴다(003 D4에서 같은 이유로 Mediator 기각). ui에 두면 A와 같다. **기각.**

### E. 암세포 분리: Decorator — `CancerRules`가 클래식 규칙을 감싼다

`step()` 뒤에 `afterGeneration()`을 덧붙이는 래퍼. M4의 `tick` 분기를 없앤다.

- 단점: `treat`(`GameController.java:118-126`)는 감쌀 기존 연산이 없어 별도 경로가 필요하다. B(규칙 인터페이스)가 먼저 있어야 감쌀 대상이 생긴다.

### F. 암세포 분리: Template Method — 공통 tick 골격 + 모드별 훅

골격(종료 검사 → step → 후처리 → 알림) + 훅. 2인용 진행은 `TwoPlayerGame`의 단계 객체(`PlacingPhase`·`SimulationPhase`·`GameOverPhase`의 `tick()`)라 골격이 달라 공통화 이득이 클래식·암 둘뿐이다. 상속으로 묶이므로 **기각**.

### G. 암세포를 독립 모드로 — Mode 메뉴: Classic / Cancer / 2 Players

`Life`가 클래식 `GameController`(암 없음)와 암 `GameController`(또는 B의 암 규칙)를 따로 조립하고 Mode 메뉴에 두 항목을 둔다.

- 장점: **M5 해소**(원본 Classic 복원). 모드 하나에 규칙 하나.
- 단점: `Life`(조립)·D 메뉴·암세포 담당 합의 필요. M5 답변이 "의도된 것"이면 필요 없다.
- ~~상태: 불필요. M5가 의도된 동작이라 원본 Classic을 따로 복원할 이유가 없다.~~ (11/24 판단, 아래에서 뒤집힘)
- **상태: 구현됨 (11/25, #109·PR #110, 2인용 담당).** 교수님 확인으로 "기본 클래식 모드가 있어야 한다"가 정해져, 모드를 Classic / Single Player / 2 Players로 나눴다. 구현은 이 문서의 G 그대로(모드마다 보드·`GameController`를 따로 조립)이고, 결정 문서는 `ui/docs/012-classic-mode-restore.md`다.

## 오버헤드 비교표

A는 `ff54fe0`에서 센 실측값이다(코드를 바꾸지 않으므로 "변경 후 = 지금"). B~G는 구현하지 않아 **추정**이다.

| 항목 | A. 현행 유지 + 문서 (채택) | B. Strategy 실행기 (추정) | C. State (추정) | D. Mediator (추정) | E. Decorator (추정) | F. Template Method (추정) | G. 암 독립 모드 (추정 → PR #110에서 구현) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 구조 비용 | 코드 0줄, 문서 1개(이 파일). 관련 코드 크기: `ModeSwitch` 109줄, `GameMode` 24줄, `ClassicMode` 36줄, `TwoPlayerMode` 57줄, `GameController` 199줄 (`wc -l`) | 타입 +2~3(실행기, `GameRules`, `ClassicRules`). 수정 6~8파일(`ModeSwitch`, `GameMode`, `ClassicMode`, `TwoPlayerMode`, `LifeFrame`, `Life`, `GameController`, 테스트) | B + 상태 전이 코드 | `ModeSwitch` 이동 + B → Swing 의존 | 타입 +1~2, `GameController`·`Life` 수정 | 추상 클래스 +1, 규칙 클래스 2 | `Life` + Mode 메뉴 1항목 + `GameMode` 구현 1 |
| 간접성 (클래식 tick 1번) | `Clock.tick` → `ModeSwitch.tick` → `ClassicMode.tick` → `GameController.tick` → `Board.step`: 호출 4단계, 잠금 2번(`tickLock`, `lock`) | 단계 수 같음 추정(`ModeSwitch.tick` 자리를 실행기가 대신) | B + 상태 위임 +1 | A와 같음 | +1 (래퍼) | 0 (훅 호출은 같은 객체) | A와 같음 |
| 결합도 | 실행 보장이 ui에 있음. `application` 중 `java.awt` import는 `PersistenceMapper` 1개(`Point`), Swing 0 | 실행 보장이 application으로. application의 Swing 0 유지 | B와 같음 | **application → Swing** | B 안에 머묾 | B 안에 머묾 | 조립만 바뀜 |
| 런타임 | 변화 없음 | 변화 없음 추정(잠금 수 같음) | 상태 객체 위임 1번 | 변화 없음 | 래퍼 호출 1번 | 변화 없음 | 변화 없음 |
| 테스트 용이성 | 보장 테스트 TP-U-11·13·14·16이 `twoplayer.ui.TwoPlayerUiTest`에 있음(17개 중 4개, `JPanel` 사용 headless). B 테스트에서 보장 검증 0개. 전체 421개 통과·3개 건너뜀(`.\gradlew.bat test`, 11/24) | 대역 `TickSource` + 대역 규칙으로 B 테스트에서 Swing 없이 검증 | B와 같음 | Swing 필요 | 래퍼 단위 테스트 | 훅 단위 테스트 | 모드별 조립 테스트 |
| 확장 비용 (다음 모드) | `GameMode` 구현 1 + `Life` 조립 1줄(2인용 007 실측). 단 암처럼 클래식 보드에 규칙을 더하는 모드는 `GameController` 분기 추가 | 규칙 클래스 1 + 등록 1줄 | 상태 클래스 + 전이 수정 | 조정자 수정 | 래퍼 1 | 하위 클래스 1 | 모드 1 |
| 다른 도메인 변경 | 0 | D 3파일 이상 + 2인용 1파일 이상, 007 결정 1 재합의 | B와 같음 | D | 0~1 | 0~1 | D·암세포·`Life` |

## 결정과 근거

**A를 채택한다.** B는 후속 작업으로 남긴다. G는 M5 답변으로 한때 불필요로 봤으나, 11/25 결정 변경으로 PR #110에서 구현됐다(B 코드 변경은 `hasCancerRules()` 1개).

1. **일정.** 오늘(11/24) 기능 동결이고 11/29 최종 제출이다. B는 D·2인용 담당의 코드까지 바뀌는 인터페이스 변경이라 AGENTS 1-2(이슈로 사전 공유 → 합의 → 구현)를 거치면 동결 전에 끝나지 않는다.
2. **#90으로 위험이 줄었다.** "한 보드만 tick"은 #74 방어선에서 앱 실행 정책으로 바뀌었다(위 절). 이 보장이 깨져도 화면이 영구히 멈추지 않는다. 그리고 지금 보장은 TP-U-13(진행 중 tick이 끝나야 전환)·TP-U-14(전환 시 시계 정지)·TP-U-11(다른 모드 보드는 진행 안 함)로 이미 고정되어 있다. 빠진 것은 "어디에 적혀 있는가"이고, 이것은 문서로 해결된다.
3. **인터페이스 변경 범위.** B는 `GameMode`(ui, D 소유)와 `TwoPlayerMode`(2인용 소유)를 바꾸고 2인용 007 결정 1을 뒤집는다. 제출 직전에 두 도메인의 통과한 테스트(TP-U 17개)를 다시 흔들 이유가 2번 이후로 약하다.
4. E·F는 B가 먼저 있어야 의미가 있고(감쌀 규칙 인터페이스가 없음), C·D는 위에서 기각했다.

### 계약 (이번 결정으로 고정하는 것)

**tick 보장** (구현: `ui/ModeSwitch`, 검증: `TwoPlayerUiTest`)

| 보장 | 코드 | 테스트 |
| --- | --- | --- |
| 시계 리스너는 `ModeSwitch` 하나뿐이고, 지금 모드의 `GameMode.tick()`만 부른다 | `ModeSwitch.java:40,103-108` | TP-U-11 |
| 메뉴가 열려 있으면 tick을 건너뛴다(원본 `GameMenu.onTick`과 같음) | `ModeSwitch.java:104` | TP-U-16 (`#openMenuSuppressesTwoPlayerTick`, 2인용 모드에서 확인) |
| 모든 모드의 tick과 전환은 `tickLock` 하나 안에서 돈다 → 두 모드의 tick이 동시에 돌지 않는다 | `ModeSwitch.java:58-60,105-107` | TP-U-13 |
| 전환은 시계를 먼저 멈춘다. 진행 중이던 tick이 끝난 뒤에 모드가 바뀐다 | `ModeSwitch.java:57-60` | TP-U-14, TP-U-13 |
| `GameMenu.install`(직접 리스너)은 `ModeSwitch`와 함께 쓰지 않는다 | `GameMenu.java:56-59` (호출처 0) | — (M2, 후속 작업에서 정리) |

**잠금 순서** — 바깥에서 안쪽으로만 잡는다.

```
ModeSwitch.tickLock  →  GameController.lock  또는  TwoPlayerSession 모니터
```

경로별 확인(`ff54fe0`):

| 경로 | 스레드 | 잡는 순서 | 비고 |
| --- | --- | --- | --- |
| 자동·단일 tick (클래식/암) | Timer / EDT(Go → Tick) | `tickLock` → `lock`(`GameController.java:89` 또는 `:98`) → 풀고 → (암 종료 시) `clock.stop()`(`:107`) | `TickSource.stop`은 `TimerTask.cancel`뿐(`UtilTimerTickSource.java:33-38`) |
| 자동·단일 tick (2인용) | Timer / EDT | `tickLock` → 세션 모니터(`TwoPlayerSession.java:78`) | |
| 모드 전환 `select` | EDT | `clock.stop()`(`ModeSwitch.java:57`) → `tickLock`(`:58`) | 겹쳐 잡지 않음 |
| 편집·Clear·프리셋·치료 | EDT | `lock`만 | |
| Load | EDT | `lock`(`GameController.java:177`) → 그 안에서 `clock.stop()`(`:179`) | `tickLock`을 잡지 않음 |
| Store | EDT | `clock.stop()`(`:189`) → `lock`(`:190`) | |
| 2인용 클릭·확정·패스·새 게임 | EDT | 세션 모니터만 | |

- 안쪽 잠금(`lock`, 세션 모니터)을 쥔 채 `tickLock`을 잡는 경로는 없다. 그래서 교착이 없다. **새 코드도 안쪽 잠금 안에서 `ModeSwitch.select`나 `Clock.tick`을 부르면 안 된다.**
- 리스너 알림(`GameListener`, 2인용 세션 리스너)은 안쪽 잠금 밖이지만 tick 경로에서는 **`tickLock` 안**에서 불린다(`GameController.java:103-109`가 `ModeSwitch.java:106` 안에서 실행). EDT가 `select`에서 `tickLock`을 기다릴 수 있으므로 리스너는 EDT를 동기로 기다리면 안 된다(`invokeAndWait` 금지). 지금 리스너는 모두 비동기다: `BoardPanel`(`repaint` / `refreshNow`의 `invokeLater`, `BoardPanel.java:61-62,79`), `GameResultAnnouncer`(`invokeLater` 주입, `LifeFrame.java:41-42`).

### 003 D3(State 보류) 재판단

003 D3은 "실행 상태 규칙이 하나뿐이라 State는 과하다. 새 기능이 실행 상태 규칙을 늘리면 다시 검토한다"고 했다. 모드 셋이 생긴 뒤 다시 본 결과:

- **모드 = Strategy.** 이미 `GameMode`(ui)로 되어 있다(2인용 007 결정 1). 모드는 사용자가 밖에서 고르므로 State가 아니다(위 후보 C).
- **실행 상태(정지/실행/게임 종료) = State는 여전히 보류.** 상태별로 다른 규칙을 세어 보면:
  - 정지/실행: `Clock`의 규칙은 여전히 "이미 실행 중이면 이전 예약 취소" 하나다(`UtilTimerTickSource.java:20`). `Clock`은 실행 여부를 기록하지도 않는다(`Clock.java:19-37`).
  - 게임 종료: 암 모드의 "종료 후 tick 무시"는 `CancerGame.isOver()` 검사 1줄(`GameController.java:99`)이다. 2인용의 단계별 tick 규칙은 이미 2인용 쪽 State(`PlacingPhase`·`SimulationPhase`·`GameOverPhase`)가 갖고 있어 B로 올라오지 않는다.
  - 즉 B가 소유한 실행 상태 규칙은 여전히 1~2개다. 상태 클래스 3개 이상(003 D3 표 C열 추정)을 넣을 만큼 늘지 않았다.
- 다시 볼 조건: "실행 중인가" 조회(M6), Undo(B5)의 "실행 중 Undo 금지", 게임 종료 후 편집 금지처럼 **상태에 따라 요청을 거부하는 규칙이 2개 이상 B에 생기면** State를 다시 비교한다.

### M5 확인 (최종: 원본 Classic 복원, 11/25)

"Classic 모드에서 암세포 규칙이 항상 켜진 것이 의도인가?"를 암세포 담당(curiousio)·팀에 물었다([#87 질문 댓글](https://github.com/DesignPattern8/DPTermProject/issues/87#issuecomment-7108837304)).

**답변 (curiousio, 11/24. 답변 정리: [#87 댓글](https://github.com/DesignPattern8/DPTermProject/issues/87#issuecomment-7123616316)): 의도된 동작이다.** 모드를 1인용/2인용으로 나눈 설계라, "Classic" 모드가 곧 1인용(암세포) 모드다.

처리:

- M5는 버그·기능 손실이 아니라 설계 의도로 보고 닫는다. 코드 변경 없음.
- 후보 G(암 독립 모드)는 불필요하다. 후속 작업 B에서도 G 단계는 넣지 않는다.
- 남는 것은 메뉴 이름뿐이다. "Classic"(`ClassicMode.java:17-19`)은 실제 동작(1인용 암세포 모드)과 맞지 않으므로 D(화면)에 이름 변경("1 Player" 등)을 제안한다. 이름 결정과 변경은 D 소유다.

**결정 변경 (11/25).** 과제 명세의 "기존 Life Game 프로젝트가 지원하던 기능은 그대로 동작해야 함"을 두고 팀이 교수님께 메일로 확인했고, "기본 클래식 모드는 있어야 한다"는 답을 받았다. 위 11/24 처리는 이 답으로 대체한다.

- #109·PR #110(2인용 담당, `main` 머지 `c194243`)에서 원본 Classic(B3/S23, 암세포 없음, 마우스 버튼 구분 없이 토글)을 복원하고 암세포 게임을 Single Player 모드로 분리했다. Mode 메뉴: Classic / Single Player / 2 Players. 실행 시 Classic으로 시작한다.
- 방식은 이 문서의 후보 G와 같다: `Life`가 모드마다 보드와 `GameController`를 따로 조립한다(Classic은 `cancerGame` 없이). Classic은 기존 특성 테스트가 지키는 코드 경로(`cancerGame == null`)를 그대로 탄다.
- 실행 제어 계약(아래 "계약" 절)은 그대로 성립한다. 시계는 하나를 공유하고 `ModeSwitch`가 현재 모드에만 tick을 넘긴다. 잠금은 `tickLock` → 각 모드 `GameController.lock` 순서다.
- B 코드 변경은 `GameController.hasCancerRules()` 1개(오른쪽 클릭을 치료로 볼지 D가 묻는 질의)다. M4의 `cancerGame == null` 분기가 1곳 늘었다.
- 판단이 바뀐 과정: 담당자 답변(의도된 설계)만으로 닫았던 것을, 과제 요구사항 해석을 출제자에게 확인한 뒤 다시 열었다. 요구사항 해석이 걸린 결정은 팀 내부 합의만으로 닫지 않는다.

## 전/후 비교

코드 변경이 없으므로 구조 그림은 2인용 007의 `ModeSwitch` 구조 그대로다. 달라진 것은 문서뿐이다.

| | 전 | 후 |
| --- | --- | --- |
| 잠금 순서 | `ModeSwitch`·`GameController`·`TwoPlayerSession` 주석에 흩어짐. 순서 규칙과 "리스너는 EDT를 기다리지 않는다"는 어디에도 없음 | 이 문서 "계약" 절에 경로 7개와 금지 규칙 2개 |
| "한 보드만 tick" | #87 본문에서 #74 방어선으로 기술 | #90 이후 앱 실행 정책으로 재분류. 코드·테스트(TP-U-11·13·14) 위치 명시 |
| 003 D3 | "새 기능이 늘리면 다시 판단" | 모드 = Strategy(유지), 실행 상태 State = 보류, 다시 볼 조건 명시 |
| M5 (Classic의 암세포) | 의도인지 불명 | 11/24 "의도됨"으로 닫음 → 11/25 교수님 확인 후 원본 Classic 복원 + Single Player 분리(PR #110) |

## 후속 작업

**B (구현 시 이 문서를 `대체됨(→ 번호)`으로 바꾼다)**

1. 사전 합의(이슈): `GameMode`를 "화면(ui)"과 "진행 규칙(application)"으로 나누는 안을 D(병국)·2인용(의권)에 공유. 2인용 007 결정 1과의 관계(Strategy는 유지, 위치만 application으로)를 명시.
2. B: `application`에 `GameRules`(가칭, `tick(): boolean`) + 실행기(가칭 `ModeRunner`: `Clock` 리스너 1개, 잠금 1개, `select(GameRules)` = 시계 정지 → 잠금 안에서 교체). `ModeSwitch.java:26,40,54-60,103-108`의 실행 부분을 옮긴다. 메뉴 열림 검사(`GameMenu.menuIsActive`, Swing)는 실행기에 주입하는 `BooleanSupplier`로 둔다(추정).
3. 테스트: 지금 TP-U-11·13·14를 B 테스트로 옮겨 Swing 없이 대역 `TickSource`·대역 규칙으로 검증. 추가로 "두 규칙의 tick이 동시에 돌지 않는다"(#87 To Do의 예외 케이스). TP-U는 화면 쪽 확인으로 남긴다.
4. ~~G: 암 규칙을 독립 모드로(Mode 메뉴 Classic / Cancer / 2 Players).~~ PR #110에서 모드 분리로 구현됐다(Classic / Single Player / 2 Players). 암 규칙을 `GameController`의 null 분기(`:87-110,119,172`)에서 꺼내는 것(M4)은 B의 규칙 인터페이스 안에서 1인용 규칙으로 정리한다(추정).
5. 같이 정리: `GameMenu.install`(`GameMenu.java:56-59`, 호출처 0) 제거 또는 `@Deprecated`(D 소유, M2). `supportsFileCommands` 이름·의미 정리(M7, PR #100 머지 후). `GAME_OVER`를 암 규칙 쪽 알림으로(M8).

**그 밖**

- ~~Mode 메뉴 이름 제안(D)~~: PR #110으로 Classic은 원본, 암세포 모드는 "Single Player"가 되어 해결됐다.
- 실행 상태 State: 위 "다시 볼 조건"이 생기면 003 D3을 다시 연다.
- #87은 이 문서로 "결정 문서" 단계까지 끝낸 것으로 보고, B 구현은 새 이슈로 나눈다(제출 이후).
