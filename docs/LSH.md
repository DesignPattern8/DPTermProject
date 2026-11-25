# 작업 기록 (leesihyeong17 / 이시형)

- 담당: B 도메인 — 실행 제어·명령·이력 (`com.holub.life.application`)
- 기간: 2026-11-07 ~ 2026-11-25
- 상세 근거는 [`application/docs/`](../Codes/GameOfLife/src/com/holub/life/application/docs/README.md)에 있고, 이 문서는 진행 순서와 결과만 모은다(AGENTS.md 2-1).

## 한눈에 보기

### 머지된 PR

| PR | 머지 | 이슈 | 내용 |
| --- | --- | --- | --- |
| #42 | 11-07 | #41 | 버그: 불러오기·저장 실패 시 파일 스트림 미해제 |
| #45 | 11-13 | #43, #44 | B 기존 패턴 분석(003) + Clock Singleton 제거, `TickSource`·`Speed`·`GameListener` 도입 |
| #56 | 11-13 | #55 | `GameController`를 A의 `Board`에 연결, 보드 호출 직렬화(004) |
| #98 | 11-24 | #88 | 저장·불러오기를 `Board.snapshot()/restore()`로 옮기고 파일용 `Cell` 인자 제거(007) |
| #99 | 11-24 | #62 | 확장 기능 E: 프리셋 목록·배치 B API(008) |
| #101 | 11-24 | #87 | 기능 통합 후 모드별 실행 제어 재판단(006, 코드 변경 없음) |
| #105 | 11-24 | #87 | 006 M5 절에 암세포 담당 답변 반영 |

#45·#56은 `main`, #98~#105는 `refactor/base`로 머지했고 #108(11-24)로 `main`에 들어갔다.

### 남은 일 (제출 이후 후속)

| 작업 | 근거 |
| --- | --- |
| 실행 정책을 application으로 옮기는 Strategy 실행기(006 후보 B): `ModeSwitch`의 tick 분배·전환 보장을 B 테스트로 | 006 후속 작업 |
| 실행 상태 State 재검토 (상태에 따른 요청 거부 규칙이 2개 이상 생기면) | 003 D3 → 006 재판단 |
| `SwingTickSource` 도입 여부 (D와 합의) | 004 D8-D |
| Undo/Redo (Command + `BoardSnapshot` Memento) | 범위 밖으로 보류 |

## 1. 파일 스트림 해제 (#41 → PR #42, 머지)

**문제**: `GameController.load/store`가 스트림을 연 뒤 예외가 나면 `close()`까지 가지 못했다. Windows에서 열린 파일을 지울 수 없어 실패 후 파일 삭제가 안 됐다.

**순서**: 케이스 정의(`9b75deb`) → 테스트(`758f191`) → 수정(`bdf3f52`) (강령 3). 리팩토링과 섞지 않고 `fix/stream-leak` 브랜치로 따로 냈다(강령 2).

**한 것**

- `load`·`store`를 `try-with-resources`로 바꿨다. 보드 상태 기대값(EXT-A02/A03/A05/A06)은 바꾸지 않았다.
- 테스트 APP-S01~S04 (`GameControllerTest`): 정상 Load, 잘못된 파일, 0바이트 파일, 정상 Store 뒤 스트림 해제 확인
  - 판정 방법 2가지: Windows는 파일 삭제 성공 여부, Linux·macOS(CI)는 열린 파일도 지워지므로 `UnixOperatingSystemMXBean.getOpenFileDescriptorCount()`가 늘지 않았는지

**결과**: 수정 전 Windows에서 APP-S02·S03 실패(열린 파일 삭제 불가), 수정 후 통과.

**한계**: 출력 파일을 연 뒤 직렬화(`LegacyLifeCodec.flush`)가 실패하는 경우는 `GameController`가 정적 함수를 직접 호출해 대역을 넣을 수 없어 테스트하지 못했다. 이 문제가 003의 P7로 이어졌다.

## 2. B 기존 패턴 분석과 리팩토링 (#43, #44 → PR #45, 머지)

### 분석 (#43, [003](../Codes/GameOfLife/src/com/holub/life/application/docs/003-pattern-analysis.md))

#25 도메인 추출 이후 코드(`4afc828`)에서 B에 남은 문제를 파일:줄 근거로 정리했다.

