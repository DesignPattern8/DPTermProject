---
name: Custom issue template
about: 해당 이슈 생성 템플릿을 사용해 주세요
title: "[REFACTOR] BoardRenderer가 Resident만 그린다 (2인용 셀이 화면에 나오지 않음)"
labels: ""
assignees: ""
---

## 📑 Description

`ui/BoardRenderer.redraw()`는 `instanceof Neighborhood`면 하위 셀을 재귀로 그리고, `instanceof Resident`일 때만 칸을 칠한다(`ui/BoardRenderer.java:14`, `39-42`). 두 조건에 맞지 않는 셀은 **아무것도 그려지지 않고 컴파일 오류도 없다.**

2인용 셀 `TwoPlayerResident`는 `Resident`가 아니라 `Cell`을 직접 구현한다(006 후보 F, model 무수정). 그래서 지금 렌더러로는 2인용 보드가 빈 화면으로 나온다. 색도 `isAlive()`·`isCancer()` 삼항(`:41-42`)이라 A·B 색을 고를 정보가 없다.

이번 #5에서는 고치지 않았다(model·ui 무수정 합의). #7(2인용 UI) 전에 어느 쪽으로 할지 정해야 한다.

## 🧭 관련 도메인

- 도메인: UI 렌더링 (`com.holub.life.ui`), 2인용 모드
- 관련 클래스: `BoardRenderer`, `BoardPanel`(`:73`, `:88`에서 호출), `twoplayer.NeighborhoodTwoPlayerBoard`, `twoplayer.TwoPlayerResident`

## 🔌 인터페이스 영향

- [ ] 없음
- [x] 있음 → 후보 B·C를 고르면 `BoardRenderer.redraw` 시그니처나 셀 쪽 공개 메서드가 바뀐다. 영향: ui 담당, 1인용·암세포 모드(같은 렌더러 사용)

## 🧩 설계 패턴 후보 (REFACTOR/FEAT)

- A: 패턴 없음 — 2인용 렌더러를 `twoplayer` 안에 따로 만든다. `Neighborhood`의 공개 API(`childAt`·`gridSize`·`isActive`·`beginRedraw`, `model/Neighborhood.java:285-303`)와 `NeighborhoodTwoPlayerBoard.owner(row, column)`을 쓴다. `ui` 무수정.
- B: Strategy — 렌더러의 칸 색 결정을 "셀 → 색" 전략(팔레트)으로 바꾸고, 분기를 "`Neighborhood`가 아닌 셀"로 넓힌다. 모드마다 팔레트만 넘긴다.
- C: Visitor — 렌더러를 `CellVisitor`로 바꿔 새 셀 종류를 빠뜨리면 컴파일 오류가 나게 한다(001 후보 D. `model` ↔ `twoplayer` 순환 위험).

## 📌 To Do

- [ ] 후보 A~C 비교와 오버헤드 기록 (ui 또는 2인용 `docs/`)
- [ ] #7과 일정 맞추기 (2인용 렌더러가 필요한 시점)
- [ ] `owner()`를 `TwoPlayerBoard` 인터페이스에 올릴지 결정 (올리면 `StubBoard`에도 필요, 006 후속 작업)

## 🧪 테스트 케이스 (예정)

- [ ] 정상: A·B 셀이 있는 보드를 `BufferedImage`에 그리면 A 칸·B 칸 픽셀 색이 서로 다르다 (2인용 test-cases.md TP-U-05)
- [ ] 경계: 클래식 보드 렌더링 결과가 바뀌지 않는다 (`RenderingExtractionTest` 유지)
- [ ] 예외: 지원하지 않는 셀 종류를 그릴 때 조용히 빈칸이 되지 않는다 (후보에 따라 기본색 또는 컴파일 오류)

## 💬 Comment

근거: [006 이슈 (a)](../../Codes/GameOfLife/src/com/holub/life/twoplayer/docs/006-two-player-cell.md), [001 문제 3](../../Codes/GameOfLife/src/com/holub/life/docs/001-cell-state-representation.md)
