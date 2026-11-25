package com.holub.life.ui;

import com.holub.life.application.GameController;
import com.holub.life.application.GameListener;
import com.holub.life.model.Cell;
import com.holub.life.model.CellPosition;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Universe's existing panel, input and redraw code. */
public final class BoardPanel extends JPanel {
  private static final int DEFAULT_CELL_SIZE = 8;
  private final Cell outermostCell;

  public BoardPanel(Cell outermostCell, GameController controller) {
    this(outermostCell, controller, null);
  }

  /**
   * @param presets the Preset menu's placement state (decision 010), or null for a panel without
   *     presets. While it waits, a click goes to it instead of toggle/treat.
   */
  public BoardPanel(Cell outermostCell, GameController controller, PresetPlacement presets) {
    this.outermostCell = outermostCell;
    final Dimension PREFERRED_SIZE =
        new Dimension(
            outermostCell.widthInCells() * DEFAULT_CELL_SIZE,
            outermostCell.widthInCells() * DEFAULT_CELL_SIZE);

    addComponentListener(
        new ComponentAdapter() {
          public void componentResized(ComponentEvent e) {
            // Make sure that the cells fit evenly into the
            // total grid size so that each cell will be the
            // same size. For example, in a 64x64 grid, the
            // total size must be an even multiple of 63.

            Rectangle bounds = getBounds();
            bounds.height /= outermostCell.widthInCells();
            bounds.height *= outermostCell.widthInCells();
            bounds.width = bounds.height;
            setBounds(bounds);
          }
        });

    setBackground(Color.white);
    setPreferredSize(PREFERRED_SIZE);
    setMaximumSize(PREFERRED_SIZE);
    setMinimumSize(PREFERRED_SIZE);
    setOpaque(true);

    addMouseListener // {=Universe.mouse}
        (
        new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            Rectangle bounds = getBounds();
            bounds.x = 0;
            bounds.y = 0;
            CellPosition cell = GridCoordinateMapper.cellAt(outermostCell, e.getPoint(), bounds);
            boolean right = SwingUtilities.isRightMouseButton(e);
            if (presets != null && presets.click(cell, right)) return;
            // Classic Life toggles on any button, as the original Universe did (ui/docs/012).
            if (right && controller.hasCancerRules()) controller.treat(cell.row(), cell.column());
            else controller.toggle(cell.row(), cell.column());
          }
        });

    if (presets != null)
      presets.addWaitingListener(
          waiting ->
              setCursor(
                  Cursor.getPredefinedCursor(
                      waiting ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR)));

    // Edits redraw the whole board; generations redraw only the changed blocks, as before.
    controller.addGameListener(
        change -> {
          if (change == GameListener.Change.GENERATION) refreshNow();
          else if (change == GameListener.Change.EDITED) repaint();
        });
  }

  /** Paint the original cell and block outlines through the extracted renderer. */
  public void paint(Graphics g) {
    Rectangle panelBounds = getBounds();

    // The panel bounds is relative to the upper-left
    // corner of the screen. Pretend that it's at (0,0)
    panelBounds.x = 0;
    panelBounds.y = 0;
    BoardRenderer.redraw(outermostCell, g, panelBounds, true); // {=Universe.redraw1}
  }

  /** Queue the original partial redraw on the Swing event queue. */
  public void refreshNow() {
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            Graphics g = getGraphics();
            if (g == null) // Panel not displayable
            return;
            try {
              Rectangle panelBounds = getBounds();
              panelBounds.x = 0;
              panelBounds.y = 0;
              BoardRenderer.redraw(outermostCell, g, panelBounds, false); // {=Universe.redraw2}
            } finally {
              g.dispose();
            }
          }
        });
  }
}
