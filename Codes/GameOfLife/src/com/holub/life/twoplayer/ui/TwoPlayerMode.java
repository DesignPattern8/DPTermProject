package com.holub.life.twoplayer.ui;

import com.holub.life.twoplayer.GameSettings;
import com.holub.life.twoplayer.TwoPlayerSession;
import com.holub.life.ui.GameMode;
import java.util.concurrent.Executor;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * 2인용 모드의 진입점 (docs/007-two-player-ui.md 결정 3). {@link TwoPlayerSession}과 화면을 만들고 LifeFrame의 Mode 메뉴에
 * {@link GameMode}로 붙는다. 시계 tick은 {@code ModeSwitch}가 이 모드가 선택되어 있을 때만 넘겨준다.
 */
public final class TwoPlayerMode implements GameMode {
  private final TwoPlayerSession session;
  private final TwoPlayerPanel panel;

  /** 기본 설정(N=20, K=20, M=3, R=5), 화면 갱신은 EDT. */
  public TwoPlayerMode() {
    this(GameSettings.defaults(), SwingUtilities::invokeLater);
  }

  TwoPlayerMode(GameSettings settings, Executor ui) {
    session = new TwoPlayerSession(settings);
    panel = new TwoPlayerPanel(session, ui);
  }

  @Override
  public String menuName() {
    return "2 Players";
  }

  @Override
  public JComponent view() {
    return panel;
  }

  /** 시뮬레이션 단계에서만 1세대 진행한다(K에 들어감). 다른 단계에서는 아무것도 하지 않는다. */
  @Override
  public void tick() {
    session.tick();
  }

  /** 2인용 모드는 저장·불러오기를 지원하지 않는다(rules.md 8장). */
  @Override
  public boolean supportsFileCommands() {
    return false;
  }

  TwoPlayerSession session() {
    return session;
  }

  TwoPlayerPanel panel() {
    return panel;
  }
}