| ID | 문제 |
| --- | --- |
| P1 | `Clock` Singleton의 이유(생성자에서 메뉴 생성)가 #25에서 사라졌고, 공개 생성자도 있어 이미 강제되지 않음 |
| P2 | `java.util.Timer`에 직접 의존. 테스트는 `Timer`를 상속해 예약을 하루 뒤로 미루는 우회 |
| P3 | tick(Timer 스레드)과 클릭 편집(Swing 스레드)이 같은 보드를 바꿈 |
| P4 | 속도 정책이 UI에 있음. 메뉴 이름 첫 글자로 주기 선택, 정지는 `startTicking(0)` |
| P5 | 화면 갱신 호출(`repaint`/`refreshNow`)이 UI 4곳에 흩어짐 |
| P6 | `Publisher` 구독자 타입이 `Object`, 사용처는 `Clock` 하나 |
| P7 | `GameController`가 구체 클래스·정적 함수를 직접 호출해 대역 불가 |
| P8 | 한 세대 진행 규약(이웃 8칸 `DUMMY`, `figureNextState` → `transition`)을 B가 앎 |

결정마다 후보를 비교했고, 다루지 않은 GoF 패턴도 B 코드에 맞는지 표로 확인했다(003 5장).

| 결정 | 후보 | 채택 | 이유 |
| --- | --- | --- | --- |
| D1 Clock 생성 | Singleton 유지 / 생성자 주입 | 생성자 주입 | Singleton의 이유가 사라졌고 테스트 격리를 막음 |
| D2 타이머 분리 | 그대로 / Strategy `TickSource` / Bridge / Template Method | Strategy | 대역 테스트가 쉽고 D의 `SwingTickSource`를 그대로 받음. 나머지는 이 구조에서 얻는 것이 없음 |
| D3 실행 상태·속도 | 문자 분기 / enum `Speed` / State | enum | 상태별로 다른 규칙이 하나뿐이라 State는 과함. 새 기능이 규칙을 늘리면 재검토 |
| D4 변경 알림 | 호출부마다 갱신 / Observer `GameListener` | Observer | 갱신 책임을 한 곳으로, B → UI 의존 없음 |
| D5 `GameController` 역할 | Facade / Mediator | Facade로 기술 | 호출 방향이 한쪽 |
| D6 Memento 보관 | — | 유지 | 바꿀 이유 없음 |

D가 `refactor/D-ui`에 올린 B 인터페이스 요청안(`TickSource`, `boardChanged`, `start/stop/step`, `toggleCell`)도 같은 문서 6장에서 항목별로 수락·보류를 정리했다.

### 구현 (#44)

**순서**: 케이스 정의(`d6f4d03`) → 테스트(`80cb382`) → 구현(`1eafc48`) → 실측·계약 문서(`b543703`)

**실측** (기준 `bdf3f52`, 물리 행 수)

| 항목 | 전 | 후 |
| --- | --- | --- |
| `application` 최상위 타입 | 2 | 6 (+`TickSource`, `UtilTimerTickSource`, `Speed`, `GameListener`) |
| `Clock.java` | 98 | 63 |
| `GameController.java` | 60 | 86 |
| 변경 파일 9개 합계 | 338 | 431 (git +169 / -76) |
| tick 경로 간접 단계 | 5 | 3 |
| 화면 갱신 호출 위치 | 4곳 | 1곳 (`BoardPanel` 리스너) |
| 테스트 | 59 | 69 (APP-R01~R10) |
| 런타임 | 미측정 | 미측정 (개선 주장 안 함) |

**동작 보존**

- 기존 테스트 기대값 변경 없음. `Clock` 생성 4곳과 `GameMenu.onTick` 인자 2곳만 바꿈
- `NativeUiSmoke`(실제 Swing 창)가 수정 없이 전후 모두 통과 (Windows 11, Temurin 26.0.2)
- 스레드는 그대로. 리스너 알림 순서도 원본 `Publisher`와 같게 유지
- 달라진 점 1개: 파일 선택 취소 시 원본은 `repaint()`를 한 번 더 했지만 지금은 안 함. 보드가 그대로라 화면 결과는 같음

## 3. Board 연결과 보드 호출 직렬화 (#55 → PR #56, 머지)

([004](../Codes/GameOfLife/src/com/holub/life/application/docs/004-board-connection.md))

**문제**: A가 #49에서 `Board.step()`을 제공했는데 B는 여전히 세대 진행 규약을 직접 알고 있었다(P8). A의 계약("`Board` 호출은 B가 직렬화한다")도 지켜지지 않았다(P3).

| 결정 | 후보 | 채택 | 이유 |
| --- | --- | --- | --- |
| D7 Board 연결 | `Cell` 유지 / 편집·진행은 `Board`, 파일은 `Cell` / C 작업 후 한 번에 | 편집·진행은 `Board` | P8 해소, `Board` 대역으로 B만 테스트. 파일 경로는 C 영역이라 과도기로 둠 |
| D8 직렬화 | 그대로 / 모니터(`synchronized`) / Command 큐 / tick을 Swing 스레드로 | 모니터 | 호출이 동기로 남아 `load`의 `IOException` 전달이 그대로. Command 큐는 요청을 비동기로 바꾸고, Swing 스레드 이전은 동작 변경이라 D와 별도 합의 |

