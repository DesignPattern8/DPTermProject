# 작업 기록 (Son2kwon)

- 담당: 2인용 모드 + 팀 공용 테스트 환경·클래식 특성 테스트 + 특성 테스트에서 발견한 버그 3건
- 기간: 2026-11-03 ~
- 상세 근거는 각 도메인 `docs/`에 있고, 이 문서는 진행 순서와 결과만 모은다(AGENTS.md 2-1).

## 한눈에 보기

### 머지된 PR (머지 순서, 2026-11-03 ~ 11-04)

| PR | 날짜 | base | 이슈 | 내용 |
| --- | --- | --- | --- | --- |
| #3 | 11-03 | main | #2 | 테스트 환경 (Gradle + JUnit 5 + CI) |
| #13 | 11-03 | main | #4 | 클래식 특성 테스트 33개 (기존 동작 고정) |
| #15 | 11-03 | main | #14 | 작업 기록 정리 |
| #16 | 11-03 | main | #10 | 버그: Store 중복 기록 |
| #17 | 11-03 | main | #11 | 버그: 불러오기 `ClassCastException` (#16 머지 후 rebase) |
| #18 | 11-03 | main | #12 | 버그: `Resident.transition()` 반환값 뒤집힘 |
| #20 | 11-03 | main | #19 | 작업 기록 갱신 (버그 3건, #6 1차) |
| #21 | 11-03 | 통합 브랜치 | #6 (Refs) | 2인용 게임 진행 모델 (단계·배치·패스·승패 판정) |
| #23 | 11-03 | main | #22 | PlantUML 다이어그램 렌더링 작업 |
| #25 | 11-04 | main | #24 | (팀원 Lidohyeon) 도메인 구조 변경: `model`·`application`·`persistence`·`ui` |
| #26 | 11-04 | 통합 브랜치 | — | main 동기화 1차 (squash로 머지됨) |
| #28 | 11-04 | main | #27 | 작업 기록 갱신 (머지 반영, 통합 브랜치 PR 규칙) |
| #32 | 11-04 | 통합 브랜치 | — | main 동기화 2차 (#25 포함, merge commit으로 머지) |

"통합 브랜치" = `feature/two-player-mode`.

### 진행 중 PR

| PR | base | 이슈 | 내용 | 상태 |
| --- | --- | --- | --- | --- |
| #34 | main | #33 | 클래식 `test-cases.md`를 #25 이후 경로로 갱신(CL-M-06 정리), 측정 문서 기준 커밋 한 줄, 이 작업 기록 | CI 통과, 리뷰 대기 |
| #35 | 통합 브랜치 | #5 (Refs) | 회의 자료(001, core `interfaces.md`)를 #25 이후 코드로 갱신, 렌더러 문제 반영 | 리뷰 대기 |

### 다음 작업

| 작업 | 이슈 | 시작 조건 |
| --- | --- | --- |
| #6 다이어그램 (상태 렌더링, 클래스·시퀀스) → 머지 후 #6 닫기 | #6 (Refs) | #35 다음 |
| 2인용 셀 상태·전이 규칙 | #5 | Cell 인터페이스 회의 후 |
| 2인용 UI 통합 | #7 | #5 이후, #25의 UI 구조 위에서 |
| 2인용 문서·다이어그램 정리 | #8 | #5·#7 이후 |

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

- #16 머지 후 #17이 `docs/test-cases.md`에서 충돌했다. #16이 CL-M-09 아래에 CL-M-10·11과 CL-M-06 메모를, #17이 같은 자리에 CL-M-12·13과 CL-M-08 메모를 넣었기 때문이다. 양쪽을 모두 살려(행은 CL-M-10~13 순서, 메모는 #16의 CL-M-06 한계 메모 + #17의 CL-M-08 메모) `main` 기준으로 rebase하고 `--force-with-lease`로 push했다. 테스트 파일은 자동으로 합쳐졌고, 로컬·CI 모두 38개 통과(main 36 + CL-M-12·13).

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

