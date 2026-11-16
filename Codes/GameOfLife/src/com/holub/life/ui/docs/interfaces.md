# 화면 추출 계약

- 제공 D / 사용 조립 지점·UI / 변경 2026-11-03, #24

```java
void BoardRenderer.redraw(Cell cell, Graphics g, Rectangle here, boolean drawAll)
Point GridCoordinateMapper.position(Cell cell, Point here, Rectangle surface)
void GridCoordinateMapper.userClicked(Cell cell, Point here, Rectangle surface)
```

redraw는 원본처럼 Resident/Neighborhood/DUMMY를 그린다. drawAll=false는 안정된 블록을 건너뛰고 안정화 직후 마지막 갱신을 소비한다. 전달받은 Graphics는 닫지 않고 필요한 복제본만 dispose한다. 좌표 변환은 원본의 재귀적 나눗셈과 예외를 보존한다. userClicked는 position 후 모델 toggle을 호출하는 기존 테스트·임시 호출부용 함수다. 새 UI는 position 결과를 제어 계층에 전달한다.

LifeFrame은 원본 창 제목·종료 동작·레이아웃을 유지한다. BoardPanel은 원본 크기 조정·paint·refreshNow를 소유한다. GameMenu.install은 Grid/Go 항목과 기존 Clock 리스너를 연결한다. FileDialogs.userSelected는 원본 JFileChooser 코드이며 취소도 FileNotFoundException으로 전달하여 기존 오류 대화상자를 유지한다.

---

# 게임 모드 전환 (`GameMode`, `ModeSwitch`, `ClassicMode`)

- 제공 ui / 사용 조립 지점(`LifeFrame`, `Life`), 2인용(`twoplayer.ui.TwoPlayerMode`) / 담당 Son2kwon
- 변경 이력: 2026-11-16, 최초 작성 (#7). 설계 근거: [twoplayer/007](../../twoplayer/docs/007-two-player-ui.md)

```java
public interface GameMode {
  String menuName();
  JComponent view();
  void tick();
  boolean supportsFileCommands();
}
public final class ModeSwitch {
  public ModeSwitch(Clock clock, GameMode first, Consumer<Boolean> fileCommandsEnabled);
  public void add(GameMode mode);
  public void select(GameMode mode);   // EDT에서
  public GameMode current();
  public JComponent view();            // CardLayout. 크기는 지금 모드 화면을 따른다
}
public final class ClassicMode implements GameMode {
  public ClassicMode(JComponent view, GameController controller);  // "Classic", tick = controller.tick(), 파일 명령 true
}
// 추가: GameMenu.installMenus(panel, controller, clock), GameMenu.setFileCommandsEnabled(panel, enabled)
// 추가: LifeFrame(Cell, GameController, Clock, List<GameMode> otherModes)
```

- `ModeSwitch`는 만들 때 시계에 리스너를 하나 단다. tick마다 메뉴가 열려 있으면 건너뛰고(기존 `onTick`과 같음), 아니면 잠금 안에서 지금 모드의 `tick()`만 부른다.
- `select`: 시계를 멈추고 → 잠금을 잡아(진행 중인 tick이 끝나길 기다림) 지금 모드를 바꾸고 → 카드를 바꾸고 창을 `pack()`하고 → `fileCommandsEnabled`에 새 모드의 값을 넘긴다. 등록하지 않은 모드면 `IllegalArgumentException`.
- 모든 모드의 tick과 전환이 한 잠금 안에서 돌므로 두 모드의 보드가 동시에 세대를 진행하지 않는다(`Neighborhood`의 `static` 잠금, #74).
- `GameMode.tick()`은 시계 스레드(Go → Tick이면 EDT)에서 잠금을 잡은 채 불린다. 오래 걸리면 모드 전환이 기다린다. 다른 스레드를 기다리는 일(예: `invokeAndWait`)을 하면 안 된다.
- `GameMenu.install`은 원래 동작 그대로다(Grid·Go 메뉴 + 클래식 tick 리스너). `LifeFrame`은 `installMenus` + `ModeSwitch`를 쓴다. `otherModes`가 비어 있으면 Mode 메뉴를 달지 않는다.
- `setFileCommandsEnabled`는 Grid의 Clear·Load·Store만 켜고 끈다. Exit은 그대로.
