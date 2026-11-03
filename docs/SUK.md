# 작업 기록 (Son2kwon)

- 담당: 2인용 모드 + 팀 공용 테스트 환경·클래식 특성 테스트 + 특성 테스트에서 발견한 버그 3건
- 기간: 2026-11-03 ~
- 상세 근거는 각 도메인 `docs/`에 있고, 이 문서는 진행 순서와 결과만 모은다(AGENTS.md 2-1).

## 한눈에 보기

| 단계 | 이슈 | 브랜치 | PR | 상태 |
| --- | --- | --- | --- | --- |
| 0. 테스트 환경 (Gradle + JUnit 5 + CI) | #2 | `chore/test-setup` | #3 | 머지됨 |
| 1. 클래식 특성 테스트 | #4 | `test/classic-characterization` | #13 | 머지됨 |
| 작업 기록 정리 | #14 | `docs/suk-worklog` | #15 | 머지됨 |
| 버그: Store 중복 기록 | #10 | `fix/store-duplicate` | #16 | 리뷰 대기 (CI 통과) |
| 버그: 불러오기 ClassCastException | #11 | `fix/load-class-cast` | #17 | 리뷰 대기 (CI 통과), #16 머지 후 rebase |
| 버그: `Resident.transition()` 반환값 뒤집힘 | #12 | `fix/resident-transition` | #18 | 리뷰 대기 (CI 통과), 먼저 머지된 PR 기준 rebase |
| 2. 2인용 셀 상태·전이 규칙 | #5 | `feature/two-player-mode` | — | Cell 인터페이스 회의 대기 |
| 3. 2인용 게임 진행 모델 | #6 | `feature/two-player-mode` | — | 1차(단계·차례·배치·패스) 구현 완료, PR 미개설 |
| 4. 2인용 UI 통합 | #7 | — | — | 대기 |
| 5. 2인용 문서·다이어그램 정리 | #8 | — | — | 대기 |

## 0. 테스트 환경 (#2 → PR #3, 머지)

**문제**: 저장소에 빌드 도구·JUnit·CI가 없어서 AGENTS.md 3장과 강령 5(CI 실패 PR 머지 금지)를 지킬 수 없었다.

**한 것**

- `Codes/GameOfLife`에 Gradle 9.8.2 Wrapper, `build.gradle.kts` 추가
  - `sourceSets`: main = 기존 `src`, test = 새 `test/` (기존 디렉터리 구조 유지)
  - Java toolchain 26 (`.idea/misc.xml`의 JDK_26과 맞춤), 테스트는 headless + `assert` 활성화
- IntelliJ: `GameOfLife.iml`에 `test/`를 테스트 소스로 등록, JUnit 5를 저장소 라이브러리(`.idea/libraries/junit_jupiter.xml`)로 추가
- CI: `.github/workflows/test.yml` (PR·main push에서 JDK 26으로 `./gradlew test`)
- 스모크 테스트 1개

**결정**

| 후보 | 내용 | 결과 |
| --- | --- | --- |
| A | IntelliJ `.iml` 수정 + 저장소 라이브러리 | 채택. 기존 모듈 설정 유지. 대신 JUnit 버전이 `build.gradle.kts`와 `.idea/libraries` 두 곳에 있어 함께 고쳐야 함 |
| B | Gradle 프로젝트로 import | 기각. 커밋된 모듈 설정이 바뀌어 팀원 전체가 다시 import해야 함 |

- 컴파일 대상은 처음에 `release 21`이었다가 IDE 설정과 맞추려고 toolchain 26으로 바꿨다.

**확인**: 로컬 JDK 26 통과, 실패 테스트를 넣으면 빌드 실패(종료 코드 1) 확인, CI 통과(Temurin 26.0.2). IntelliJ에서 직접 열어 보는 확인은 리뷰어에게 요청했다.

## 1. 클래식 특성 테스트 (#4 → PR #13, 머지)

**목적**: 2인용 기능을 넣기 전에 클래식 모드의 현재 동작을 고정한다(강령 1). 프로덕션 코드는 바꾸지 않았다.

**순서**: 케이스 정의([`life/docs/test-cases.md`](../Codes/GameOfLife/src/com/holub/life/docs/test-cases.md)) 커밋 → 테스트 커밋 (강령 3)

**구성**

