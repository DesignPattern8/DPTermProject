package com.holub.life.ui;

import com.holub.life.application.Clock;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Mode 메뉴의 모드 전환 (twoplayer/docs/007-two-player-ui.md 결정 1 D). 모드마다 화면을 CardLayout 카드로 두고, 시계 하나에
 * 리스너를 하나만 달아 지금 모드의 {@link GameMode#tick()}만 부른다.
 *
 * <p>모든 모드의 tick과 모드 전환은 잠금 하나 안에서 돈다. 그래서 두 모드의 보드가 동시에 세대를 진행하지 않는다.
 * {@code Neighborhood}의 전이 상태는 보드별로 분리했지만(#74), 한 번에 한 모드만 진행하는 실행 정책은 유지한다.
 */
public final class ModeSwitch {
  private final Clock clock;
  private final Consumer<Boolean> fileCommandsEnabled;
  private final JPanel cards = new Cards();
  private final Map<GameMode, String> cardNames = new IdentityHashMap<>();
  private final Object tickLock = new Object();
  private volatile GameMode current;

  /**
   * {@code first}를 지금 모드로 두고 시계에 리스너를 단다.
   *
   * @param fileCommandsEnabled 지금 모드가 Clear·Load·Store를 쓸 수 있는지 받는다. 만들 때 한 번, 전환할 때마다 불린다.
   */
  public ModeSwitch(Clock clock, GameMode first, Consumer<Boolean> fileCommandsEnabled) {
    this.clock = clock;
    this.fileCommandsEnabled = fileCommandsEnabled;
    this.current = first;
    add(first);
    fileCommandsEnabled.accept(first.supportsFileCommands());
    clock.addClockListener(this::tick);
  }

  /** 고를 수 있는 모드를 더한다. 화면은 숨긴 카드로 붙는다. */
  public void add(GameMode mode) {
    String name = "mode-" + cardNames.size();
    cardNames.put(mode, name);
    cards.add(mode.view(), name);
  }

  /**
   * 모드를 바꾼다. 시계를 멈추고, 진행 중인 tick이 있으면 끝날 때까지 기다린 뒤 바꾼다. 화면을 바꾸고 파일 명령 사용 여부를 알린다.
   * EDT에서 부른다.
   */
  public void select(GameMode mode) {
    String name = cardNames.get(mode);
    if (name == null) throw new IllegalArgumentException("unknown mode: " + mode.menuName());
    clock.stop();
    synchronized (tickLock) {
      current = mode;
    }
    ((CardLayout) cards.getLayout()).show(cards, name);
    cards.revalidate();
    Window window = SwingUtilities.getWindowAncestor(cards);
    if (window != null) window.pack(); // 창 크기를 새 모드 화면에 맞춘다
    fileCommandsEnabled.accept(mode.supportsFileCommands());
  }

  public GameMode current() {
    return current;
  }

  /** 모드 화면들을 담은 패널 (CardLayout). */
  public JComponent view() {
    return cards;
  }

  /**
   * CardLayout은 가장 큰 카드에 크기를 맞추므로, 지금 모드 화면의 크기를 쓰도록 바꾼다. 클래식 화면이 2인용 화면(상태 표시줄 포함)
   * 크기로 늘어나지 않게 한다(클래식 512×512 유지).
   */
  private final class Cards extends JPanel {
    Cards() {
      super(new CardLayout());
    }

    @Override
    public Dimension getPreferredSize() {
      return current.view().getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
      return current.view().getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
      return current.view().getMaximumSize();
    }
  }

  /** 시계 tick. 메뉴가 열려 있으면 건너뛴다(기존 {@link GameMenu#onTick}과 같음). */
  private void tick() {
    if (GameMenu.menuIsActive()) return;
    synchronized (tickLock) {
      current.tick();
    }
  }
}
