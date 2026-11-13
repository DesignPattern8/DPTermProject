# [006] 파일 선택·오류 표시·종료를 메뉴에서 분리

- 상태: 채택
- 작성자: D 담당 / 2026-11-08 / 이슈 #9, 브랜치 `refactor/D-ui`
- 기준 코드: `refactor/44-b-application` `b543703` (#45)

## 문제

Grid 메뉴의 Load·Store·Exit가 Swing 대화상자와 프로세스 종료를 정적 호출로 직접 부른다.

| 위치 | 호출 |
| --- | --- |
| `GameMenu.java:59` | `FileDialogs.userSelected(".", ".life", "Life File", "Load")` |
| `GameMenu.java:68` | `FileDialogs.userSelected(".", ".life", "Life File", "Write")` |
| `GameMenu.java:61-62, 70-71` | `JOptionPane.showMessageDialog(null, "Read Failed!" / "Write Failed!", ...)` |
| `GameMenu.java:23` | `System.exit(0)` |

- 그래서 "고른 파일로 불러오기/저장을 요청한다", "실패하면 `Read Failed!`/`Write Failed!`를 띄운다", "선택을 취소해도 실패 메시지를 띄운다(원본 동작)", "Exit는 종료한다"를 JUnit으로 확인할 수 없다. headless에서 `JFileChooser`를 띄우면 `HeadlessException`이 나고, `System.exit`는 테스트 JVM을 끝낸다.
- 지금은 `NativeUiSmoke`(EXT-N01)가 디스플레이 있는 환경에서 실제 창을 띄워서만 확인한다. CI(headless)에서는 돌지 않는다.

## 후보

### A. Strategy: `UserDialogs` 인터페이스 주입 + 종료 `Runnable`
- `interface UserDialogs { File chooseFile(String selectButtonText) throws FileNotFoundException; void showError(String message); }`
- 기본 구현 `SwingUserDialogs`는 지금 코드(`FileDialogs.userSelected`, `JOptionPane`)를 그대로 옮긴 것. 종료는 `Runnable exit`(기본 `() -> System.exit(0)`).
- `GameMenu.entries(controller, clock, dialogs, exit)`가 받아서 쓴다(005와 같은 진입점). `install(panel, controller, clock)`은 시그니처를 유지하고 기본 구현을 넘긴다.
- 장점: 테스트에서 기록용 대역으로 바꿔 창 없이 확인한다. `LifeFrame`·`Life`는 바뀌지 않는다.
- 단점: 타입 +2(인터페이스, 기본 구현). Load·Store 경로에 인터페이스 호출 1단계가 더해진다.

### B. Template Method: `GameMenu`를 상속 가능한 클래스로 바꾸고 `chooseFile`/`showError`/`exit`를 protected 훅으로
- 장점: 인터페이스 없이 테스트용 하위 클래스로 바꿀 수 있다.
- 단점: 지금 `GameMenu`는 정적 메서드만 있는 `final` 클래스다. 인스턴스 클래스로 바꾸고 `LifeFrame`의 호출도 고쳐야 한다. 대화상자 정책과 메뉴 구성이 상속으로 묶인다.

### C. 그대로 두기 (정적 Facade `FileDialogs`)
- 장점: 변경 없음.
- 단점: 위 동작을 계속 JUnit으로 볼 수 없다.

### 기타 검토
| 패턴 | 판단 | 이유 |
| --- | --- | --- |
| Abstract Factory | 기각 | 대화상자 "가족"을 바꿔 끼울 일이 테스트 대역 하나뿐이다. 인터페이스 하나로 충분하다 |
| Adapter | 부분 해당 | `SwingUserDialogs`가 `FileDialogs`의 인자 4개짜리 정적 함수를 `chooseFile(text)`에 맞춘다. 별도 구조를 더하지 않는다 |
| Proxy | 기각 | 접근 제어·지연 생성이 필요 없다 |

## 오버헤드 비교표

| 항목 | A. Strategy | B. Template Method | C. 그대로 |
| --- | --- | --- | --- |
| 구조 비용 | 타입 +2(`UserDialogs`, `SwingUserDialogs`), 새 파일 2 | 타입 0, `GameMenu`·`LifeFrame` 수정 | 0 |
| 간접성 (Load 1회) | 메뉴 → `load` → `UserDialogs.chooseFile` → `FileDialogs` (+1) | 메뉴 → `load` → 훅 → `FileDialogs` (+1) | 메뉴 → `load` → `FileDialogs` |
| 결합도 | `GameMenu`의 `JOptionPane`·`FileDialogs` 직접 의존 제거(→ `SwingUserDialogs`로 이동) | 같음, 대신 상속 결합 | 변화 없음 |
| 런타임 비용 | 메뉴 클릭당 인터페이스 호출 1회. 측정하지 않음(사람 입력 1회당 1번) | 같음 | — |
| 테스트 용이성 | 기록용 대역으로 Load·Store·실패·취소·Exit를 headless 확인 | 하위 클래스로 가능 | 불가 (EXT-N01만) |
| 확장 비용 (대화상자 1종 추가) | 인터페이스 메서드 1 + 구현 1 + 대역 1 | 훅 1 + 하위 클래스 1 | `GameMenu` 수정 |

## 결정과 근거

A를 채택한다. B는 `GameMenu`를 인스턴스 클래스로 바꾸고 `LifeFrame`까지 고쳐야 해서 #45와 겹치는 파일이 늘어난다. A는 `install` 시그니처를 유지하므로 `LifeFrame`·`Life`·`BoardPanel`을 건드리지 않는다.

동작을 유지하는 부분
- 파일 선택 기준 디렉터리 `"."`, 확장자 `".life"`, 설명 `"Life File"`, 버튼 `"Load"`/`"Write"`, 오류 제목 `"The Game of Life"`, 아이콘 `ERROR_MESSAGE`.
- 선택을 취소하면 `FileDialogs.userSelected`가 `FileNotFoundException`을 던지고 `IOException`으로 잡혀 `"Read Failed!"`/`"Write Failed!"`가 뜬다. 버그 후보지만 리팩토링이라 그대로 둔다(AGENTS.md 3-2 강령 2).

## 전/후 비교

- 다이어그램: [diagrams/005-before.puml](diagrams/005-before.puml) / [diagrams/005-after.puml](diagrams/005-after.puml)
- 실측: [005의 구현 결과](005-menu-binding.md#구현-결과)

## 후속 작업

- 취소 시 실패 메시지가 뜨는 동작은 별도 이슈로 다룬다(UI-R07이 지금 동작을 고정한다).
- `NativeUiSmoke`는 실제 `SwingUserDialogs` 경로를 계속 확인한다.
