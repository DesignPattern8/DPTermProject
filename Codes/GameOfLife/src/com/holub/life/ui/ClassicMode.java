package com.holub.life.ui;

import com.holub.life.application.GameController;
import javax.swing.JComponent;

/** 골격. */
public final class ClassicMode implements GameMode {
  public ClassicMode(JComponent view, GameController controller) {}

  public String menuName() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public JComponent view() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public void tick() { throw new UnsupportedOperationException("not implemented (#7)"); }

  public boolean supportsFileCommands() { throw new UnsupportedOperationException("not implemented (#7)"); }
}
