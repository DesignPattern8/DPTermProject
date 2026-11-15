# [006] 2인용 셀 상태 표현과 `TwoPlayerBoard` 실제 구현

- 상태: 채택 (구현 완료 2026-11-15, `NeighborhoodTwoPlayerBoard`·`TwoPlayerResident`)
- 작성자: Son2kwon / 2026-11-15 / 관련 이슈: #5
- 대체하는 문서: [001 셀 상태 표현과 이웃 상태 조회](../../docs/001-cell-state-representation.md) (상태: 대체됨(→ twoplayer/006))
- 근거 명세: [rules.md](rules.md) 3장(세대 전이), 6장(셀 수), 8장 요구사항 1~3
- 코드 근거: `feature/two-player` `8dcec2f` (v1.0 = `origin/feature/base` + 2인용 커밋 7개). 파일 경로는 `Codes/GameOfLife/src/com/holub/life/` 아래 기준
- 측정 코드: `Codes/GameOfLife/test/com/holub/life/twoplayer/CellCandidateBenchmark.java` (아래 "측정 방법")

## 문제

게임 진행(`TwoPlayerGame`, 단계, `GameJudge`)은 `TwoPlayerBoard` 인터페이스와 대역 `StubBoard`로 구현·테스트를 마쳤다([003](003-game-phase.md), [005](005-cell-count-and-judge.md)). 이번에 정할 것은 **`TwoPlayerBoard`의 실제 구현**, 즉 셀 상태(`DEAD`/`A`/`B`)를 어디에 어떻게 둘 것인가이다.

001은 #25 직후 코드를 기준으로 "`Cell`에 `state()`를 추가한다(C)"를 제안했다. 그 뒤 전제가 두 가지 바뀌었다.

