# [001] 셀 상태 표현과 이웃 상태 조회

- 상태: 제안 (Cell 인터페이스 회의에서 확정 예정)
- 작성자: Son2kwon / 2026-11-03 / 관련 이슈: #5
- 변경 이력
  - 2026-11-03: 최초 작성
  - 2026-11-04: #25(도메인 구조 변경) 이후 코드 기준으로 경로·줄 번호 갱신. 그리기가 `ui/BoardRenderer`로 옮겨진 것을 후보 비교와 오버헤드 표에 반영하고 D(Visitor)를 다시 평가. 제안(C)은 유지
- 관련 명세: 2인용 Life Game 규칙 명세(`twoplayer/docs/rules.md`) 3장, 8장 요구사항 1·2
- 코드 근거: `feature/two-player-mode` `945723d`(= main `b1a1103`의 core 코드). 파일 경로는 `Codes/GameOfLife/src/com/holub/` 아래 기준
- 이 문서를 core의 `docs/`(`life/docs/`)에 두는 이유: 바뀌는 대상이 `Cell` 인터페이스이고, 2인용 외의 모드도 영향을 받기 때문이다(AGENTS.md 2-2). #25 이후 `Cell`은 `life/model/`에 있다(위치는 "회의에서 정할 것" 7번)

## 문제

2인용 모드는 셀 상태가 `DEAD` / `A` / `B` 세 가지이고, 탄생할 때 이웃 3개 중 다수인 색을 따른다(명세 3장). 화면에는 A·B를 다른 색으로 그려야 한다(명세 8장 요구사항 1). 지금 코드로는 이것을 표현할 수 없다.

1. **셀 상태가 boolean 하나다.** `Resident`는 `amAlive`, `willBeAlive` 두 boolean만 가진다(`life/model/Resident.java:13-14`). 소유자를 담을 곳이 없다.
2. **이웃의 종류를 물어볼 방법이 없다.** `Resident.figureNextState()`는 이웃을 `Cell` 타입으로 받아 `isAlive()`만 호출한다(`life/model/Resident.java:45-52`). `Cell` 인터페이스에 상태를 묻는 메서드는 `isAlive()`뿐이다(`life/model/Cell.java:50`).
3. **그리는 쪽도 셀 종류를 `instanceof`로 고르고, 색은 `isAlive()`만 본다.** #25 이후 그리기는 모델 밖 `ui/BoardRenderer`로 옮겨졌다. `BoardRenderer.redraw()`는 `instanceof Neighborhood`면 하위 셀을 재귀로 그리고, `instanceof Resident`면 `isAlive()`로 빨강/노랑을 고른다(`life/ui/BoardRenderer.java:13-48`, 색은 41행). **두 조건 어디에도 맞지 않는 셀은 아무것도 그려지지 않는다.** 그래서 2인용 셀을 `Resident`가 아닌 클래스로 만들면 화면에 나오지 않고, `Resident`를 쓰더라도 A·B 색을 고를 정보가 없다.

코드에서 확인한 사실(설계에 영향을 주는 것):

