---
name: Custom issue template
about: 해당 이슈 생성 템플릿을 사용해 주세요
title: "[REFACTOR] model API가 java.awt.Point를 노출한다"
labels: ""
assignees: ""
---

## 📑 Description

`Cell`이 `java.awt.Point`를 import하고(`model/Cell.java:3`), `Cell.transfer(Memento, Point, boolean)`(`:93`)과 `Cell.Memento`의 네 메서드(`:76-85`)가 `Point`를 받는다. 그래서 `Cell`을 구현하는 클래스는 모두 `java.awt`에 의존한다.

- 2인용 셀 `TwoPlayerResident`(#5)는 저장을 지원하지 않는데도 `transfer` 시그니처 때문에 `java.awt.Point`를 import한다. 이것이 `twoplayer` 패키지의 첫 `java.awt` 의존이다(그 전에는 0개, 006 감수하는 비용).
- model에는 이미 좌표 record `CellPosition`(`model/CellPosition.java:4`)이 있다. `Board`·`BoardSnapshot`은 `CellPosition`을 쓴다.

이번 #5에서는 고치지 않았다(model 무수정 합의).

## 🧭 관련 도메인

- 도메인: model, persistence(저장·불러오기)
- 관련 클래스: `Cell`, `Cell.Memento`, `NeighborhoodState`, `Neighborhood.transfer`, `Resident.transfer`, `persistence/LegacyLifeCodec`, `application/GameController.load()`(`new Point(0, 0)`)

## 🔌 인터페이스 영향

- [ ] 없음
- [x] 있음 → `Cell.transfer`, `Cell.Memento` 시그니처 변경. 영향: persistence, application(`GameController`), 모든 `Cell` 구현

## 🧩 설계 패턴 후보 (REFACTOR/FEAT)

- A: 패턴 없음 — `Point`를 `CellPosition`으로 바꾼다. 저장 파일 형식(`Point` 직렬화)은 persistence에서 변환한다.
- B: 패턴 없음 — 그대로 둔다. `java.awt.Point`는 Swing이 아니라서 headless 테스트에는 문제가 없다.
- C: Memento 분리 — 저장 경로(`transfer`/`createMemento`)를 `Cell`에서 빼서 저장을 지원하는 셀만 구현하게 한다(006b와 같은 인터페이스 분리).

## 📌 To Do

- [ ] 후보 비교와 오버헤드(바뀌는 파일 수, 저장 형식 호환) 기록
- [ ] persistence 담당과 저장 형식 호환 확인
- [ ] 바꾸면 `TwoPlayerResident`의 `java.awt.Point` import 제거

## 🧪 테스트 케이스 (예정)

- [ ] 정상: 저장 → 불러오기 결과가 같다 (CL-M, 암 저장 CAN-P)
- [ ] 경계: 이전 형식(`Collection<Point>`)으로 저장한 파일을 계속 불러올 수 있다
- [ ] 예외: 형식이 잘못된 파일은 지금처럼 `IOException` (#11)

## 💬 Comment

근거: [006 이슈 (c)](../../Codes/GameOfLife/src/com/holub/life/twoplayer/docs/006-two-player-cell.md)
