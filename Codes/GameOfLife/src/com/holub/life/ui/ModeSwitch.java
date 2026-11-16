package com.holub.life.ui;

import com.holub.life.application.Clock;
import java.util.function.Consumer;
import javax.swing.JComponent;

/** 골격. */
public final class ModeSwitch {
  public ModeSwitch(Clock clock, GameMode first, Consumer<Boolean> fileCommandsEnabled) {}

  public void add(GameMode mode) { throw new UnsupportedOperationException("not implemented (#7)"); }

  public void select(GameMode mode) { throw new UnsupportedOperationException("not implemented (#7)"); }

  public GameMode current() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public JComponent view() { throw new UnsupportedOperationException("not implemented (#7)"); }
}
