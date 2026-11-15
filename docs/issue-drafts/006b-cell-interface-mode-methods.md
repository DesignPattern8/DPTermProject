---
name: Custom issue template
about: 해당 이슈 생성 템플릿을 사용해 주세요
title: "[REFACTOR] Cell 인터페이스에 모드별(암) 메서드가 쌓인다"
labels: ""
assignees: ""
---

## 📑 Description

v1.0의 암세포 기능(PR #60)이 `Cell`에 `isCancer`·`setCancer`·`cureCancer`(`model/Cell.java:108-115`)를, `Cell.Memento`에 `markAsCancer`·`isCancer`(`model/Cell.java:81-85`)를 추가했다. `Cell`의 메서드는 14개가 됐고, 모든 구현이 암 메서드를 가져야 한다.

- `Cell.DUMMY`와 `Neighborhood`는 암 메서드를 "항상 false / 아무것도 안 함"으로 구현한다(`model/Cell.java:166-172`, `model/Neighborhood.java:366-376`).
- 2인용 셀 `TwoPlayerResident`(#5)도 암 메서드 3개를 할 일 없이 구현했다.
- 모드가 하나 더 생겨 같은 방식으로 메서드를 추가하면, 모든 `Cell` 구현이 또 늘어난다(인터페이스 분리 원칙 위반).

이번 #5에서는 고치지 않았다(model 무수정 합의).

## 🧭 관련 도메인

- 도메인: model (`com.holub.life.model`), 암세포 모드, 2인용 모드
- 관련 클래스: `Cell`, `Cell.Memento`, `Resident`, `Neighborhood`, `Cell.DUMMY`, `NeighborhoodState`, `application/CancerGame`, `twoplayer/TwoPlayerResident`

## 🔌 인터페이스 영향

- [ ] 없음
- [x] 있음 → `Cell`(공개 인터페이스) 변경. 영향: 암세포 모드(`CancerGame`, `ui/BoardRenderer`의 암 색), 저장 형식(`persistence/LegacyLifeCodec`), 2인용(`TwoPlayerResident`)

## 🧩 설계 패턴 후보 (REFACTOR/FEAT)

- A: 패턴 없음 — 그대로 둔다. 모드마다 할 일 없는 구현이 늘어나는 비용을 받아들인다.
- B: 인터페이스 분리 — 암 메서드를 `CancerCell extends Cell`(또는 별도 인터페이스)로 옮기고 `Resident`만 구현한다. 암 기능 쪽은 `instanceof`나 타입이 정해진 보드로 접근한다.
- C: 001 후보 C — 셀 상태를 `CellState`(모드별 enum)로 일반화하고 `Cell.state()` 하나로 암·2인용 상태를 모두 표현한다. 암 메서드는 상태 값이 된다.

## 📌 To Do

- [ ] 후보 비교와 오버헤드(고칠 파일 수, `Cell` 구현 수 3 + 2인용 1) 기록
- [ ] 암세포 모드 담당과 합의 (설계 결정 문서 작성 중)
- [ ] 바꾸면 `TwoPlayerResident`의 할 일 없는 메서드 정리

## 🧪 테스트 케이스 (예정)

- [ ] 정상: 암세포 기능 테스트(CAN-M·CAN-P·CAN-A·CAN-C)가 그대로 통과
- [ ] 경계: 클래식 특성 테스트, 2인용 TP-R·TP-B가 그대로 통과
- [ ] 예외: 암 메서드가 없는 셀에 암 연산을 요청하면 컴파일 오류 또는 명시적 예외 (후보에 따라)

## 💬 Comment

근거: [006 이슈 (b)](../../Codes/GameOfLife/src/com/holub/life/twoplayer/docs/006-two-player-cell.md), [cancer-feature-changes.md](../../Codes/GameOfLife/src/com/holub/life/docs/cancer-feature-changes.md)
