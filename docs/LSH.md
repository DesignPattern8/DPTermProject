# 작업 기록 (leesihyeong17 / 이시형)

- 담당: B 도메인 — 실행 제어·명령·이력 (`com.holub.life.application`)
- 기간: 2026-11-07 ~
- 상세 근거는 [`application/docs/`](../Codes/GameOfLife/src/com/holub/life/application/docs/README.md)에 있고, 이 문서는 진행 순서와 결과만 모은다(AGENTS.md 2-1).

## 한눈에 보기

### 머지된 PR

| PR | 머지 | 이슈 | 내용 |
| --- | --- | --- | --- |
| #42 | 11-07 | #41 | 버그: 불러오기·저장 실패 시 파일 스트림 미해제 |
| #45 | 11-13 | #43, #44 | B 기존 패턴 분석(003) + Clock Singleton 제거, `TickSource`·`Speed`·`GameListener` 도입 |
| #56 | 11-13 | #55 | `GameController`를 A의 `Board`에 연결, 보드 호출 직렬화(004) |

### 다음 작업 (v1.0 이후)

| 작업 | 근거 | 시작 조건 |
| --- | --- | --- |
| C 파일 API 연결: `read/write`로 저장·로드, `LegacyLifeCodec` 직접 호출·스트림 관리·파일용 `Cell` 인자 제거 | 11.12 회의 C 요청 B-1, 004 후속 | C의 시그니처·예외 계약 확정 |
| 로드 실패 시 기존 보드 유지 (동작 변경 → 별도 이슈·PR) | 회의 B-2 | 위 작업과 함께 또는 직후 |
| 저장·로드 중 실행 정책과 알림 정리 | 회의 B-3·B-4 | C·D와 정책 합의 |
| 통합 코드(1인용·2인용) 기준 실행 제어 재설계: State(003 D3)·Mediator(003 D5) 재판단 | 003 9장 | v1.0 |
| `SwingTickSource` 도입 여부 | 003 7장 5단계, D `004-swing-tick-source` | D와 합의 |

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

## 협업

- PR #45, #56 머지
- D의 B 인터페이스 요청안 검토 결과를 003 6장에 기록
- 11.12 회의에서 C(도현)가 정리한 B 요구사항 B-1~B-5를 받음 → 다음 작업 표에 반영. 소유권(B가 컨트롤러 연결, C가 파일 API·테스트)은 C와 합의 중
