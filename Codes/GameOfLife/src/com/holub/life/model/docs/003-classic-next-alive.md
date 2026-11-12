# [003] 클래식 생사 판정 분리

- 상태: 채택
- 담당: A 모델 · 태영 · 2026-11-11 · [이슈 #52](https://github.com/DesignPattern8/DPTermProject/issues/52)

## 문제

[Resident.figureNextState()](../Resident.java#L25)는 이웃 8칸을 검증·집계한 뒤 생사 판정식을 바로 적용했다. 집계 이후 규칙을 적용하는 지점에 `nextAlive`라는 이름을 붙인다. 기존 식이 한 줄이므로 이 변경의 효과는 작은 가독성 개선이다.

## 후보

### A. 내부 함수 추출

`private static nextAlive(boolean alive, int liveNeighbors)`로 생사 판정만 계산한다. 입력 두 개로 결과를 반환하며 `figureNextState()`가 결과를 `willBeAlive`에 저장한다. 함수 추출 리팩토링으로 적용한다.

### B. Strategy 규칙 주입

`LifeRule`과 기본 규칙 구현을 두고 `Resident`가 주입받아 호출한다. 여러 규칙을 교체할 수 있다. 현재 클래식 규칙은 하나이며, 이 안은 규칙 타입·필드·생성자를 추가하고 `Resident.create()`의 복제 경로에도 규칙을 전달해야 한다. 1·2인용 공용 상태와 입력은 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)에서 논의한다.

## 오버헤드 비교표

A는 이번 소스 diff를 세었고 B는 구현 전 추정이다.

| 항목 | A. 내부 함수 추출 | B. Strategy 주입 |
| --- | --- | --- |
| 구조 비용 | 제품 타입 증감 0, `Resident.java` 1개 수정, 118 → 122줄, 5줄 추가·1줄 삭제 | 인터페이스·기본 구현 최소 2개 타입, 규칙 필드·생성자 및 복제 경로 변경 추정 |
| 간접성 | 계산한 `Resident`마다 내부 정적 함수 호출 1단계 추가 | 규칙 객체를 통한 호출 1단계 추가 추정 |
| 결합도 | import·외부 패키지·공개 시그니처 증감 0 | `Resident`가 규칙 인터페이스에 의존 |
| 런타임 비용 | 규칙 객체 생성 0, 시간 측정 없음 | 규칙 객체의 생성·공유 정책 필요, 시간 측정 없음 |
| 테스트 용이성 | 공개 세대 계산 경로를 Swing 없이 검사 | 대체 규칙 주입 가능 |
| 확장 비용 | 클래식 함수 한 곳을 편집, 모드별 교체는 별도 설계 | 합의한 입력으로 표현되는 규칙은 구현 추가·주입으로 교체 |

## 결정과 근거

A를 채택한다. 현재 요구는 클래식 판정에 이름을 붙이는 것이다. 판정 함수는 현재 생사와 살아 있는 이웃 수만 읽어 다음 생사를 반환한다. 이웃 검증·집계, 현재 상태 반영, `Cell`·`Board`의 공개 계약은 기존 경로를 사용한다.

## 전/후 비교

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

## 육하원칙

| 항목 | 기록 |
| --- | --- |
| 언제 | 2026-11-11, 이슈 #52 작업 |
| 어디서 | 원래 IntelliJ 프로젝트의 `refactor/52-classic-rule`, `model/Resident.java`와 모델 테스트·문서 |
| 누가 | 태영의 A 모델 작업 |
| 무엇을 | 생사 판정을 내부 함수로 추출하고 전이표·예외 검증을 추가했다. 전후 클래스 다이어그램은 Notion 개발 기록에 저장했다. |
| 왜 | 이웃을 세는 과정에서 규칙을 적용하는 지점을 이름으로 드러내기 위해서다. |
| 어떻게 | 테스트 기준 → 특성 테스트 → 기존 구현에서 통과 확인 → 함수 추출 → 전체 테스트 순서로 진행했다. |

## 검증

- 프로젝트가 지정한 Azul JDK 26.0.2.1에서 기존 테스트 68개를 실행한 뒤 특성 테스트를 추가했다.
- 추출 전 `./gradlew test --tests com.holub.life.model.ResidentRuleTest --no-daemon`으로 전이표 18가지와 예외 1가지가 통과했다.
- 추출 후 `./gradlew test --no-daemon`으로 기존 68개와 신규 19가지, 총 87개가 통과했다. 실패·오류·건너뜀은 0개다.
- [RULE-01·02](test-cases.md#클래식-생사-판정-분리-52)는 공개 메서드로 계산과 반영 시점, 변경 반환값, 잘못된 이웃 타입의 assert를 확인한다. Block·Blinker·Glider·경계·고정 시드 참조 비교는 기존 특성 테스트를 사용한다.

## 후속 작업

B는 [#51](https://github.com/DesignPattern8/DPTermProject/issues/51)에서 게임 실행 경로를 `Board`에 연결한다. 모드별 셀 상태와 공통 규칙 입력은 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)의 합의에 맞춘다.
