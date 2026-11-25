# DPTermProject — Holub Life Game 확장·리팩토링

중앙대학교 설계패턴 텀프로젝트(8팀) 저장소입니다. Allen Holub의 Java Life Game(Conway의 Game of Life)에 **1인용 암세포 게임**과 **2인용 대전 게임**을 추가하고, 기존 코드를 도메인별로 나눠 리팩토링했습니다.

- 실행하면 원본 그대로의 **클래식 모드**로 시작하고, 1인용·2인용 모드를 따로 고를 수 있습니다.
- 모든 설계 결정은 GoF 패턴 후보(패턴을 쓰지 않는 구현 포함)를 2개 이상 비교하고 근거를 각 도메인의 `docs/`에 남겼습니다. 규칙은 [AGENTS.md](AGENTS.md)를 따릅니다.

## 게임 모드

Mode 메뉴에서 세 모드를 오갑니다. 메뉴 항목 이름은 `Classic`, `Single Player`, `2 Players`입니다. 모드마다 보드가 따로 있고, 선택한 모드의 보드만 진행합니다.

### 클래식 — 원본 Life

Allen Holub의 원본 Game of Life와 같습니다(저장소 최초 커밋 `cb32b00` 기준). 승패와 암세포가 없습니다.

| 조작 | 동작 |
| --- | --- |
| 클릭(왼쪽·오른쪽 모두) | 셀 놓기/지우기 |
| Go 메뉴 | 연속 실행(Agonizing·Slow·Medium·Fast), Halt, Tick |
| Grid 메뉴 | Clear, Load, Store, Exit |

- **셀 규칙:** Conway B3/S23. 보드 밖은 죽은 셀입니다.

설계 근거: [ui/docs/012](Codes/GameOfLife/src/com/holub/life/ui/docs/012-classic-mode-restore.md)

### 1인용 — 암세포 게임

세대가 지날수록 퍼지는 암세포를 치료로 막는 생존 게임입니다.

| 조작 | 동작 |
| --- | --- |
| 왼쪽 클릭 | 셀 놓기/지우기 |
| 오른쪽 클릭 | 클릭한 칸 중심 5×5 범위의 암세포 치료 |
| Preset 메뉴 | 패턴을 고른 뒤 보드를 클릭하면 그 칸을 왼쪽 위로 배치(오른쪽 클릭은 취소). 1인용에서만 켜짐 |
| Go 메뉴 | 연속 실행(Agonizing·Slow·Medium·Fast), Halt, Tick |
| Grid 메뉴 | Clear, Load, Store, Exit |

- **셀 규칙:**
  - 일반 세포는 B3/S23이고, 이웃 수에 암세포도 셉니다.
  - 암세포는 살아 있는 이웃이 1~5개면 살아남습니다.
  - 이웃 암세포가 3~4개인 빈 칸에는 암세포가 태어납니다.
- **확산:**
  - 매 세대 뒤 자연 발생, 전이(확률 1 − 0.97ⁿ), 집단 발생이 일어납니다.
  - 발생 확률은 100세대마다 0.001 → 0.005 → 0.02 → 0.05로 오릅니다.
- **치료:**
  - 게임당 5회입니다.
  - 암세포가 10개 이하이거나 범위 안에 암세포가 없으면 거부되고, 횟수는 줄지 않습니다.
- **승패:** 치료로 암세포가 0개가 되면 승리, 암세포가 1024개(64×64 보드의 25%) 이상이면 패배입니다.

규칙 원본: [docs/cancer-feature-changes.md](Codes/GameOfLife/src/com/holub/life/docs/cancer-feature-changes.md)

### 2인용 — 대전 게임

두 플레이어(A, B)가 한 화면에서 번갈아 셀을 두고 영역을 다툽니다.

- **진행:** 초기 배치(각 20개, 자기 진영) → 시뮬레이션(20세대) → 개입(각 최대 3개, 보드 어디든)을 5라운드 반복합니다.
- **조작:** 보드 클릭으로 칸을 고르고 Confirm(또는 Enter)으로 확정합니다. 개입 단계에서는 Pass를 쓸 수 있습니다. 시뮬레이션은 Go 메뉴로 진행합니다.
- **셀 규칙:**
  - 생존과 사망은 색과 관계없이 B3/S23으로 정합니다.
  - 새로 태어나는 셀은 이웃 3개 중 다수인 색을 따릅니다.
- **승패:**
  - 시뮬레이션 중 한쪽이 전멸하면 즉시 끝납니다.
  - 5라운드가 끝나면 살아 있는 셀이 많은 쪽이 이기고, 같으면 무승부입니다.
- 2인용 모드에서는 Clear·Load·Store가 꺼집니다.

