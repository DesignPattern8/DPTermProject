# 008 프리셋 목록·배치 API

- 상태: 채택
- 작성자: 이시형 / 2026-11-24
- 관련: 이슈 #62 (11/17 D 제안 댓글과 B 답 댓글로 API·정책 합의), C `PresetCatalog`, 테스트 [PRE-01~11](test-cases.md#62-프리셋-선택배치-b-api--구현-전-정의-2026-11-24)

## 문제

C는 `PresetCatalog`(`block`, `blinker`, `glider`, 좌상단 (0,0) 기준 상대 좌표)를 만들어 두었고, `PatternPreset` 주석에 "placement belongs to the application domain"이라고 배치를 B에 넘겼다. 그런데 `GameController`(#88 머지 시점 `ce74be2`)의 편집 API는 `toggle(int, int)` 하나뿐이었다.

D가 메뉴에서 프리셋을 놓으려면 다음이 필요했다.

1. D는 `persistence.*` 타입을 직접 쓰지 않는다(#62 Q6). 목록을 B 값으로 받아야 한다.
2. 놓을 칸에 이미 산 세포가 있으면 산 채로 둬야 한다(Q8-2). `Board`에는 `toggle`만 있어서, 칸마다 `toggle`을 그대로 부르면 겹친 칸이 **죽는다**.
3. 암세포 칸은 건드리지 않아야 한다(Q8-4). `Resident.toggle`(`Resident.java:94-96`)은 `amAlive`만 뒤집고 `isCancer`는 그대로 두므로, 암세포 칸에 `toggle`하면 "죽은 암세포"가 된다(007 결정 2에서 본 것과 같은 상태, `BoardSnapshot`으로는 표현 불가).
4. 일부만 놓이면 안 되고(Q8), 실행 중 tick과 섞이면 안 된다(Q8-3, 004 D8-B의 잠금 계약).

## 후보

### A. 패턴 없음 — B가 같은 잠금 안에서 `snapshot()`으로 확인하고 죽은 칸만 `toggle`

- `applyPreset`이 잠금 하나 안에서 ① 모든 칸의 범위 확인 ② `board.snapshot().liveCells()` 1번 ③ 생존 목록에 없는 칸만 `board.toggle` (`GameController.java:143-168`).
- 장점: A의 `Board` 계약을 바꾸지 않는다. 다른 도메인 일정(오늘 기능 동결)과 무관하게 B 혼자 끝낼 수 있다. 암세포는 `liveCells`에 들어 있으므로(`BoardSnapshot` 주석 "Cancer cells are also live") 따로 분기하지 않아도 건드리지 않는다.
- 단점: 프리셋 칸이 3~5개인데 보드 전체를 스냅샷한다. 아래 실측에서 배치 시간의 대부분이 `snapshot()`이다.

### B. A에 `Board.set(CellPosition, boolean alive)` 요청

- `Board`에 "살리기" 연산을 추가하고 B는 칸마다 `set(pos, true)`만 부른다.
- 장점: 스냅샷 없이 칸 수만큼만 일한다. 의도("이 칸을 살린다")가 시그니처에 드러난다.
- 단점: A의 공개 계약 변경이다. `Board`(21행) + 구현 `CompositeBoard` + `Board` 대역(`GameControllerBoardTest.RecordingBoard`)을 고쳐야 하고, `CompositeBoard`가 위임하는 `Neighborhood`에는 지금 살리기 경로가 없다(`toggle`, `clear`뿐). 암세포 칸에서 `set(true)`가 암을 유지하는지도 A가 정해야 한다. 이슈로 사전 합의(AGENTS 1-2) → A 구현 → B 연결 순서라 오늘(11/24 동결) 안에 끝나지 않는다.

### C. Command — 칸마다 명령 객체를 만들어 잠금 안에서 배치 실행

- `PlaceCell(position)` 같은 명령 목록을 만들고 잠금 안에서 `execute()`를 차례로 부른다.
- 장점: 명령에 역연산을 붙이면 Undo(B5)로 이어질 수 있다.
- 단점: "산 칸은 그대로" 문제를 풀지 않는다. 명령 안에서도 결국 A(스냅샷 확인)나 B(`set`)가 필요하다. Undo는 이번 범위 밖(#62 "범위 밖")이고 B5도 후속으로 미뤘으므로, 지금은 쓰지 않는 확장점만 생긴다.

## 오버헤드 비교표

A는 이 브랜치 실측, B·C는 구현하지 않아 **추정**이다.

| 항목 | A. 스냅샷 확인 + 죽은 칸만 toggle (채택) | B. `Board.set` 요청 | C. Command 배치 |
| --- | --- | --- | --- |
| 구조 비용 | 새 타입 2 (`PresetChoice` 11행, `PresetResult` 10행). `GameController.java` 146 → 199행(+53, 생성자 1개·메서드 2개). 다른 도메인 변경 0 | A의 새 타입 2 + `Board`·`CompositeBoard`·`Neighborhood`(또는 `Cell` 트리)·테스트 대역에 메서드 추가. 추정: 다른 도메인 3~4개 파일 | A + 명령 인터페이스·구현 1~2개. 추정 +30행 |
| 간접성 (applyPreset 한 번의 보드 호출) | `rows`·`columns` + `snapshot` 1 + 죽은 칸 수만큼 `toggle` (빈 보드 glider: 1 + 5) | `set` × 칸 수 (glider: 5) | A 또는 B + 명령마다 `execute()` 1단계 |
| 결합도 (`GameController`가 쓰는 프로젝트 타입) | 5 → 7개 (+`PresetCatalog`, `PatternPreset`). `java.awt` 0. D는 B의 `PresetChoice`·`PresetResult`만 앎 | A와 같음. 대신 B가 A의 새 메서드 의미(암세포 처리)에 의존 | A + 명령 타입 |
| 런타임 (아래 측정) | applyPreset 약 **112~127 µs**, 그중 `snapshot()` 약 110~118 µs | toggle 5번 기준 약 **0.3~0.4 µs** (추정 하한, `set` 구현에 따라 다름) | A 또는 B + 칸마다 객체 1개 |
| 테스트 용이성 | `Board` 대역으로 호출 순서 확인(PRE-10), 실제 보드로 정책 확인(PRE-02~09) | 대역에 `set` 추가 필요 | 명령 단위 테스트가 늘어남 |
| 확장 비용 | 프리셋 추가는 C의 `PresetCatalog`만. 회전·뒤집기는 `applyPreset` 안 좌표 계산 1곳 | A와 같음 | Undo 추가가 쉬움(지금은 범위 밖) |

**런타임 측정**: 스크래치 코드(커밋하지 않음)로 64×64 실제 보드(`CompositeBoard(Neighborhood(8, Neighborhood(8, Resident)))`)의 짝수 행·열 1024칸을 살린 뒤 `(31,31)`에 glider를 20,000번 배치(매번 `restore`로 초기화, 측정은 `applyPreset` 호출만). 같은 조건에서 `snapshot()` 단독과 `toggle` 5번 단독도 측정. 3회 반복, `System.nanoTime` 평균. 환경: Windows 11, OpenJDK 26.0.2.1, `build/classes/java/main`.

| 회차 | applyPreset | snapshot() 단독 | toggle 5번 |
| --- | --- | --- | --- |
| 0 | 126.70 µs | 109.49 µs | 0.39 µs |
| 1 | 112.39 µs | 109.67 µs | 0.30 µs |
| 2 | 118.78 µs | 117.50 µs | 0.30 µs |

B가 A보다 300배 이상 빠르지만, 배치는 메뉴 선택 뒤 클릭 1번에 1회 일어나는 동작이다. 가장 빠른 자동 실행 주기 `Speed.FAST`(30 ms)와 비교해도 0.4% 수준이고, Load(`restore`)·Store(`snapshot`)도 같은 비용을 이미 낸다. tick 경로가 아니라서 이 차이는 결정에 영향을 주지 않았다.

## 결정과 근거

**A를 채택한다.**

- 11/17 #62 댓글에서 "A 방식으로 시작하고 `set` 요청 안과 비교해 남긴다"고 D와 합의했다. 비교 결과 B의 이점은 런타임뿐이고, 사람이 클릭하는 빈도에서는 의미가 없다.
- B는 A 도메인의 계약 변경이라 동결일(11/24)에 시작할 수 없다. 암세포 칸에서 `set(true)`의 의미를 정하는 일도 A에 새로 생긴다. A 방식은 "산 칸(암 포함)은 건드리지 않는다"를 `liveCells` 하나로 해결해 이 질문 자체가 없다.
- C는 겹침 문제를 풀지 않고, 기대 이점(Undo)이 범위 밖이다.

정책(#62 합의, PRE 테스트로 고정):

| 경우 | 결과 | 보드 | 알림 | 테스트 |
| --- | --- | --- | --- | --- |
| 없는 id | `UNKNOWN_ID` | 그대로 (보드 호출 없음) | 없음 | PRE-07 |
| 한 칸이라도 보드 밖 | `OUT_OF_BOUNDS` | 그대로 (`snapshot` 전에 판단) | 없음 | PRE-04 |
| 성공 | `APPLIED` | 죽은 칸만 살림, 산 칸·암세포는 그대로 | 잠금 밖에서 `EDITED` 1번 | PRE-02·03·05·06·10 |
| `origin == null` | `NullPointerException` | 그대로 | 없음 | PRE-09 (B 결정, 합의 범위 밖의 호출 오류) |

- 시계는 멈추지 않는다(PRE-08). 범위 확인·스냅샷·toggle 전체가 잠금 하나 안이라 tick 중간에 끼지 않는다(PRE-11).
- 모드별 허용(Classic만)은 D가 `GameMode` 플래그로 처리한다. B는 모드를 모른다.
- `PresetCatalog`는 5번째 생성자로 주입한다. 기존 생성자 3개는 `new PresetCatalog()`로 위임하므로 `Life`와 기존 테스트의 생성자 호출은 그대로다.

## 전/후 비교

전 (`ce74be2`): D가 프리셋을 놓으려면 `PresetCatalog`(C)를 직접 읽고 칸마다 `toggle(row, column)`을 불러야 했다. 겹친 칸은 죽고, 칸마다 잠금·`EDITED`가 따로 일어나 tick이 중간에 낄 수 있었다.

후: [diagrams/008-after.puml](diagrams/008-after.puml) ([png](diagrams/008-after.png))

```java
// GameController.java:143-168
public PresetResult applyPreset(String id, CellPosition origin) {
  Objects.requireNonNull(origin, "origin");
  Optional<PatternPreset> preset = presets.find(id);
  if (preset.isEmpty()) return PresetResult.UNKNOWN_ID;
  List<CellPosition> pattern = preset.get().data().cells();

  synchronized (lock) {
    // every cell inside the board, otherwise OUT_OF_BOUNDS before any change
    ...
    Set<CellPosition> live = board.snapshot().liveCells();
    for (CellPosition cell : pattern) {
      CellPosition target = ...;
      if (!live.contains(target)) board.toggle(target);
    }
  }
  notifyListeners(GameListener.Change.EDITED);
  return PresetResult.APPLIED;
}
```

## 후속 작업

- D: `Preset` 메뉴·배치 대기·오류 표시(#62 Q1~Q5, Q10)와 `GameMode` 플래그. 이 API를 그대로 쓰면 된다.
- 프리셋 배치가 잦아지거나 보드가 커져 `snapshot()` 비용이 문제가 되면 B안(`Board.set`)을 A에 이슈로 요청한다. 그때 이 문서를 `대체됨`으로 바꾼다.
- #87(모드별 실행 제어 통합)에서 모드별 기능 허용을 B로 옮기기로 하면 `applyPreset`의 모드 검사 위치를 다시 정한다.
- Undo(B5)를 하게 되면 C안(Command)을 다시 비교한다. 배치 1번 = 명령 1개(칸 단위 아님)로 묶는 것이 이 API의 "전부 또는 없음" 정책과 맞다(추정).
