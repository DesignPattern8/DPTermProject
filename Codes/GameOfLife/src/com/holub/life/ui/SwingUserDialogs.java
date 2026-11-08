package com.holub.life.ui;

import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.JOptionPane;

/** The original Grid menu dialogs: a {@link FileDialogs} chooser and a JOptionPane error box. */
public final class SwingUserDialogs implements UserDialogs {
  @Override
  public File chooseFile(String selectButtonText) throws FileNotFoundException {
    return FileDialogs.userSelected(".", ".life", "Life File", selectButtonText);
  }

  @Override
  public void showError(String message) {
    JOptionPane.showMessageDialog(null, message, "The Game of Life", JOptionPane.ERROR_MESSAGE);
  }
}
