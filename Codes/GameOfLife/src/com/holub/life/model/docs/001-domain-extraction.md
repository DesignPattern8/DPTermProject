# [001] 모델과 화면 메서드 분리

- 상태: 채택
- 작성: Codex / 2026-11-03 / #24

## 문제
기준 e38ce7a의 Cell.java:56/63이 Graphics와 픽셀 입력을 모델에 요구한다. Neighborhood.java:286/345와 Resident.java:85/100은 계산과 UI를 함께 구현한다.

## 후보
A. 기존 클래스를 model로 단순 이동: UI 의존이 유지된다.
B. 기존 그리기·픽셀 변환을 구체 UI 클래스로 추출: 채택. 새 Visitor/Strategy/Facade는 도입하지 않는다.

## 결정과 전/후
Cell/Resident/Neighborhood/Direction/NeighborhoodState를 model로 이동한다. 규칙·재귀·transition 반환값은 유지한다. redraw는 BoardRenderer로, 픽셀 정수 나눗셈은 GridCoordinateMapper로 추출한다. 모델 toggle(row,column)이 실제 셀과 활성 경계를 갱신한다.

## 오버헤드
새 UI 구체 클래스 2개. 모델의 Graphics/Rectangle/Swing/Colors 의존을 제거한다. 기존 Memento의 Point와 ConditionVariable은 유지한다. 렌더러의 타입 분기와 읽기 접근자 호출이 추가되지만 셀·블록 재귀 횟수는 유지한다. 픽셀 변환 뒤 모델 토글을 별도 순회하므로 클릭 경로는 깊이 d에 대해 d+d 단계가 된다. 실행 시간은 측정하지 않았다.

## 후속 작업
불변 스냅샷/읽기 전용 셀 인터페이스는 새 설계를 요구하므로 이번에 도입하지 않는다. 모델은 기존 Point 좌표를 유지한다. childAt은 렌더링/좌표 변환을 위해 제공하며 UI는 반환된 셀을 직접 변경하지 않는다. #12 반환값 문제는 수정하지 않는다.
