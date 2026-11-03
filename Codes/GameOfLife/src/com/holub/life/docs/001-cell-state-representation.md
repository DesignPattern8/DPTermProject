# [001] 셀 상태 표현과 이웃 상태 조회

- 상태: 제안 (Cell 인터페이스 회의에서 확정 예정)
- 작성자: Son2kwon / 2026-11-03 / 관련 이슈: (생성 후 기입)
- 관련 명세: 2인용 Life Game 규칙 명세(초안, 2026-11-02) 3장, 8장 요구사항 1·2
- 이 문서를 core(`com.holub.life`)의 `docs/`에 두는 이유: 바뀌는 대상이 `Cell` 인터페이스이고, 2인용 외의 모드도 영향을 받기 때문이다(AGENTS.md 2-2)

## 문제

2인용 모드는 셀 상태가 `DEAD` / `A` / `B` 세 가지이고, 탄생할 때 이웃 3개 중 다수인 색을 따른다(명세 3장). 지금 코드로는 이 두 가지를 표현할 수 없다.

1. **셀 상태가 boolean 하나다.** `Resident`는 `amAlive`, `willBeAlive` 두 boolean만 가진다(`Resident.java:20-21`). 소유자를 담을 곳이 없다.
2. **이웃의 종류를 물어볼 방법이 없다.** `Resident.figureNextState()`는 이웃을 `Cell` 타입으로 받아 `isAlive()`만 호출한다(`Resident.java:52-59`). `Cell` 인터페이스에 상태를 묻는 메서드는 `isAlive()`뿐이다(`Cell.java:66`).

코드에서 확인한 사실(설계에 영향을 주는 것):

- `Cell` 구현체는 3개다: `Resident`, `Neighborhood`, `Cell.DUMMY`(익명 클래스, `Cell.java:124`).
- `figureNextState()`에 이웃으로 넘어오는 객체는 `Resident` 또는 `Cell.DUMMY`뿐이다. `Resident.verify()`가 assert로 이를 검사한다(`Resident.java:64-67`). `Neighborhood`는 `edge()`로 가장자리 `Resident`를 꺼내 넘긴다(`Neighborhood.java:136-180`).
- 그래서 `Neighborhood.isAlive()`가 항상 `true`를 반환하지만(`Neighborhood.java:360-362`) 이웃 수 계산에는 쓰이지 않는다. 이 메서드를 호출하는 곳은 저장소 안에 없다.
- `Cell`은 1인용 모드(다른 팀원 작업)도 함께 쓴다. 인터페이스를 바꾸면 그쪽 브랜치와 충돌한다.

## 후보

네 후보 모두 기존 `isAlive()`는 그대로 두므로 클래식 모드 코드(`Resident.figureNextState`)는 바꾸지 않아도 된다.

### A. 공용 enum + `Cell.state()` 추가

```java
public enum CellState { DEAD, ALIVE, PLAYER_A, PLAYER_B }   // 모드가 늘면 상수 추가
interface Cell { ... CellState state(); }
```

- 장점: 가장 단순하다. 캐스팅이 없고 `switch`로 바로 쓸 수 있다.
- 단점: 모든 모드의 상태가 enum 하나에 모인다. 암세포 모드 등을 추가할 때마다 공용 파일을 고쳐야 하고, 기존 `switch`에 빠진 case가 생긴다. 1인용 모드 셀이 `PLAYER_A`를 가질 수 있는 것처럼 보이는 등 타입이 모드를 구분해 주지 않는다.

### B. `Cell`은 그대로, 2인용 전용 하위 인터페이스 (`instanceof` 판별)

```java
interface OwnedCell extends Cell { Owner owner(); }       // Owner { A, B }
// 2인용 셀의 figureNextState 안에서
Owner o = (n instanceof OwnedCell oc && oc.isAlive()) ? oc.owner() : null;
```

