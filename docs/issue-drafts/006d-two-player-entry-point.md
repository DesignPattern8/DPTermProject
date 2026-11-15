---
name: Custom issue template
about: 해당 이슈 생성 템플릿을 사용해 주세요
title: "[FEAT] 2인용 진입점: GameController 대신 TwoPlayerGame을 부르는 경로 (#7)"
labels: ""
assignees: ""
---

## 📑 Description

2인용 보드(`NeighborhoodTwoPlayerBoard`, #5)는 `model.Board`를 구현하지 않으므로 `GameController`에 넣을 수 없다(006 W3). 넣을 수 있게 만들어도 아래 이유로 동작이 틀린다(006 W2, 측정 클래스로 확인).

- `GameController.tick()`이 `board.step()`을 직접 부른다(`application/GameController.java:65-90`). 2인용에서는 `TwoPlayerGame.tick()`이 세대 진행의 주인이라, 이 경로로는 단계·K세대 카운트·전멸 판정이 모두 빠진다.
- 클릭은 `board.toggle(CellPosition)`(`:46-51`)이라 소유자를 넘길 수 없다. 2인용 입력은 `select`·`confirm`·`pass`다(2인용 004).
- `CompositeBoard`로 감싸면 `toggle`은 칸을 살리지 못하고, `snapshot`/`restore`는 소유자를 잃는다(006 "`model.Board`와의 관계" 실측).

그래서 #7에서는 `Clock`만 재사용하고, `TwoPlayerGame`을 부르는 2인용 진입점을 따로 둔다. **`GameController`(application, B 도메인)는 바꾸지 않는다.** 다만 메뉴·패널(`LifeFrame`, `GameMenu`, `BoardPanel`)이 모드에 따라 어느 진입점을 쓸지는 B 도메인 담당과 맞춰야 한다.

## 🧭 관련 도메인

- 도메인: application(B 도메인), ui, 2인용 모드
- 관련 클래스: `application/GameController`, `application/Clock`, `ui/LifeFrame`, `ui/GameMenu`, `ui/BoardPanel`, `twoplayer/TwoPlayerGame`, `twoplayer/NeighborhoodTwoPlayerBoard`

## 🔌 인터페이스 영향

- [ ] 없음
- [x] 있음 → 새 2인용 진입점 추가(`twoplayer` 또는 `application`). `GameController`는 그대로. 모드 선택 방식에 따라 `LifeFrame`·`GameMenu` 생성 경로가 바뀔 수 있음 → B 도메인·ui 담당과 협의

## 🧩 설계 패턴 후보 (REFACTOR/FEAT)

- A: Facade — `GameController`와 같은 역할의 2인용 컨트롤러(`TwoPlayerController`)를 따로 두고, UI 입력을 `select`·`confirm`·`pass`·`tick`으로 옮긴다.
- B: State/Strategy — UI가 "현재 모드" 객체를 갖고 클릭·tick을 모드에 위임한다(클래식 = `GameController`, 2인용 = 새 컨트롤러).
- C: Adapter — 2인용 보드를 `Board`로 감싸 `GameController`에 넣는다. **기각 예상**: 위 Description의 세 가지가 깨진다(006 W2).

## 📌 To Do

- [ ] B 도메인 담당에게 006 W2·W3 결과 공유
- [ ] #7 설계 결정 문서에서 후보 비교 (모드 전환, Store 비활성화 포함)
- [ ] 2인용 렌더러(006a)와 함께 진행

## 🧪 테스트 케이스 (예정)

- [ ] 정상: 2인용 모드에서 클릭이 토글이 아니라 현재 플레이어의 임시 배치로 동작한다 (TP-U-01)
- [ ] 경계: 2인용 → 클래식으로 돌아오면 기존 기능이 그대로 동작한다 (TP-U-08)
- [ ] 예외: 2인용 모드에서 Store를 쓸 수 없다 (TP-U-06)

## 💬 Comment

근거: [006 이슈 (d), "`model.Board`와의 관계"](../../Codes/GameOfLife/src/com/holub/life/twoplayer/docs/006-two-player-cell.md). B 도메인 담당에게 알릴 것.
