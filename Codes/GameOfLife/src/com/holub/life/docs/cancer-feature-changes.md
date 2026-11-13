# 암세포 기능 변경 요약 (PR #60)

- 상태: 머지 시점 기록. 설계 결정(A/B 비교) 문서는 PR 작성자가 따로 작성 중이다.
- PR #60 `feat: 암세포 발생과 전이 및 치료 기능 추가` (작성: curiousio)
  - 충돌 해결: `6eb87f0`
  - 테스트 추가: `8cd3442`
- 이 문서는 무엇이 어디에 들어갔는지만 정리한다. 왜 이렇게 설계했는지는 위 결정 문서에서 다룬다.

## 1. 배경: 왜 코드 위치가 PR 원본과 다른가

PR #60은 #25(도메인 구조 변경) 이전 구조를 기준으로 작성되었다. 그래서 `Universe`, `Cell`, `Resident`, `Neighborhood`를 직접 고쳤다. 그 사이 main에서 다음과 같이 바뀌었다.

- `Universe`가 삭제되었다.
- 클래스가 `model`, `application`, `ui`, `persistence` 패키지로 나뉘었다.
- 그리기 코드는 `ui/BoardRenderer`로 옮겨졌다.
- 저장 형식 코드는 `persistence/LegacyLifeCodec`으로 옮겨졌다.

이 때문에 단순 병합이 되지 않았다. PR의 기능을 새 구조의 각 위치로 옮겨 심었다. 규칙과 수치(확률, 생존 조건, 치료 횟수, 승패 기준)는 PR 원본 그대로다.

| PR 원본 위치 | 옮긴 위치 |
| --- | --- |
| `Cell` 암 상태 메서드, `Cell.Memento` 암 저장 메서드 | `model/Cell.java` |
| `Resident` 암 상태·규칙·저장 | `model/Resident.java` |
| `Resident.redraw()`의 암세포 색(마젠타) | `ui/BoardRenderer.java` |
| `Neighborhood` 암 발생·전이·치료·개수 | `model/Neighborhood.java` |
| `Neighborhood` 안의 `CellPosition` 내부 클래스 | 삭제. main에 생긴 `model/CellPosition` record를 사용한다(이름이 같아서). |
| `NeighborhoodState`의 `cancerCells`, `StateData` | `model/NeighborhoodState.java`, `persistence/LegacyLifeCodec.java`의 `CancerStateData` |
| `Universe`의 tick 처리(단계별 확률, 승패, 치료 횟수) | 새 클래스 `application/CancerGame.java` |
| `Universe`의 우클릭 치료 | `ui/BoardPanel.java` → `GameController.treat()` |
| `Universe.finishGame()`(시계 정지, 결과 메시지) | 시계 정지: `GameController.tick()`. 메시지 창: `ui/LifeFrame.java` |

## 2. 공개 인터페이스 변경

다른 도메인이 쓰게 되는 것만 적는다.

**model**
- `Cell`: `isCancer()`, `setCancer()`, `cureCancer()`
  - `Cell.DUMMY`와 `Neighborhood`에서는 각각 항상 false, 아무 동작 안 함, 아무 동작 안 함이다.
- `Cell.Memento`: `markAsCancer(Point)`, `isCancer(Point)`
- `Neighborhood`: `tryNaturalCancer(double, Random)`, `applyInvasion(double, Random)`, `tryCancerOutbreak(double, Random)`, `treatAt(int row, int column)`, `cancerCount()`
- `NeighborhoodState`: `cancerCells()`, `replaceCells(Collection, Set<Point>)`
  - 기존 `replaceCells(Collection)`은 암세포 목록을 비운다.

**application**
- 새 클래스 `CancerGame`
  - `afterGeneration()`: 게임이 이번 호출에서 끝났으면 true를 돌려준다.
  - 그 밖에 `treat(row, column)`, `isOver()`, `resultMessage()`, `treatmentCharges()`
- `GameController`
  - 생성자 `GameController(Board, Cell, Clock, CancerGame)` 추가. 기존 3인자 생성자는 그대로 있고, 이 경우 암 규칙이 꺼진다.
  - `treat(row, column)`, `gameResultMessage()` 추가
- `GameListener.Change.GAME_OVER` 추가

**ui**: 공개 API 변경은 없다. 동작만 바뀌었다.
- 오른쪽 클릭은 치료, 왼쪽 클릭은 기존 토글이다.
- 게임이 끝나면 결과 메시지 창이 뜬다.

**조립**: `Life.main()`이 `CancerGame(board, new Random())`을 만들어 `GameController`에 넣는다. 따라서 실행하면 항상 암세포 모드다.

## 3. 동작

PR 원본과 같다.

**세포 규칙** (`Resident.figureNextState`)
- 일반 세포는 기존 Conway 규칙을 따른다.
- 암세포는 살아 있는 이웃이 1~5개면 살아남고 암세포로 남는다. 그 밖에는 죽는다.
- 빈칸에 이웃 암세포가 3~4개 있으면 그 칸에 암세포가 태어난다.

**한 세대 순서** (`GameController.tick()` → `CancerGame.afterGeneration()`)
1. `Board.step()`으로 기존 세대 진행
2. 세대 수에 따른 단계를 정한다. 100세대마다 한 단계씩 오르고 4단계가 최대다. 단계별 발생 확률은 0.001, 0.005, 0.02, 0.05다.
3. 자연 발생: 위 확률에 걸리면 살아 있는 일반 세포 하나를 암세포로 바꾼다.
4. 전이: 이웃 암세포가 n개인 일반 세포는 확률 `1 - 0.97^n`으로 감염된다. 감염될 칸을 먼저 모은 뒤 한꺼번에 바꾸므로 한 세대에 한 칸씩만 번진다.
5. 승패 판정
   - 암세포가 1024개(64×64 보드의 25%) 이상이면 패배다.
   - 게임이 시작된 뒤 암세포가 한 번이라도 나타났고 지금 0개면 승리다.
