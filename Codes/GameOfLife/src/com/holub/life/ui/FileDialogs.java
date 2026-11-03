package com.holub.life.ui;

import java.awt.Component;
import java.io.File;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * File selection and error display for the Life window. Moved from com.holub.io.Files, whose only
 * real job was showing a JFileChooser. Reading and writing the chosen file is not done here.
 */
public class FileDialogs {
  private static final String START_DIRECTORY = ".";
  private static final String EXTENSION = ".life";
  private static final String DESCRIPTION = "Life File";
  private static final String TITLE = "The Game of Life";

  private final Component parent;

  public FileDialogs(Component parent) {
    this.parent = parent;
  }

  /**
   * Lets the user pick a .life file.
   *
   * @param selectButtonText text on the approve button ("Load" or "Write").
   * @return the chosen file, or empty if the user cancelled.
   */
  public Optional<File> chooseLifeFile(String selectButtonText) {
    JFileChooser chooser = new JFileChooser(START_DIRECTORY);
    chooser.setFileFilter(
        new FileFilter() {
          @Override
          public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith(EXTENSION);
          }

          @Override
          public String getDescription() {
            return DESCRIPTION;
          }
        });

    int result = chooser.showDialog(parent, selectButtonText);
    return result == JFileChooser.APPROVE_OPTION
        ? Optional.of(chooser.getSelectedFile())
        : Optional.empty();
  }

  public void showError(String message) {
    JOptionPane.showMessageDialog(null, message, TITLE, JOptionPane.ERROR_MESSAGE);
  }
}
