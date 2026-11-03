# 작업 기록 (Son2kwon)

- 담당: 2인용 모드 + 팀 공용 테스트 환경·클래식 특성 테스트
- 기간: 2026-11-03 ~
- 상세 근거는 각 도메인 `docs/`에 있고, 이 문서는 진행 순서와 결과만 모은다(AGENTS.md 2-1).

## 한눈에 보기

| 단계 | 이슈 | 브랜치 | PR | 상태 |
| --- | --- | --- | --- | --- |
| 0. 테스트 환경 (Gradle + JUnit 5 + CI) | #2 | `chore/test-setup` | #3 | 머지됨 |
| 1. 클래식 특성 테스트 | #4 | `test/classic-characterization` | #13 | 리뷰 대기 (CI 통과) |
| 2. 2인용 셀 상태·전이 규칙 | #5 | `feature/two-player-mode` | — | Cell 인터페이스 회의 대기 |
| 3. 2인용 게임 진행 모델 | #6 | — | — | 대기 |
| 4. 2인용 UI 통합 | #7 | — | — | 대기 |
| 5. 2인용 문서·다이어그램 정리 | #8 | — | — | 대기 |
| 버그: Store 중복 기록 | #10 | — | — | 등록만 |
| 버그: 불러오기 ClassCastException | #11 | — | — | 등록만 |
| 버그: `Resident.transition()` 반환값 뒤집힘 | #12 | — | — | 등록만 |

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

## 1. 클래식 특성 테스트 (#4 → PR #13)

**목적**: 2인용 기능을 넣기 전에 클래식 모드의 현재 동작을 고정한다(강령 1). 프로덕션 코드는 바꾸지 않았다.

**순서**: 케이스 정의([`life/docs/test-cases.md`](../Codes/GameOfLife/src/com/holub/life/docs/test-cases.md)) 커밋 → 테스트 커밋 (강령 3)

**구성**

- 도우미 `ClassicBoard`: `Universe`와 같은 셀 구성(8×8 안에 8×8)을 Swing 없이 만들고, 마우스·Clear·Tick·Store·Load가 모델에 하는 호출을 `Universe` 코드와 같은 순서로 재현
- 도우미 `ReferenceLife`: 비교 기준용 단순 B3/S23
- 테스트 33개: 세대 진행 10, 마우스 토글·Clear 7, 저장·불러오기 9, 셀 단위 7

**결과**: 로컬·CI 모두 34개 통과(새 33 + 스모크 1). 무작위 보드(시드 42) 5개 × 100세대, R-pentomino 300세대, 가장자리에 닿는 glider 240세대가 매 세대 단순 구현과 같았다.

**한계**: `Universe`·`Clock`은 생성자에서 메뉴를 등록하고, 메뉴는 `JFrame`이 있어야 해서 headless에서 만들 수 없다. Go 메뉴(속도·Tick), Exit, 파일 선택 대화상자는 직접 테스트하지 못했고, 각 메뉴가 모델에 하는 호출만 재현했다.

### 발견한 버그 3건 (고치지 않고 현재 동작으로 고정, 강령 2)

| 이슈 | 내용 | 위치 | 고정한 테스트 |
| --- | --- | --- | --- |
| #10 | Store가 살아 있는 칸을 두 번씩 기록. `createMemento()`가 이미 transfer한 뒤 `doStore()`가 한 번 더 transfer. 불러온 결과는 같음 | `Universe.java:180-181` | CL-M-06 |
| #11 | `Collection`이 아닌 파일을 불러오면 `ClassCastException`. `doLoad`는 `IOException`만 잡아 "Read Failed!"가 안 뜸 | `Neighborhood.java:421`, `Universe.java:166` | CL-M-08 |
| #12 | `Resident.transition()`이 "바뀌었는가"의 반대 값을 반환 | `Resident.java:79-83`, `Neighborhood.java:232-235` | CL-C-03 |

#12의 영향은 측정해서 확인했다. `Resident` 상태 로직을 복사하고 반환값만 바꿀 수 있게 한 셀을 실제 `Neighborhood`(64×64)에 넣고, tick마다 셀 `figureNextState` 호출 수를 셌다(JDK 26, `-da`, 프로덕션 코드 변경 없음).

| 패턴 | 현재(뒤집힘), tick 2부터 | 고친 경우 |
| --- | --- | --- |
| blinker | 4096 | 192 |
| glider | 4096 | 64 |
| block(정물) | 0 | 0 |

- 활성 블록 계산(`activeEdges`)이 거의 항상 "모든 방향"이 돼서, 움직이는 것이 있으면 64개 블록 전체를 매 tick 다시 계산한다(재계산 생략 최적화가 사실상 꺼짐).
- 다시 그리기(`refreshNow`) 호출 횟수는 두 경우가 같았다(20 tick 중 20).
- 보드 결과는 같다. 고친 경우도 무작위 보드 5개 × 100세대에서 단순 구현과 같았다.

## 2인용 모드 준비 (`feature/two-player-mode`)

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

- [`twoplayer/docs/test-cases.md`](https://github.com/DesignPattern8/DPTermProject/blob/feature/two-player-mode/Codes/GameOfLife/src/com/holub/life/twoplayer/docs/test-cases.md): 66개 (전이 규칙 18, 게임 진행 17, 배치 15, 승패 8, UI 8). 테스트 메서드 이름은 설계 결정 뒤 확정.
- 명세 7장 예시 1~4의 기대값은 별도 Python 시뮬레이터로 다시 계산해 일치를 확인했다.

## 남은 일

- PR #13 리뷰·머지
- Cell 인터페이스 회의 → 001 상태 변경(채택/기각) → #5 구현
- #6 게임 진행 모델: 단계 전환·차례·패스는 셀 색과 무관해서 회의 전에 먼저 시작할 수 있음
- 버그 #10, #11, #12: 각각 별도 브랜치·PR로 수정, 해당 특성 테스트 기대값만 그 PR에서 변경
- 도메인 `docs/README.md`(`life/docs/`, `twoplayer/docs/`) 아직 없음
- 검증에 쓴 시뮬레이터·측정 프로그램은 저장소에 없음(결과만 문서에 기록)
- 다이어그램: 코드 변경이 시작되는 #5부터 전/후 다이어그램 추가

## 진행하면서 있었던 실수

- 로컬 테스트 확인 전에 push한 적이 있다(#13). 명령의 `exit=1`이 Gradle이 아니라 `grep`의 종료 코드였고, 다시 돌려 Gradle 종료 코드 0과 34개 통과를 확인했다.
- 커밋하지 않은 `.gitignore` 수정이 있는 상태에서 `git checkout -- .`가 포함된 명령을 실행하려다 차단됐다. 이후 기존 파일을 건드리지 않는 방식(임시 테스트 파일 추가 후 삭제)으로 확인했다.
