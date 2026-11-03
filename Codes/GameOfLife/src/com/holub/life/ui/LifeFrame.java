package com.holub.life.ui;

import com.holub.ui.MenuSite;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;

/** The main window. Moved from the original Life constructor. */
public final class LifeFrame extends JFrame {
  public LifeFrame() {
    super("The Game of Life. " + "(c)2003 Allen I. Holub <http://www.holub.com>");

    // Must establish the MenuSite very early in case
    // a subcomponent puts menus on it.
    MenuSite.establish(this);

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
  }

  /** Puts the board in the window and shows it. */
  public void showBoard(JComponent board) {
    getContentPane().add(board, BorderLayout.CENTER);
    pack();
    setVisible(true);
  }
}
