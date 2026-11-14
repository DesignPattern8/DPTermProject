package com.holub.life.ui;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.JOptionPane;

/**
 * The original dialogs: a {@link FileDialogs} chooser, a JOptionPane error box with no parent, and
 * the cancer game's information box over the Life window.
 */
public final class SwingUserDialogs implements UserDialogs {
  private final Component messageParent;

  /** Dialogs for the Grid menu. Information messages, if any, have no parent window. */
  public SwingUserDialogs() {
    this(null);
  }

  /** @param messageParent the window {@link #showMessage} is centered on (the original Life frame) */
  public SwingUserDialogs(Component messageParent) {
    this.messageParent = messageParent;
  }

  @Override
  public File chooseFile(String selectButtonText) throws FileNotFoundException {
    return FileDialogs.userSelected(".", ".life", "Life File", selectButtonText);
  }

  @Override
  public void showError(String message) {
    JOptionPane.showMessageDialog(null, message, "The Game of Life", JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void showMessage(String message) {
    JOptionPane.showMessageDialog(
        messageParent, message, "Game of Life", JOptionPane.INFORMATION_MESSAGE);
  }
}
