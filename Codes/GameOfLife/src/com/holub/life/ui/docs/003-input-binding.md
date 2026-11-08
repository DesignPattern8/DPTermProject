# [003] 메뉴·입력과 동작 연결: 명시적 (메뉴, 라벨, 동작) 목록

- 상태: 대체됨(→ 005). Go 메뉴의 속도 정책은 B가 #44에서 `Speed` enum으로 가져갔다(후보 C에 해당). 나머지 "명시적 목록"은 005에서 #45 코드 기준으로 다시 비교했다.
- 작성자: 병국 / 11/3 / 브랜치 `refactor/D-ui`

## 문제

- Go 메뉴는 모든 항목이 리스너 하나를 공유하고, 메뉴 항목 이름의 **첫 글자**로 동작을 고른다(`life/Clock.java:88~105`).
  - `'T'` → 한 세대, `'A'` → 500ms, `'S'` → 150ms, `'M'` → 70ms, `'F'` → 30ms, 나머지 → 0(정지)
  - "Halt"는 어디에도 해당하지 않아 기본값 0으로 떨어지는 우연에 기대고 있다. 메뉴 이름만 바꿔도("Stop", "Super fast") 동작이 깨진다.
- Grid 메뉴는 `Universe` 생성자 안에서 항목마다 익명 `ActionListener`를 만든다(`life/Universe.java:84~125`). 화면 클래스가 메뉴 정의, 보드 조작, 파일 처리까지 직접 한다.
- 타이밍 클래스(`Clock`)가 메뉴(UI)를 만든다.
- 메뉴 구성을 Swing 창 없이 확인할 방법이 없다.

## 후보

### A. 명시적 목록: `GameMenu.entries()` → (메뉴, 라벨, 동작)
- 구조: 각 항목이 `Entry(menu, label, Runnable action)`. 동작은 `GameController` 메서드 호출뿐이다. `install()`이 목록을 기존 `MenuSite.addLine`에 넘긴다.
- 장점: 라벨과 동작이 분리된다. 메뉴 구성을 데이터로 꺼낼 수 있어 창 없이 테스트할 수 있다(UI-15~18). 기존 `MenuSite`를 고치지 않는다. 단축키나 버튼을 추가해도 같은 `Runnable`을 쓰면 동작이 하나로 맞춰진다.
- 단점: Command 패턴의 실행 취소·이력은 없다. 하지만 그건 분담안에서 B 소유라 의도한 범위다.

### B. 완전한 Command 객체: 항목마다 `Command` 클래스
- 구조: `ClearCommand`, `SetSpeedCommand(ms)` 등을 D에서 정의
- 장점: GoF Command 구조에 그대로 맞는다.
- 단점: 편집 명령과 Undo/Redo는 B가 `application.command`에 만든다. D가 따로 Command 계층을 만들면 두 곳에 비슷한 계층이 생긴다. 메뉴는 "어떤 요청을 보낼지"만 알면 된다.

### C. `enum Speed` + switch
- 구조: `enum Speed { HALT(0), AGONIZING(500), ... }`로 이름 파싱을 없앰
- 장점: 가장 적은 변경으로 첫 글자 파싱과 매직 넘버를 없앤다.
- 단점: Go 메뉴만 해결되고 Grid 메뉴와 `Universe`의 혼재는 그대로다. 메뉴 구성을 테스트하기도 여전히 어렵다.

## 오버헤드 비교표

| 항목 | A. 명시적 목록 | B. Command 객체 (예상) | C. enum (예상) | 실측 (2단계) |
| --- | --- | --- | --- | --- |
| 구조 비용 | 클래스 1개(`GameMenu` 105줄, 실제 카운트). 기존 `Clock.createMenus` 34줄 + `Universe` 메뉴 부분 대체 | 명령 클래스 약 10개 | enum 1개 | 최종 LOC 증감 |
| 간접성 | 메뉴 → `Runnable` → 컨트롤러 (+0, 기존 익명 리스너와 같은 깊이) | 메뉴 → Command → 컨트롤러 (+1) | 메뉴 → switch → Clock | |
| 결합도 | `ui` → `application.GameController`만. `Clock`의 UI 의존 제거 | 같음 + B의 Command와 중복 | `Clock`의 UI 의존 유지 | |
| 테스트 용이성 | `entries()`로 창 없이 검증 | 명령별 검증 | 창 필요 | |
| 확장 비용 | 항목 추가 = 목록 1줄 | 클래스 1개 | enum 상수 1개 + Grid는 별도 | |

## 결정과 근거 (제안)

A를 제안한다. 메뉴의 책임은 사용자 요청을 컨트롤러에 전달하는 것까지다. 실행·이력은 B가 맡으므로 D는 가벼운 바인딩 목록으로 충분하다. B는 계층 중복, C는 Grid 메뉴와 테스트 문제를 남긴다.

### 동작을 그대로 유지한 부분

- 메뉴 순서: 기존에는 `Universe`가 먼저 Grid를 등록하고 `Clock`이 Go를 등록했다. `entries()`도 Grid → Go 순서다.
- 속도 값: 500/150/70/30ms 그대로
- Tick은 실행 중에도 한 세대를 진행한다(기존 `Clock.tick()`과 같음). 실행 중 처리 정책은 B가 정한다.
- 파일 선택 취소 시에도 실패 메시지를 표시한다. 기존 코드는 취소를 `FileNotFoundException`으로 처리해서 `"Read Failed!"` / `"Write Failed!"`가 떴다. 리팩토링이라 그대로 두고 별도 이슈 후보로 남긴다.

## 후속 작업

- B와 확인: `start(ms)` / `stop()` / `step()` 시그니처 ([interfaces.md](interfaces.md))
- 2단계: UI-15~22 테스트 작성
