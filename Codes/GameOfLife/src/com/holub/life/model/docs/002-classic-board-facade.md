# [002] 클래식 보드 Facade

- 상태: 채택 · A 모델의 클래식 보드
- 담당: A 모델 · 2026-11-10 · [이슈 #49](https://github.com/DesignPattern8/DPTermProject/issues/49)
- 계약 초안과 인계 기준: [PR #48](https://github.com/DesignPattern8/DPTermProject/pull/48)

## 문제

[GameController.tick()](../../application/GameController.java#L28)은 보드 밖 이웃을 `Cell.DUMMY`로 채우고 세대 계산과 상태 반영을 조정한다. 실행·저장·화면에 보드 단위 연산을 제공하려면 셀 트리와 세대 계산 순서를 모델 내부에서 관리해야 한다.

## 후보

- A. 기존 Composite의 `Cell`을 계속 진입점으로 쓴다. 좌표와 스냅샷 값은 분리되지만 보드 호출부가 하위 셀 연산과 세대 계산 순서를 알아야 한다.
- B. `CompositeBoard`가 기존 `Neighborhood`를 감싸고, 호출부는 `Board`를 통해 보드 단위로 접근한다.

## 오버헤드 비교표

| 항목 | A. 기존 Composite 진입점 | B. Facade |
| --- | --- | --- |
| 구조 비용 | 좌표·스냅샷 2개 타입 추가 추정 | 모델 타입·파일 4개, 물리 줄 125줄 추가 |
| 세대 진행 호출 | 호출부가 `figureNextState()` → `transition()` 관리 | `Board.step()` 호출과 구현 위임 1단계, 기존 계산을 재사용 |
| 캡처 비용 | 셀 트리 또는 기존 Memento를 통해 생존 위치 수집 | 셀 트리 전체 순회 후 생존 위치를 불변 집합으로 복사 |
| 결합·검증 | 호출부가 `Cell`과 계산 순서에 결합 | 공개 API는 모델 타입과 `Set`을 사용하며 Swing 없이 검증 |

## 결정과 근거

B를 채택한다. `step()`은 기존 세대 계산과 반영 순서를 감싼다. `snapshot()`은 셀을 직접 읽어 기존 저장용 `createMemento()`의 경계 활성 정보 변경을 피한다. `restore()`는 크기를 확인한 뒤 전체를 비우고 생존 칸을 편집 경로로 배치한다. 이 경로는 경계에 놓인 칸이 다음 세대의 이웃 블록 계산을 깨우도록 한다. [클래식 보드 테스트](test-cases.md#클래식-보드-api-49)

## 전/후 비교

전후 클래스 다이어그램은 [Notion 개발 기록의 #49 항목](https://app.notion.com/p/40a61a0bb83480208e4accb053cf40f7)에 이미지로 보관한다. PR #50 시점의 구조와 B·C·D 후속 연동 범위를 함께 확인할 수 있다.

## 후속 작업

B·C·D는 [인계 기준](interfaces.md#후속-작업-인계)에 따라 각 호출부를 연결한다. 2인용 셀 소유자는 [#5](https://github.com/DesignPattern8/DPTermProject/issues/5)에서 다룬다.
