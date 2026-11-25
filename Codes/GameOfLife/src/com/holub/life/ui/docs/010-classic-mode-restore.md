# [010] 원본 클래식 모드 복원과 1인용 모드 분리

- 상태: 채택
- 작성자: Son2kwon / 2026-11-25 / 브랜치 `feature/classic-mode`
- 기준 코드: `main` `c321d15`
- 원본 기준: 저장소 최초 커밋 `cb32b00`의 `Universe.java`·`Resident.java`·`Life.java` (Holub 원본 소스)

## 문제

과제 조건(교수 확인): 결과물은 **원본(naive) Life를 바꾸지 않은 클래식 모드**를 제공해야 한다. 2인용 모드는 그대로 두어도 된다.

지금 코드에는 원본 Life를 실행할 방법이 없다.

| 위치 | 지금 동작 | 원본(`cb32b00`)과의 차이 |
| --- | --- | --- |
| `Life.java:24-33` | 보드 하나에 `CancerGame`을 붙인 `GameController` 하나만 만든다 | 원본에는 암세포 규칙이 없다 |
| `ui/ClassicMode.java:18` | 메뉴 이름은 `"Classic"`인데 실제로는 암세포 게임이다 | 이름과 내용이 다르다 (README "Classic(1인용)") |
| `ui/BoardPanel.java:52-53` | 오른쪽 클릭 = `controller.treat` | 원본 `Universe.java:94-103`은 버튼 구분 없이 `userClicked`(토글) |
| `application/GameController.java:76-99` | `cancerGame`이 있으면 tick마다 자연 발생·전이·승패 판정 | 원본은 B3/S23 세대 진행만 |

`GameController`의 `cancerGame == null` 경로(`tick`의 첫 분기, `treat`의 `return false`)는 원본 동작 그대로이고, 기존 특성 테스트(`ClassicGenerationTest`, `TwoPlayerUiTest#classicFeaturesWorkAfterSwitchingBack` 등)도 이 경로로 돈다. 즉 모델은 이미 원본을 돌릴 수 있고, **조립과 화면만** 원본을 막고 있다.

결정할 것은 세 가지다.

1. 클래식과 1인용을 어떻게 나눌까
2. Grid 메뉴(Clear·Load·Store)를 어느 보드로 보낼까 — 메뉴는 한 벌인데 보드가 둘이 된다
3. 오른쪽 클릭을 어떻게 원본대로 되돌릴까

## 결정 1. 클래식과 1인용을 나누는 방법

### A. 모드 2개 + 보드·컨트롤러 2벌 (Strategy, 기존 `GameMode` 재사용)
- `ClassicMode`는 그대로 두고 암 규칙 없는 컨트롤러에 붙인다. 암세포 게임은 새 `SinglePlayerMode`("Single Player")가 맡는다. 각자 `Neighborhood`·`GameController`·`BoardPanel`을 갖는다.
- 장점: 클래식 쪽 코드 경로가 원본과 같다(`cancerGame == null`). `ModeSwitch`가 이미 "지금 모드만 tick"을 보장한다(twoplayer/007 결정 1). 두 게임의 보드가 섞이지 않는다.
- 단점: 보드를 두 벌 만든다(64×64 `Resident` 4,096개 + `Neighborhood` 65개 추가). `SinglePlayerMode`는 `ClassicMode`와 이름만 다른 복사본이다(39줄).

### B. 모드 1개 + 암 규칙 켜기/끄기 토글
- `CancerGame`에 `enabled` 플래그를 두고 메뉴로 켜고 끈다.
- 장점: 보드 한 벌, 새 클래스 없음.
- 단점: 끌 때 남아 있는 암세포, 치료 횟수, 세대 수(확률 단계)를 어떻게 할지 새 규칙이 필요하다. 암세포가 남은 채 끄면 `Resident`의 암세포 규칙(`Resident.java:56-63`)이 계속 돌아 **원본과 다르게 진행한다.** "원본을 바꾸지 않는다"를 보장하려면 끌 때 보드를 비워야 해서 결국 A와 같은 사용자 경험이 된다.

### C. Null Object (`NoCancerRules implements CancerRules`)
- `GameController`의 `cancerGame == null` 분기를 아무것도 안 하는 규칙 객체로 바꾼다.
- 장점: `null` 검사가 사라진다.
- 단점: `CancerGame`을 인터페이스로 바꿔야 해서 application 리팩토링이 된다(인터페이스 +1, 구현 +1). 지금 `null` 경로는 원본 동작을 고정한 특성 테스트로 지켜지고 있는데, 그 경로를 바꾸면 "원본을 바꾸지 않는다"의 근거가 약해진다. 결정 1의 문제(모드 분리)는 그대로 남는다.

