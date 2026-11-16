package com.holub.life.twoplayer.ui;

import com.holub.life.twoplayer.GameSettings;
import com.holub.life.twoplayer.TwoPlayerSession;
import com.holub.life.ui.GameMode;
import java.util.concurrent.Executor;
import javax.swing.JComponent;

/** 골격. */
public final class TwoPlayerMode implements GameMode {
  TwoPlayerMode(GameSettings settings, Executor ui) {}

  public String menuName() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public JComponent view() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public void tick() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public boolean supportsFileCommands() { throw new UnsupportedOperationException("not implemented (#7)"); }

  TwoPlayerSession session() { throw new UnsupportedOperationException("not implemented (#7)"); }

  TwoPlayerPanel panel() { throw new UnsupportedOperationException("not implemented (#7)"); }
}
