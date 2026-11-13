# [002] 보드 렌더링 구조: Decorator

- 상태: 대체됨(→ 007)
- 작성자: 병국 / 11/3 / 브랜치 `refactor/D-ui`

## 문제

- 그리기 코드가 모델 클래스에 흩어져 있다.
  - `Resident.redraw` (`life/Resident.java:85~98`): 셀 색(살아 있음 `RED`, 죽음 `LIGHT_YELLOW`)과 격자선(`DARK_YELLOW`)
  - `Neighborhood.redraw` (`life/Neighborhood.java:286~331`): 하위 셀 영역 계산, 블록 테두리(`LIGHT_ORANGE`), 활성 블록 안쪽 테두리(`BLUE`)
- 표시 정책(색, 테두리를 그릴지)이 모델에 박혀 있어 화면 옵션을 바꾸려면 모델을 고쳐야 한다.
- 화면을 모델에서 떼어내려면 D가 같은 그림을 스냅샷만으로 다시 그려야 한다.

## 후보

### A. Decorator: 기본 셀 레이어 + 테두리 데코레이터 + 활성 블록 데코레이터
- 구조: `BoardRenderer` 인터페이스. `CellLayerRenderer`가 셀을 그리고, `BlockBorderDecorator`가 블록·보드 테두리를, `ActiveBlockDecorator`가 활성 블록 테두리를 그 위에 덧그린다. 표준 조합은 `BoardRenderer.standard()`.
- 장점: 표시 하나 = 클래스 하나라 표시를 켜고 끄는 조합이 자유롭다(예: 격자선 없는 보기). 각 레이어를 따로 테스트할 수 있다. 기능 확장(2인용 색 구분, 암세포 표시 등)에서 데코레이터 하나만 추가하면 된다.
- 단점: 클래스 4개. 레이어마다 보드를 한 번씩 순회한다. 그리는 순서가 바뀌면 겹치는 픽셀이 달라질 수 있어 순서를 검증해야 한다.

### B. 단일 렌더러 + 표시 플래그
- 구조: `BoardRenderer(boolean showBlocks, boolean showActive)` 한 클래스.
- 장점: 클래스 1개, 순회 1번. 지금처럼 표시 옵션이 2개뿐이면 가장 단순하다.
- 단점: 표시가 늘 때마다 같은 클래스에 `if`와 플래그가 늘어난다. 기능 확장 단계에서 표시가 추가될 가능성이 높다.

### C. Composite 위 Visitor
- 구조: 모델의 `Cell` 트리에 `accept(visitor)`를 두고 렌더 Visitor가 순회한다.
- 장점: 기존 Composite 순서 그대로 그려서 화면이 같을 가능성이 높다.
- 단점: Visitor가 `Neighborhood`·`Resident` 내부를 알아야 한다. 분담안의 의존 규칙("`ui`는 `Resident`·`Neighborhood` 내부에 접근하지 않는다")에 어긋나고, A가 모델 구조를 바꾸면 D도 같이 바뀐다.

## 오버헤드 비교표

| 항목 | A. Decorator | B. 단일 렌더러 (예상) | C. Visitor (예상) | 실측 (2단계) |
| --- | --- | --- | --- | --- |
| 구조 비용 | 인터페이스 1 + 클래스 3, 4개 파일 142줄 (작성 완료, 실제 카운트) | 클래스 1개 | 인터페이스 1 + 모델 3개 클래스에 `accept` 추가 | |
| 간접성 | 데코레이터 체인 2단계 | 0 | 이중 디스패치 +1 | |
| 결합도 | `ui` → `model.BoardSnapshot`만 | 같음 | `ui` → 모델 내부 클래스 | |
| 런타임 비용 | 보드 순회 3회(셀 4096 + 블록 64 + 블록 64) | 순회 1회 | 순회 1회 | 그리기 시간: 미측정 |
| 테스트 용이성 | 레이어별 단독 테스트 가능 | 플래그 조합 테스트 | 모델 객체 필요 | |
| 확장 비용 | 표시 추가 = 데코레이터 1개 추가, 기존 수정 0 | 기존 클래스 수정 | Visitor·모델 모두 수정 | |

### 사전 확인 (참고용, 커밋하지 않은 확인 코드)

D 코드를 A·B 인터페이스 요청안대로 만든 임시 스텁과 함께 컴파일했다. 그리고 같은 보드를 기존 `Neighborhood.redraw(g, rect, true)`와 `BoardRenderer.standard()`로 각각 `BufferedImage`에 그려 비교했다.
- 글라이더(블록 경계를 넘는 위치) + 블링커, 클릭 직후와 1~6세대
- 512px, 640px
- 결과: **모든 경우 픽셀 차이 0**
- 레이어로 나눠 그리는 순서가 원래 블록 단위 순서와 겹치는 픽셀에서도 같은 결과를 낸다는 뜻이다.
- 같은 방식으로 512px 보드의 17,613개 지점에서 클릭 좌표를 비교했고 `GridCoordinateMapper`와 기존 `userClicked`의 결과가 모두 같았다.
- 이 확인은 정식 테스트가 아니다. 2단계에서 UI-01, UI-06, UI-07로 다시 작성한다([test-cases.md](test-cases.md)).

## 결정과 근거 (제안)

A를 제안한다. 지금 표시 옵션은 2개라서 B가 더 단순하다. 하지만 이어서 진행할 기능 확장(1인용 암세포, 2인용 플레이어 구분)에서 표시가 늘어날 것이 분명하다. 그때 기존 클래스를 고치지 않고 데코레이터만 추가할 수 있다. C는 의존 규칙 위반이라 제외한다. 순회 3회의 비용은 2단계에서 실측하고, 문제가 되면 B로 바꾸고 이 문서를 `대체됨`으로 표시한다.

## 전/후 비교

- 전: `Neighborhood.redraw`가 블록마다 [하위 셀 그리기 → 주황 테두리 → 활성이면 파란 테두리]를 재귀로 반복
- 후: `ActiveBlockDecorator(BlockBorderDecorator(CellLayerRenderer))`. 셀 전체 → 주황 테두리 전체 → 파란 테두리 전체 순으로 그린다
- 다이어그램: 2단계에서 전/후 `.puml` 추가

## 후속 작업

- A에게 요청: `BoardSnapshot.blockSize()`, `isBlockActive()`, `isBoardActive()` ([interfaces.md](interfaces.md))
- 2단계: 그리기 시간 실측, UI-06~10 테스트 작성