6. 끝나지 않았으면 집단 발생을 시도한다. 확률은 3과 같고, 걸리면 무작위로 고른 일반 세포와 8방향으로 이어진 일반 세포 묶음 전체가 암세포가 된다.
7. 화면 전체를 다시 그린다(`EDITED`). 게임이 끝났으면 시계를 멈추고 `GAME_OVER`를 알린다. 끝난 뒤의 tick은 아무것도 하지 않는다.

**치료**
- 클릭한 칸을 중심으로 3×3 안의 암세포를 죽인다. 일반 세포는 건드리지 않는다.
- 게임당 3회까지다. 횟수가 없으면 아무 일도 일어나지 않는다.

## 4. 저장 형식

| 보드 상태 | 기록되는 객체 |
| --- | --- |
| 암세포 없음 | 기존과 같은 `Collection<Point>` |
| 암세포 있음 | `LegacyLifeCodec.CancerStateData`(생존 좌표 전체 + 암세포 좌표). 암세포 좌표는 생존 좌표에도 들어 있다. |

- PR 원본은 항상 새 형식으로 저장했다. 옮기면서 암세포가 없을 때는 기존 형식으로 저장하도록 바꿨다. 이유는 기존 특성 테스트 CL-M-04, CL-M-10과 `GameControllerTest`가 "저장 파일 = 직렬화된 `Collection<Point>`"를 고정하고 있어서다.
- 불러오기는 두 형식을 모두 읽는다.
- 두 형식 모두 원소가 `Point`가 아니면 `IOException`을 던진다(기존 CL-M-12와 같은 기준).
- PR 원본처럼 `ObjectOutputStream`을 쓴 뒤 `flush()`한다.

## 5. 테스트

정의는 각 도메인의 `test-cases.md`에 있다.

| 도메인 | 케이스 | 테스트 클래스 | 실행 수 |
| --- | --- | --- | --- |
| model | [CAN-M01~14](../model/docs/test-cases.md#암세포-모델-pr-60) | `model/CancerModelTest` | 25 (M01은 9개, M02는 4개 파라미터 포함) |
| persistence | [CAN-P01~04](../persistence/docs/test-cases.md#암세포-저장-pr-60) | `persistence/CancerCodecTest` | 4 |
| application | [CAN-A01~08, CAN-C01~04](../application/docs/test-cases.md#암세포-게임-규칙-pr-60) | `application/CancerGameTest`, `application/GameControllerCancerTest` | 12 |

- 전체 결과: 144개 통과, 실패 0개(로컬 `./gradlew test`와 CI 모두).
- 기존 테스트는 고치지 않았다.
- 무작위 값은 항상 같은 값을 돌려주는 `Random` 대역으로, 시계는 수동 `TickSource`로 고정했다.
- 기존 `GameController` 테스트는 3인자 생성자를 써서 암 규칙이 꺼진 상태로 돈다. 그래서 무작위 값의 영향을 받지 않는다.

## 6. 알려진 한계와 후속 작업

아래 문제들은 고치지 않았고, 테스트로 고정하지도 않았다. 리팩토링·병합과 버그 수정을 섞지 않기 위해서다(AGENTS.md 3-2 강령 2).

- **암 연산 후 블록이 비활성으로 남음**
  - 현상: `setCancer()`, `cureCancer()`와 이를 쓰는 발생·전이·치료는 `Resident`만 바꾸고, 그 칸이 속한 `Neighborhood`를 활성으로 표시하지 않는다.
  - 영향: 안정 상태였던 블록에서 암세포가 생기거나 치료되어도 다음 세대 계산에서 그 블록이 빠질 수 있다(추정, 재현 테스트 없음).
  - PR 원본에도 같은 동작이 있다.
- **토글한 암세포의 암 상태가 남음**
  - 현상: 왼쪽 클릭(`Resident.toggle`)은 생사만 뒤집는다.
  - 영향: 암세포를 클릭해 죽여도 암 상태가 남는다. 그 칸을 다시 클릭하면 암세포로 살아난다.
  - `figureNextState`는 생사보다 `isCancer`를 먼저 보고 암세포 규칙을 쓴다. 그래서 죽은 칸이라도 암 상태가 남아 있으면, 살아 있는 이웃이 1~5개일 때 다음 세대에 암세포로 되살아난다(코드로 확인, 재현 테스트 없음).
- **승리 판정은 게임 시작 뒤에 본 암세포만 반영함**
  - 현상: 승리 판정의 "암세포가 나타났음"은 게임이 시작된 뒤 `afterGeneration()`에서 센 경우만 친다.
  - 영향: 불러온 파일에만 있던 암세포를 새 암세포가 생기기 전에 모두 치료하면 승리로 끝나지 않는다.
  - CAN-A03이 이 동작을 고정한다.
- **암 게임 상태가 초기화되지 않음**
  - 현상: Clear와 Load는 세대 수, 치료 횟수, 승패 상태를 초기화하지 않는다.
  - PR 본문의 후속 작업에도 같은 항목이 있다.
- **문서 미비**
  - 남은 것: 설계 결정 문서(A/B 비교), 도메인별 `interfaces.md`, 다이어그램. AGENTS.md 1-2 기준으로는 FEAT PR에 함께 들어가야 하지만, 팀 합의로 머지를 먼저 했다.
