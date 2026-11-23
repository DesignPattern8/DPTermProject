# [010] 프리셋 선택·배치 화면

- 상태: 채택
- 작성자: D 담당(전병국) / 2026-11-24 / 이슈 #62
- 기준 코드: `feature/62-b-preset-api` `b89c93c` (B의 `presets()`·`applyPreset()`, PR #99)
- 합의: #62 댓글 (11/17 D 제안 Q1~Q11, B 답변). 실패 시 대기 처리(A안)는 11/24 D 담당 결정

## 문제

C의 `PresetCatalog`(block, blinker, glider)와 B의 `GameController.presets()`·`applyPreset(id, origin)`이 생겼지만, 사용자가 프리셋을 고르고 놓을 화면이 없다. D가 정할 것은 다음이다.

1. 메뉴: 프리셋을 어디서 고르나. 기존 메뉴 구성 테스트(UI-R01, `GameMenu.entries()` 10개)는 원본 메뉴를 고정하고 있다.
2. 배치 대기 상태: 메뉴에서 고른 뒤 "다음 클릭"까지 어떤 프리셋을 기다리는지 어디에 둘까. 지금 클릭은 `BoardPanel`의 마우스 처리(`BoardPanel.java:48-55`)가 왼쪽 → `toggle`, 오른쪽 → `treat`로 바로 보낸다.
3. 모드별 사용: 2인용 모드에서는 쓰지 않는다(Q9). 모드 정보는 `ModeSwitch`가 `supportsFileCommands()`로 알려 준다(`ModeSwitch.java:54-66`, `LifeFrame.java:30-32`).
4. 실패 표시: B는 `OUT_OF_BOUNDS`, `UNKNOWN_ID`를 결과값으로 돌려준다.

## 후보

### 결정 1. 배치 대기 상태를 어디에 둘까

#### A. 별도 클래스 `PresetPlacement` (패턴 없이)
- 대기 중인 프리셋 id 하나만 가진다. `select(id)`, `cancel()`, `click(cell, rightButton)`. `click`은 대기 중이면 배치(왼쪽) 또는 취소(오른쪽)를 하고 `true`를 돌려준다. `BoardPanel`은 `true`면 평소 토글·치료를 하지 않는다.
- 장점: `BoardPanel`(Swing) 없이 테스트할 수 있다. 메뉴·패널·모드 전환이 같은 객체 하나를 공유한다.
- 단점: 타입 +1. `BoardPanel`에 "먼저 물어보는" 분기 1곳이 생긴다.

#### B. State 패턴: `BoardPanel`의 입력 처리를 `NormalInput` / `PlacementInput` 상태 객체로
- 장점: 입력 모드가 더 늘면(예: 2인용 배치) 상태 클래스만 추가하면 된다.
- 단점: 지금 상태는 "평소"와 "프리셋 대기" 둘뿐이고, 대기 상태의 동작도 클릭 1번이다. 타입 +3(인터페이스, 상태 2개). 2인용은 이미 자기 패널(`TwoPlayerPanel`)이 입력을 따로 처리해서 이 패널에 입력 모드가 늘어날 계획이 없다.

#### C. `BoardPanel` 안에 필드 하나 (`String pendingPreset`)
- 장점: 타입 0.
- 단점: 메뉴(`GameMenu`)와 모드 전환(`LifeFrame`)이 패널 내부 필드를 건드려야 한다. 대기·취소·실패 표시를 창 없이 테스트하려면 `BoardPanel`을 만들어야 한다.

### 결정 2. 메뉴 항목

| 후보 | 판단 |
| --- | --- |
| A. `GameMenu.presetEntries(controller, placement)`를 따로 두고 `Preset` 메뉴에 등록 | ✅ 기존 `entries()`와 UI-R01을 건드리지 않는다. 005의 항목 표(`Entry`)를 그대로 쓴다 |
| B. `entries()`에 Preset 항목 추가 | ❌ 원본 메뉴 구성을 고정한 UI-R01(특성 테스트)의 기대값을 바꿔야 한다 |
| C. Command(항목마다 명령 객체) | ❌ 005와 같은 이유. 동작이 `placement.select(id)` 하나 |

### 결정 3. 2인용 모드에서 끄기

| 후보 | 판단 |
| --- | --- |
| A. 기존 `supportsFileCommands()` 신호를 같이 사용. `LifeFrame`이 받는 콜백에서 Grid 파일 명령과 함께 `Preset` 메뉴를 켜고 끄고, 대기도 취소 | ✅ `GameMode`·`TwoPlayerMode`(2인용 팀 코드) 무수정. B가 #62 답변에서 허용 |
| B. `GameMode.supportsPresets()` 추가 | ❌ 인터페이스 변경이라 `TwoPlayerMode`(2인용 팀)도 고쳐야 한다. 지금 두 신호가 모든 모드에서 같은 값이다 |

#87(모드별 실행 제어 통합)에서 모드별 기능 허용 방식을 다시 정하면 B로 옮길 수 있다.

### 결정 4. 실패했을 때 대기를 유지할까

| 후보 | 판단 |
| --- | --- |
| A. 결과와 관계없이 클릭 1번이면 대기를 끝낸다 | ✅ 11/24 D 결정. "선택 1번 = 시도 1번"으로 단순하고, Q2(배치 후 바로 평소 클릭으로)와 같은 규칙 |
| B. `OUT_OF_BOUNDS`면 대기를 유지해서 다른 칸을 다시 누를 수 있게 | ❌ 오류창을 닫은 뒤에도 커서가 계속 십자라 다음 클릭이 토글인지 배치인지 헷갈린다 |

## 오버헤드 비교표 (결정 1)

A·C는 구현 전 추정, 구현 후 [구현 결과](#구현-결과)에 실측을 채운다.

| 항목 | A. `PresetPlacement` | B. State | C. 패널 필드 |
| --- | --- | --- | --- |
| 구조 비용 | 타입 +1 | 타입 +3 | 0 |
| 간접성 (평소 클릭) | `placement.click` 확인 +1단계 | 상태 객체 디스패치 +1 | 필드 확인 (0) |
| 결합도 | `ui` 안에서만. 새 외부 의존은 B의 `PresetChoice`·`PresetResult`·`CellPosition` | 같음 | 같음 + 메뉴·`LifeFrame`이 패널 내부 필드에 의존 |
| 테스트 용이성 | 창 없이 대기·취소·실패를 확인 | 상태별로 가능 | `BoardPanel`이 필요 |
| 확장 비용 (입력 모드 1개 추가) | `PresetPlacement`와 같은 클래스 1개 + 패널 분기 1줄 | 상태 클래스 1개 | 패널 수정 |

## 결정과 근거

- 결정 1은 A. 입력 모드가 둘뿐이라 State(B)는 과하고, C는 메뉴·모드 전환이 패널 내부를 알아야 한다.
- 결정 2·3·4는 위 표의 A.
- 실패 문구는 원본 오류창과 같은 짧은 영어 문장으로 한다: `OUT_OF_BOUNDS` → `"Preset does not fit here."`, `UNKNOWN_ID` → `"Unknown preset."`. 표시는 006의 `UserDialogs.showError`.
- 대기 중에는 커서를 십자(`Cursor.CROSSHAIR_CURSOR`)로 바꾼다(Q5). 미리보기 윤곽은 범위 밖.

## 전/후 비교

- 다이어그램: [diagrams/010-after.puml](diagrams/010-after.puml)

## 구현 결과

전 = `feature/62-b-preset-api` `b89c93c`, 후 = 브랜치 `feature/62-d-preset-ui`. 행 수는 `wc -l`.

| 항목 | 전 | 후 |
| --- | --- | --- |
| 새 타입 | — | `PresetPlacement` 1개 (72행) |
| 수정 파일 | — | `GameMenu` 122 → 165행, `BoardPanel` 96 → 113행, `LifeFrame` 46 → 54행. `Life`·B·C·2인용 코드 0 |
| `ui` Java 소스 전체 | 694행 | 834행 (+144/−4) |
| 평소 클릭 경로 | `cellAt` → `toggle`/`treat` | `cellAt` → `presets.click`(대기 아니면 `false`) → `toggle`/`treat` (+1단계) |
| `ui`가 새로 쓰는 B 타입 | — | `PresetChoice`, `PresetResult` (`persistence.*` 직접 사용 0) |
| 프리셋 화면 headless 테스트 | 0 | 9 (UI-R21~R29) |

- 테스트: `./gradlew test` 430개 통과(기준 421 + 새 9). 기존 테스트(UI-R01 메뉴 10개 포함)는 수정하지 않았다. 환경: Linux, OpenJDK 21(toolchain만 21로 바꾸는 init 스크립트).
- `NativeUiSmoke`(EXT-N01): Xvfb에서 통과, 종료 코드 0. 새 단계 "Clear → `Preset`/`Glider` → (10,10) 클릭 → 살아 있는 칸 5개" 포함.
- 모드 전환(창 필요, 저장소에 넣지 않은 임시 프로그램, Xvfb): 실제 `Life`를 띄워 `Preset`/`Glider`로 대기를 시작한 뒤 `Mode`/`2 Players`를 고르면 `Preset` 항목과 `Grid`/`Load`가 비활성, `Mode`/`Classic`으로 돌아오면 `Preset`이 다시 활성.

## 후속 작업

- #87 결과에 따라 결정 3을 다시 본다.
- 미리보기 윤곽, 회전·뒤집기는 범위 밖(#62 합의).
