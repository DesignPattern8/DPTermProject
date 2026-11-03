# [001] 기존 화면 메서드 추출

- 상태: 채택
- 작성: Codex / 2026-11-03 / #24

## 문제와 후보
기준 e38ce7a Neighborhood.java:286, Resident.java:85의 그리기가 모델에 있다. 단순 이동(A)은 결합이 남는다. 구체 BoardRenderer/GridCoordinateMapper 추출(B)을 채택한다. Visitor·Decorator·렌더링 Strategy는 추가하지 않는다.

## 결정과 전/후
redraw의 색·사각형·순회 순서를 BoardRenderer로 옮긴다. 활성 블록 테두리와 oneLastRefreshRequired/ConditionVariable 동작을 유지한다. 픽셀은 각 Neighborhood마다 정수 나눗셈하는 원본 계산을 유지하여 전체 폭으로 한 번에 나누는 계산으로 바꾸지 않는다.

## 오버헤드
구체 클래스 2개 추가. 원본 셀별 redraw 디스패치 대신 Renderer 내 기존 셀 타입 분기가 들어간다. 픽셀 좌표 변환과 모델 토글은 두 번의 트리 경로 순회가 된다. 성능은 측정하지 않았다. 전후 픽셀 해시로 출력 동일성을 검증한다.

## 후속 작업
Universe의 패널/메뉴/파일 대화상자와 Life의 프레임 구성도 이 도메인으로 추출한다. 타이머 방식·새 표시 옵션은 변경하지 않는다.