- 전체 표와 측정 방법: [`life/docs/measurement-resident-transition.md`](../Codes/GameOfLife/src/com/holub/life/docs/measurement-resident-transition.md)
- CL-C-08은 glider 보드에서 tick 2~10마다 5개 이하인지 확인한다(수정 전 tick 2에서 65로 실패, 수정 후 매 tick 2).

## 다이어그램 렌더링 (#22)

**문제**: AGENTS.md 2-5는 `.puml` 원본과 png/svg를 함께 커밋하도록 정했는데, 렌더링 도구가 없어 팀원마다 따로 설치해야 했다(이번 세션에서 PlantUML jar를 직접 받아 실행하는 것도 차단됨).

**한 것**: `build.gradle.kts`에 Maven Central의 `net.sourceforge.plantuml:plantuml:1.2026.9`를 별도 configuration으로 넣고 `./gradlew renderDiagrams`(png·svg 작업 2개를 묶음)를 추가했다. 대상은 `src/**/docs/diagrams/*.puml`이고, 결과는 같은 폴더에 생긴다. 루트 `diagrams/`(리팩토링 전 원본)는 보존 대상이라 렌더링하지 않는다.

**확인** (Graphviz 없는 macOS, JDK 26)

| 경우 | 결과 |
| --- | --- |
| `.puml`이 없음 (main) | 두 작업 SKIPPED, 성공 |
| 상태(#6의 게임 단계)·클래스·시퀀스 다이어그램 | png·svg 생성, 이미지를 열어 정상 렌더링 확인. `!pragma layout smetana` 없이도 됨 |
| 문법 오류가 있는 `.puml` | 작업 실패(종료 코드 1) |
| `./gradlew test` | 영향 없음 (34개 통과) |

- 결과 파일 이름은 파일명이 아니라 `@startuml <이름>`을 따른다. 그래서 `@startuml` 이름을 파일명과 같게 쓴다.
- 처음 쓴 `val plantuml by configurations.creating`이 Gradle 9에서 deprecated 경고를 내서 `configurations.create("plantuml")`로 바꿨다.

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

### 게임 진행 모델 (#6)

`main`(테스트 환경·특성 테스트)을 먼저 `feature/two-player-mode`에 merge한 뒤, **설계 결정 문서 → 테스트 → 구현** 순서로 진행했다.

브랜치 정리: 1차 커밋 5개가 통합 브랜치에 바로 들어가 있어서, `feat/6-game-flow`를 그 끝(`f3829d5`)에 만들어 먼저 push한 뒤 `feature/two-player-mode`를 merge 커밋(`99cc727`)으로 되돌렸다(`--force-with-lease`). 이후 2차도 `feat/6-game-flow`에서 진행하고, PR은 `feat/6-game-flow` → `feature/two-player-mode`로 연다.

**설계 결정**

| 문서 | 비교한 후보 | 채택 | 이유 |
| --- | --- | --- | --- |
| [003 게임 단계](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/003-game-phase.md) | State / enum + switch / Strategy | State | 지금까지 바뀐 규칙(개입 선공, 빈 확정 거부, 자동 패스)이 모두 "한 단계의 규칙"이라, 단계별 클래스면 한 곳에서 끝난다 |
| [004 플레이어 입력](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/004-player-input.md) | Command / 직접 호출 + 결과 값 / Observer | 직접 호출 + `Response` | Command가 값을 하는 경우(undo, 큐, 재생)가 없다. undo는 규칙으로 금지, hot-seat라 큐도 없다 |
| [005 셀 수 집계와 승패 판정](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/005-cell-count-and-judge.md) | Phase 안 / 별도 판정 클래스 / Strategy / Observer | 별도 판정 클래스(패턴 없음) | 판정은 셀 수 두 개 → 결과인 순수 함수라 숫자만으로 테스트할 수 있다. 다른 점수 방식은 rules.md에서 기각돼 Strategy로 바꿀 대상이 없다 |

**셀 상태와의 분리**: 셀 상태는 회의 전이라, 게임 진행이 보드에 묻는 메서드 6개(크기 `rows`·`columns`, 빈 칸인가, 빈 칸이 있는가, 놓기, 1세대 진행)를 `TwoPlayerBoard` 인터페이스로 숨겼다. 테스트는 대역 `StubBoard`로 돌린다. 계약: [`twoplayer/docs/interfaces.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/interfaces.md)

**구현 범위**

- 1차: 단계 전환, 차례, 선공 교대, 임시 배치·확정, 패스·자동 패스, 진영 제한, 남은 개수, 라운드, tick, 설정값(N=20, K=20, M=3, R=5)
- 2차: `TwoPlayerBoard.count(Player)` 추가, 시뮬레이션 중 매 세대 직후 전멸 검사(K세대 전이라도, 마지막 라운드가 아니어도 종료), R라운드 종료 셀 수 비교, `score()`·`result()`

**결과**

- 1차: 테스트 32개(TP-F 17, TP-P 15). 골격만 있을 때 32개 모두 실패 → 구현 후 66개 통과(새 32 + 기존 34).
- 2차: 테스트 10개(TP-E). 대역에 명세 7장 예시의 세대별 셀 수를 주입했다. 골격만 있을 때 10개 모두 실패 → 구현 후 76개 통과.
- 실측(003·005에 기록): 패키지 `com.holub.life.twoplayer` 파일 17개, 빈 줄·주석 제외 423줄(1차 367줄). 입력 1번에 위임 +1단계, 세대마다 판정 정적 호출 1번. 기존 코드 변경 없음, `java.awt`·Swing 의존 없음.
- 문서: 게임 단계 상태 다이어그램 원본([`.puml`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/diagrams/twoplayer-game-phase-state.puml)), [`twoplayer/docs/README.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/README.md). png/svg는 렌더링 작업(CHORE) 추가 후.
- 진행 중 바꾼 것
  - TP-P-10 정의 수정: 남은 개수가 0이면 자동 패스로 차례가 오지 않아서 "남은 개수 없음" 거부는 생길 수 없다 → "차례 아님"으로 거부되는 것으로 정의를 바꾸고 해당 거부 이유를 없앴다.
  - 임시 배치 좌표를 처음에 `java.awt.Point`로 썼다가, 003의 "`java.awt` 의존 없음"과 맞지 않아 `Coordinate` record로 바꿨다.
  - TP-F만 요청받았지만 구현에 배치 규칙(진영·빈 칸·범위)이 들어가서 TP-P 15개도 함께 작성했다(강령 4).
  - TP-E에 "마지막 라운드가 아니어도 K번째 세대에 전멸하면 개입 없이 종료"(TP-E-09)와 "판정 규칙만 숫자로 확인"(TP-E-10)을 추가했다.
  - 005의 추정(새 타입 2개)과 달리 실제로는 4개가 생겼다. 결과를 승패(`Outcome`)와 이유(`EndReason`)로 나눴기 때문이다.

## 머지 기록과 통합 브랜치 규칙 (2026-11-03)

**머지된 PR**: #16·#17·#18(버그 3건, main), #20(작업 기록, main), #21(#6 게임 진행 모델, `feature/two-player-mode`), #23(렌더링 작업, main).

- #21은 base가 기본 브랜치가 아니라서 `Closes #6`이 동작하지 않았다. #6은 열려 있다.
- 리팩토링 담당에게 보낼 요약(버그 3건이 무엇을 바꿨고 어떤 테스트가 고정하는지)을 작성했다. 팀원의 #25(도메인 구조 변경, `Universe`·`Neighborhood`·`Resident` 이동)가 버그 수정 전 main(`e38ce7a`)에서 갈라져 있어서, rebase할 때 세 수정과 CL-C-08의 package-private 계측 카운터가 빠지지 않아야 한다는 점을 담았다.

**통합 브랜치 PR 규칙**

- `feature/two-player-mode`로 가는 PR은 본문에 `Closes #번호` 대신 **`Refs #번호`**를 쓴다.
- 머지한 뒤 이슈에 **PR 링크를 댓글로 남기고 직접 닫는다.** (GitHub는 기본 브랜치로 머지될 때만 `Closes`로 이슈를 닫는다.)
- `main`으로 가는 PR은 그대로 `Closes #번호`.
- **main 동기화 PR은 반드시 merge commit으로 머지한다(squash 금지).** 이유: 히스토리가 끊기면 다음 동기화에서 충돌한다. #26을 squash로 머지했더니 main의 커밋이 통합 브랜치의 조상이 아니게 돼, 다음 동기화(#32)의 merge base가 오래된 `e38ce7a`로 잡히고 이미 반영된 변경 6개 파일이 다시 충돌했다(+ 충돌 표시 없이 잘못 합쳐진 파일 1개).

**통합 브랜치 동기화**: main의 버그 수정·렌더링 작업을 2인용 쪽에 넣기 위해 `feature/two-player-mode`에서 `chore/sync-main-into-2p`를 따서 main만 merge했다(#26). 충돌은 없었고 81개 테스트 통과(클래식 39 + 2인용 42). 이 PR에는 새 코드·문서를 넣지 않았다.

**#6 다이어그램 순서**: #26 머지 → 다이어그램 PR(게임 단계 상태 다이어그램 렌더링, 클래스 다이어그램, 한 라운드 시퀀스 다이어그램, `Refs #6`) → 머지 후 #6에 #21과 다이어그램 PR 링크를 남기고 닫는다.

**통합 브랜치 동기화 2차 (#32)**: #25(도메인 구조 변경)가 main에 머지된 뒤 다시 main을 merge했다. 충돌 6개(`Universe` 삭제, `model/Neighborhood`·`model/Resident`, `ClassicBoard`, `ClassicMementoTest`, `SUK.md`)와 자동 병합 오류 1개(`ClassicCellTest`에 #25가 다른 파일로 옮긴 CL-C-08이 남아 컴파일 실패)를 모두 main 쪽으로 해결했다. 근거: 통합 브랜치는 이 파일들을 직접 바꾼 적이 없다(지난 동기화 때의 main `7f10a65`와 파일별로 같음). 2인용 코드는 core 클래스를 import하지 않아 #25의 경로 이동(`com.holub.life.model`)에 영향이 없었다. 97개 통과(클래식·core 55 + 2인용 42), main CI도 55개 통과.

**버그 수정이 #25 이후에도 남았는지**: #25 머지 전 #25에 확인 요청 댓글을 남겼고, 머지 후 main에서 확인했다. #16 중복 `transfer` 없음, #17 형식 검사는 `persistence/LegacyLifeCodec.load()`로 이동, #18 `!isStable()`과 계측 카운터는 `model/`에, CL-C-08은 `model/NeighborhoodRecomputationTest`로 이동.

**CL-M-06 (#33)**: #25 이후 `GameController`는 화면 없이 만들 수 있고(`Cell` + `Clock(Timer)`), 실제 저장 경로(`GameController.store()`)는 `GameControllerTest#storeWritesEachCoordinateOnceAndRoundTrip`이 이미 직접 검증한다. CL-M 테스트를 이 경로로 옮기는 안(a)과 문서만 고치는 안(b) 중 (b)로 정했다(#9 담당과 겹칠 수 있어서). `life/docs/test-cases.md`에서 "doStore 회귀는 못 잡음"을 지우고, 옛 `Universe.java` 등 줄 번호를 현재 위치로 바꾸고(원본 위치는 괄호), 남은 한계(`GameMenu` → `FileDialogs` 경로, 메뉴 등록, Exit)를 기록했다. 코드는 바꾸지 않았다. 케이스 정의는 #25가 `life/docs/domain-extraction-tests.md`에 EXT-A01~A06 등으로 이미 써 두었다(실제 저장 경로 검증은 EXT-A05). 같은 PR에서 측정 문서(`measurement-resident-transition.md`)는 줄 번호를 측정 기록 그대로 두고, "줄 번호는 측정 시점 커밋(`db637eb` 수정 전, `94cdaa6` 수정 후) 기준, #25 이후 현재 위치: `model/Neighborhood.java`" 한 줄만 추가했다.

**팀원에게 남긴 댓글 2개**: 담당별로 나누고 `@` 멘션은 넣지 않았다. [#9 댓글](https://github.com/DesignPattern8/DPTermProject/issues/9#issuecomment-6629193687), [#25 댓글](https://github.com/DesignPattern8/DPTermProject/pull/25#issuecomment-6629194101).

- #9(UI 분리 담당): CL-M 테스트(`ClassicBoard#store()`/`#load()`)를 `GameController` 경로로 옮기는 (a)안을 #9 작업 때 같이 할지
- #25(작성자): #25가 추가한 테스트 16개(`GameControllerTest` 5, `ClockTest` 1, `GameMenuTest` 2, `RenderingExtractionTest` 5, `LoadFailureCharacterizationTest` 3)에 케이스 ID와 목적이 드러나는 `@DisplayName`을 붙이고, 정의 문서(`domain-extraction-tests.md`)를 각 도메인 `test-cases.md`에서 찾을 수 있게 연결해 달라는 요청. 바쁘면 별도 PR로 붙여도 될지 함께 물었다.

**회의 자료 갱신 (#5, 통합 브랜치)**: #32 머지 후 001과 core `interfaces.md`의 경로·줄 번호를 #25 이후 코드(`life/model`, `life/ui`)로 고쳤다. #25에서 그리기가 `ui/BoardRenderer`로 옮겨지면서 제안 내용에 영향이 생겼다.

- `BoardRenderer`는 `instanceof Neighborhood`/`instanceof Resident`로 셀 종류를 고르고, 색은 `isAlive()`만 본다. 두 조건에 맞지 않는 셀은 그려지지 않으므로, 2인용 셀을 새 클래스로 만들면 화면에 나오지 않고 A·B 색을 고를 정보도 없다.
- 오버헤드 표에 렌더러 항목(고칠 파일, 그리는 셀 1개당 간접성, 새 셀 종류를 빠뜨렸을 때, 충돌 대상)을 추가했다. 네 후보 모두 렌더러(#9 담당 영역)를 한 번 고쳐야 한다.
- D(Visitor)를 다시 평가했다. 렌더러·셀 수 집계까지 쓸 곳이 늘고 새 셀 종류 누락이 컴파일 오류로 잡히는 이점이 생겼지만, 가장 자주 부르는 이웃 조회에서 호출이 2배가 되고 `life/model` ↔ `twoplayer` 패키지 순환이 생긴다. 제안은 C(상태 인터페이스 + 모드별 enum, 렌더러는 상태 → 팔레트 색) 유지, 상태는 "제안".
- 회의에서 정할 것에 3개를 추가했다: 렌더러가 색을 고르는 방법과 담당, 다른 도메인의 Visitor 계획, `CellState` 패키지 위치와 회의 자료 문서 위치(`life/docs` vs #25의 `life/model/docs`).

## 남은 일

- #35(회의 자료, → 통합 브랜치, Refs #5) 머지 → #6 다이어그램 PR → #6 닫기
- #9·#25 댓글 답변 확인 (CL-M 경로 이동 시점, `@DisplayName` 담당)
- Cell 인터페이스 회의 → 001 상태 변경(채택/기각) → #5 구현 → `TwoPlayerBoard` 실제 구현 (#25 이후)
- #7 UI 통합: #25가 나눈 UI 구조(`BoardPanel`, `GameMenu`, `GameController`) 위에서 설계
- 브랜치 방식: `feature/two-player-mode`는 2인용 통합 브랜치. 단계마다 여기서 브랜치를 따서 이 브랜치로 PR을 보내고(위 규칙), `main`으로는 #5까지 끝나면 한 번에 PR을 연다
- 다이어그램 원본과 렌더링 결과는 관련 코드 PR에 함께 넣는다(AGENTS.md 2-5)
- 도메인 `docs/README.md`: `twoplayer/docs/`는 #21에 넣음. `life/docs/README.md`는 #25가 추가함(문서 목록에 001·`interfaces.md`는 통합 브랜치가 main에 머지될 때 반영)
- 검증에 쓴 시뮬레이터·측정 프로그램은 저장소에 없음(결과만 문서에 기록)
