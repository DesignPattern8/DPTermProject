# 화면 추출 계약

- 제공 D / 사용 조립 지점·UI / 변경 2026-11-03, #24

```java
void BoardRenderer.redraw(Cell cell, Graphics g, Rectangle here, boolean drawAll)
Point GridCoordinateMapper.position(Cell cell, Point here, Rectangle surface)
void GridCoordinateMapper.userClicked(Cell cell, Point here, Rectangle surface)
```

redraw는 원본처럼 Resident/Neighborhood/DUMMY를 그린다. drawAll=false는 안정된 블록을 건너뛰고 안정화 직후 마지막 갱신을 소비한다. 전달받은 Graphics는 닫지 않고 필요한 복제본만 dispose한다. 좌표 변환은 원본의 재귀적 나눗셈과 예외를 보존한다. userClicked는 position 후 모델 toggle을 호출하는 기존 테스트·임시 호출부용 함수다. 새 UI는 position 결과를 제어 계층에 전달한다.

LifeFrame은 원본 창 제목·종료 동작·레이아웃을 유지한다. BoardPanel은 원본 크기 조정·paint·refreshNow를 소유한다. GameMenu.install은 Grid/Go 항목과 기존 Clock 리스너를 연결한다. FileDialogs.userSelected는 원본 JFileChooser 코드이며 취소도 FileNotFoundException으로 전달하여 기존 오류 대화상자를 유지한다.
