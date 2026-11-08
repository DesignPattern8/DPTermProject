# [005] 메뉴 항목과 동작 연결

- 상태: 채택
- 작성자: D 담당 / 2026-11-08 / 이슈 #9, 브랜치 `refactor/D-ui`
- 기준 코드: `refactor/44-b-application` `b543703` (#45, main + #42 + B 리팩토링). 줄 번호는 이 커밋 기준이다.
- 대체하는 문서: [003-input-binding](003-input-binding.md) (#25 이전 구조 기준 초안)

## 문제

`GameMenu.install` (`GameMenu.java:15-33`)

- Go 항목 6개가 리스너 하나(`modifier`, `:25`)를 같이 쓰고, 눌린 항목의 `getName()`을 `go(clock, name)`의 `switch`(`:36-45`)로 다시 분기한다. 라벨 문자열 5개("Tick (Single Step)", "Agonizing", "Slow", "Medium", "Fast")가 `install`(`:27-31`)과 `go`(`:38-42`) 두 곳에 있다. 메뉴 라벨을 바꾸면 `go`의 `case`도 같이 바꿔야 하고, 하나만 바꾸면 컴파일은 되지만 그 항목은 `default`로 떨어져 **정지**한다.
- 이름 분기는 `MenuSite.addLine`이 항목 이름을 라벨로 정해 주는 것(`MenuSite.java:323` `lineItem.setName(name)`)에 기대고 있다.
- 메뉴 구성(어떤 메뉴에 어떤 항목이 어떤 순서로)은 `MenuSite.addLine` 호출로만 표현된다. `MenuSite`는 `JFrame`이 있어야 해서(`MenuSite.establish`) headless JUnit에서 메뉴 구성과 Grid 항목 동작(Clear, Load, Store, Exit)을 확인할 수 없다. 지금 JUnit이 보는 것은 `go`와 `onTick`뿐이다(`GameMenuTest` 2개). 나머지는 디스플레이가 필요한 `NativeUiSmoke`(EXT-N01)만 본다.

## 후보

### A. 명시적 항목 표 (패턴 없이)
- `record Entry(String menu, String label, ActionListener action)`의 목록을 `GameMenu.entries(...)`가 만든다. 항목마다 자기 동작을 갖는다(`Halt` → `clock.stop()`, `Agonizing` → `clock.start(Speed.AGONIZING)` …). `install`은 목록을 돌며 기존과 같은 requester(Grid = 패널, Go = clock)로 `MenuSite.addLine`을 부른다.
- 장점: 라벨과 동작이 한 줄에 같이 있어 어긋날 수 없다. 목록은 Swing 창 없이 만들 수 있어 메뉴 구성과 각 동작을 JUnit으로 본다. `MenuSite`는 고치지 않는다.
- 단점: `record` 1개가 늘어난다. 메뉴를 등록하는 방식(requester 구분)을 `install`이 알고 있어야 한다.

### B. Command (항목마다 명령 클래스)
- `ClearCommand`, `LoadCommand`, `SpeedCommand(Speed)`, `TickCommand`, `HaltCommand`, `ExitCommand` 등이 `ActionListener`를 구현.
- 장점: GoF Command 구조 그대로. 단축키·툴바 버튼이 생기면 같은 객체를 쓴다.
- 단점: 실행 취소·기록·대기열이 없어서 명령 객체가 람다 하나와 하는 일이 같다. B도 #43에서 같은 이유로 Command를 기각했다(`application/docs/003-pattern-analysis.md` D3 후보 D). 클래스가 6개 이상 늘어난다.

### C. 지금 그대로 (이름 `switch`)
- 장점: 변경 없음. #45에서 첫 글자 분기는 이미 이름 전체 분기로 바뀌었다.
- 단점: 위 문제가 그대로 남는다.

### D. 기타 검토
| 패턴 | 판단 | 이유 |
| --- | --- | --- |
| Interpreter | 기각 | 해석할 문법이 없다. 라벨은 고정 문자열 6개다 |
| Chain of Responsibility | 기각 | 항목마다 처리자가 하나로 정해져 있다. 요청을 넘길 일이 없다 |
| Abstract Factory / Builder | 기각 | 메뉴 구성 가족이 하나뿐이다. 2인용 모드(#7)에서 모드별 메뉴가 생기면 다시 본다 |
| Observer | 이미 사용 | `ActionListener`가 Swing의 Observer다. 바꾸지 않는다 |

## 오버헤드 비교표

| 항목 | A. 항목 표 | B. Command | C. 그대로 |
| --- | --- | --- | --- |
| 구조 비용 | 중첩 `record` +1, `GameMenu` 1파일. LOC는 아래 실측 | 클래스 +6 이상(추정), 6파일 이상 | 0 |
| 간접성 (Go 항목) | 메뉴 → 항목 람다 → `Clock` (1단계 줄어듦) | 메뉴 → 명령 객체 → `Clock` (A와 같음) | 메뉴 → 공용 리스너 → `go` switch → `Clock` |
| 결합도 | 변화 없음 (`ui` → `application`, `MenuSite`) | 같음 | 같음 |
| 테스트 용이성 | `entries()`로 창 없이 메뉴 구성·모든 항목 동작 확인 | 명령별 확인 가능, 구성 확인은 별도 | `go`만 확인 가능 |
| 확장 비용 (메뉴 항목 1개 추가) | 목록 1줄 | 클래스 1개 + 등록 1줄 | `addLine` 1줄 + 필요하면 `case` 1줄 |

## 결정과 근거

A를 채택한다. 지금 메뉴 동작은 모두 `GameController`나 `Clock` 메서드 하나를 부르는 것이라 Command 객체가 람다보다 얻는 것이 없다. A는 라벨·동작 중복을 없애고, 지금까지 `NativeUiSmoke`로만 보던 Grid 메뉴 동작을 JUnit으로 옮길 수 있게 한다.

동작을 유지하는 부분
- 메뉴·항목 순서: Grid(Clear, Load, Store, Exit) → Go(Halt, Tick (Single Step), Agonizing, Slow, Medium, Fast). `MenuSite`에 등록하는 순서와 requester도 같다.
- `GameMenu.go(clock, name)`은 지우지 않는다. 기존 테스트 `GameMenuTest#goCommandsKeepOriginalActions`(EXT-U01)가 쓰므로 Go 항목 표에서 라벨을 찾아 실행하도록 바꾸고, 없는 이름은 원래처럼 정지한다.
- `onTick`·`menuIsActive`(메뉴가 열려 있으면 tick을 건너뜀)는 #45 그대로 둔다.

## 전/후 비교

- 다이어그램: [diagrams/005-before.puml](diagrams/005-before.puml) / [diagrams/005-after.puml](diagrams/005-after.puml) (006 포함)
- 실측: [구현 결과](#구현-결과)

## 구현 결과

(구현 후 실측값을 채운다)

## 후속 작업

- 2인용 모드(#7)에서 Mode 메뉴가 생기면 같은 표에 항목을 더한다. 모드에 따라 메뉴가 달라지면 Abstract Factory를 다시 검토한다.