규칙 원본: [twoplayer/docs/rules.md](Codes/GameOfLife/src/com/holub/life/twoplayer/docs/rules.md)

## 빌드와 실행

JDK 26이 필요합니다(`build.gradle.kts`의 toolchain, `.idea/misc.xml`의 JDK_26과 같음). 모든 명령은 `Codes/GameOfLife`에서 실행합니다.

```bash
cd Codes/GameOfLife

# 테스트 (JUnit 5, headless)
./gradlew test

# 실행
./gradlew compileJava
java -cp build/classes/java/main com.holub.life.Life
```

IntelliJ에서는 `Codes/GameOfLife`를 열고 `com.holub.life.Life`의 `main`을 실행하면 됩니다.

PlantUML 다이어그램은 자기 도메인 폴더만 렌더링합니다.

```bash
./gradlew renderDiagrams -Pdir=src/com/holub/life/twoplayer
```

## 구조

```
Codes/GameOfLife/
├── src/com/holub/life/
│   ├── Life.java        진입점. 각 도메인을 조립
│   ├── model/           셀·보드 규칙, 활성 블록 (Cell, Resident, Neighborhood, Board)
│   ├── application/     실행 제어 (Clock, TickSource, GameController, CancerGame)
│   ├── persistence/     .life 파일 저장·불러오기, 프리셋 데이터
│   ├── ui/              Swing 화면·입력, 모드 전환 (LifeFrame, BoardPanel, GameMenu, ModeSwitch, ClassicMode, SinglePlayerMode)
│   └── twoplayer/       2인용 게임 모델 (TwoPlayerGame, 단계별 State, GameJudge)
│       └── ui/          2인용 화면 (TwoPlayerMode, TwoPlayerPanel)
└── test/                JUnit 5 테스트
diagrams/                리팩토링 전 원본 다이어그램 (보존용, 수정하지 않음)
docs/                    팀원별 작업 기록
```

각 패키지의 `docs/`에 설계 결정(A/B 비교), 공개 계약(`interfaces.md`), 테스트 케이스(`test-cases.md`), 다이어그램이 있습니다.

| 도메인 | 문서 |
| --- | --- |
| model | [model/docs](Codes/GameOfLife/src/com/holub/life/model/docs/README.md), [docs/GTY.md](docs/GTY.md) |
| application | [application/docs](Codes/GameOfLife/src/com/holub/life/application/docs/README.md), [docs/LSH.md](docs/LSH.md) |
| persistence | [persistence/docs](Codes/GameOfLife/src/com/holub/life/persistence/docs/README.md) |
| ui | [ui/docs](Codes/GameOfLife/src/com/holub/life/ui/docs/README.md) |
| twoplayer | [twoplayer/docs](Codes/GameOfLife/src/com/holub/life/twoplayer/docs/README.md), [docs/SUK.md](docs/SUK.md) |

## 주요 설계 개선

| 도메인 | 내용 |
| --- | --- |
| model | 모델의 Swing·Graphics 의존 제거, 클래식 보드 Facade(`Board`), 생사 판정 함수 분리 |
| application | Clock Singleton 제거, `TickSource` 도입, 보드 호출 직렬화 |
| persistence | 파일 입출력 API 분리, 저장 실패 시 기존 파일 보존(원자적 교체), 실패해도 보드를 유지하는 Load |
| ui | 메뉴를 (메뉴, 라벨, 동작) 항목 표로 구성, 대화상자를 `UserDialogs`(Strategy)로 주입, 게임 결과 알림 분리 |
| twoplayer | 게임 단계 State, 모드 전환 `GameMode`(Strategy) + `ModeSwitch`, 진입점 `TwoPlayerSession`(Facade), 상태 표시줄 Observer |

## 작업 방식

- 작업 단위마다 이슈를 만들고, 브랜치 → PR → `Closes #번호`로 연결합니다.
- 기능은 `feature/base`, 리팩토링은 `refactor/base`에서 브랜치를 따서 진행합니다.
- REFACTOR·FEAT PR에는 작업한 도메인의 `docs/` 문서를 같이 올립니다.
- 기존 동작은 특성 테스트로 먼저 고정하고, 테스트 케이스를 먼저 정의한 뒤 구현합니다.
- CI(GitHub Actions, JDK 26, `./gradlew test`)를 통과한 PR만 머지합니다.

## 팀

| 이름 | GitHub |
| --- | --- |
| 손의권 (팀장, 2인용 게임) | [@Son2kwon](https://github.com/Son2kwon) |
| 고태영 | |
| 김경민 | |
| 이시형 | [@leesihyeong17](https://github.com/leesihyeong17) |
| 임도현 | |
| 전병국 | |