### D. Decorator (`CancerController`가 클래식 컨트롤러를 감쌈)
- 단점: `GameController`가 `final` 클래스라 인터페이스 추출이 필요하다. A와 같은 모드 분리가 여전히 필요하다.

### 오버헤드 비교표 (결정 1)

| 항목 | A | B | C | D |
| --- | --- | --- | --- | --- |
| 구조 비용 | 클래스 +1 (`SinglePlayerMode` 39줄), `Life.java` +16/−8 (실측) | 플래그 + 메뉴 항목 + 끌 때 규칙 (추정 30~50줄) | 인터페이스 +1, 클래스 +1, `GameController` 분기 수정 (추정) | 인터페이스 +1, 클래스 +1 (추정) |
| 간접성 | 없음 (클래식 tick = `GameController.tick` 첫 분기, 기존과 같음) | tick마다 플래그 검사 +1 | 규칙 호출 +1 (클래식에서도) | 위임 +1 |
| 결합도 | 변화 없음 (`ui`는 이미 `GameController`에 의존) | `ui` → `CancerGame` 직접 의존 추가 | 변화 없음 | 변화 없음 |
| 런타임 비용 | 보드 한 벌 추가 (시작 시 1회 생성). tick 비용 변화 없음 | 없음 | 클래식 tick마다 빈 호출 3번 | 위임 1번 |
| 테스트 용이성 | 두 모드 모두 창 없이 (UI-R21~R24) | 켜기/끄기 전환 케이스가 추가로 필요 | 같음 | 같음 |
| 확장 비용 (모드 1개 추가) | `GameMode` 구현 1 + `Life` 조립 1곳 | 플래그가 늘어날 때마다 조합 폭발 | A와 같음 | A와 같음 |

**A를 채택한다.** 클래식이 원본과 같은 코드 경로를 탄다는 것이 이번 과제 조건의 핵심이고, 이를 바꾸지 않는 것은 A뿐이다. B는 끌 때 남은 암세포가 원본 진행을 바꾼다. C·D는 원본 경로를 고치는 리팩토링이라 "기존 동작을 바꾸지 않는다"(AGENTS 3-2 1)와 충돌한다.

`SinglePlayerMode`가 `ClassicMode`의 복사본인 점은 알려진 중복이다. 둘을 한 클래스(`BoardMode(name, ...)`)로 합치는 것도 가능하지만, 기존 `ClassicMode`를 쓰는 테스트(`TwoPlayerUiTest`)를 건드리지 않기 위해 이번에는 나눠 둔다.

## 결정 2. Grid 메뉴를 어느 컨트롤러로 보낼까

`GameMenu.entries(controller, ...)`는 컨트롤러 하나를 람다에 붙잡는다(`GameMenu.java:28-35`). 보드가 둘이 되면 1인용에서 Clear를 눌러도 클래식 보드가 지워진다.

| 후보 | 구조 | 판단 |
| --- | --- | --- |
| A. `Supplier<GameController>` | `entries(Supplier, ...)` 오버로드 추가, 기존 `entries(GameController, ...)`는 `() -> controller`로 위임. `LifeFrame`이 `ModeSwitch.current()` → 컨트롤러 표에서 고른다 | **채택**. `GameMenu` +19/−4줄(실측), 기존 시그니처·테스트 유지 |
| B. 모드마다 Grid 메뉴 따로 (`MenuSite.addLine(panel, ...)` + 전환 시 `removeMyMenus`) | 모드 전환마다 메뉴를 떼고 다시 단다 | 기각. 전환할 때마다 메뉴를 떼고 다는 코드가 `ModeSwitch`에 들어가고, `setFileCommandsEnabled`의 요청자도 모드마다 바뀌어야 한다. 다시 단 항목의 순서가 유지되는지는 확인하지 않았다 |
| C. Proxy (`ActiveController`가 지금 컨트롤러로 위임) | `GameController`가 `final` 클래스라 인터페이스 추출 필요 | 기각. application 리팩토링, 메서드 7개 위임 |
| D. Command 객체를 모드가 제공 (`GameMode.gridCommands()`) | `GameMode`에 메서드 추가 → 2인용·테스트 대역(`StubMode`)도 구현해야 함 | 기각. 2인용은 파일 명령이 없어 빈 구현만 늘어난다 |