- 장점: `Cell`을 전혀 바꾸지 않는다. 1인용 작업과 충돌이 없어서 회의 결과를 기다리지 않아도 된다.
- 단점: 이웃 8개마다 `instanceof`와 캐스팅이 필요하다. 이웃이 `OwnedCell`이라는 보장이 런타임에만 있다(보드에 다른 셀이 섞이면 조용히 "소유자 없음"으로 처리된다). 암세포 모드도 같은 문제를 겪으면 비슷한 하위 인터페이스가 하나 더 생긴다.

### C. 상태 인터페이스 + 모드별 enum, `Cell.state()` 추가

```java
public interface CellState { boolean isAlive(); }
enum ClassicState implements CellState { DEAD, ALIVE; ... }       // DUMMY, Resident
enum TwoPlayerState implements CellState { DEAD, A, B; ... }      // 2인용 셀
interface Cell { ... CellState state(); }
```

2인용 규칙은 `n.state().isAlive()`로 이웃 수를 세고, `n.state() == TwoPlayerState.A`로 색을 센다. 동일성 비교라 캐스팅이 필요 없고, `DUMMY`가 `ClassicState.DEAD`를 반환해도 결과가 맞다.

- 장점: `Cell`은 한 번만 바꾸고, 이후 모드를 추가할 때는 자기 enum만 만든다(공용 파일 수정 없음). 모드마다 상태 타입이 분리된다.
- 단점: `Cell`을 바꾸므로 1인용 작업과 회의로 맞춰야 한다. A보다 타입이 하나 더 많다. 모드별 enum이 섞여 있으면 `switch` 하나로 모든 상태를 처리할 수 없다(2인용 코드는 자기 enum만 다루므로 실제로는 문제가 작다고 본다).

### D. Visitor (이중 디스패치로 이웃 종류 판별)

```java
interface CellVisitor { void visit(Resident r); void visit(TwoPlayerResident r); void visitDummy(); }
interface Cell { ... void accept(CellVisitor v); }
```

- 장점: 캐스팅 없이 구체 타입별로 처리할 수 있다. 렌더링 분리(다른 팀 Visitor 후보)와 구조를 맞출 여지가 있다.
- 단점: 이웃 하나를 볼 때마다 `accept` → `visit` 호출 2단계가 생긴다. 셀 종류를 추가하면 `CellVisitor`와 모든 구현체를 고쳐야 한다. 이웃 색 하나를 알기 위한 구조로는 무겁다.

## 오버헤드 비교표

구조 비용은 코드를 쓰기 전 **추정**이다. "현재"로 표시한 값은 저장소 코드에서 센 값이다. 런타임 비용은 아직 측정하지 않았다(호출 횟수만 셈).

| 항목 | A. 공용 enum | B. 하위 인터페이스 | C. 상태 인터페이스 + 모드별 enum | D. Visitor |
| --- | --- | --- | --- | --- |
| 구조 비용 — 새 타입 | 1 (`CellState`) | 2 (`OwnedCell`, `Owner`) | 3 (`CellState`, `ClassicState`, `TwoPlayerState`) | 1 (`CellVisitor`) + 판별용 Visitor 구현 1 |
| 구조 비용 — 고칠 기존 파일 | 3 (`Cell`·`DUMMY`, `Resident`, `Neighborhood`) | 0 | 3 (A와 같음) | 3 (A와 같음) |
| 구조 비용 — 기존 코드 LOC | +약 15 (추정) | 0 | +약 15 (추정) | +약 15 (추정) |
| 간접성 (이웃 1개당) | 가상 호출 1 (`state()`) | `instanceof` 1 + 가상 호출 1 | 가상 호출 1 (`state()`), 비교는 동일성 | 가상 호출 2 (`accept` → `visit`) |
| 결합도 | 모든 모드가 공용 enum 하나에 의존 | 2인용만 `OwnedCell`에 의존, 공용 코드 변화 없음 | 공용 `CellState`(메서드 1개)에만 의존 | `CellVisitor`가 모든 구체 셀 타입에 의존 |
| `java.awt` 의존 추가 | 없음 | 없음 | 없음 | 없음 |
| 런타임 (현재 기준) | 64×64 보드에서 세대당 활성 셀 × 8회 `state()` 호출 (현재 `isAlive()` 호출 횟수와 같음) | 같은 횟수 + `instanceof` | A와 같음 | A의 2배 호출 |
| 테스트 용이성 | Swing 없이 가능 | Swing 없이 가능, 대역 셀은 `OwnedCell` 구현 필요 | Swing 없이 가능, `state()`만 구현한 Stub으로 이웃 대체 가능 | 가능하나 Visitor 대역까지 필요 |
| 확장 비용 (암세포 상태 추가 시) | 공용 enum 1곳 + 이를 `switch`하는 모든 곳 | 새 하위 인터페이스 1개 + 판별 코드 | 새 enum 1개, 공용 파일 수정 0 | `CellVisitor` + 모든 Visitor 구현 |
| 1인용 작업과 충돌 | 있음 | 없음 | 있음 | 있음 |