- 알림(`GameListener`)은 잠금 밖에서 보낸다. 리스너가 다시 호출하거나 오래 걸려도 잠금을 잡지 않게 하기 위해서다.
- **순서**: 결정·케이스(`330a232`) → 테스트(`32baf20`) → 구현(`e1fef50`) → 실측·다이어그램(`f45fb40`)
- **실측**: 타입 0개 추가, `GameController.java` 86 → 96, git +36 / -26, 편집·진행 경로 간접 +1(`Board` → `Neighborhood`)
- **검증**: 테스트 97 → 103 (APP-B01~B06, `GameControllerBoardTest`). APP-B05는 잠금을 지운 코드에서 실패하고(편집이 진행 중인 `step()` 안으로 들어감), 잠금이 있으면 통과하는 것을 확인했다. `NativeUiSmoke` 수정 없이 통과.

## 4. 저장·불러오기 스냅샷 경로 (#88 → PR #98, 머지)

([007](../Codes/GameOfLife/src/com/holub/life/application/docs/007-snapshot-file-connection.md))

**문제**: 004 이후에도 Load·Store는 파일용 `Cell`의 Memento 경로였다. 생성자가 같은 보드를 `Board`와 `Cell`로 두 번 받았고(둘이 같아야 한다는 계약은 주석뿐), B는 `java.awt.Point`·`NeighborhoodState`·`Cell`에 의존했다(#71의 B 쪽).

**선행**: 11.12 회의에서 C(도현)가 B에 요청한 B-1~B-5를 받았다. 소유권은 "B가 컨트롤러 연결, C가 파일 API·테스트"로 정했다. Load 실패 시 보드·Clock 유지(B-2)는 C가 #63·PR #82로 먼저 처리했고, 나는 Load 실패 시 Clock 정책에 B 담당으로 동의하는 리뷰를 남겼다(005). 스냅샷의 암세포 보존은 A의 #73·PR #83, 좌표 필터링·저장 순서는 C의 #92·PR #93(내가 승인)이 맡았다.

| 후보 | 판단 |
| --- | --- |
| A. C 파일 API 직접 호출 (패턴 없음) | **채택.** 새 타입 0, 12파일 +39 / -44행. B가 부르는 단계 Load 4→2, Store 3→2. `GameController`가 쓰는 프로젝트·AWT 타입 8→5 (`java.awt` 0) |
| B. B에 변환기(Adapter) | 기각. 같은 좌표 규칙이 B와 C 두 곳에 생긴다 |
| C. 현상 유지 | 기각. #71의 B 쪽을 해결할 수 없다 |

- **순서**: 케이스 정의(`8980376`) → 결정 대기 항목 확정(`0eca803`) → 테스트(`b2f7d00`) → 구현(`5106b08`) → 007·계약·다이어그램(`f53c159`)
- **의도된 동작 변경 SNAP-09**: 생존 목록에 없는 암세포 좌표가 든 파일은 이제 Load 실패(보드·Clock 유지). 기존에는 "죽은 암세포"라는 표현 불가능한 상태로 불러와졌다. 앱이 저장한 파일은 해당 없음
- **검증**: 전체 405 → 410 (SNAP-01·02·06·08·09). 기존 테스트 기대값 변경 없음. 다른 도메인 테스트 5곳은 생성자 인자 하나만 삭제 → #88 댓글로 공유
- #93과 공통 조상이 2개인 상태라 GitHub이 충돌로 표시했지만, `refactor/base`를 병합(`e995712`, 충돌 없음)해 해결했다

## 5. 확장 기능 E: 프리셋 배치 (#62 → PR #99, 머지)

([008](../Codes/GameOfLife/src/com/holub/life/application/docs/008-preset-placement.md), D 화면은 PR #100)

**합의 (11/17, #62 댓글)**: D(병국)가 Q1~Q11을 올렸고, B 담당 Q6~Q8을 정해 답했다.

- `presets(): List<PresetChoice>` (C 타입을 D에 노출하지 않음), `applyPreset(id, origin): PresetResult` (`APPLIED`, `OUT_OF_BOUNDS`, `UNKNOWN_ID` — 사용자 실수라 예외 대신 결과값)
- 한 칸이라도 보드 밖이면 아무것도 놓지 않음 / 이미 산 칸(암세포 포함)은 그대로 / 시계는 멈추지 않고 잠금 하나 안에서 적용 / 성공만 잠금 밖 `EDITED` 1회

| 겹친 칸 처리 후보 | 판단 |
| --- | --- |
| A. 같은 잠금 안에서 `snapshot()`으로 확인 후 죽은 칸만 `toggle` | **채택.** 새 타입 2(`PresetChoice`, `PresetResult`), 다른 도메인 변경 0. 실측 112~127 µs/회로 `Speed.FAST`(30 ms)의 0.4% 수준. 암세포도 `liveCells`에 포함되어 분기 없이 보존 |
| B. A에 `Board.set(pos, alive)` 요청 | 기각. 300배 이상 빠르지만 A 계약 변경이라 동결일에 시작할 수 없고, 암세포 칸의 의미를 새로 정해야 함 |
| C. Command 배치 | 기각. 겹침 문제를 풀지 않고 기대 이점(Undo)이 범위 밖 |

- **순서**: 케이스 정의(`ed1315b`) → 테스트(`1ee3c33`) → 구현(`e37e424`) → 008(`b89c93c`)
- **검증**: PRE-01~11 (`GameControllerPresetTest` 9, `GameControllerBoardTest` 2). 머지 후 #62에 D 진행 가능 알림
- PR #100(D)을 B 쪽에서 리뷰: API 사용·잠금 계약(006) 확인, 플래그 의미(M7)·종료 후 편집은 후속으로 기록

## 6. 기능 통합 후 재판단: 모드별 실행 제어 (#87 → PR #101, #105, 머지)

([006](../Codes/GameOfLife/src/com/holub/life/application/docs/006-mode-control.md))

**배경**: 003은 D3(State 보류)·D5(Facade)에 "새 기능이 들어오면 다시 판단한다"는 조건을 달았다. 1인용 암세포·2인용이 들어온 `v2.0` 이후, 실행 제어가 `GameController`(클래식), 같은 클래스의 `cancerGame == null` 분기(암세포), `TwoPlayerSession`(2인용), `ui/ModeSwitch`(모드 전환·tick 분배)로 나뉜 것을 문제 M1~M8로 정리했다(PM 분석 → `refactor/base` `ff54fe0` 줄 번호로 재확인).

- 후보 A(현행 유지 + 계약 문서화) / B(Strategy 실행기를 application으로) / C(State) / D(Mediator) / E(Decorator) / F(Template Method) / G(암세포 독립 모드) 비교. A만 실측, 나머지는 추정 표시
- **결정 A**: 11/24 기능 동결·11/29 제출 일정, #74가 A의 PR #90으로 해결되어 `tickLock`이 모델 안전장치에서 앱 실행 정책으로 바뀐 점, B안이 D·2인용 인터페이스를 바꾸는 점. 대신 잠금 순서(`tickLock` → 각 컨트롤러 잠금, 경로 7개 확인)와 tick 보장(TP-U-11·13·14)을 계약으로 고정
- **003 D3 재판단**: 모드 = Strategy(이미 `GameMode`) 유지. 실행 상태 State는 B가 가진 규칙이 여전히 1~2개라 계속 보류. 다시 볼 조건 명시
- **M5 (Classic 모드에 암세포가 항상 켜짐)**: 11/24 암세포 담당 답변("의도된 설계")으로 닫았다가(PR #105), 11/25 팀이 교수님께 확인해 "기본 클래식 모드는 있어야 한다"는 답을 받아 #109·PR #110(2인용 담당)으로 원본 Classic 복원 + Single Player 분리. 006 후보 G와 같은 방식이다. B 변경은 `GameController.hasCancerRules()` 1개
- M2(호출처 없는 `GameMenu.install`)는 D가 #102·PR #103으로 정리했다

## 7. 리뷰·질문·정리

| 대상 | 한 것 |
| --- | --- |
| PR #82 (C, Load 실패 보존) | B 파일 수정이라 리뷰: `Closes`→`Refs`, 파일 읽기를 잠금 밖으로 제안, Load 실패 Clock 정책 B 동의 |
| PR #93 (C, 스냅샷 파일 API) | 승인 후 머지 |
| PR #100 (D, 프리셋 화면) | B API 사용·잠금 계약 확인, 후속 항목 기록 |
| PR #97 (암세포 치료 규칙) | 머지 후 검토: B 코드 이상 없음, "흩어지면 승리 불가" 질문 → 의도된 규칙으로 답변 받음 |
| #74, #71 | B 쪽 영향 댓글(한 보드만 tick 보장은 B가 맡음 / B 쪽은 #88로 해결) |
| #51, #81, #74, #86 | 해결된 이슈 PR 링크 남기고 닫기, #86은 #87 중복으로 닫기 |

## 협업

- PR #45, #56, #98, #99, #101, #105 머지 (#93도 승인 후 머지)
- D의 B 인터페이스 요청안 검토 결과를 003 6장에 기록
- 11.12 회의에서 C(도현)가 정리한 B 요구사항 B-1~B-5를 받음 → 다음 작업 표에 반영. 소유권은 B가 컨트롤러 연결, C가 파일 API·테스트로 정함(4절)
