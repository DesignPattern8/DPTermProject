# [004] 타이머 구현: `javax.swing.Timer` 기반 `SwingTickSource`

- 상태: 보류. 스레드가 바뀌는 변경이라 B(#45 후속 작업)와 함께 별도 이슈·PR로 한다. 이 문서의 `SwingTickSource` 초안 코드는 #45 병합 때 제거했다(커밋 `4eeaa8b`에 남아 있음).
- 작성자: 병국 / 11/3 / 브랜치 `refactor/D-ui`

## 문제

- `Clock`은 `java.util.Timer`를 쓴다(`life/Clock.java:30`, `:58~72`). 틱은 별도 스레드에서 보드를 바꾸고, 그리기는 Swing 이벤트 스레드(EDT)와 `getGraphics()` 직접 그리기로 이루어진다.
- 두 스레드가 같은 보드를 다루기 때문에 우회 코드가 생겼다.
  - `Neighborhood`의 `ConditionVariable readingPermitted`: 계산 중에는 그리지 않음(`life/Neighborhood.java:33`, `:305~308`)
  - `Clock.menuIsActive()`: 메뉴가 열려 있으면 틱을 버림(`life/Clock.java:151`, `:171~174`). 원저자 주석에 따르면 실행 중인 게임이 열린 메뉴를 덮어 그리는 문제를 막으려는 "kluge"다.
- 분담안에서 시간 정책(언제 시작·정지하고 틱에 무엇을 할지)은 B, Swing 타이머 구현은 D 소유다.

## 후보

### A. `javax.swing.Timer`: 틱이 EDT에서 실행
- 구조: `SwingTickSource implements TickSource`. `start(ms, onTick)`이 Swing 타이머를 만들고 `setInitialDelay(0)`으로 기존처럼 바로 첫 틱을 낸다.
- 장점: 틱, 클릭, 그리기가 모두 한 스레드에서 순차 실행된다. 분담안 공통 규칙 2("보드 변경은 한 실행 흐름에서 순차 처리")를 구조로 보장한다. 그리기도 `repaint()`를 거치므로 Swing이 메뉴 같은 위쪽 컴포넌트와 그리는 순서를 맞춘다. 그래서 `readingPermitted`와 `menuIsActive()`가 막으려던 문제가 원인부터 사라질 것으로 예상한다.
- 단점: 틱 처리(보드 계산)가 오래 걸리면 화면 응답이 느려진다. Swing 타이머는 밀린 이벤트를 합치므로(coalesce) 정확한 고정 간격은 보장하지 않는다(기존은 `scheduleAtFixedRate`).

### B. `java.util.Timer` 유지 + `SwingUtilities.invokeLater`로 EDT에 넘기기
- 구조: 지금 타이머를 두고 틱 처리 전체를 EDT로 넘긴다.
- 장점: 고정 간격 일정이 유지된다.
- 단점: 스레드가 2개이므로 무엇을 어느 스레드에서 하는지 계속 관리해야 한다. 틱 처리를 EDT로 넘기면 결국 A와 같은 단일 스레드가 되면서 코드만 늘어난다.

## 오버헤드 비교표

| 항목 | A. Swing Timer | B. Util Timer + invokeLater (예상) | 실측 (2단계) |
| --- | --- | --- | --- |
| 구조 비용 | 클래스 1개(`SwingTickSource` 31줄, 실제 카운트) | 클래스 1개 + 스레드 전환 코드 | |
| 간접성 | 타이머 → onTick (+0) | 타이머 → invokeLater → onTick (+1) | |
| 결합도 | B는 `TickSource`만 알고 Swing을 모른다 | 같음 | |
| 런타임 비용 | 틱 처리 시간만큼 EDT 점유 | 같음 + 스레드 전환 | Fast(30ms)에서 틱 처리 시간, 실제 틱 간격 |
| 사라지는 우회 코드 | `readingPermitted`(잠금), `menuIsActive()` (예상, 시연으로 확인) | 같음 | |
| 테스트 용이성 | B는 가짜 `TickSource`로 테스트, D는 UI-23~26 | 같음 | |

## 결정과 근거 (제안)

A를 제안한다. 화면과 보드 변경을 한 스레드로 모으는 게 분담안의 공통 규칙과 맞고, 기존 우회 코드 두 개가 해결하던 문제를 구조로 없앤다. 간격 정확도 차이는 2단계에서 Fast(30ms) 기준으로 실제 틱 간격을 측정해 확인한다.

### 주의 사항

- `menuIsActive()`와 `readingPermitted` 제거는 각각 B·A의 코드다. 이 문서는 근거만 제공하고, 실제 제거는 소유자와 협의한다.
- "메뉴가 덮이지 않는다"는 자동 테스트로 확인하기 어렵다. 2단계에서 실행 중 메뉴를 연 화면을 기존 버전과 나란히 캡처해 근거로 남긴다.

## 후속 작업

- B와 확인: `TickSource` 시그니처 `start(int intervalMillis, Runnable onTick)` / `stop()` ([interfaces.md](interfaces.md))
- 2단계: 틱 간격 실측, UI-23~26 테스트 작성, 메뉴 덮어쓰기 시연