## 결정과 근거 (제안)

**제안: C.** 회의에서 `Cell` 변경에 합의하지 못하면 **B**로 간다.

- C는 `Cell`을 한 번만 바꾸고, 이후 암세포 모드 등을 추가해도 공용 파일을 고치지 않는다. 명세 8장의 요구사항 1·2를 Cell 리팩토링·암세포 모드와 함께 확정하라는 조건에 맞는 공통 구조다.
- A는 당장은 가장 짧지만, 모드가 늘 때마다 공용 enum과 그 enum을 `switch`하는 곳을 모두 고쳐야 한다. 세 모드가 같은 enum을 고치게 되어 충돌 지점이 오히려 늘어난다.
- B는 회의 없이 진행할 수 있는 게 가장 큰 장점이다. 다만 이웃 타입 보장이 런타임에만 있고, 다른 모드도 같은 방식을 쓰면 하위 인터페이스가 계속 늘어난다. 그래서 대안으로만 둔다.
- D는 이웃 색 하나를 알려고 호출이 두 배가 되고, 셀 종류를 추가할 때 고칠 곳이 가장 많다. 렌더링 분리에 Visitor를 쓰게 되더라도 이웃 조회까지 Visitor로 할 이유는 없다고 본다.

## 회의에서 정할 것

1. `Cell`에 `state()`를 추가하는 데 동의하는가 (C), 아니면 `Cell`을 그대로 두는가 (B).
2. `Neighborhood.state()`의 계약: 이웃으로 넘어오지 않으니 `UnsupportedOperationException`을 던질지(`Resident.createMemento()`와 같은 방식), 아니면 "살아 있는 셀이 하나라도 있으면 ALIVE"처럼 의미를 줄지. `Neighborhood.isAlive()`가 항상 `true`인 것(`Neighborhood.java:360-362`)과 맞출지도 함께 정한다.
3. `isAlive()`를 남겨 둘지. 이 문서는 남겨 두는 것을 전제로 한다(클래식 코드 수정 없음). 이때 불변조건은 `isAlive() == state().isAlive()`이다.
4. 1인용 모드가 `ClassicState`를 그대로 쓸지, 자기 enum을 만들지.

## 전/후 비교

회의에서 확정한 뒤 클래스 다이어그램(`diagrams/`)을 추가한다. 핵심 변경은 아래와 같다(C 기준).

```java
// 전: Resident.java:52
if (north.isAlive()) ++neighbors;

// 후: 2인용 셀 (클래식 Resident는 그대로)
CellState s = north.state();
if (s.isAlive()) ++n;
if (s == TwoPlayerState.A) ++a;
```

## 후속 작업

- 회의 결과에 따라 상태를 `채택` 또는 `기각`으로 바꾸고, 기각이면 B로 새 문서를 쓴다.
- 2인용 셀을 `Resident`와 별도 클래스로 만들지(Prototype: `new Neighborhood(size, new TwoPlayerResident())`), `Resident`에 전이 규칙을 주입할지(Strategy)는 별도 결정 문서(002)로 다룬다.
- 명세 8장의 요구사항 3(상태별 셀 수 집계)과 4(클릭 입력 처리)는 이 문서 범위 밖이다.
- 런타임 비용은 구현 후 세대당 처리 시간을 측정해 이 표를 실측값으로 바꾼다.
