# [001] 화면 갱신 방식: 스냅샷 Observer + repaint

- 상태: 대체됨(→ application/003 D4). B가 #44에서 `GameListener.boardChanged(Change)`로 구현했다. `BoardSnapshot` 인자는 A가 만든 뒤 다시 논의한다.
- 작성자: 병국 / 11/3 / 브랜치 `refactor/D-ui`

## 문제

- 모델이 직접 화면을 그린다. `Universe.paint()`가 `outermostCell.redraw(g, ...)`를 부르고(`life/Universe.java:196~205`), 그리기 코드는 `Resident`·`Neighborhood` 안에 있다. 모델이 `java.awt.Graphics`에 의존한다.
- 틱마다 `Universe.refreshNow()`가 `getGraphics()`로 얻은 그래픽에 직접 그린다(`life/Universe.java:212~230`). Swing 그리기 체계(`repaint` → `paintComponent`)를 거치지 않아 다른 컴포넌트(열린 메뉴 등)와 그리는 순서를 맞출 수 없다.
- 틱은 `java.util.Timer` 스레드에서 온다. 그래서 계산 중에는 읽지 못하게 막는 `ConditionVariable readingPermitted`가 `Neighborhood`에 있다(`life/Neighborhood.java:33`, `:305~308`).
- 클릭·Clear·Load 뒤에는 `Universe`가 `repaint()`를 직접 부른다. 무엇이 바뀌었을 때 다시 그려야 하는지를 화면 쪽이 알고 있다.

## 후보

### A. Observer: 컨트롤러가 변경 후 스냅샷을 push → `repaint()`
- 구조: `BoardPanel implements GameListener`. B가 보드를 바꿀 때마다 `boardChanged(snapshot)`를 부르고, 패널은 스냅샷을 저장한 뒤 `repaint()`만 한다. 실제 그리기는 `paintComponent`에서 한다.
- 장점: 화면은 "언제 바뀌었는지"를 몰라도 된다. 변경 불가능한 스냅샷을 그리므로 계산 중 읽기를 막는 잠금이 필요 없다. 기존 `Publisher`·`Clock.Listener`와 같은 패턴이라 팀이 이해하기 쉽다. 통계 패널 같은 다른 화면도 같은 알림을 구독할 수 있다.
- 단점: 매번 전체 보드를 그린다. 기존 코드는 바뀐 블록만 다시 그렸다. 알림 시점을 B가 빠짐없이 지켜야 한다.

### B. Pull: 틱마다 화면이 컨트롤러에 현재 상태를 묻는다
- 구조: `SwingTickSource`의 틱이나 Swing 타이머에서 `controller.snapshot()`을 읽어 그린다.
- 장점: 알림 인터페이스가 필요 없다.
- 단점: 정지 상태에서 클릭·Clear·Load를 하면 틱이 없어서 화면이 갱신되지 않는다. 결국 이런 경우마다 따로 `repaint()`를 불러야 해서 지금의 `Universe`와 같은 문제가 남는다. 화면이 시간 정책(B 소유)에 묶인다.

### C. 기존 방식 유지: 모델이 직접 그리기
- 장점: 변경이 없다. 바뀐 블록만 그리는 최적화가 그대로다.
- 단점: 역할 분담안의 핵심 목표(모델과 화면 분리)를 이룰 수 없다. 모델을 Swing 없이 테스트할 수 없다.

## 오버헤드 비교표

| 항목 | A. Observer (예상) | B. Pull (예상) | C. 유지 | 실측 (2단계) |
| --- | --- | --- | --- | --- |
| 구조 비용 | 인터페이스 1개(`GameListener`, B 소유) + 패널 메서드 1개 | 인터페이스 0개, 대신 갱신 지점마다 `repaint` 호출 | 0 | |
| 간접성 | 변경 → 알림 → `repaint` → `paintComponent` (+1단계: 알림) | 틱 → 조회 → 그리기 | 직접 호출 | |
| 결합도 | `ui` → `application`(인터페이스), `model`(스냅샷). 모델의 `java.awt` 의존 제거 | 같음 + 시간 정책 결합 | 모델이 `java.awt` 의존 | |
| 런타임 비용 | 매번 64×64 전체 그리기 | 같음 + 바뀌지 않아도 그림 | 바뀐 블록만 | tick당 그리기 시간: 미측정 |
| 테스트 용이성 | 스냅샷만 넘기면 headless로 그리기 검증 가능 | 컨트롤러 대역 필요 | Swing 필요 | |
| 확장 비용 | 다른 화면은 리스너 등록 1줄 | 화면마다 폴링 | 모델 수정 필요 | |

## 결정과 근거 (제안)

A를 제안한다. 64×64 보드를 매번 전부 그리는 비용은 바뀐 블록만 그리는 기존 방식보다 크다. 하지만 기존 최적화는 모델 안의 상태(`amActive`, `oneLastRefreshRequired`)와 잠금(`readingPermitted`)에 기대고 있어서 화면을 분리하면 유지할 수 없다. 전체 그리기 비용은 2단계에서 실측해 틱 간격(가장 빠른 Fast 30ms) 안에 들어오는지 확인한다. 들어오지 않으면 바뀐 영역만 `repaint(Rectangle)`하는 방식으로 보완한다.

## 전/후 비교

- 전: `Clock`(Timer 스레드) → `Universe` 익명 리스너 → `figureNextState`/`transition` → `refreshNow()` → `getGraphics()` → `Neighborhood.redraw` → `Resident.redraw`
- 후: `TickSource` → `GameController`(B) → 보드 진행(A) → `GameListener.boardChanged(snapshot)` → `BoardPanel.repaint()` → `paintComponent` → `BoardRenderer`
- 다이어그램: 2단계에서 `diagrams/`(이 폴더 아래)에 전/후 `.puml`로 추가

## 후속 작업

- 2단계: tick당 그리기 시간 실측(측정 코드와 환경 기록)
- B와 확인: 모든 보드 변경 뒤 알림 호출
