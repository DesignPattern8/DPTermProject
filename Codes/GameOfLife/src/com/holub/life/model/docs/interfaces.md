# 모델 계약

- 제공 A / 사용 B·C·D / 변경 2026-11-03, #24
- 기존 Cell/Resident/Neighborhood/Direction의 패키지는 com.holub.life.model이다.

```java
void Cell.toggle(int row, int column)
NeighborhoodState Cell.createMemento()
boolean Cell.transfer(Cell.Memento state, Point upperLeftCorner, boolean load)
int Neighborhood.gridSize()
Cell Neighborhood.childAt(int row, int column)
boolean Neighborhood.isActive()
boolean Neighborhood.beginRedraw(boolean drawAll) throws InterruptedException
```

좌표 순서는 행·열이다. toggle은 기존 클릭처럼 amAlive와 상위 블록 활성 상태를 바꾼다. 경계 밖 좌표를 보정하지 않는다. Point는 x=열/y=행을 유지한다. Memento는 기존 가변 컬렉션이며 중복·순서를 보존한다. createMemento는 내부에서 transfer(STORE)를 호출한다. transfer의 활성 정보 부수효과도 유지한다. Resident와 DUMMY의 createMemento는 UnsupportedOperationException이다.

beginRedraw는 기존 마지막 갱신 플래그를 소비하고 ConditionVariable 읽기 조건을 확인한다. 읽기 접근은 기존 동시성 계약을 유지하며 새 스레드 안전성을 보장하지 않는다. UI는 childAt으로 받은 셀을 읽기·좌표 변환에만 사용한다.