간접성: 메뉴 선택 → `Supplier.get()` (+1) → `IdentityHashMap` 조회 → 컨트롤러. 메뉴 클릭 1번에 1회라 측정하지 않았다.

## 결정 3. 오른쪽 클릭

원본 `Universe`의 `mousePressed`는 버튼을 보지 않고 토글한다(`cb32b00:Universe.java:94-103`). 지금 `BoardPanel`은 오른쪽 클릭이면 무조건 `treat`를 부르고, 암 규칙이 없으면 `treat`가 `false`만 돌려주므로 **클래식에서는 오른쪽 클릭이 아무 일도 하지 않는다**(원본과 다름).

| 후보 | 구조 | 판단 |
| --- | --- | --- |
| A. `GameController.hasCancerRules()` 질의 | `BoardPanel`: 오른쪽 클릭 && `hasCancerRules()`면 `treat`, 아니면 `toggle` | **채택**. `GameController` +5줄, `BoardPanel` +3/−1줄(실측). 생성자 변경 없음 → 기존 UI-R13~R15 그대로 |
| B. `BoardPanel` 생성자에 `boolean rightClickTreats` | 조립하는 쪽이 정한다 | 기각. "암 규칙이 있는가"와 "치료 클릭을 받는가"를 따로 맞춰야 해서 어긋날 수 있다(암 규칙 없는데 치료 켬 → 클릭 무반응) |
| C. 입력 Strategy (`MouseBinding`) | 모드마다 클릭 처리 객체 | 기각. 분기 하나에 타입 2~3개. twoplayer/007 후보 B에서 이미 같은 이유로 보류 |
| D. `treat`가 `false`면 `toggle` | `BoardPanel`만 수정 | 기각. 1인용에서 치료가 거부되면(암세포 10개 이하 등, #96) 셀이 토글된다 — 1인용 동작이 바뀐다 |

## 전/후 비교

```
전                                         후
Life                                       Life
 └ GameController(+CancerGame)              ├ GameController (클래식, cancerGame = null)
                                            └ GameController (1인용, +CancerGame)
LifeFrame                                  LifeFrame
 ├ ClassicMode("Classic") ← 암세포 게임      ├ ClassicMode("Classic")        ← 원본 Life, 시작 화면
 └ TwoPlayerMode("2 Players")               ├ SinglePlayerMode("Single Player") ← 암세포 게임
                                            └ TwoPlayerMode("2 Players")
GameMenu.installMenus(panel, controller)   GameMenu.installMenus(panel, () -> 지금 모드의 컨트롤러)
BoardPanel 오른쪽 클릭 = treat              BoardPanel 오른쪽 클릭 = hasCancerRules ? treat : toggle
```

| 파일 | 변경 (실측, `git diff --numstat`) |
| --- | --- |
| `ui/SinglePlayerMode.java` | 신규 39줄 |
| `ui/LifeFrame.java` | +50 / −18 |
| `ui/GameMenu.java` | +19 / −4 |
| `Life.java` | +16 / −8 |
| `application/GameController.java` | +5 / −0 |
| `ui/BoardPanel.java` | +3 / −1 |
| `ui/ClassicMode.java` | +4 / −1 (주석만) |

## 검증

- `./gradlew test`: 342개 통과, 실패 0 (2026-11-25, macOS, JDK 26). 새 테스트 UI-R20~R25 포함, 기존 테스트는 수정·비활성화하지 않았다.
- `NativeUiSmoke`(실제 창): 시작 화면이 클래식이고 Mode 메뉴에 Classic / Single Player / 2 Players가 있으며, 클래식 보드에서 마우스 토글·Tick·Store·Clear·Load·잘못된 파일 Load가 원본처럼 동작함을 확인. 출력 `PASS: launch, 10 menu items, 3 modes, mouse, Tick, Store unique coordinates, Clear, Load, cancel error, invalid-load preservation`.

## 후속 작업

- **클래식에서 암세포가 든 파일 Load**: `LegacyLifeCodec`은 암세포 확장 형식도 읽는다. 1인용에서 저장한 파일을 클래식에서 불러오면 암세포가 보드에 올라와 `Resident`의 암세포 규칙으로 진행한다(원본은 이 형식을 모르므로 "Read Failed!"). 거부할지, 암세포를 일반 셀로 읽을지 정해야 한다. 버그 수정과 섞지 않기 위해 이번 PR에서는 건드리지 않는다(AGENTS 3-2 2).
- `SinglePlayerMode`·`ClassicMode` 중복(결정 1 끝 문단).
- 1인용 화면에 남은 치료 횟수·세대 표시는 없다(기존과 같음).