- 도우미 `ClassicBoard`: `Universe`와 같은 셀 구성(8×8 안에 8×8)을 Swing 없이 만들고, 마우스·Clear·Tick·Store·Load가 모델에 하는 호출을 `Universe` 코드와 같은 순서로 재현
- 도우미 `ReferenceLife`: 비교 기준용 단순 B3/S23
- 테스트 33개: 세대 진행 10, 마우스 토글·Clear 7, 저장·불러오기 9, 셀 단위 7

**결과**: 로컬·CI 모두 34개 통과(새 33 + 스모크 1). 무작위 보드(시드 42) 5개 × 100세대, R-pentomino 300세대, 가장자리에 닿는 glider 240세대가 매 세대 단순 구현과 같았다.

**한계**: `Universe`·`Clock`은 생성자에서 메뉴를 등록하고, 메뉴는 `JFrame`이 있어야 해서 headless에서 만들 수 없다. `Universe` 클래스를 불러오기만 해도 `MenuSite not established` assert가 실패한다. Go 메뉴(속도·Tick), Exit, 파일 선택 대화상자는 직접 테스트하지 못했고, 각 메뉴가 모델에 하는 호출만 재현했다.

## 버그 3건 수정 (#10, #11, #12)

특성 테스트(#4)를 쓰면서 찾은 버그를 고치지 않고 현재 동작으로 고정해 두었다가(강령 2), 버그마다 `main`에서 브랜치를 따로 만들어 PR 하나씩 올렸다. 순서는 모두 **테스트 케이스 정의 변경 → 테스트 변경 → 코드 수정**이고, 바뀐 케이스 말고 다른 특성 테스트는 수정 없이 통과했다. 세 PR은 `docs/test-cases.md`의 인접한 곳을 고쳐서, 먼저 머지된 PR 기준으로 rebase하기로 했다.

| 이슈 | PR | 수정 | 바뀐·추가한 케이스 | 수정 전 → 후 |
| --- | --- | --- | --- | --- |
| #10 Store 중복 기록 | #16 | `Universe.doStore()`의 중복 `transfer(STORE)` 한 줄 삭제 | CL-M-06 기대값(각 칸 1번), CL-M-10·11 추가 | 수정 전후 모두 36개 통과 (아래 한계) |
| #11 불러오기 `ClassCastException` | #17 | `NeighborhoodState.load()`가 `Collection`이 아니거나 원소가 `Point`가 아니면 `IOException`으로 던짐. `Universe`는 안 바꿈 | CL-M-08 기대값(`IOException`), CL-M-12·13 추가 | 2개 실패 → 36개 통과 |
| #12 `transition()` 반환값 | #18 | `Resident.transition()`을 `!isStable()` 반환으로 | CL-C-03 기대값, CL-C-08(재계산 블록 수) 추가 | 2개 실패 → 35개 통과 |

### #10의 결정과 한계

- 테스트가 `doStore()` 자체를 검증하게 하려면 저장 경로를 `Universe` 밖으로 빼야 했다. `Universe`는 #9(UI 분리) 담당이 구조를 바꾸고 있어서 **구조는 그대로 두고 한 줄만 지웠다.**
- 그래서 **Universe를 화면 없이 띄울 수 없어서 doStore 자체의 회귀는 테스트로 잡지 못한다.** CL-M-06은 `doStore()`와 같은 순서로 호출하는 `ClassicBoard#store()`를 검증하고, 수정 전에도 통과했다. test-cases.md와 PR #16 본문에 적었고, #9에 "UI 분리 후 CL-M-06이 doStore를 직접 검증하게 바꿔 달라"고 댓글을 남겼다.

### #11의 결정

- 수정 위치를 `Universe.doLoad()`가 아니라 `NeighborhoodState.load()`로 했다. 형식 오류를 `IOException`으로 바꾸면 `doLoad()`의 기존 `catch`가 받으므로 `Universe`를 건드리지 않고, 테스트도 `load()`를 직접 호출할 수 있다.
- 수정 전: CL-M-08은 `ClassCastException`, CL-M-12는 "예외 없음"으로 실패. 수정 후: 통과.

### #12 조사와 측정

고치기 전에 반환값을 뒤집으면 무엇이 달라지는지 먼저 조사했다(코드는 바꾸지 않고, 뒤집은 복사본을 scratch에서 컴파일해 비교).

| 항목 | 수정 전후 |
| --- | --- |
| 활성 블록 계산(`Neighborhood.java:232`의 `activeEdges`) | **바뀜.** 다시 계산하는 블록 수가 크게 줄어듦 |
| 다시 그리기(블록별 `amActive`·`oneLastRefreshRequired`) | 같음. 1,190세대 전부 일치, `BufferedImage` 렌더링 픽셀 해시도 일치 |
| `Universe`의 `refreshNow()` 호출 | 같음. `transition()`은 `figureNextState()`가 true일 때만 불리므로 |
| 보드 결과 | 같음. 뒤집은 상태로 테스트를 돌리면 CL-C-03만 실패, CL-G(`ReferenceLife` 비교) 전부 통과 |
| tick당 시간 (best of 10, 200 tick) | glider 0.0397 → 0.0062 ms, 무작위 보드 0.0562 → 0.0477 ms |

수정 PR에서는 재계산 수를 세는 package-private 카운터(`Neighborhood.recomputedNeighborhoods`)를 넣고 실측했다. tick당 셀을 다시 계산한 `Neighborhood` 수(바깥 블록 1 포함, 최대 65)의 평균:

| 보드 | tick 수 | 수정 전 | 수정 후 |
| --- | --- | --- | --- |
| glider | 240 | 64.7 | 3.3 |
| blinker | 50 | 63.8 | 4.0 |
| R-pentomino | 300 | 64.8 | 14.5 |
| 무작위 5개 (시드 42) | 각 100 | 65.0 | 50.6~55.7 |
| block (정물) | 10 | 0.2 | 0.2 |

- 전체 표와 측정 방법: [`life/docs/measurement-resident-transition.md`](https://github.com/DesignPattern8/DPTermProject/blob/fix/resident-transition/Codes/GameOfLife/src/com/holub/life/docs/measurement-resident-transition.md)
- CL-C-08은 glider 보드에서 tick 2~10마다 5개 이하인지 확인한다(수정 전 tick 2에서 65로 실패, 수정 후 매 tick 2).

## 2인용 모드 (`feature/two-player-mode`)

### 규칙 명세

- [`twoplayer/docs/rules.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/rules.md): 팀 초안(2026-11-02)을 저장소에 넣으면서 아래 세 가지를 확정·정정했다.
  - 개입 선공: 선공·후공은 초기 배치부터 이어서 번갈아 간다 → 라운드 1 개입 선공은 B
  - 임시 배치 없이 확정하면 거부
  - 예시 5: "(5,8) 대응 시 세대 10부터 B=24 고정"은 오류. 실제로는 세대 10에 24가 된 뒤 40, 32, 40, 44, 56, 20을 거쳐 **세대 17부터** 24로 고정(시뮬레이터 두 개로 교차 확인)

### Cell 인터페이스 변경 제안 (#5, 회의 자료)

- [`life/docs/001-cell-state-representation.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/docs/001-cell-state-representation.md) (상태: 제안)
- [`life/docs/interfaces.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/docs/interfaces.md) (초안)
- 문제: 셀 상태가 boolean 하나(`Resident.java:20-21`)이고, 이웃 상태를 묻는 메서드가 `isAlive()`뿐(`Cell.java:66`)이라 DEAD/A/B와 다수색 탄생을 표현할 수 없다.

| 후보 | 내용 | 비고 |
| --- | --- | --- |
| A | 공용 enum + `Cell.state()` | 모드가 늘 때마다 공용 enum 수정 |
| B | `Cell`은 그대로, 2인용 하위 인터페이스 + `instanceof` | 회의 없이 가능. 이웃 타입 보장이 런타임에만 있음 |
| C | `CellState` 인터페이스 + 모드별 enum + `Cell.state()` | **제안안.** `Cell`은 한 번만 바꾸고, 이후 모드는 자기 enum만 추가 |
| D | Visitor | 이웃 1개당 호출 2단계, 셀 종류 추가 시 고칠 곳이 가장 많음 |

- 회의에서 C에 합의하지 못하면 B로 간다. 회의 전까지 인터페이스 코드는 바꾸지 않는다(1인용 모드 작업과 충돌 방지).

### 2인용 테스트 케이스 정의

- [`twoplayer/docs/test-cases.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/test-cases.md): 66개 (전이 규칙 18, 게임 진행 17, 배치 15, 승패 8, UI 8).
- 명세 7장 예시 1~4의 기대값은 별도 Python 시뮬레이터로 다시 계산해 일치를 확인했다.

### 게임 진행 모델 1차 (#6)

`main`(테스트 환경·특성 테스트)을 먼저 `feature/two-player-mode`에 merge한 뒤, **설계 결정 문서 → 테스트 → 구현** 순서로 진행했다.

**설계 결정**

| 문서 | 비교한 후보 | 채택 | 이유 |
| --- | --- | --- | --- |
| [003 게임 단계](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/003-game-phase.md) | State / enum + switch / Strategy | State | 지금까지 바뀐 규칙(개입 선공, 빈 확정 거부, 자동 패스)이 모두 "한 단계의 규칙"이라, 단계별 클래스면 한 곳에서 끝난다 |
| [004 플레이어 입력](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/004-player-input.md) | Command / 직접 호출 + 결과 값 / Observer | 직접 호출 + `Response` | Command가 값을 하는 경우(undo, 큐, 재생)가 없다. undo는 규칙으로 금지, hot-seat라 큐도 없다 |

**셀 상태와의 분리**: 셀 상태는 회의 전이라, 게임 진행이 보드에 묻는 메서드 6개(크기 `rows`·`columns`, 빈 칸인가, 빈 칸이 있는가, 놓기, 1세대 진행)를 `TwoPlayerBoard` 인터페이스로 숨겼다. 테스트는 대역 `StubBoard`로 돌린다. 계약: [`twoplayer/docs/interfaces.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/interfaces.md)

**구현 범위**: 단계 전환, 차례, 선공 교대, 임시 배치·확정, 패스·자동 패스, 진영 제한, 남은 개수, 라운드, tick, 설정값(N=20, K=20, M=3, R=5).

**결과**

- 테스트 32개(TP-F 17, TP-P 15). 골격만 있을 때 32개 모두 실패 → 구현 후 66개 통과(새 32 + 기존 34).
- 실측(003에 기록): 새 패키지 `com.holub.life.twoplayer`, 단계 클래스 6개(package-private) + 공개 타입 7개, 빈 줄·주석 제외 367줄. 입력 1번에 위임 +1단계. 기존 코드 변경 없음, `java.awt`·Swing 의존 없음.
- 진행 중 바꾼 것
  - TP-P-10 정의 수정: 남은 개수가 0이면 자동 패스로 차례가 오지 않아서 "남은 개수 없음" 거부는 생길 수 없다 → "차례 아님"으로 거부되는 것으로 정의를 바꾸고 해당 거부 이유를 없앴다.
  - 임시 배치 좌표를 처음에 `java.awt.Point`로 썼다가, 003의 "`java.awt` 의존 없음"과 맞지 않아 `Coordinate` record로 바꿨다.
  - TP-F만 요청받았지만 구현에 배치 규칙(진영·빈 칸·범위)이 들어가서 TP-P 15개도 함께 작성했다(강령 4).

## 남은 일

- PR #16 → #17·#18 rebase·재테스트 → 머지
- 세 버그 PR 머지 후: 리팩토링 담당에게 보낼 요약(무엇이 바뀌었고 어떤 테스트가 고정하는지)
- #6 2차: 시뮬레이션 중 매 세대 전멸 검사와 승패 판정(TP-E). 셀 수 집계가 필요해서 설계 결정 005(집계 방법)와 함께
- #6 PR: base를 정해야 함(`main` 또는 `feature/two-player-mode`에 계속 모으기)
- Cell 인터페이스 회의 → 001 상태 변경(채택/기각) → #5 구현 → `TwoPlayerBoard` 실제 구현
- 다이어그램: 게임 단계 상태 다이어그램, 2인용 클래스·시퀀스 다이어그램(.puml) 미작성. PlantUML 렌더링 도구를 받는 것은 이번 세션에서 차단돼 png/svg는 따로 만들어야 함
- 도메인 `docs/README.md`(`life/docs/`, `twoplayer/docs/`) 아직 없음
- 검증에 쓴 시뮬레이터·측정 프로그램은 저장소에 없음(결과만 문서에 기록)

## 진행하면서 있었던 실수

- 로컬 테스트 확인 전에 push한 적이 있다(#13). 명령의 `exit=1`이 Gradle이 아니라 `grep`의 종료 코드였고, 다시 돌려 Gradle 종료 코드 0과 34개 통과를 확인했다.
- 커밋하지 않은 `.gitignore` 수정이 있는 상태에서 `git checkout -- .`가 포함된 명령을 실행하려다 차단됐다. 이후 기존 파일을 건드리지 않는 방식(임시 테스트 파일 추가 후 삭제)으로 확인했다.
- 테스트 출력 한 번을 scratchpad 대신 `/tmp/x`에 썼다. 바로 지웠다.