- `Cell` 구현체는 3개다: `Resident`, `Neighborhood`, `Cell.DUMMY`(익명 클래스, `life/model/Cell.java:108`).
- `figureNextState()`에 이웃으로 넘어오는 객체는 `Resident` 또는 `Cell.DUMMY`뿐이다. `Resident.verify()`가 assert로 이를 검사한다(`life/model/Resident.java:58-61`). `Neighborhood`는 `edge()`로 가장자리 `Resident`를 꺼내 넘긴다(`life/model/Neighborhood.java:139-184`).
- `Neighborhood.isAlive()`는 항상 `true`를 반환한다(`life/model/Neighborhood.java:318-320`). 이웃 수 계산에는 쓰이지 않고, `BoardRenderer`도 `Resident` 분기에서만 `isAlive()`를 부른다. `Neighborhood.isAlive()`를 호출하는 곳은 저장소 안에 없다.
- #25가 정리한 모델 계약(`life/model/docs/interfaces.md`)에 `Cell.toggle`, `Neighborhood.childAt` 등이 추가됐다. `ui`는 `childAt`으로 받은 셀을 읽기·좌표 변환에만 쓴다.
- `twoplayer` 패키지는 지금 core 클래스를 하나도 import하지 않는다. 아래 A·C·D는 2인용 쪽이 처음으로 `life/model`에 의존하게 만든다.
- `Cell`은 1인용 모드(다른 팀원 작업)도 함께 쓰고, `BoardRenderer`는 UI 분리(#9) 담당이 다루는 `ui` 패키지에 있다. 어느 쪽을 바꿔도 그쪽 작업과 맞춰야 한다.

## 후보

네 후보 모두 기존 `isAlive()`는 그대로 두므로 클래식 모드 코드(`Resident.figureNextState`)는 바꾸지 않아도 된다. 각 후보마다 **이웃 조회**와 **그리기(렌더러)** 두 곳을 함께 본다.

### A. 공용 enum + `Cell.state()` 추가

```java
public enum CellState { DEAD, ALIVE, PLAYER_A, PLAYER_B }   // 모드가 늘면 상수 추가
interface Cell { ... CellState state(); }
// 렌더러: Neighborhood가 아니면 state()를 switch해서 색을 고름
```

- 장점: 가장 단순하다. 캐스팅이 없고 `switch`로 바로 쓸 수 있다. 렌더러의 `instanceof Resident` 분기를 "`Neighborhood`가 아닌 모든 셀"로 넓히고 `state()`로 색을 고르면 된다.
- 단점: 모든 모드의 상태가 enum 하나에 모인다. 암세포 모드 등을 추가할 때마다 공용 enum과, 그 enum을 `switch`하는 곳(전이 규칙, **렌더러**)을 모두 고쳐야 한다. 1인용 모드 셀이 `PLAYER_A`를 가질 수 있는 것처럼 보이는 등 타입이 모드를 구분해 주지 않는다.

### B. `Cell`은 그대로, 2인용 전용 하위 인터페이스 (`instanceof` 판별)

```java
interface OwnedCell extends Cell { Owner owner(); }       // Owner { A, B }
// 2인용 셀의 figureNextState 안에서
Owner o = (n instanceof OwnedCell oc && oc.isAlive()) ? oc.owner() : null;
// 렌더러: instanceof OwnedCell 분기를 하나 더 추가
```

- 장점: `Cell`을 전혀 바꾸지 않는다. 1인용 작업과 충돌이 없어서 회의 결과를 기다리지 않아도 된다.
- 단점: 이웃 8개마다 `instanceof`와 캐스팅이 필요하다. 이웃이 `OwnedCell`이라는 보장이 런타임에만 있다(보드에 다른 셀이 섞이면 조용히 "소유자 없음"으로 처리된다). **렌더러의 `instanceof` 사슬이 모드마다 한 칸씩 길어지고**, 분기를 빠뜨리면 컴파일 오류 없이 셀이 그려지지 않는다(위 문제 3). 암세포 모드도 같은 방식이면 하위 인터페이스와 렌더러 분기가 하나씩 더 생긴다.

### C. 상태 인터페이스 + 모드별 enum, `Cell.state()` 추가

```java
public interface CellState { boolean isAlive(); }                 // life/model
enum ClassicState implements CellState { DEAD, ALIVE; ... }       // life/model: DUMMY, Resident
enum TwoPlayerState implements CellState { DEAD, A, B; ... }      // twoplayer: 2인용 셀
interface Cell { ... CellState state(); }
// 렌더러: Neighborhood가 아니면 state()를 모드가 넘겨준 팔레트(CellState → Color)로 칠함
```

2인용 규칙은 `n.state().isAlive()`로 이웃 수를 세고, `n.state() == TwoPlayerState.A`로 색을 센다. 동일성 비교라 캐스팅이 필요 없고, `DUMMY`가 `ClassicState.DEAD`를 반환해도 결과가 맞다. 렌더러는 상태별 색을 모드에서 받는다. 색(`java.awt.Color`)은 렌더러 쪽에 두고 `CellState`에는 넣지 않는다(모델이 `java.awt`에 더 의존하지 않도록).

- 장점: `Cell`은 한 번만 바꾸고, 이후 모드를 추가할 때는 자기 enum과 팔레트만 만든다(공용 파일·렌더러 수정 없음). 모드마다 상태 타입이 분리된다. 렌더러의 `instanceof Resident`가 "`Neighborhood`가 아닌 셀"로 바뀌어 새 셀 종류도 빠지지 않고 그려진다.
- 단점: `Cell`을 바꾸므로 1인용 작업과 회의로 맞춰야 한다. 렌더러(`ui`)를 한 번 고쳐야 하고, 팔레트를 넘기는 방법을 #9 담당과 정해야 한다. A보다 타입이 하나 더 많다. 모드별 enum이 섞여 있으면 `switch` 하나로 모든 상태를 처리할 수 없다(2인용 코드는 자기 enum만 다루므로 실제로는 문제가 작다고 본다).

### D. Visitor (이중 디스패치로 셀 종류 판별)

```java
interface CellVisitor { void visit(Neighborhood n); void visit(Resident r); void visit(TwoPlayerResident r); void visitDummy(); }
interface Cell { ... void accept(CellVisitor v); }
// 이웃 조회용 Visitor, 렌더러(= BoardRenderer를 Visitor로), 셀 수 집계용 Visitor
```

- 장점: 캐스팅·`instanceof` 없이 구체 타입별로 처리한다. **#25 이후에는 쓸 곳이 둘 이상이다.** 렌더러의 `instanceof` 사슬을 Visitor로 바꾸면, 셀 종류를 추가할 때 처리하지 않은 곳이 **컴파일 오류**로 드러난다(B·현재 코드처럼 조용히 안 그려지는 일이 없다). 셀 수 집계(#6 [005](../twoplayer/docs/005-cell-count-and-judge.md) 2절의 `CountingVisitor` 후보)도 같은 구조를 쓸 수 있다.
- 단점:
  - 이웃 하나를 볼 때마다 `accept` → `visit` 호출 2단계가 생긴다. 이웃 조회는 활성 셀마다 8번 부르는 가장 자주 쓰는 경로다.
  - `CellVisitor`가 모든 구체 셀 타입을 알아야 한다. `CellVisitor`가 `life/model`에 있으면 `life/model`이 `twoplayer.TwoPlayerResident`에 의존하게 되어 **패키지 순환**(`twoplayer` → `model` → `twoplayer`)이 생긴다. 피하려면 모드별 Visitor 하위 인터페이스를 두고 `accept` 안에서 `instanceof`로 맞는 Visitor인지 확인해야 하는데(Acyclic Visitor), 그러면 `instanceof`가 다시 들어온다.
  - 셀 종류를 추가하면 `CellVisitor`와 모든 Visitor 구현(이웃 조회, 렌더러, 집계)을 고쳐야 한다.

## 오버헤드 비교표

구조 비용은 코드를 쓰기 전 **추정**이다. "현재"로 표시한 값은 저장소 코드에서 센 값이다. 런타임 비용은 아직 측정하지 않았다(호출 횟수만 셈).

| 항목 | A. 공용 enum | B. 하위 인터페이스 | C. 상태 인터페이스 + 모드별 enum | D. Visitor |
| --- | --- | --- | --- | --- |
| 구조 비용 — 새 타입 | 1 (`CellState`) | 2 (`OwnedCell`, `Owner`) | 3 (`CellState`, `ClassicState`, `TwoPlayerState`) + 팔레트 1 (추정) | 1 (`CellVisitor`) + Visitor 구현 2~3 (이웃 조회, 렌더러, 집계) |
| 구조 비용 — 고칠 기존 파일 (모델) | 3 (`Cell`·`DUMMY`, `Resident`, `Neighborhood`) | 0 | 3 (A와 같음) | 3 (A와 같음) |
| 구조 비용 — 고칠 기존 파일 (렌더러 `ui/BoardRenderer`) | 1 (분기 일반화 + `switch`) | 1 (`instanceof OwnedCell` 분기 추가) | 1 (분기 일반화 + 팔레트) | 1 (Visitor로 다시 작성) |
| 구조 비용 — 기존 코드 LOC | 모델 +약 15, 렌더러 +약 10 (추정) | 렌더러 +약 8 (추정) | 모델 +약 15, 렌더러 +약 10 (추정) | 모델 +약 15, 렌더러 약 40줄을 Visitor로 재작성 (추정) |
| 간접성 — 이웃 1개당 | 가상 호출 1 (`state()`) | `instanceof` 1 + 가상 호출 1 | 가상 호출 1 (`state()`), 비교는 동일성 | 가상 호출 2 (`accept` → `visit`) |
| 간접성 — 그리는 셀 1개당 (현재: `instanceof` 최대 2 + `isAlive()` 1) | `instanceof` 1 + `state()` 1 + `switch` | `instanceof` 최대 3 + 호출 1 | `instanceof` 1 + `state()` 1 + 팔레트 조회 1 | 가상 호출 2 (`accept` → `visit`) |
| 렌더러 — 새 셀 종류를 빠뜨렸을 때 | `switch`에 case가 없으면 기본색 (컴파일 오류 없음) | 그려지지 않음 (컴파일 오류 없음) | 팔레트에 없으면 기본색 (컴파일 오류 없음) | **컴파일 오류** |
| 결합도 | 모든 모드와 렌더러가 공용 enum 하나에 의존 | 2인용만 `OwnedCell`에 의존. 렌더러가 2인용 타입을 앎 | 공용 `CellState`(메서드 1개)에만 의존. 렌더러는 팔레트만 앎 | `CellVisitor`가 모든 구체 셀 타입에 의존 → `model`과 `twoplayer` 사이 순환 위험 |
| 패키지 의존 (`twoplayer` → `life/model`) | 생김 | 생김 (`Cell` 상속) | 생김 | 생김 + 반대 방향 위험 |
| `java.awt` 의존 추가 (모델) | 없음 | 없음 | 없음 (색은 렌더러 팔레트) | 없음 |
| 런타임 (현재 기준) | 64×64 보드에서 세대당 활성 셀 × 8회 `state()` 호출 (현재 `isAlive()` 호출 횟수와 같음) | 같은 횟수 + `instanceof` | A와 같음 | A의 2배 호출 |
| 테스트 용이성 | Swing 없이 가능 | Swing 없이 가능, 대역 셀은 `OwnedCell` 구현 필요 | Swing 없이 가능, `state()`만 구현한 Stub으로 이웃 대체 가능. 렌더러는 팔레트 대역으로 `BufferedImage` 검증 | 가능하나 Visitor 대역까지 필요 |
| 확장 비용 (암세포 상태 추가 시) | 공용 enum 1곳 + 전이 규칙·렌더러의 `switch` | 새 하위 인터페이스 1개 + 판별 코드 + 렌더러 분기 1개 | 새 enum 1개 + 팔레트 1개, 공용 파일·렌더러 수정 0 | `CellVisitor` + 모든 Visitor 구현(이웃 조회, 렌더러, 집계) |
| 다른 작업과 충돌 | 1인용(`Cell`), #9(`BoardRenderer`) | #9(`BoardRenderer`) | 1인용(`Cell`), #9(`BoardRenderer`) | 1인용(`Cell`), #9(`BoardRenderer` 전체) |

## 결정과 근거 (제안)

**제안: C.** 회의에서 `Cell` 변경에 합의하지 못하면 **B**로 간다.

- C는 `Cell`과 렌더러를 한 번씩만 바꾸고, 이후 암세포 모드 등을 추가해도 자기 enum과 팔레트만 만든다. 명세 8장의 요구사항 1·2를 Cell 리팩토링·암세포 모드와 함께 확정하라는 조건에 맞는 공통 구조다. 렌더러의 "어느 분기에도 맞지 않으면 안 그려짐" 문제도 분기를 "`Neighborhood`가 아닌 셀"로 넓히면서 함께 없어진다.
- A는 당장은 가장 짧지만, 모드가 늘 때마다 공용 enum과 그 enum을 `switch`하는 곳(전이 규칙, 렌더러)을 모두 고쳐야 한다. 세 모드가 같은 enum을 고치게 되어 충돌 지점이 오히려 늘어난다.
- B는 회의 없이 진행할 수 있는 게 가장 큰 장점이다. 다만 이웃 타입 보장이 런타임에만 있고, #25 이후에는 렌더러에도 `instanceof` 분기가 모드마다 늘어난다. 그래서 대안으로만 둔다.
- **D 다시 평가**: #25 이후 D는 쓸 곳이 이웃 조회 하나에서 렌더러·셀 수 집계까지 늘었고, 새 셀 종류를 빠뜨리면 컴파일 오류로 잡히는 이점이 생겼다. 처음 판단("이웃 색 하나를 알기 위한 구조로는 무겁다")보다 득이 커졌다. 그래도 제안에서 빼는 이유는 두 가지다.
  1. 가장 자주 부르는 이웃 조회(활성 셀마다 8번)에서 호출이 2배가 된다.
  2. `CellVisitor`가 2인용 셀 타입을 알아야 해서 `life/model` ↔ `twoplayer` 순환이 생기거나, 피하려면 `instanceof`가 다시 들어온다(Acyclic Visitor).
  - 렌더러만 Visitor로 바꾸고 이웃 조회는 C로 하는 조합도 가능하다. 다른 도메인(렌더링·집계)에서도 Visitor를 쓸 계획이 있으면 회의에서 이 조합을 다시 본다("회의에서 정할 것" 6번).

## 회의에서 정할 것

1. `Cell`에 `state()`를 추가하는 데 동의하는가 (C), 아니면 `Cell`을 그대로 두는가 (B).
2. `Neighborhood.state()`의 계약: 이웃으로 넘어오지 않으니 `UnsupportedOperationException`을 던질지(`Resident.createMemento()`와 같은 방식), 아니면 "살아 있는 셀이 하나라도 있으면 ALIVE"처럼 의미를 줄지. `Neighborhood.isAlive()`가 항상 `true`인 것(`life/model/Neighborhood.java:318-320`)과 맞출지도 함께 정한다.
3. `isAlive()`를 남겨 둘지. 이 문서는 남겨 두는 것을 전제로 한다(클래식 코드 수정 없음). 이때 불변조건은 `isAlive() == state().isAlive()`이다.
4. 1인용 모드가 `ClassicState`를 그대로 쓸지, 자기 enum을 만들지.
5. **렌더러가 셀 색을 고르는 방법과 담당**: `BoardRenderer`의 `instanceof Resident` 분기를 "`Neighborhood`가 아닌 셀"로 넓히고 상태별 색을 모드에서 받을지(C), 모드마다 `instanceof` 분기를 늘릴지(B), Visitor로 바꿀지(D). `ui`는 #9 담당 영역이라 누가 언제 고칠지도 정한다.
6. **Visitor 계획**: 렌더링·셀 수 집계 등 다른 도메인에서도 `CellVisitor`를 쓸 계획이 있는가. 있으면 "렌더러는 Visitor, 이웃 조회는 C" 조합을 검토한다.
7. **패키지와 문서 위치**: `CellState`·`ClassicState`를 `life/model`에 두고 `twoplayer`가 `life/model`에 의존하는 것에 동의하는가. 이 문서(001)와 core `interfaces.md`를 #25가 만든 `life/model/docs/`로 옮길지.

## 전/후 비교

회의에서 확정한 뒤 클래스 다이어그램(`diagrams/`)을 추가한다. 핵심 변경은 아래와 같다(C 기준).

```java
// 전: life/model/Resident.java:45
if (north.isAlive()) ++neighbors;

// 후: 2인용 셀 (클래식 Resident는 그대로)
CellState s = north.state();
if (s.isAlive()) ++n;
if (s == TwoPlayerState.A) ++a;
```

```java
// 전: life/ui/BoardRenderer.java:39-41
} else if (cell instanceof Resident) {
  g.setColor(cell.isAlive() ? Color.RED : Colors.LIGHT_YELLOW);

// 후 (C, 회의 5번 결과에 따라 달라짐): Neighborhood가 아닌 셀은 상태 → 팔레트 색
} else {
  g.setColor(palette.colorOf(cell.state()));
```

## 후속 작업

- 회의 결과에 따라 상태를 `채택` 또는 `기각`으로 바꾸고, 기각이면 B로 새 문서를 쓴다.
- 2인용 셀을 `Resident`와 별도 클래스로 만들지(Prototype: `new Neighborhood(size, new TwoPlayerResident())`), `Resident`에 전이 규칙을 주입할지(Strategy)는 별도 결정 문서(002)로 다룬다. 별도 클래스로 만들면 렌더러 변경(회의 5번)이 반드시 함께 들어가야 한다.
- 명세 8장의 요구사항 3(상태별 셀 수 집계)은 #6 [005](../twoplayer/docs/005-cell-count-and-judge.md)에서 `TwoPlayerBoard.count()`로 분리했다. 요구사항 4(클릭 입력 처리)는 이 문서 범위 밖이다.
- 런타임 비용은 구현 후 세대당 처리 시간을 측정해 이 표를 실측값으로 바꾼다.
