# 작업 기록 (고태영 / Yarlang)

- 담당 A 모델 · 게임 규칙·보드·상태 캡처와 복원
- 설계 결정과 작업 결과는 이 파일에 누적한다. 공개 계약은 [model/docs/interfaces.md](../Codes/GameOfLife/src/com/holub/life/model/docs/interfaces.md), 테스트 기준은 [model/docs/test-cases.md](../Codes/GameOfLife/src/com/holub/life/model/docs/test-cases.md)에서 관리한다.
- `001·002·003`은 기존 작성자·날짜·이슈·결정 근거를 보존해 통합한 기록이다. 001은 도메인 분리 당시의 팀 선행 작업이며, 아래 수치와 구현 설명은 각 작업 시점을 기준으로 한다.

## 한눈에 보기

| 작업 | 이슈 | PR | 결과 |
| --- | --- | --- | --- |
| 공통 보드 계약 정의 | [#47](https://github.com/DesignPattern8/DPTermProject/issues/47) | [#48](https://github.com/DesignPattern8/DPTermProject/pull/48) | main 병합, 좌표·스냅샷·도메인별 인계 기준 작성 |
| 클래식 보드 Facade | [#49](https://github.com/DesignPattern8/DPTermProject/issues/49) | [#50](https://github.com/DesignPattern8/DPTermProject/pull/50) | main 병합, 당시 전체 68개 테스트 통과 |
| 클래식 생사 판정 함수 추출 | [#52](https://github.com/DesignPattern8/DPTermProject/issues/52) | [#54](https://github.com/DesignPattern8/DPTermProject/pull/54) | main 병합, 당시 전체 87개 테스트 통과 |
| 암세포 스냅샷 보존·설계 기록 통합 | [#73](https://github.com/DesignPattern8/DPTermProject/issues/73) | [#83](https://github.com/DesignPattern8/DPTermProject/pull/83) | refactor/base 병합·이슈 종료, JDK 26 CI 통과 |
| legacy Load 경계 활성화 | [#81](https://github.com/DesignPattern8/DPTermProject/issues/81) | [#89](https://github.com/DesignPattern8/DPTermProject/pull/89) | refactor/base 병합, 로컬·CI 통과 |
| 보드별 전이 상태 분리 | [#74](https://github.com/DesignPattern8/DPTermProject/issues/74) | [#90](https://github.com/DesignPattern8/DPTermProject/pull/90) | refactor/base 병합 |
| 모델 좌표 계약 분리 | [#71](https://github.com/DesignPattern8/DPTermProject/issues/71) | PR 준비 | 구현·검증 완료 |

## 001. 모델과 화면 메서드 분리

- 상태: 채택
- 작성: Codex / 2026-11-03 / #24

### 문제
기준 e38ce7a의 Cell.java:56/63이 Graphics와 픽셀 입력을 모델에 요구한다. Neighborhood.java:286/345와 Resident.java:85/100은 계산과 UI를 함께 구현한다.

### 후보
A. 기존 클래스를 model로 단순 이동: UI 의존이 유지된다.
B. 기존 그리기·픽셀 변환을 구체 UI 클래스로 추출: 채택. 새 Visitor/Strategy/Facade는 도입하지 않는다.

### 결정과 전/후
Cell/Resident/Neighborhood/Direction/NeighborhoodState를 model로 이동한다. 규칙·재귀·transition 반환값은 유지한다. redraw는 BoardRenderer로, 픽셀 정수 나눗셈은 GridCoordinateMapper로 추출한다. 모델 toggle(row,column)이 실제 셀과 활성 경계를 갱신한다.

### 오버헤드
새 UI 구체 클래스 2개. 모델의 Graphics/Rectangle/Swing/Colors 의존을 제거한다. 기존 Memento의 Point와 ConditionVariable은 유지한다. 렌더러의 타입 분기와 읽기 접근자 호출이 추가되지만 셀·블록 재귀 횟수는 유지한다. 픽셀 변환 뒤 모델 토글을 별도 순회하므로 클릭 경로는 깊이 d에 대해 d+d 단계가 된다. 실행 시간은 측정하지 않았다.

### 후속 작업
불변 스냅샷/읽기 전용 셀 인터페이스는 새 설계를 요구하므로 이번에 도입하지 않는다. 모델은 기존 Point 좌표를 유지한다. childAt은 렌더링/좌표 변환을 위해 제공하며 UI는 반환된 셀을 직접 변경하지 않는다. #12 반환값 문제는 수정하지 않는다.

## 002. 클래식 보드 Facade

- 상태: 채택 · A 모델의 클래식 보드
- 담당: A 모델 · 2026-11-10 · [이슈 #49](https://github.com/DesignPattern8/DPTermProject/issues/49)
- 계약 초안과 인계 기준: [PR #48](https://github.com/DesignPattern8/DPTermProject/pull/48)

### 문제

[GameController.tick()](../Codes/GameOfLife/src/com/holub/life/application/GameController.java#L28)은 보드 밖 이웃을 `Cell.DUMMY`로 채우고 세대 계산과 상태 반영을 조정한다. 실행·저장·화면에 보드 단위 연산을 제공하려면 셀 트리와 세대 계산 순서를 모델 내부에서 관리해야 한다.

### 후보

- A. 기존 Composite의 `Cell`을 계속 진입점으로 쓴다. 좌표와 스냅샷 값은 분리되지만 보드 호출부가 하위 셀 연산과 세대 계산 순서를 알아야 한다.
- B. `CompositeBoard`가 기존 `Neighborhood`를 감싸고, 호출부는 `Board`를 통해 보드 단위로 접근한다.

### 오버헤드 비교표

| 항목 | A. 기존 Composite 진입점 | B. Facade |
| --- | --- | --- |
| 구조 비용 | 좌표·스냅샷 2개 타입 추가 추정 | 모델 타입·파일 4개, 물리 줄 125줄 추가 |
| 세대 진행 호출 | 호출부가 `figureNextState()` → `transition()` 관리 | `Board.step()` 호출과 구현 위임 1단계, 기존 계산을 재사용 |
| 캡처 비용 | 셀 트리 또는 기존 Memento를 통해 생존 위치 수집 | 셀 트리 전체 순회 후 생존 위치를 불변 집합으로 복사 |
| 결합·검증 | 호출부가 `Cell`과 계산 순서에 결합 | 공개 API는 모델 타입과 `Set`을 사용하며 Swing 없이 검증 |

### 결정과 근거

B를 채택한다. `step()`은 기존 세대 계산과 반영 순서를 감싼다. `snapshot()`은 셀을 직접 읽어 기존 저장용 `createMemento()`의 경계 활성 정보 변경을 피한다. `restore()`는 크기를 확인한 뒤 전체를 비우고 생존 칸을 편집 경로로 배치한다. 이 경로는 경계에 놓인 칸이 다음 세대의 이웃 블록 계산을 깨우도록 한다. [클래식 보드 테스트](../Codes/GameOfLife/src/com/holub/life/model/docs/test-cases.md#클래식-보드-api-49)

### 전/후 비교

전후 클래스 다이어그램은 [Notion 개발 기록의 #49 항목](https://app.notion.com/p/40a61a0bb83480208e4accb053cf40f7)에 이미지로 보관한다. PR #50 시점의 구조와 B·C·D 후속 연동 범위를 함께 확인할 수 있다.

### 후속 작업

B·C·D는 [인계 기준](../Codes/GameOfLife/src/com/holub/life/model/docs/interfaces.md#후속-작업-인계)에 따라 각 호출부를 연결한다. 2인용 셀 소유자는 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)에서 다룬다.

## 003. 클래식 생사 판정 분리

- 상태: 채택
- 담당: A 모델 · 태영 · 2026-11-11 · [이슈 #52](https://github.com/DesignPattern8/DPTermProject/issues/52)

### 문제

[Resident.figureNextState()](../Codes/GameOfLife/src/com/holub/life/model/Resident.java#L25)는 이웃 8칸을 검증·집계한 뒤 생사 판정식을 바로 적용했다. 집계 이후 규칙을 적용하는 지점에 `nextAlive`라는 이름을 붙인다. 기존 식이 한 줄이므로 이 변경의 효과는 작은 가독성 개선이다.

### 후보

#### A. 내부 함수 추출

`private static nextAlive(boolean alive, int liveNeighbors)`로 생사 판정만 계산한다. 입력 두 개로 결과를 반환하며 `figureNextState()`가 결과를 `willBeAlive`에 저장한다. 함수 추출 리팩토링으로 적용한다.

#### B. Strategy 규칙 주입

`LifeRule`과 기본 규칙 구현을 두고 `Resident`가 주입받아 호출한다. 여러 규칙을 교체할 수 있다. 현재 클래식 규칙은 하나이며, 이 안은 규칙 타입·필드·생성자를 추가하고 `Resident.create()`의 복제 경로에도 규칙을 전달해야 한다. 1·2인용 공용 상태와 입력은 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)에서 논의한다.

### 오버헤드 비교표

A는 이번 소스 diff를 세었고 B는 구현 전 추정이다.

| 항목 | A. 내부 함수 추출 | B. Strategy 주입 |
| --- | --- | --- |
| 구조 비용 | 제품 타입 증감 0, `Resident.java` 1개 수정, 118 → 122줄, 5줄 추가·1줄 삭제 | 인터페이스·기본 구현 최소 2개 타입, 규칙 필드·생성자 및 복제 경로 변경 추정 |
| 간접성 | 계산한 `Resident`마다 내부 정적 함수 호출 1단계 추가 | 규칙 객체를 통한 호출 1단계 추가 추정 |
| 결합도 | import·외부 패키지·공개 시그니처 증감 0 | `Resident`가 규칙 인터페이스에 의존 |
| 런타임 비용 | 규칙 객체 생성 0, 시간 측정 없음 | 규칙 객체의 생성·공유 정책 필요, 시간 측정 없음 |
| 테스트 용이성 | 공개 세대 계산 경로를 Swing 없이 검사 | 대체 규칙 주입 가능 |
| 확장 비용 | 클래식 함수 한 곳을 편집, 모드별 교체는 별도 설계 | 합의한 입력으로 표현되는 규칙은 구현 추가·주입으로 교체 |

### 결정과 근거

A를 채택한다. 현재 요구는 클래식 판정에 이름을 붙이는 것이다. 판정 함수는 현재 생사와 살아 있는 이웃 수만 읽어 다음 생사를 반환한다. 이웃 검증·집계, 현재 상태 반영, `Cell`·`Board`의 공개 계약은 기존 경로를 사용한다.

### 전/후 비교

전후 클래스 다이어그램은 [Notion 개발 기록의 #52 항목](https://app.notion.com/p/40a61a0bb83480208e4accb053cf40f7)에 이미지로 남겼다. 다음은 판정식의 코드 전후 비교다.

```java
// 추출 전: figureNextState() 내부
willBeAlive = (neighbors == 3 || (amAlive && neighbors == 2));

// 추출 후: figureNextState() 내부
willBeAlive = nextAlive(amAlive, neighbors);

private static boolean nextAlive(boolean alive, int liveNeighbors) {
  return liveNeighbors == 3 || (alive && liveNeighbors == 2);
}
```

### 육하원칙

| 항목 | 기록 |
| --- | --- |
| 언제 | 2026-11-11, 이슈 #52 작업 |
| 어디서 | 원래 IntelliJ 프로젝트의 `refactor/52-classic-rule`, `model/Resident.java`와 모델 테스트·문서 |
| 누가 | 태영의 A 모델 작업 |
| 무엇을 | 생사 판정을 내부 함수로 추출하고 전이표·예외 검증을 추가했다. 전후 클래스 다이어그램은 Notion 개발 기록에 저장했다. |
| 왜 | 이웃을 세는 과정에서 규칙을 적용하는 지점을 이름으로 드러내기 위해서다. |
| 어떻게 | 테스트 기준 → 특성 테스트 → 기존 구현에서 통과 확인 → 함수 추출 → 전체 테스트 순서로 진행했다. |

### 검증

- 프로젝트가 지정한 Azul JDK 26.0.2.1에서 기존 테스트 68개를 실행한 뒤 특성 테스트를 추가했다.
- 추출 전 `./gradlew test --tests com.holub.life.model.ResidentRuleTest --no-daemon`으로 전이표 18가지와 예외 1가지가 통과했다.
- 추출 후 `./gradlew test --no-daemon`으로 기존 68개와 신규 19가지, 총 87개가 통과했다. 실패·오류·건너뜀은 0개다.
- [RULE-01·02](../Codes/GameOfLife/src/com/holub/life/model/docs/test-cases.md#클래식-생사-판정-분리-52)는 공개 메서드로 계산과 반영 시점, 변경 반환값, 잘못된 이웃 타입의 assert를 확인한다. Block·Blinker·Glider·경계·고정 시드 참조 비교는 기존 특성 테스트를 사용한다.

### 후속 작업

B는 [#51](https://github.com/DesignPattern8/DPTermProject/issues/51)에서 게임 실행 경로를 `Board`에 연결한다. 모드별 셀 상태와 공통 규칙 입력은 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)의 합의에 맞춘다.

## 004. 암세포 스냅샷 보존 (#73)

- 상태: 채택 · 구현 완료, [PR #83](https://github.com/DesignPattern8/DPTermProject/pull/83) 작성
- 담당 A 모델 · 태영 · 2026-11-17
- 기준 `refactor/base` `881819f`, 작업 브랜치 `fix/73-cancer-snapshot`

### 문제

`BoardSnapshot`은 생존 좌표만 저장해 암세포 배치를 캡처·복원하면 일반 세포가 된다. 기존 스냅샷은 클래식 용도로 도입됐고, PR #60의 암세포 상태도 보존하도록 확장한다. 현재 파일 경로는 암세포를 담는 기존 Memento를 사용한다.

### 후보와 결정

| 후보 | 구조와 비용 | 판단 |
| --- | --- | --- |
| A. 불변 스냅샷 확장 | `cancerCells` 집합 추가·방어 복사, 기존 읽기 전용 순회에 암 여부 조회 추가, 복원 시 암 좌표의 셀을 찾아 상태 적용 | 채택 |
| B. 기존 Memento 재사용 | `NeighborhoodState`·`Cell.transfer()` 및 `Point` 변환 재사용. 캡처 시 활성 정보 변경 가능성과 복원 직후 블록 경계 처리를 함께 검토해야 함 | 이번 스냅샷에는 적용하지 않음 |

A를 채택한다. `cancerCells`는 `liveCells`의 부분집합이며 두 집합을 불변 값으로 만든다. 기존 3인자 생성자는 빈 암세포 집합으로 위임해 클래식 호출과 C의 변환 API를 지원한다. `snapshot()`은 한 번의 셀 트리 순회로 두 집합을 수집한다. 복원은 기존 좌표 편집 경로로 활성 경계를 설정한 뒤 암세포 좌표에 암 상태를 적용한다.

### 수정 내용과 이유

| 대상 | 수정 | 이유 |
| --- | --- | --- |
| [BoardSnapshot.java](../Codes/GameOfLife/src/com/holub/life/model/BoardSnapshot.java) | `cancerCells` 구성 요소·4인자 생성자 추가, 두 집합 방어 복사·부분집합 검사, 3인자 생성자 유지 | 암세포와 일반 세포를 구분하고 잘못된 값을 보드 변경 전에 거부한다. |
| [CompositeBoard.java](../Codes/GameOfLife/src/com/holub/life/model/CompositeBoard.java) | 캡처 시 생존·암 좌표를 함께 수집, 복원 시 생존 칸 배치 후 암 상태 적용 | 캡처가 활성 정보를 바꾸지 않으면서 암 상태와 다음 세대의 블록 경계 계산을 보존한다. |
| [Board.java](../Codes/GameOfLife/src/com/holub/life/model/Board.java)·[공개 계약](../Codes/GameOfLife/src/com/holub/life/model/docs/interfaces.md) | 클래식·암세포 캡처/복원 범위와 호출부 기준 명시 | B·C·D가 생존 집합에 암세포도 들어간다는 점과 직렬화·저장 기준을 알 수 있게 한다. |
| [CancerSnapshotTest.java](../Codes/GameOfLife/test/com/holub/life/model/CancerSnapshotTest.java)·[테스트 기준](../Codes/GameOfLife/src/com/holub/life/model/docs/test-cases.md#암세포-스냅샷-73) | 정상·경계·예외 9개 목적, 매개변수 사례를 포함한 12개 실행 추가 | 실제 상태 손실을 재현하고 복원 직후 계산·불변성·이전 호출 호환을 검증한다. |
| `model/docs/001·002·003` → 이 파일 | 기존 작성 이력·결정·수치·코드를 보존해 통합, 원래 파일 제거, README·계약의 링크 변경 | A의 설계와 작업 기록을 루트 `docs/GTY.md`에서 이어서 읽도록 한다. 공개 계약·테스트 기준은 원래 위치를 유지한다. |
| [AGENTS.md](../AGENTS.md) | A 모델 작업 기록의 `docs/GTY.md` 누적 규칙 추가 | 다음 작업에서도 같은 위치를 갱신하도록 한다. |

### 전/후 비교

```java
// 전: 생존 좌표만 담으므로 암 여부를 구분할 수 없다.
public record BoardSnapshot(int rows, int columns, Set<CellPosition> liveCells) {}

// 후: 암세포는 살아 있는 세포 중 일부다.
public record BoardSnapshot(
    int rows, int columns, Set<CellPosition> liveCells, Set<CellPosition> cancerCells) {
  public BoardSnapshot(int rows, int columns, Set<CellPosition> liveCells) {
    this(rows, columns, liveCells, Set.of());
  }
  // 전체 생성자에서 크기·범위·부분집합을 검증하고 두 집합을 방어 복사한다.
}
```

복원 순서는 `크기 확인 → clear → 모든 생존 좌표 toggle → 암 좌표 setCancer`다. `toggle`이 상위 블록과 활성 경계를 표시하므로, 마지막 `setCancer`가 개별 셀만 바꾸어도 인접 블록이 다음 세대 계산에 참여한다. `setCancer`만 직접 호출하는 방식은 이 활성화 과정을 빠뜨릴 수 있다. 암 좌표의 실제 셀을 찾는 탐색은 `CompositeBoard`의 비공개 메서드로 둔다.

새 GoF 패턴은 도입하지 않는다. 기존 Facade·Composite 구조에서 스냅샷이 표현하는 상태를 확장하는 버그 수정이다. 기존 Memento와 파일 입출력 경로는 바꾸지 않는다.

### 오버헤드와 호환성

아래 물리 줄 수와 diff는 기준 `881819f` 대비 실제 집계다. 시간·힙 사용량은 측정하지 않았으며 복잡도는 코드 구조에서 계산했다. `N`은 전체 셀 수, `L`은 생존 셀 수, `C`는 암세포 수, `d`는 셀 트리 깊이다.

| 항목 | 결과 |
| --- | --- |
| 구조 비용 | 제품 타입 추가·삭제 0, 제품 소스 3개 수정. `Board` 21→21줄, `BoardSnapshot` 21→31줄, `CompositeBoard` 79→105줄. 합계 49줄 추가·13줄 삭제(순증 36줄). 신규 테스트 클래스 1개·223줄. |
| 간접성 | 공개 `Board` 호출 단계와 세대 진행 경로는 유지. 복원 중 암세포마다 내부 셀 탐색 호출 1회 추가. |
| 결합도 | 새 외부 라이브러리·패키지·AWT/Swing 의존 없음. 기존 `Cell.isCancer()`·`setCancer()` 사용. |
| 런타임 비용 | 캡처는 기존과 같은 전체 트리 1회 순회에 암 여부 조회를 추가. 두 집합 복사·부분집합 검사는 `O(L+C)`, 암 좌표 저장 공간 `O(C)` 추가. 복원은 기존 clear·생존 좌표 배치에 `O(C×d)` 탐색 추가. 세대 계산 코드는 변경 없음. |
| 테스트 용이성 | Swing 창 없이 모델만 검사. 고정 좌표로 암 상태와 블록 경계를 재현. |
| 확장 비용 | 현재 암 상태는 기존 필드·순회 확장으로 처리. 향후 다른 상태를 추가하면 스냅샷·캡처·복원과 계약·테스트도 함께 검토해야 함. |

기존 `new BoardSnapshot(rows, columns, liveCells)`는 암세포 없는 값으로 동작한다. `Board` 메서드 시그니처는 유지하고 레코드 동등성에는 암세포 집합도 포함한다. 현재 저장소 Java 호출부에는 레코드 패턴·리플렉션으로 구성 요소 개수에 의존하는 코드가 없다. C가 암세포 저장을 연결할 때는 생존 좌표와 암 좌표를 모두 전달해야 한다. 자세한 소비자 기준은 [공개 계약의 인계표](../Codes/GameOfLife/src/com/holub/life/model/docs/interfaces.md#후속-작업-인계)에 둔다.

### 육하원칙

| 항목 | 기록 |
| --- | --- |
| 누가 | 태영의 A 모델 작업 |
| 언제 | 2026-11-17, 수정된 이슈 #73 기준, 커밋 전 |
| 어디서 | 원래 IntelliJ 프로젝트, `refactor/base`에서 만든 `fix/73-cancer-snapshot` 브랜치의 모델·테스트·문서 |
| 무엇을 | 생존 좌표뿐 아니라 암 여부도 캡처·복원하도록 수정하고, 문서 001·002·003을 `docs/GTY.md`로 통합했다. |
| 왜 | 복원하면 암세포가 일반 세포로 바뀌던 누락을 해결하고 A의 작업 이력을 한곳에서 이어 쓰기 위해서다. |
| 어떻게 | 테스트 기준 정의 → 기존 157개 통과 확인 → 회귀 테스트 2개 실패로 문제 재현 → 스냅샷 확장·복원 순서 보완 → 관련·전체 테스트 → 문서·링크 점검 순서로 진행했다. |

### 검증

- 수정 전 전체 157개 통과. 기존 API만 사용하는 회귀 테스트 2개에서 복원 후 암 상태가 사라지는 실패를 확인했다.
- 수정 후 `CancerSnapshotTest` 12개와 기존 `CompositeBoardTest` 9개, 총 21개 통과.
- 최종 전체 169개 통과(기존 157 + 신규 12), 실패·오류·건너뜀 0. 정상 복원, 블록 경계의 첫 세대, 읽기 전용 캡처, 두 집합의 불변성, 유효성 검사·원래 보드 유지, 3인자 생성자·빈 보드 복원을 확인했다.
- 문서 통합 후 원본 001·002·003의 본문이 제목 계층·링크 위치 변경 외에는 그대로 보존됐는지 대조했다. 관련 로컬 파일·제목 링크 40개와 `git diff --check`를 확인했다.
- 로컬에 지정 JDK 26이 없어 설치된 Eclipse Temurin 25.0.3을 사용했다. 실행 시 `JAVA_HOME`을 해당 JDK로 지정하고 임시 초기화 파일에서 테스트 도구 체인만 25로 덮어썼다. 저장소의 JDK 26 설정은 변경하지 않았다. **JDK 26 검증은 미실행**이다.

실제 Gradle 실행 명령(`Codes/GameOfLife`에서 실행, 임시 초기화 파일은 저장소 밖에 둠):

```sh
./gradlew test --tests com.holub.life.model.CancerSnapshotTest --tests com.holub.life.model.CompositeBoardTest --no-daemon --init-script /private/tmp/dp73-jdk25.init.gradle
./gradlew test --no-daemon --init-script /private/tmp/dp73-jdk25.init.gradle
```

임시 초기화 파일은 Java 도구 체인의 `languageVersion`만 25로 지정했다. 프로젝트 기본 환경인 JDK 26이 준비되면 `./gradlew test --no-daemon`으로 다시 확인한다. UI 수동 실행과 성능 측정은 하지 않았다.

### 후속 작업

- PR base는 `refactor/base`, 본문 연결은 `Refs #73`을 사용한다. 병합 후 이슈에 PR 링크를 남겨 직접 닫는다.
- 이번 복원 대상은 클래식·암세포 보드의 셀 상태다. 2인용 소유자는 전용 보드 계약에서 다룬다.
- 개인 Notion 개발 기록의 07번에 이번 #73의 육하원칙·대안·수정 내용·검증 결과를 갱신했다. 이후 사용자가 버그 수정 `baf7935` → 테스트 `df2c454` → 문서 `d255b8e` 순서로 커밋·푸시하고 PR #83을 작성했다.
- 앞으로 A 모델의 설계 결정·고민·검증 결과·육하원칙은 이 파일에 추가한다. 공개 시그니처와 테스트 케이스는 모델의 계약·테스트 문서에 갱신한다.

## 005. PR #83의 refactor/base 충돌 해결

- 담당 A 모델 · 태영 · 2026-11-17
- 작업 브랜치 `fix/73-cancer-snapshot`, 병합 대상 `origin/refactor/base` `084d6c7`

### 원인과 해결

PR #83 작성 후 확인한 통합 브랜치에는 C의 파일 API·모델 값 연동(#61)이 반영돼 있었다. 모델 README의 같은 문단에서 A는 기록 위치를 `docs/GTY.md`로 바꾸고 C는 신규 연동 문서 링크를 추가해 내용 충돌이 발생했다. 소스 충돌은 없었다.

A의 GTY 누적 기록 안내와 C의 신규 파일 API 연동 문서 링크를 모두 유지했다. C가 추가한 파일·테스트·문서·다이어그램은 그대로 통합한다. A가 옮긴 문서 001·002·003은 복구하지 않는다. `git merge --no-commit --no-ff origin/refactor/base`로 가져왔으며 병합 커밋·푸시는 사용자가 진행한다.

### 분류 기준

이슈 #73에는 기존 `[REFACTOR]` 제목이 남아 있지만 이번 변경은 캡처·복원 시 암세포가 일반 세포가 되는 오류를 수정한다. PR의 `fix`와 BUG 유형이 실제 동작 변경에 맞는다. 이슈 제목의 분류를 정리할 수 있으나 이번 충돌 해결에서 원격 이슈나 PR은 수정하지 않았다. PR base는 팀 작업 흐름에 따라 `refactor/base`를 유지한다.

### 육하원칙

| 항목 | 기록 |
| --- | --- |
| 누가 | 태영의 A 모델 작업 |
| 언제 | 2026-11-17, PR #83 작성 이후 |
| 어디서 | 기존 IntelliJ 프로젝트의 `fix/73-cancer-snapshot` |
| 무엇을 | 최신 refactor/base를 가져와 모델 README 충돌을 해결했다. |
| 왜 | A의 기록 통합과 C의 연동 문서 추가가 같은 문단을 수정했기 때문이다. |
| 어떻게 | C의 새 문서 링크와 A의 GTY 안내를 함께 유지한 뒤 통합 상태의 전체 테스트를 확인했다. |

### 검증

- `Codes/GameOfLife`에서 기존과 같은 JDK 25.0.3 및 임시 초기화 파일로 `./gradlew test --no-daemon --init-script /private/tmp/dp73-jdk25.init.gradle` 실행: 전체 189개 통과, 실패·오류·건너뜀 0. 최초 #73 검증 169개에 C가 추가한 20개가 함께 실행됐다. JDK 26에서는 미검증이다.
- README·GTY의 로컬 파일 링크 26개, 변경 Java·Markdown의 충돌 표시 제거, `git diff --check`와 `git diff --cached --check`를 확인했다.
- 당시 미해결 충돌 파일 0개로 스테이징했다. 이후 사용자가 병합 커밋 `608e6f0`을 푸시하고 PR #83을 `refactor/base`에 병합했다(`84d23db`). #73 종료와 PR의 JDK 26 CI 통과도 확인했다. 위 JDK 25 수치는 충돌 해결 당시의 로컬 결과다.

## 006. legacy Load 경계 활성화 (#81)

- 상태: 채택 · A 모델 수정 완료, [PR #89](https://github.com/DesignPattern8/DPTermProject/pull/89) 병합
- 담당 A 모델 · 태영 · 2026-11-17
- 착수 기준 `refactor/base` `422833e`, 작업 브랜치 `fix/81-load-boundary`. 작업 중 최신 `refactor/base` `03fe4ed`를 반영했다.
- 사용자 최종 요청에 따라 A의 모델 수정 범위를 먼저 진행한다. B와 #81 공동 담당이며, 최신 기준선에는 #63의 안전한 Load 실패 처리(PR #82)가 반영돼 있다.

### 문제와 원인

정상 파일의 `(7,7), (7,8), (7,9)`를 Load하면 셀 배치는 맞지만 첫 세대의 `(8,8)` 출생이 빠진다. `Resident.transfer(LOAD)`는 현재·다음 생사를 동시에 맞추므로 직후 `isDisruptiveTo()`가 `NONE`이다. 기존 `Neighborhood.transfer()`는 활성 여부를 켜도 변경된 자식이 놓인 경계를 표시하지 않아, 비어 있는 인접 블록이 계산에 참여하지 못한다.

#73의 `Board.restore()`는 `toggle()`로 블록·경계를 활성화한다. 실제 파일 Load는 `Neighborhood.transfer(LOAD)`를 사용하므로 이 경로의 활성화를 보완해야 한다. 기존 테스트의 Load 후 블링커는 블록 내부 좌표여서 해당 결함을 검출하지 못했다.

### 후보와 결정

| 후보 | 변경 범위·비용 | 판단 |
| --- | --- | --- |
| A. 기존 Composite/Memento 경계 처리 보완 | `Neighborhood.transfer()`에서 LOAD가 활성화한 자식의 격자 위치로 기존 경계 표시 함수를 호출. 공개 타입·파일 형식·호출부 변경 없음. | 채택 |
| B. Board Facade 복원으로 파일 경로 이전 | B의 `GameController`와 C 값 변환을 `BoardSnapshot`에 연결해 `Board.restore()`를 사용. #71 후속 방향과 맞지만 파일 순서·좌표·암세포 변환, 저장 호출부와 계약을 함께 검토해야 함. | 별도 연동 작업에서 검토 |

이번에는 모델 내부에서 같은 결함을 사용하는 모든 legacy 호출 경로를 고친다. STORE의 기존 활성 정보 부수효과와 `Neighborhood.transfer()`의 반환 정책은 유지한다. B의 실패 시 상태 보존·Clock·알림 정책, C 저장 형식, 규칙·동시성 설계는 이번 수정 범위에 추가하지 않는다.

### 테스트 순서

[테스트 기준 LDB-01~07](../Codes/GameOfLife/src/com/holub/life/model/docs/test-cases.md#legacy-load-경계-활성화-81)을 먼저 정의했다. 프로젝트 지정 JDK 26.0.2.1에서 기존 전체 233개가 통과하는 기준선을 확인했다. 새 회귀 테스트 17개 중 10개 실패로 문제를 재현한 뒤 구현을 적용했다.

### 전/후 비교

[Neighborhood.transfer()](../Codes/GameOfLife/src/com/holub/life/model/Neighborhood.java)에서 활성 자식의 위치를 경계에 표시한다. 각 부모가 자기 격자 내 좌표로 같은 작업을 수행하므로 중첩 깊이가 달라도 다음 블록으로 전파된다.

```java
// 전
if (grid[row][column].transfer(memento, upperLeft, load)) amActive = true;

// 후
if (grid[row][column].transfer(memento, upperLeft, load)) {
  amActive = true;
  // LOAD makes leaves stable, so wake neighbors from the changed child's position.
  if (load) rememberThatCellAtEdgeChangedState(row, column);
}
```

`활성 여부만 설정 → 자기 격자 기준 경계도 표시 → 다음 세대에 인접 블록 계산`으로 보완했다. `(7,7), (7,8), (7,9)`를 불러온 첫 세대는 이전의 `{(6,8), (7,8)}`에서 `{(6,8), (7,8), (8,8)}`로 바로잡힌다. 새 GoF 패턴을 추가하는 작업은 아니며 기존 Composite/Memento 경로의 버그 수정이다.

### 실제 비용과 계약

| 항목 | 결과 |
| --- | --- |
| 구조 비용 | 제품 소스 `Neighborhood.java` 1개, 555→559줄(5줄 추가·1줄 삭제), 제품 타입 증감 0. 테스트 클래스 1개·227줄, 5개 메서드·17개 실행 추가. |
| 간접성 | LOAD에서 자식이 활성으로 보고될 때 기존 비공개 경계 표시 함수 호출 1회 추가. 세대 계산 경로는 유지. |
| 결합도 | 공개 시그니처·외부 라이브러리·패키지 의존 증감 0. B·C 제품 코드 수정 없음. |
| 런타임 비용 | 기존 재귀 순회에 상수 시간 경계 표시를 추가해 전체 `O(N)` 순회 유지. 새 필드·컬렉션 없음. 실제 시간·힙 사용량은 측정하지 않음. |
| 검증·확장 | Swing 창 없이 실제 파일 읽기→Controller→모델 경로 검증. 3중 구조도 같은 위치 기반 경계 표시 사용. |

STORE의 기존 방향 합치기·활성 정보 부수효과와 `return amActive`를 유지한다. LOAD에서 자식의 반환값이 실제 변경뿐 아니라 이전 활성 상태를 포함할 수 있어 보수적으로 더 계산할 수 있다. 이 반환 정책의 재정의와 STORE 부수효과 제거는 이번 범위에 넣지 않는다. [모델 계약](../Codes/GameOfLife/src/com/holub/life/model/docs/interfaces.md)에 LOAD 경계 보존 동작을 추가했다.

### 육하원칙

| 항목 | 기록 |
| --- | --- |
| 누가 | 태영의 A 모델 작업, #81 공동 담당 중 모델 수정 범위 |
| 언제 | 2026-11-17, 최신 refactor/base 기준, 커밋 전 |
| 어디서 | 원래 IntelliJ 프로젝트의 `fix/81-load-boundary`, 모델·테스트·계약 문서 |
| 무엇을 | legacy LOAD가 활성화한 자식의 위치를 경계에 표시하고, 파일 Load·직접 Memento 복원의 회귀 테스트를 추가했다. |
| 왜 | 복원 직후 안정 상태가 된 셀의 방향 정보만 읽으면 이웃 블록의 계산을 깨우지 못해 첫 세대 출생이 누락되기 때문이다. |
| 어떻게 | 테스트 기준 정의 → 기존 233개 통과 → 새 17개 중 10개 실패 재현 → 경계 표시 함수 재사용 → 관련 43개·전체 250개 통과 → 최신 base 통합 후 전체 346개 통과 → 문서·링크 점검 순서로 진행했다. |

### 검증

- 환경: macOS arm64, Eclipse Temurin JDK `26.0.2.1+1`, 저장소의 JDK 26 도구 체인. 공식 배포본을 저장소 밖 임시 폴더에 준비하고 SHA-256을 확인했다. 저장소 빌드 설정은 변경하지 않았다.
- 기준선: 기존 전체 233개 통과.
- 수정 전: 신규 17개 중 10개 실패, 오류·건너뜀 0. 클래식 경계 6개, 암세포 경계 3개, 직접 Memento 복원 1개에서 누락을 확인했다.
- 수정 후: 관련 43개 통과. 신규 17개와 #63의 Load 실패 보존, 기존 클래식·암세포 파일 테스트를 확인했다.
- 최초 수정 후 전체: 기존 233 + 신규 17 = **250개 통과**, 실패·오류·건너뜀 0.
- 최신 base 반영 후 최종 전체: **346개 통과**, 실패·오류·건너뜀 0. 신규 경계 회귀 17개도 모두 통과했다. 2인용·모드 전환 등 통합된 테스트를 함께 실행했다.
- 블록 내부·하단·우측·모서리·보드 가장자리·3중 구조에서 첫 세대와 4세대 진행을 검사했다. 암세포 종류 보존·경계 탄생, 빈 복원, Controller 밖 직접 LOAD도 확인했다.
- 관련 로컬 파일·제목 링크 43개와 `git diff --check`를 확인했다. 개인 Notion 개발 기록의 09번에도 원인·수정·대안·육하원칙·최신 base 반영과 검증 결과를 남겼다.
- UI 수동 실행과 성능 측정은 하지 않았다.

실제 Gradle 실행 명령(`Codes/GameOfLife`에서 JDK 26의 JAVA_HOME을 지정):

```sh
# 기준선
./gradlew test --no-daemon --rerun-tasks
# 수정 전 회귀 테스트
./gradlew test --tests com.holub.life.model.LegacyLoadBoundaryTest --no-daemon
# 수정 후 관련 테스트
./gradlew test --no-daemon \
  --tests com.holub.life.model.LegacyLoadBoundaryTest \
  --tests com.holub.life.application.GameControllerLoadTest \
  --tests com.holub.life.ClassicMementoTest \
  --tests com.holub.life.persistence.CancerCodecTest
# 최종 전체
./gradlew test --no-daemon
```

### 작업 중 기준 브랜치 갱신

사용자 요청으로 main·feature 통합이 반영된 최신 `origin/refactor/base` `03fe4ed`를 가져왔다. 커밋 전 #81 파일 6개를 저장소 밖에 백업하고, `git merge --ff-only origin/refactor/base`로 반영했다. 기존 HEAD가 새 base의 조상이어서 새 병합 커밋 없이 fast-forward됐고 충돌은 없었다. 반영 전후 파일별 SHA-256이 같아 진행 중인 코드·문서·새 테스트가 그대로 보존됐음을 확인했다.

추가된 2인용 경로는 `toggle`·세대 계산을 사용하고 UI 파일 명령을 지원하지 않는다. `TwoPlayerResident.transfer()`도 false의 no-op이므로 이번 LOAD 한정 분기는 정상 2인용 실행에 추가 영향을 주지 않는다. 실제 2인용·모드 전환 회귀는 최종 전체 테스트에 포함했다.

### 후속 작업

- PR base는 `refactor/base`, 연결은 `Refs #81`이다. 공동 이슈이므로 A 범위 PR 병합만으로 닫지 않고 B의 작업 범위 완료도 확인한다.
- #70·#71·#74는 별도 후속 작업이다. 규칙 Strategy 도입도 이번 버그 수정과 분리한다.

## 007. 보드별 전이 읽기 허가 분리 (#74)

- 상태: 채택 · 구현·검증 완료
- 담당 A 모델 · 태영 · 2026-11-17
- 기준 `refactor/base` `333057b`(PR #89 병합), 브랜치 `fix/74-board-transition`
- A는 `Neighborhood` 내부의 공유 상태를 담당한다. B는 [이슈 댓글](https://github.com/DesignPattern8/DPTermProject/issues/74#issuecomment-6944284960)에 따라 한 번에 한 모드만 tick하는 실행 정책을 별도로 담당한다.

### 문제

모든 `Neighborhood`가 static 읽기 허가와 중첩 깊이를 공유해 한 보드의 전이가 다른 보드의 그리기를 막는다. 두 보드가 서로 다른 스레드에서 진행하면 깊이 증가·감소가 섞여 상태가 깨질 수 있다. 중첩 셀에서 예외가 나면 기존 `transition()`은 해제 코드까지 도달하지 않아 읽기가 계속 금지된다.

### 대안과 결정

| 후보 | 이 코드에서의 결과 | 판단 |
| --- | --- | --- |
| A. 블록마다 인스턴스 필드 | 간단하지만 같은 루트의 먼저 끝난 자식과 아직 시작하지 않은 형제 블록이 읽기를 허용한다. 기존 전체 보드 단위 허가를 보존하지 못함. | 기각 |
| B. 트리별 비공개 공유 상태 | 루트가 읽기 허가·중첩 깊이를 만들고 중첩 블록 생성에 같은 객체를 전달. 공개 생성자·create()는 독립 상태를 만든다. | 채택 |
| C. Facade에서 상태 주입 | CompositeBoard뿐 아니라 legacy 직접 생성·2인용 생성과 조립 경로까지 바꿔야 한다. | 이번 범위에서 기각 |
| D. 기존 공유 상태 + 실행 직렬화만 문서화 | 앱의 현재 ModeSwitch에는 맞지만 모델의 보드 간 간섭과 예외 시 영구 잠김이 남는다. | B의 실행 정책과 별개로 모델 개선 필요 |

비공개 상태 객체를 트리마다 공유하고 `try/finally`로 중첩 깊이·읽기 허가를 복구한다. 같은 프로토타입으로 여러 보드를 만들어도 상태를 공유하지 않으며, 공개 `create()`도 새 루트 상태를 만든다. 새 GoF 패턴을 추가하는 작업은 아니다.

### 검증 계획

[모델 테스트 기준 GATE-01~08](../Codes/GameOfLife/src/com/holub/life/model/docs/test-cases.md#보드별-전이-읽기-허가-74)을 먼저 정의했다. JDK 26.0.2.1+1에서 기존 전체 346개가 통과했다. 전이 중 관찰·두 스레드의 정확한 실행 순서를 콜백·래치로 고정해 실패를 재현하고 수정 후 전체 회귀를 확인한다.

### 전후 코드와 수정 이유

```java
// 전: 서로 다른 보드까지 하나의 상태를 사용하고 예외가 나면 복구하지 못함
private static final ConditionVariable readingPermitted = new ConditionVariable(true);
private static int nestingLevel = -1;
// ... transition(): 깊이 증가 → 순회 → 깊이 감소·허가 복구

// 후: 한 트리에만 공유되는 비공개 상태
private static final class TransitionState {
  private final ConditionVariable readingPermitted = new ConditionVariable(true);
  private int nestingLevel = -1;
}
private final TransitionState transitionState;

// transition(): 전이 시작 시 해당 보드만 닫고 모든 종료 경로에서 복구
if (++transitionState.nestingLevel == 0) transitionState.readingPermitted.set(false);
try {
  // 기존 셀 전이와 활성 경계 수집
} finally {
  if (transitionState.nestingLevel-- == 0) transitionState.readingPermitted.set(true);
}
```

`TransitionState`의 **타입 선언만 static**이며, 그 안의 허가·깊이는 인스턴스 필드다. 공개 생성자는 새 상태를 만들고, 비공개 생성자는 같은 루트의 중첩 블록에 그 상태를 전달한다. `Neighborhood` 프로토타입을 복제할 때도 새 트리의 상태를 사용한다. 공개 `create()`는 새 루트의 공개 생성자로 위임하므로 원본과 공유하지 않는다. `beginRedraw()`는 소속 보드의 허가를 읽는다.

`StaticLockCheck`는 삭제된 private static 필드의 반사 조회·강제 초기화를 제거하고, 공개 `beginRedraw(true)`로 검사한다. 두 Clock 진단은 각 보드 100 tick이 끝났다는 래치를 기다린 뒤 관찰하고 finally에서 Clock·Timer를 종료한다. 기존 `ModeSwitch`의 한 모드 실행 정책은 바꾸지 않고, static 상태를 현재 원인으로 설명하던 주석만 맞췄다.

### 실제 비용

| 항목 | 결과 |
| --- | --- |
| 구조 비용 | 제품 동작 변경은 `Neighborhood.java` 1개, 559→552줄(39줄 추가·46줄 삭제). `ModeSwitch.java`는 주석 1줄 교체, 109줄 유지. 비공개 중첩 타입 1개 추가, 공개 타입·API·외부 의존성 증감 0. |
| 테스트 비용 | `NeighborhoodReadGateTest` 1개·352줄, 6개 메서드·10개 실행 추가. 기존 수동 `StaticLockCheck` 130→135줄(52줄 추가·47줄 삭제). |
| 간접성·결합도 | 전이·그리기에서 상태 필드 접근 1단계 추가, 새 메서드 호출 계층 없음. 기존 ConditionVariable 의존 유지, AWT·Swing 의존 추가 없음. B·C의 공개 호출부 변경 없음. |
| 런타임 비용 | 루트 생성마다 상태 객체·ConditionVariable 각 1개, 각 Neighborhood마다 상태 참조 1개. 기존에는 전체에 ConditionVariable 1개였다. 기존 재귀 순회와 O(N) 계산량 유지, tick마다 객체 할당 추가 없음. 실제 시간·힙 크기는 측정하지 않음. |
| 검증·확장 | Swing 창 없이 실제 Composite 트리와 public API로 확인한다. 중첩 깊이가 늘어도 같은 루트 상태를 전달하며, 공개 생성·create() 경로의 추가 수정이 필요 없다. |

### 육하원칙

| 항목 | 기록 |
| --- | --- |
| 누가 | 태영의 A 모델 작업. #74 공동 담당 중 모델 상태 격리와 실패 복구 범위. |
| 언제 | 2026-11-17, PR #89 병합 후 최신 refactor/base에서 착수. |
| 어디서 | 원래 IntelliJ 프로젝트의 `fix/74-board-transition`, 모델·회귀 테스트·공개 계약·GTY. |
| 무엇을 | 전이 읽기 허가·깊이를 보드 트리별로 분리하고 finally로 복구했다. 독립 보드·복제·예외 회귀와 수동 진단을 보완했다. |
| 왜 | 전역 상태 때문에 다른 보드까지 읽기를 막고 동시 전이가 서로 간섭하며, 예외 발생 시 읽기 금지가 남기 때문이다. 단순히 블록별 필드로 바꾸면 같은 보드의 조기 그리기를 허용해 잘못된다. |
| 어떻게 | 테스트 기준 정의 → 기존 전체 346개 통과 → 새 10개 중 7개 실패 재현 → 트리별 상태 전달·finally 적용 → 관련 49개·전체 356개 통과 → 수동 두 Clock 진단·문서·검토 순서로 진행했다. |

### 검증 결과

- 환경: macOS arm64, Eclipse Temurin JDK `26.0.2.1+1`, 저장소의 Java 26 도구 체인. 저장소 빌드 설정·의존성 변경 없음.
- 기준선 전체 **346개 통과**. 새 테스트는 구현 전에 추가해 정상·경계 8개 중 5개, 실패 복구 2개 중 2개의 실패를 확인했다. 기존 static 상태가 실패 테스트 사이에 남지 않도록 실패 복구 그룹은 별도 Gradle 실행으로 재현했다.
- 수정 후 관련 **49개 통과**: 신규 읽기 허가 10개, 기존 렌더링 5개, Load 경계 17개, 2인용 규칙 17개.
- 최종 전체 **356개 통과**, 실패·오류·건너뜀 0. 클래식·암세포·2인용·모드 전환·파일 호환 테스트를 포함한다.
- 새 테스트는 2중·3중 트리 전체의 허가, 별도 루트·공유 프로토타입·공개 create() 격리, 다른 보드/같은 트리 중첩 호출, 래치로 순서를 고정한 두 스레드, RuntimeException·AssertionError 후 원래 오류 전파·다음 전이 복구를 검사한다. sleep이나 확률적 타이밍에 기대지 않는다.
- 수동 `StaticLockCheck`: 순차 실행 뒤 false **0/200000**, 두 스레드 5회 모두 **0/10000**, Fast 속도의 실제 두 Clock이 각 100 tick을 완료한 뒤 **0/10000**. 무작위 스케줄의 보조 진단이며 결정적 JUnit 검증을 대신하지 않는다.
- 공개 계약과 현재 2인용·UI 문서의 static 공유 설명을 갱신했다. 코드·호출부·정리 경로를 별도로 읽어 검토했다.
- 네이티브 UI 수동 조작과 성능·힙 벤치마크는 하지 않았다. 전후 클래스 그림과 같은 개발 기록은 개인 Notion의 10번에 남긴다.

실제 실행 명령(`Codes/GameOfLife`에서 JDK 26의 JAVA_HOME 지정):

```sh
# 기준선
./gradlew test --no-daemon --rerun-tasks
# 수정 후 관련 테스트
./gradlew test --no-daemon \
  --tests com.holub.life.model.NeighborhoodReadGateTest \
  --tests com.holub.life.RenderingExtractionTest \
  --tests com.holub.life.model.LegacyLoadBoundaryTest \
  --tests com.holub.life.twoplayer.TwoPlayerRuleTest
# 최종 전체
./gradlew test --no-daemon
# 기존 수동 진단 (test로 컴파일한 뒤)
java -ea -Djava.awt.headless=true \
  -cp build/classes/java/main:build/classes/java/test \
  com.holub.life.twoplayer.ui.StaticLockCheck
```

#74는 REFACTOR 제목의 이슈지만 보드 간 간섭과 실패 후 상태를 바로잡아 동작이 달라진다. 따라서 이번 브랜치·PR은 `fix`와 BUG 유형으로 분류하고, 기존 한 보드의 정상 동작은 회귀 테스트로 유지한다. 커밋은 테스트·버그 수정·문서/주석으로 구분하고 `refactor/base` 대상으로 일반 PR을 작성한다.

### 계약과 후속 작업

- 같은 보드의 수정은 호출자가 기존처럼 직렬화한다. 이 상태는 전이 중 그리기의 진입 허가이며, `beginRedraw()` 이후 전체 프레임을 잠그는 read lock을 새로 제공하지 않는다.
- 예외는 그대로 전파한다. finally는 조정 상태를 복구하며 이미 반영된 셀을 rollback하지 않는다.
- 기존 private static 필드를 반사로 읽던 수동 `StaticLockCheck`는 public 동작 관찰로 갱신했다. 과거 재현 결과는 기존 문서·커밋 이력에 보존한다.
- `Refs #74`, PR base는 `refactor/base`. 공동 이슈이므로 A의 모델 처리와 B의 실행 정책 완료 범위를 구분한다.


## 008. 모델 좌표 계약의 Point 의존 제거 (#71)

- 상태: 채택 · 구현·검증 완료
- 담당 A 모델 · 태영 · 2026-11-17
- 기준 `refactor/base` `c2d09a9`(PR #90 병합), 브랜치 `refactor/71-model-coordinates`

### 문제와 성공 기준

`Cell.transfer`·`Cell.Memento`·`NeighborhoodState`가 AWT의 가변 Point를 노출해 모델 구현과 저장 미지원 2인용 셀까지 java.desktop에 의존한다. 기존 `CellPosition(row, column)`으로 통일하고, 같은 배치·암 상태·블록 순회 순서·중복·범위 밖 좌표 해석·파일 실패 처리를 보존한다. 파일에서만 Point의 `x=column`, `y=row`를 사용한다.

성공 기준은 모델과 UI 제외 2인용의 Point/AWT import 0, 기존 파일과 신규 파일 상호 호환, 같은 특성 테스트의 전후 통과, 기존 전체 회귀 통과다. 추가로 해당 제품 소스 30개(ConditionVariable 포함)를 java.base만 보이는 상태에서 컴파일해 java.desktop 결합 제거를 확인한다. 수정 전에는 Point import로 컴파일이 실패했다.

### 시형 작업과의 경계

- [#71의 B 댓글](https://github.com/DesignPattern8/DPTermProject/issues/71#issuecomment-6944309778)은 Cell·Memento의 시그니처 판단을 A·C에 맡기고, B 저장·불러오기 전환을 별도 책임으로 구분했다.
- 시형의 [#88](https://github.com/DesignPattern8/DPTermProject/issues/88)은 Board.snapshot()/restore() 전환 작업이다. [최신 댓글](https://github.com/DesignPattern8/DPTermProject/issues/88#issuecomment-6945278696)에서 SNAP-01~08을 정의하고 C의 필터링·저장 순서 결정을 기다리는 상태였다. 조사 시점에 열린 PR과 #88 공개 원격 작업 브랜치는 없었다. 미공개 로컬 작업은 확인할 수 없다.
- 이번 A 범위는 모델 계약·구현과 필수 소비자의 좌표 타입 호환이다. `GameController`의 원점 인자와 `PersistenceMapper`의 컬렉션 복사만 맞추며, 생성자/fileCells 제거·snapshot/restore 전환·필터링·저장 순서 변경은 #88로 남긴다.
- C의 `LegacyLifeCodec`은 이미 CellPosition과 Point를 변환하므로 제품 코드 수정이 필요 없다. 직렬화 클래스·UID·필드 타입·입력 필터·원자 저장은 유지한다. 이 판단을 실제 파일 호환 테스트로 검증하며 새로운 팀원 합의가 있었다고 주장하지 않는다.

### 대안과 결정

| 후보 | 적용과 비용 | 판단 |
| --- | --- | --- |
| A. 기존 CellPosition 재사용 | Cell/Memento의 좌표와 가변 State의 원소 타입을 바꾸고 소비자 타입만 맞춘다. 신규 제품 타입·외부 의존성 없음. | 채택 |
| B. Point 유지 | headless 테스트는 가능하고 수정 비용은 없지만 모델·2인용의 java.desktop 결합과 가변 좌표 복사가 남는다. | 기각 |
| C. Memento 역할 분리 | 저장을 지원하는 Cell만 별도 계약을 구현하게 한다. 모델·순회·파일 소비자의 책임 재설계가 필요하다. | #70 등 별도 역할 정리에서 검토 |
| D. Board Facade 경로로 이전 | B의 snapshot/restore 전환으로 앱의 legacy 호출은 줄지만 Cell 내부 좌표 문제는 남는다. | #88의 B 작업으로 분리 |

기존 Composite/Memento·Board Facade 구조를 유지한다. 이번에는 새 GoF 패턴을 추가하지 않는다. 순서는 테스트 기준 정의 → 기존 API의 특성 테스트 → 좌표 타입 전환 → 관련/전체 테스트 → 모듈 의존·문서 검증이다.

### 전후 코드와 수정 이유

```java
// 전: Point는 x=열, y=행이며 블록 안에서 translate로 계속 변경
boolean transfer(Memento memento, Point corner, boolean load);
Point upperLeft = new Point(corner);
// 자식 호출 후 upperLeft.translate(...)

// 후: 기존 불변 모델 좌표에 자식의 오프셋을 더해 전달
boolean transfer(Memento memento, CellPosition corner, boolean load);
CellPosition upperLeft =
    new CellPosition(
        corner.row() + row * subcellWidth, corner.column() + column * subcellWidth);
// 같은 재귀 블록 순서와 기존 LOAD/STORE 상태 처리를 유지
```

`Cell`·중첩 `Memento`와 구현체 `Resident`·`Neighborhood`·`TwoPlayerResident`의 좌표 인자를 바꿨다. `NeighborhoodState`의 raw Collection과 Set<Point>는 Collection<CellPosition>·Set<CellPosition>이 된다. 컬렉션은 같은 참조를 주고받는 기존 가변 계약을 유지하지만 원소가 불변이므로 좌표를 다시 복사하지 않는다. mark 메서드는 기존 Point 복사 생성자의 null 거부를 Objects.requireNonNull로 보존한다. 진단용 toString은 CellPosition의 row/column 표기가 되며, 컬렉션에 직접 null을 넣고 toString을 호출한 경우의 기존 NPE도 유지한다.

현재 B의 `GameController`는 원점 인자만 바꾸고 잠금·Clock·실패 순서·알림·fileCells 필드를 유지한다. `PersistenceMapper`는 이미 같은 좌표 값을 사용하므로 캡처 시 불변 PatternData로 컬렉션을 복사하고 복원 시 별도의 LinkedList/HashSet을 만든다. 보드 순회·정렬·필터링·snapshot/restore 연결은 추가하지 않는다.

모델 원소 타입이 달라진 기존 테스트 10개 파일의 입력·대역·기대 타입도 맞췄다. 파일 자체를 검사하는 테스트의 Point 기대값과 예전 암세포 바이너리 fixture는 유지했다. 신규 특성 테스트는 구현 전에 Point API로 실행하고, 구현 후에는 position() 도우미의 반환 타입·생성식과 import만 바꿨다. 원점·배치·순서·예외의 모든 기대값은 같다.

### 실제 비용과 호환성

| 항목 | 결과 |
| --- | --- |
| 구조 비용 | 제품 소스 7개, 총 1285→1261줄. 35줄 추가·59줄 삭제(순감 24줄). 새 클래스/인터페이스·외부 의존성 증감 0. |
| 제품별 LOC | Cell 174→173, Resident 162→160, Neighborhood 552→549, NeighborhoodState 58→56, TwoPlayerResident 155→155, GameController 147→146, PersistenceMapper 37→22. |
| 테스트·문서 비용 | 신규 MementoCoordinateTest 258줄, 7개 메서드·8개 실행. 기존 테스트/도우미 10개 파일의 타입 호환 수정. 테스트 정의·현재 계약·GTY 등 문서 8개. 전체 변경 파일 26개. |
| 간접성·결합도 | 새 호출 계층 0. 모델·UI 제외 2인용의 AWT/Swing import 0. 기존 ConditionVariable와 Java 표준 컬렉션 사용. C·D 제품 코드 변경 없음. |
| 런타임 비용 | 기존 블록 순회·LOAD contains 조회 복잡도는 유지한다. Point의 블록별 재사용 대신 자식 호출마다 불변 CellPosition을 만든다. 64×64의 8×8 2중 트리는 한 transfer당 자식 좌표 4160개이며, 이전 블록별 Point 복사 65개와 다르다(두 수 모두 호출자의 원점 제외). 이전 STORE는 생존·암 표시의 추가 Point 복사도 있었지만 이제 원소 참조를 공유하며, Mapper의 원소별 좌표 변환도 없어진다. 코드 기준 생성 횟수 계산이며 실제 시간·힙 크기·JIT 최적화는 측정하지 않음. |
| 외부 호환 | Point 인자의 Cell/Memento 호출·구현은 소스와 바이너리 호환이 바뀌어 CellPosition으로 수정 후 재컴파일해야 한다. 공개 Board/BoardSnapshot·B Controller·C Codec API는 유지한다. .life 직렬화 형식과 C 입력 제한·원자 저장 구현도 그대로다. |

이번 작업은 기존 게임 동작과 파일 형식을 유지하는 좌표 계약 리팩토링이다. 불변 좌표가 늘어나는 비용을 숨기지 않으며, 성능 최적화나 Memento 역할 분리는 실제 필요를 확인한 별도 작업으로 둔다.

### 육하원칙

| 항목 | 기록 |
| --- | --- |
| 누가 | 태영의 A 모델 작업. 시형의 B #88 구조 전환과 C의 파일 경계를 확인했다. |
| 언제 | 2026-11-17, PR #90 병합 후 최신 refactor/base에서 착수. |
| 어디서 | 원래 IntelliJ 프로젝트의 refactor/71-model-coordinates. 모델과 필수 소비자·테스트·현재 계약 문서. |
| 무엇을 | Cell/Memento/State의 Point를 기존 CellPosition으로 통일하고 2인용의 AWT 의존과 Mapper의 좌표 변환을 제거했다. |
| 왜 | 화면 좌표 타입이 공통 모델 계약에 들어가 저장 미지원 셀까지 java.desktop에 결합하기 때문이다. 이미 있는 행·열 값 타입으로 책임 경계를 맞춘다. |
| 어떻게 | #71/#88·공개 브랜치/PR 확인 → 테스트 기준 정의 → 기존 356개·신규 특성 8개 통과 → 타입/필수 호출부 변경 → 관련 35개·전체 364개 통과 → java.base 단독 컴파일·최종 diff/문서 검토. |

### 검증 결과

- 환경: macOS arm64, Eclipse Temurin 26.0.2.1+1, 저장소 Java 26 도구 체인. 빌드 설정·의존성 변경 없음.
- 기준선: 기존 전체 **356개 통과**. 구현 전 새 특성 **8개도 통과**. 이번 작업은 실패 재현 후 수정하는 BUG가 아니라 같은 관찰을 유지하는 REFACTOR다.
- 수정 후 관련 **35개 통과**: MementoCoordinateTest 8, ClassicMementoTest, CancerCodecTest, LegacyLifeCodecTest, PersistenceMapperTest, GameControllerPersistenceTest.
- 최종 전체 **364개 통과**, 실패·오류·건너뜀 0. 기존 일반·암·2인용·Load 경계·전이 허가·잘못된 입력/필터 제한·안전 저장·UI headless 검증을 포함한다.
- 구조 검증: model·UI 제외 twoplayer·ConditionVariable 제품 소스 **30개**를 `javac --limit-modules java.base`로 컴파일했다. 수정 전에는 Point가 java.desktop에 있어 실패했고 수정 후 성공했다. 해당 범위의 AWT/Swing import도 0이다.
- 기존 파일의 Point(x=열,y=행), 중복·순서·음수 좌표, 이전 암세포 fixture와 잘못된 파일의 IOException 계약을 유지했다. C 제품 소스 diff는 0이다.
- 코드/호출부·기존 null 실패·B #88 경계·현재 계약을 별도로 읽어 검토했다. 변경 Markdown의 로컬 파일·제목 링크 104개, 추가 특성의 전후 입력·assertion 보존과 git diff --check를 확인했다.
- 네이티브 UI 수동 조작과 실제 시간·힙 벤치마크는 하지 않았다. 전후 클래스 그림과 같은 개발 기록은 개인 Notion의 11번에 남긴다.

실제 Gradle 명령(Codes/GameOfLife에서 JDK 26의 JAVA_HOME 지정):

```sh
# 기준선
./gradlew test --no-daemon --rerun-tasks
# 구현 전 같은 특성 확인
./gradlew test --tests com.holub.life.twoplayer.MementoCoordinateTest --no-daemon
# 수정 후 관련 검증
./gradlew test --no-daemon \
  --tests com.holub.life.twoplayer.MementoCoordinateTest \
  --tests com.holub.life.ClassicMementoTest \
  --tests com.holub.life.persistence.CancerCodecTest \
  --tests com.holub.life.persistence.LegacyLifeCodecTest \
  --tests com.holub.life.application.PersistenceMapperTest \
  --tests com.holub.life.application.GameControllerPersistenceTest
# 최종 전체
./gradlew test --no-daemon
```

java.base 검증은 위 30개 소스의 실제 경로를 모아 저장소 밖 임시 출력 디렉터리에 `javac --limit-modules java.base -d <임시 경로> <소스 경로들>`로 실행했다. 저장소에 별도 빌드 작업·검증 프레임워크는 추가하지 않았다.

### 계약과 후속 작업

Cell/Memento의 Point 호출은 CellPosition(row, column)으로 이전해야 하므로 소스 호환성이 바뀐다. .life 파일 형식은 바꾸지 않는다. 가변 Memento의 컬렉션·순서·중복 계약과 STORE 활성 정보 부수효과, #81 LOAD 경계 활성화, #74 보드별 전이 상태는 유지한다.

PR base는 refactor/base, 연결은 Refs #71. B #88의 구조 전환 완료와 공동 이슈 종료는 별도로 판단한다.

### 2026-11-23 · #94 병합 후 #71 검증

`refactor/base`의 `ef8ee24`를 #71 작업 브랜치에 병합했다. 두 브랜치가 함께 수정한 `GameMenuTest`에서 보드 비교에는 #71의 `CellPosition`을 쓰고, 파일 선택 결과에는 #94의 취소 처리를 적용했다. 파일 선택을 취소하면 보드가 유지되고 변경 알림과 오류 메시지가 발생하지 않는다. 존재하지 않는 파일을 선택하면 읽기 실패 메시지가 표시된다.

JDK 26에서 `./gradlew test --no-daemon --rerun-tasks`로 전체 365개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개였다.

### 2026-11-24 · #88 저장·불러오기 통합

`refactor/base`의 `6b20c07`을 병합할 때 #71의 기존 Memento 호출과 #88의 스냅샷 경로가 `GameController`에서 충돌했다. 최신 `GameController`의 `Board.restore()`·`Board.snapshot()`과 프리셋 연결을 유지했다. #71의 `Cell.transfer()`·`Cell.Memento`·`NeighborhoodState`는 `CellPosition`을 쓰며 독립 `PersistenceMapper`도 같은 좌표를 복사한다.

`SnapshotFileStoreTest`는 직렬화된 파일을 확인할 때 `Point(column, row)` 순서를 비교하고, 코덱이 읽은 좌표는 `CellPosition` 순서로 비교한다. `NativeUiSmoke`의 관찰 경로는 기존 `Point`와 새 행·열 좌표를 모두 화면 좌표로 읽는다.

JDK 26에서 `./gradlew test --no-daemon --rerun-tasks`로 전체 439개가 통과했다. 실패·오류·건너뜀은 각각 0개였다. 저장 순서·암세포 복원·불러오기 실패 후 보드 보존·프리셋 검증을 포함한다.
