# [011] 호출처 없는 `GameMenu.install` 정리

- 상태: 채택
- 작성자: D 담당(전병국) / 2026-11-24 / 이슈 #102
- 기준 코드: `refactor/base` `66111fc` (#100 머지 직후)
- 근거: B [006 M2](../../application/docs/006-mode-control.md) "tick 연결 경로가 코드상 2개 남아 있다", 006 후속 작업 5번(D 소유)

## 문제

`GameMenu.install(BoardPanel, GameController, Clock)`(`GameMenu.java:54-61`)은 메뉴를 달고 **시계에 클래식 tick 리스너를 직접 단다**(`clock.addClockListener(() -> onTick(controller))`).

- 2인용 모드(#7)가 들어오면서 `LifeFrame`은 `installMenus` + `ModeSwitch`로 바뀌었다(`LifeFrame.java`, 2인용 [007 결정 1](../../twoplayer/docs/007-two-player-ui.md)). 시계 리스너는 `ModeSwitch`가 하나만 단다(`ModeSwitch.java:40`). 모드 목록이 비어도 `ModeSwitch`를 만든다.
- 그래서 `install`은 호출처가 0이다. `git grep "GameMenu.install("`: `src`·`test` 모두 0건(`installMenus` 제외).
- 남아 있으면 위험하다. 누가 `LifeFrame` 밖에서 `install`을 다시 부르면 `ModeSwitch` 리스너와 함께 클래식 보드가 tick 1번에 2세대 진행한다. 2인용 모드에서도 클래식 보드가 진행한다(TP-U-11이 지키는 "지금 모드만 tick" 보장을 우회).
- `ui/docs/interfaces.md`는 아직 "`GameMenu.install`은 원래 동작 그대로"라고 공개 계약으로 적고 있다.

## 후보

### A. 제거
- `install`과 그 Javadoc을 지운다. tick 연결 경로는 `ModeSwitch` 하나만 남는다.
- `onTick`·`menuIsActive`는 남긴다. `menuIsActive`는 `ModeSwitch.tick`이 쓴다(`ModeSwitch.java:104`). `onTick`은 기존 특성 테스트 `GameMenuTest#openMenuSuppressesTick`이 원본의 "메뉴가 열려 있으면 tick 무시"를 고정하는 데 쓴다. 이 테스트를 지우거나 바꾸지 않는다(AGENTS 3-2 강령 4).
- 장점: 잘못 부를 길이 사라진다(컴파일 오류). 공개 API 1개 감소.
- 단점: 공개 메서드 삭제라 인터페이스 변경이다. 다만 호출처가 다른 도메인 포함 0이다.

### B. `@Deprecated`만 표시
- 장점: 시그니처 유지.
- 단점: 부를 수는 있다(경고만). M2의 "tick 두 번" 위험이 그대로 남는다. 호출처가 0이라 하위 호환으로 지킬 대상이 없다.

### C. 현행 유지
- 006 M2와 `interfaces.md`의 틀린 계약이 남는다.

### 기타 검토
| 패턴 | 판단 | 이유 |
| --- | --- | --- |
| Facade (`install`을 `installMenus` + `new ModeSwitch(...)`로 바꿔 "한 번에 조립") | 기각 | `ModeSwitch`는 카드 화면·모드 목록·메뉴 활성화 콜백이 필요하다. 그것을 아는 곳은 `LifeFrame`이다. 같은 조립이 두 군데 생긴다 |
| Template Method / Builder (조립 순서를 고정) | 기각 | 조립하는 곳이 `LifeFrame` 하나뿐이다 |

## 오버헤드 비교표

| 항목 | A. 제거 | B. `@Deprecated` | C. 유지 |
| --- | --- | --- | --- |
| 구조 비용 | 공개 메서드 −1, `GameMenu` 165 → 156행(−9, 실측) | 애노테이션 +1행 | 0 |
| 간접성 | 변화 없음 (실행 경로에 없던 메서드) | 같음 | 같음 |
| 결합도 | `GameMenu` → `Clock.addClockListener` 의존 제거. `GameMenu`가 `Clock`을 쓰는 곳은 Go 항목(`start`/`stop`/`tick`)만 남음 | 그대로 | 그대로 |
| 런타임 비용 | 없음 | 없음 | 없음 |
| 테스트 용이성 | 구조 검사 1개(UI-R30)로 다시 생기지 않게 고정 | 경고만이라 테스트로 막을 대상 없음 | — |
| 확장 비용 (새 모드 추가) | `GameMode` 구현 1 + `LifeFrame` 조립 (지금과 같음). 잘못된 진입점 없음 | 같음 + `install`을 쓰면 안 된다는 주석 의존 | 같음 |
| 다른 도메인 변경 | 0 (호출처 0) | 0 | 0 |

## 결정과 근거

A를 채택한다.
- 호출처가 0이라 B의 하위 호환은 지킬 대상이 없고, B는 M2의 위험(tick 두 번)을 막지 못한다.
- 실행 경로에 없던 메서드라 동작은 바뀌지 않는다. 기존 테스트(메뉴 10개 UI-R01, Go 항목 UI-R09, 메뉴 열림 tick 무시 `openMenuSuppressesTick`, 2인용 TP-U-11·16)는 그대로 통과해야 한다.
- 기능 동결(11/24)과 충돌하지 않는다. 기능 추가가 아니라 사용하지 않는 공개 진입점 삭제다.

## 전/후 비교

```java
// 전 (GameMenu.java:54-61)
/** Install the menus ... and a clock listener that advances controller on every tick. */
public static void install(BoardPanel panel, GameController controller, Clock clock) {
  installMenus(panel, controller, clock);
  clock.addClockListener(() -> onTick(controller));
}

// 후: 삭제. 조립은 LifeFrame 한 곳
GameMenu.installMenus(panel, controller, clock, presets);   // 메뉴만
new ModeSwitch(clock, ...);                                  // 시계 리스너 1개
```

- 클래스 다이어그램 `005-before/after.puml`은 #9 당시(전 = `install`만 있던 구조)의 기록이라 고치지 않는다. 현재 구조는 [010-after](diagrams/010-after.puml)(`installMenus`만 표시)가 보여 준다.

## 구현 결과

전 = `refactor/base` `66111fc`, 후 = 브랜치 `refactor/102-d-gamemenu-install`. 행 수는 `wc -l`.

| 항목 | 전 | 후 |
| --- | --- | --- |
| `GameMenu` 공개 메서드 | 7 (`entries`, `install`, `installMenus` ×2, `presetEntries`, `setPresetsEnabled`, `setFileCommandsEnabled`) | 6 (`install` 삭제) |
| `GameMenu.java` 행 수 | 165 | 156 (−9: Javadoc 4, 메서드 4, 빈 줄 1) |
| `ui` Java 소스 전체 | 834 | 825 |
| 시계에 리스너를 다는 `ui` 코드 | `GameMenu.java:60`, `ModeSwitch.java:40` | `ModeSwitch.java:40` 하나 |
| 수정 파일 | — | `GameMenu.java` 1개. 다른 도메인 코드 0 |

- 테스트: `./gradlew test` 431개 통과(기준 430 + 새 1). 기존 테스트는 수정하지 않았다. 새 UI-R30은 삭제 전 코드에서 실패(`NoSuchMethodException`이 나지 않음)하고 삭제 후 통과하는 것을 확인했다. 환경: Linux, OpenJDK 21(toolchain만 21로 바꾸는 init 스크립트).
- `NativeUiSmoke`(EXT-N01): 수정 없이 Xvfb에서 통과, 종료 코드 0(메뉴 10개, Tick, Store/Load, 프리셋 단계 포함).

## 후속 작업

- 2인용 [007](../../twoplayer/docs/007-two-player-ui.md) 151~157행은 "`GameMenu.install`은 그대로 둔다"고 적혀 있다. 2인용 담당 문서라 이 PR에서 고치지 않고 README에서 이 문서로 링크한다.
- 3인자 `installMenus(panel, controller, clock)`도 `install`이 유일한 호출처였다. 시계 리스너를 달지 않아 M2 위험이 없고, 2인용 담당이 [interfaces.md](interfaces.md) 모드 전환 절에 추가한 공개 메서드라 이 PR에서는 남긴다.
- `onTick`은 특성 테스트용으로만 남는다. `ModeSwitch.tick`과 같은 필터다. 006 B안(실행기를 application으로)을 할 때 함께 정리한다.