1. **v1.0에서 `Cell`이 커졌다.** 암세포 기능(PR #60, [cancer-feature-changes.md](../../docs/cancer-feature-changes.md))이 `Cell`에 `isCancer`·`setCancer`·`cureCancer`(`model/Cell.java:108-115`)를, `Cell.Memento`에 `markAsCancer`·`isCancer`(`model/Cell.java:81-85`)를 추가했다. `Cell`의 메서드는 14개이고, 구현체는 `Resident`, `Neighborhood`, `Cell.DUMMY`(`model/Cell.java:123`) 3개다.
2. **팀 합의가 정해졌다.** `life/model`(`Cell`, `Resident`, `Neighborhood`, `CompositeBoard`, `Board`)은 리팩토링 팀 영역이다. "현재 `Cell` 구조를 보고 진행하고, 리팩토링 팀에 필요한 변경은 이슈로 남긴다."

현재 코드에서 2인용과 관련된 사실:

| 위치 | 사실 | 영향 |
| --- | --- | --- |
| `model/Resident.java:12` | `final` 클래스 | 상속으로 2인용 셀을 만들 수 없다 |
| `model/Resident.java:13-16` | 상태가 boolean 4개(`amAlive`, `willBeAlive`, `isCancer`, `willBeCancer`) | 소유자를 담을 곳이 없다 |
| `model/Resident.java:56-64` | 클래식 규칙과 암 규칙이 `if`로 갈라져 있다 | 여기에 2인용 규칙을 넣으면 세 모드가 한 메서드에 섞이고, 클래식 특성 테스트가 직접 영향을 받는다 |
| `model/Resident.java:73-76` | 이웃이 `Resident` 또는 `DUMMY`인지 assert | 한 보드에 셀 종류를 섞을 때만 문제. `Neighborhood`는 프로토타입 하나로 채우므로 2인용 셀로만 된 보드에서는 걸리지 않는다 |
| `model/Neighborhood.java:50-56` | `Neighborhood(gridSize, prototype)`가 모든 칸을 `prototype.create()`로 채운다 (Prototype) | 새 `Cell` 구현을 넣어도 `Neighborhood`는 그대로 쓸 수 있다 |
| `model/Neighborhood.java:141-201` | 이웃 `Cell` 객체를 그대로 넘긴다(블록 경계는 `edge()`). 이웃의 타입을 보지 않는다 | 2인용 셀은 이웃을 자기 타입으로 받는다 |
| `model/Neighborhood.java:120-128`, `313-320` | 활성 블록만 다시 계산한다. `amActive`와 가장자리 표시는 `toggle()`이 켠다 | 셀 상태를 직접 바꾸고 `toggle()`을 거치지 않으면 다음 세대에 그 블록이 계산되지 않는다 |
| `model/Neighborhood.java:32`, `258` | `readingPermitted`, `nestingLevel`이 `static` | 모든 보드가 공유한다. 한 스레드 계약(`interfaces.md`)에서는 문제 없음 |
| `model/Neighborhood.java:379-395` | 암 기능 메서드는 `instanceof Resident`인 칸만 모은다 | 2인용 보드에서는 아무 칸도 찾지 않는다(부르지 않으므로 무해) |
| `model/Cell.java:3`, `93` | `Cell`이 `java.awt.Point`를 import하고 `transfer`가 `Point`를 받는다 | `Cell`을 구현하면 `twoplayer`에 `java.awt` 의존이 생긴다 (지금 `twoplayer` 패키지의 `java.awt` import는 0개) |
| `ui/BoardRenderer.java:39-42` | `instanceof Resident`일 때만 칠한다. 색은 `isAlive()`·`isCancer()` 삼항 | `Resident`가 아닌 셀은 화면에 그려지지 않는다 (#7 문제) |
| `model/Board.java:3`, `model/CompositeBoard.java:11` | `Board`는 "Classic board operations", `CompositeBoard`는 "built from Resident cells" | 아래 "`model.Board`와의 관계" |

## 후보

### A. 공용 enum + `Cell.state()` (001-A)

`CellState { DEAD, ALIVE, PLAYER_A, PLAYER_B }`를 model에 두고 `Cell`에 `state()`를 추가한다.

- 장점: 가장 단순하다.
- 단점: model 3개 파일(`Cell`·`DUMMY`, `Resident`, `Neighborhood`)과 렌더러를 고친다. 모드가 늘 때마다 공용 enum을 고친다. 리팩토링 팀 영역이다.

### B. 2인용 하위 인터페이스 `OwnedCell extends Cell` (001-B)

- 장점: model 수정 없음.
- 단점: 셀 구현 클래스가 `final`이고 보드의 모든 칸이 같은 종류라서, 하위 인터페이스가 하는 일이 F의 `instanceof 구체 클래스`와 같다. 타입만 하나 늘어난다.

### C. `CellState` 인터페이스 + 모드별 enum + `Cell.state()` (001-C, 001의 제안안)

- 장점: 구조상 가장 깔끔하다. 이후 모드는 자기 enum만 추가한다.
- 단점: `Cell`을 바꾼다. 리팩토링 팀 영역이고, v1.0에서 `Cell`이 암 메서드로 이미 커져 있어 같은 회의가 다시 필요하다.

### D. Visitor (001-D)

- 단점: 이웃 1개당 호출 2단계, `CellVisitor`가 모든 구체 셀을 알아야 해서 `model` ↔ `twoplayer` 순환 위험. model과 렌더러를 모두 고친다.

### E. 암세포 방식 — `Cell`·`Resident`에 소유자 메서드 추가

`Cell`에 소유자 조회·설정 메서드를 넣고 `Resident`에 필드와 2인용 분기를 추가한다. `Neighborhood`에 배치·집계 메서드를 추가한다.

- 장점: 이미 한 번 해 본 방식이다(같은 `Resident`·`Neighborhood` 재사용).
- 단점: PR #60이 이 방식으로 model 4개 파일을 고쳤다(아래 표). `Resident.figureNextState`가 세 모드를 분기하게 된다. model이 `twoplayer.Player`를 import할 수 없어서(순환) model 쪽에 소유자 타입을 따로 둬야 한다. 모든 `Cell` 구현에 할 일 없는 메서드가 또 생긴다.

### F. twoplayer 안에서 해결 — `Neighborhood` 재사용 + 2인용 셀 (model 무수정)

```java
// twoplayer 패키지
final class TwoPlayerResident implements Cell { ... }   // 소유자: Player (null = DEAD)
Neighborhood root = new Neighborhood(8, new Neighborhood(8, new TwoPlayerResident()));
// 이웃 색: neighbor instanceof TwoPlayerResident t && t.owner == Player.A
// 배치: 셀에 소유자를 지정해 둔 뒤 root.toggle(row, column) → 활성 블록 경로 유지
// 집계: 생성 때 childAt으로 만든 64×64 평면 색인을 순회
```

- 장점: model·ui 수정 0. `Neighborhood`의 Prototype·Composite 구조와 활성 블록 최적화를 그대로 쓴다. 이웃 조회는 한 private 메서드에 모여 있어서, 팀이 나중에 C를 채택하면 그 한 곳만 바꾼다.
- 단점: `Cell`의 14개 메서드를 모두 구현해야 한다(암 메서드 3개는 할 일 없음, `transfer`·`createMemento`는 미지원). `java.awt.Point` 의존이 생긴다. 배치가 "소유자 지정 → `toggle`" 두 단계다. 렌더러가 그리지 못한다(#7).

### G. twoplayer 안에서 해결 — 전용 격자 (`Cell` 미사용)

`int[64][64]` 두 장과 자체 세대 진행.

- 장점: 가장 빠르고 단순하다. model·`java.awt` 의존 0.
- 단점: 세대 진행 엔진을 중복으로 만든다. TP-R-10·11(`Neighborhood` 경계, 활성 블록)이 실제 보드가 아니라 별도 엔진을 검증하게 된다. 렌더러도 따로 만들어야 한다.

## `model.Board`와의 관계 (F를 전제로)

`GameController`는 `Board` 인터페이스만 쓰고(`application/GameController.java:23`, `48`, `55`, `69`, `79`), `Life.java:23`이 루트 `Neighborhood`를 `CompositeBoard`로 감싼다. F의 루트도 `Neighborhood`이므로 `CompositeBoard`를 재사용할 수 있는지 측정했다(측정 클래스 3번).

| `CompositeBoard` 메서드 | F의 루트를 감쌌을 때 (실측) | 원인 |
| --- | --- | --- |
| `rows()`, `columns()` | 64, 64 | 정상 |
| `step()` | 20세대 동안 직접 진행과 모든 칸이 같음 | `figureNextState && transition`만 부르므로 셀 종류와 무관 (`CompositeBoard.java:27-32`) |
| `clear()` | A=0, B=0 | 정상 |
| `toggle(position)` | DEAD 칸이 살아나지 않음 (`(10,10) = null`) | 소유자를 넘길 인자가 없다 (`Board.java:12`) |
| `snapshot()` | 살아 있는 5칸의 위치 5개, 소유자 없음 | `BoardSnapshot`이 `Set<CellPosition>`뿐 (`BoardSnapshot.java:6`) |
| `restore(snapshot)` | 살아 있던 5칸이 모두 DEAD (A=0, B=0) | `clear()` 뒤 `toggle()`로 되살리는데(`CompositeBoard.java:59-63`) 소유자가 없다 |

`TwoPlayerBoard` 구현과 `Board`의 관계 후보:

| 후보 | 내용 | 결과 |
| --- | --- | --- |
| W1. `CompositeBoard`를 감싸 `step`·`clear`·`rows`·`columns` 위임 | 내부 필드로 `CompositeBoard`를 두고 일부만 위임 | 줄이는 코드는 `step()` 본문 4줄 정도(추정). 대신 "Resident로 만든 클래식 보드"라는 `CompositeBoard`의 전제(`CompositeBoard.java:11`)를 어기는 쓰임이 되고, `toggle`·`snapshot`·`restore`는 쓰면 안 되는 메서드로 남는다 |
| W2. `TwoPlayerBoard` 구현이 `Board`도 구현(Adapter)해 `GameController`에 꽂음 | #7에서 기존 컨트롤러·UI를 그대로 쓰려는 안 | 깨진다. `GameController.tick()`이 `board.step()`을 직접 불러(`GameController.java:69`, `79`) `TwoPlayerGame.tick()`의 단계·K세대 카운트·전멸 판정을 건너뛴다. 클릭은 `toggle`(`:48`)이라 소유자가 없다. Load/Store는 `fileCells`의 memento 경로(`:113-135`)라 2인용 셀의 `transfer`가 할 일이 없다 |
| W3. `Neighborhood`를 직접 쓰고 `CompositeBoard`·`Board`는 쓰지 않음 | `step` 4줄을 2인용 보드에 둔다 | **채택** |

결정: **W3.** `TwoPlayerBoard` 구현은 루트 `Neighborhood`를 직접 갖고, `CompositeBoard`를 감싸지도 `Board`를 구현하지도 않는다.

- 재사용으로 줄일 수 있는 코드는 `step()` 4줄뿐이고, 그 대가로 클래식 전제를 가진 타입에 묶인다.
- `Board`의 6개 연산 중 3개(`toggle`, `snapshot`, `restore`)가 2인용에서 틀린 결과를 낸다(위 실측). `Board`를 구현하면 이 메서드들이 "부르면 조용히 틀리는" 메서드가 된다.
- **#7 영향**: `GameController`는 2인용에 그대로 쓸 수 없다. 2인용의 tick은 `TwoPlayerGame.tick()`이 주인이고(003), 입력은 `select`·`confirm`·`pass`(004)다. #7에서는 `Clock`은 재사용하고, `TwoPlayerGame`을 부르는 2인용 진입점을 따로 설계한다. Store 비활성(rules.md 8장)과도 맞는다.

## 오버헤드 비교표

- **실측**: 저장소 코드에서 센 값, `git diff --numstat`, 측정 클래스 출력. **추정**: 프로토타입이 없는 후보(A~E)의 값.
- LOC는 Google Java Style로 쓴 프로토타입 기준, 빈 줄·주석 제외.

| 항목 | A. 공용 enum | B. 하위 인터페이스 | C. `CellState` + 모드별 enum | D. Visitor | E. 암세포 방식 | **F. twoplayer + `Neighborhood`** | G. 전용 격자 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 구조 — 고칠 model 파일 | 3 (`Cell`·`DUMMY`, `Resident`, `Neighborhood`) (`Cell` 구현체 수에서 셈) | 0 | 3 + 새 파일 2 | 3 + `CellVisitor` | 실측 유사 사례: PR #60이 model 4개 파일 `Cell` +23, `Resident` +54/−14, `Neighborhood` +194, `NeighborhoodState` +23 | **0** | 0 |
| 구조 — 고칠 ui 파일 | `BoardRenderer` 1 | 1 | 1 | `BoardRenderer` 전체(45줄) 재작성 | 1 (PR #60: `BoardRenderer` +2/−1) | 0 (그리기는 #7) | 0 (그리기는 #7) |
| 구조 — 새 타입·LOC | enum 1 + 2인용 셀 (추정) | 인터페이스 1 + enum 1 + 셀 (추정) | 3 + 팔레트 1 (추정) | 1 + 구현 2~3 (추정) | 0~1 (추정) | 셀 1 + 보드 1. 실제 구현 `TwoPlayerResident` 105줄, `NeighborhoodTwoPlayerBoard` 87줄 (실측, 구현 전 프로토타입은 88줄·41줄) | 보드 1. 프로토타입 54줄 (실측) |
| 간접성 — 이웃 1개당 | 가상 호출 1 (추정) | `instanceof` + 호출 1 (추정) | 인터페이스 호출 1 (추정) | 가상 호출 2 (추정) | `isAlive` + 소유자 조회 (현재 암: `isAlive` + `isCancer`, `Resident.java:50-52`) | `instanceof` + 필드 읽기, 가상 호출 0 | 배열 읽기 |
| 결합도 | 모든 모드가 공용 enum에 의존 | `twoplayer` → `model` | `twoplayer` → `model.CellState` | `model` ↔ `twoplayer` 순환 위험 | model이 2인용 소유자 개념을 가짐 | `twoplayer` → `model`(`Cell`, `Neighborhood`, `Direction`, `NeighborhoodState`) | 없음 |
| `java.awt` 의존 (`twoplayer`) | 셀이 `Cell`을 구현하면 `Point` (추정) | `Point` | `Point` (추정) | `Point` (추정) | 없음 | `Point` 1개 (실측) | 없음 |
| 런타임 — tick당 | 측정 안 함 | 측정 안 함 | 측정 안 함 | 측정 안 함 | 측정 안 함 | **0.0591~0.0618 ms** (실제 구현) | **0.0314~0.0359 ms** |
| 런타임 — `count(owner)` 1회 | — | — | — | — | — | 0.0011~0.0021 ms (실제 구현) | 0.0009 ms |
| 테스트 용이성 | Swing 없이 가능 | 가능 | 가능 | Visitor 대역 필요 | 가능, 클래식 `Resident`를 거침 | 가능. 예시 4·TP-R-10·11 확인 (실측) | 가능 |
| 확장 — 다음 모드 추가 | 공용 enum + 모든 `switch` | 하위 인터페이스 + 렌더러 분기 | enum + 팔레트 | `CellVisitor` + 모든 구현 | model 4개 파일 다시 | 새 `Cell` 구현 1개, model 0 | 새 엔진 |
| 리팩토링 팀 영역과 충돌 | 있음 | 렌더러 | 있음 | 가장 큼 | 가장 큼 | **없음** | 없음 |
| `model.Board` 관계 | — | — | — | — | — | W3: `Neighborhood` 직접 사용 (`CompositeBoard`의 `toggle`·`snapshot`·`restore`가 소유자를 잃음, 실측) | `Board`와 무관 |

클래식 기준선(같은 보드, 같은 측정): tick당 **0.0570~0.0584 ms**.

측정값 갱신(2026-11-15): 위 F·G·클래식 값은 `CellCandidateBenchmark`의 F를 실제 구현으로 바꾼 뒤 다시 잰 값이다. 프로토타입 F일 때는 tick당 0.0623~0.0626 ms, `count` 0.0020~0.0021 ms, G 0.0337~0.0352 ms, 클래식 0.0577~0.0585 ms였다. 실제 구현의 `count`는 3번 중 2번이 약 0.0011 ms, 1번이 0.0021 ms로 실행마다 갈렸다(원인은 확인하지 않음).

### 측정 방법

```sh
cd Codes/GameOfLife
./gradlew testClasses
java -ea -cp build/classes/java/main:build/classes/java/test \
    com.holub.life.twoplayer.CellCandidateBenchmark
```

- 환경: Apple M4 Pro (12 코어), macOS, JDK 26.0.2.1 (aarch64), `-ea` 켬. 프로그램이 첫 줄에 환경을 출력한다.
- 보드: 64×64, 칸마다 확률 0.3으로 살림(`Random` 시드 42), 왼쪽 절반 A, 오른쪽 절반 B. 클래식은 같은 칸을 `toggle`.
- tick: 200 tick 진행 시간 / 200, 10번 반복 중 최소. 워밍업 3회. 표의 범위는 3번 실행한 결과.
- F는 실제 구현(`NeighborhoodTwoPlayerBoard`)을 쓴다. `CompositeBoard` 점검(3번)은 `NeighborhoodTwoPlayerBoard`가 루트를 감추므로, 같은 구성의 루트를 실제 `TwoPlayerResident`로 따로 만들어 감싼다.
- `count(owner)`: 워밍업 10,000회 뒤 10,000회 평균.
- `@Test`가 없어 `./gradlew test`에서는 실행되지 않는다(이 클래스를 넣은 뒤 `./gradlew test --rerun` 186개 통과, 결과 파일 없음 확인).

### 측정에서 확인한 것

- 동등성: 200세대 동안 F와 G의 칸별 소유자가 같고, F의 살아 있는 칸이 클래식 `Resident`와 같다(TP-R-15 불변조건).
- 명세 예시 4: 세대 0~10의 A:B가 5:4 ×7, 6:5, 3:3, 1:2, 0:0으로 rules.md 7장과 같다.
- TP-R-10: (7,7)·(7,8)에 A, (8,7)에 B → 1세대 뒤 블록 경계 건너편 (8,8)이 A.
- TP-R-11: 400세대 진행해 정체시킨 뒤 블록 경계 옆에 6칸을 배치 → 200세대 동안 F와 G가 같다. "소유자 지정 → `toggle`" 배치가 활성 블록 경로를 지킨다.
- `-ea`를 끄고 돌려도 클래식 0.0565~0.0576 ms, F 0.0607~0.0637 ms로 차이가 거의 없다(프로토타입 F 시점 측정).
- 같은 F를 이웃 8개를 펼쳐 쓰고 상태를 `int`로 둔 첫 프로토타입(저장소에 없음)은 0.048 ms였다. 구현 방식만으로 20% 안팎 차이가 나므로, F와 클래식의 차이(프로토타입 약 7%, 실제 구현 약 3~6%)는 후보 선택 근거로 쓰지 않는다. F와 G의 차이(약 0.028 ms/tick)는 K = 20 기준 라운드당 약 0.6 ms다.

## 결정과 근거

**F를 채택한다.** `TwoPlayerBoard`는 `twoplayer` 안의 2인용 셀과 `Neighborhood`로 구현하고, model·ui는 고치지 않는다. `CompositeBoard`·`Board`는 쓰지 않는다(W3).

- **팀 합의와 맞는다.** 현재 `Cell` 구조 위에서 동작하고, model 변경이 0이다. 필요한 변경은 아래 이슈로만 남긴다.
- **`Neighborhood`가 열어 둔 확장 지점을 쓴다.** 프로토타입 교체(`Neighborhood.java:50-56`)만으로 Composite 구조와 활성 블록 최적화를 그대로 쓴다. 측정에서 동등성·예시 4·TP-R-10·11이 모두 맞았다.
- **G를 기각한 이유**: 가장 빠르지만(라운드당 약 0.6 ms 차이로 체감 불가) 세대 진행 엔진을 중복으로 만들고, TP-R-10·11이 실제 보드 구조를 검증하지 못한다. 렌더러도 처음부터 다시 만들어야 한다.
- **E를 기각한 이유**: PR #60과 같은 방식이고, 그 결과(model 4개 파일 수정, `Resident`의 모드 분기, 모든 구현에 할 일 없는 메서드)가 지금의 문제다. 세 번째 모드를 넣으면 더 나빠진다.
- **A·C·D를 기각한 이유**: 모두 model 변경이 필요해 리팩토링 팀의 결정이다. C는 구조상 가장 낫지만 팀 합의에 따라 이슈로 넘긴다. F의 이웃 조회는 한 private 메서드에 모여 있어서, 팀이 C를 채택하면 그 한 곳만 바꾸면 된다.
- **B를 기각한 이유**: 셀 클래스가 `final`이고 보드의 모든 칸이 같은 종류라, 하위 인터페이스는 F의 `instanceof`와 같은 일을 하면서 타입만 하나 늘린다.

## 감수하는 비용

- `twoplayer`에 처음으로 `java.awt` 의존(`java.awt.Point`)이 생긴다. `Cell.transfer`의 시그니처 때문이다(`Cell.java:3`, `93`). Swing 의존은 없다.
- 2인용 셀은 `Cell`의 14개 메서드를 모두 구현한다. 암 메서드 3개는 할 일 없음(`DUMMY`와 같은 방식, `Cell.java:166-172`), `transfer`는 false, `createMemento`는 `UnsupportedOperationException`(`Resident.createMemento`와 같은 방식). 2인용 저장은 rules.md 8장에서 지원하지 않으므로 충돌하지 않는다.
- 배치가 "셀에 소유자 지정 → `Neighborhood.toggle`" 두 단계다. 보드 클래스 안에 숨긴다.
- `step()` 4줄이 `CompositeBoard.step()`과 중복된다(W3).
- `Neighborhood`의 `static` 잠금(`Neighborhood.java:32`)을 클래식 보드와 공유한다. 한 스레드 계약 안에서는 문제가 없다.

## 리팩토링 팀에 이슈로 남길 것

이번 작업에서는 고치지 않는다. (a)~(c)는 model·ui, (d)는 application(B 도메인)에 관한 것이다.

- **(a) 렌더러가 `Resident`만 그린다.** `BoardRenderer.java:39`의 `instanceof Resident` 분기에 맞지 않는 셀은 조용히 그려지지 않는다. #7에서 2인용 렌더러를 `twoplayer` 안에 따로 만들지(`Neighborhood`의 공개 API `childAt`·`gridSize`·`isActive`·`beginRedraw`, `Neighborhood.java:285-303` 사용), 셀 종류에 의존하지 않는 렌더링을 팀에 요청할지 정해야 한다.
- **(b) `Cell` 인터페이스가 모드별 메서드로 커진다.** 암 메서드 3개(`Cell.java:108-115`)와 `Memento`의 암 메서드 2개(`Cell.java:81-85`)를 모든 구현이 갖는다. 2인용 셀도 할 일 없는 구현을 3개 둔다(인터페이스 분리 문제).
- **(d) #7은 `GameController`가 아닌 2인용 진입점이 필요하다 (B 도메인 담당에게 알림).** W2·W3 결과, 2인용 보드는 `Board`를 구현하지 않고 `GameController`를 거치지 않는다. `GameController.tick()`(`GameController.java:65-90`)이 `board.step()`을 직접 부르면 `TwoPlayerGame`의 단계·K세대 카운트·전멸 판정을 건너뛰기 때문이다. #7에서는 `Clock`만 재사용하고 `TwoPlayerGame`의 `select`·`confirm`·`pass`·`tick`을 부르는 진입점을 따로 둔다. `GameController`(application, B 도메인)는 바꾸지 않는다. 다만 메뉴·패널이 컨트롤러를 어떻게 고르는지는 B 도메인 담당과 #7에서 맞춘다.
- **(c) model API가 `java.awt.Point`를 노출한다.** `Cell.transfer`와 `Cell.Memento`(`Cell.java:3`, `76-93`)가 `Point`를 쓴다. `Cell`을 구현하는 쪽은 모두 `java.awt`에 의존하게 된다.

## 전/후 비교

```java
// 전: 테스트 대역만 있음 (test/com/holub/life/twoplayer/StubBoard.java)
final class StubBoard implements TwoPlayerBoard { ... }   // advanceGeneration()은 보드를 바꾸지 않음

// 후: twoplayer 패키지 (클래스 이름은 구현 때 확정)
final class TwoPlayerResident implements Cell { ... }
public final class NeighborhoodTwoPlayerBoard implements TwoPlayerBoard {
  private final Neighborhood root = new Neighborhood(8, new Neighborhood(8, new TwoPlayerResident()));
  // place: 소유자 지정 → root.toggle(row, column)
  // advanceGeneration: root.figureNextState(DUMMY × 8) && root.transition()
  // count: 평면 색인 순회
}
```

클래스 다이어그램은 구현 PR에서 `diagrams/`에 추가한다(AGENTS.md 2-5).

## 후속 작업

- 테스트 케이스: test-cases.md 1장 TP-R-01~18을 이 구조 기준으로 다시 보고(예정 클래스 `TwoPlayerRuleTest`), 보드 계약(`isEmpty`, `hasEmptyCell`, `place`, `count`) 케이스를 추가한 뒤 테스트 → 구현 순서로 진행한다.
- 게임 진행 테스트(TP-E)의 예시를 실제 보드로 다시 확인한다(005 "구현 후 실측"의 남은 일).
- 이슈 (a)~(c)를 리팩토링 팀에, (d)를 B 도메인 담당에게 남긴다.
- #7: 2인용 렌더러, `TwoPlayerGame`을 부르는 2인용 진입점. `GameController`는 쓰지 않는다(W3, 이슈 (d)).
- #7: 렌더러가 `owner()`를 `TwoPlayerBoard` 인터페이스로 읽어야 하는지 정한다(그러면 `StubBoard`에도 필요하다). 지금은 `NeighborhoodTwoPlayerBoard`에만 있다([interfaces.md](interfaces.md)).
- ~~실제 2인용 셀·보드가 `src`에 들어오면 `CellCandidateBenchmark`의 F 쪽을 실제 클래스로 바꾼다.~~ 완료(2026-11-15). `PrototypeCell`을 지우고 F는 `NeighborhoodTwoPlayerBoard`를 쓴다. G와 클래식 기준선은 비교용으로 남겼다.
- ~~구현 후 이 문서의 F 열을 실제 클래스의 LOC·tick 시간으로 다시 적는다.~~ 완료(오버헤드 비교표, "측정값 갱신").
