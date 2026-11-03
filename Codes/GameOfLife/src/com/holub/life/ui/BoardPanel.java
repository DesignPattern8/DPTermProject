package com.holub.life.ui;

import com.holub.life.application.GameController;
import com.holub.life.application.GameListener;
import com.holub.life.model.BoardSnapshot;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * The board view. It draws whatever snapshot the controller last published and turns mouse clicks
 * into cell requests. It never computes the next generation or touches the model directly.
 *
 * <p>Replaces the drawing and mouse-handling half of the original Universe.
 */
public final class BoardPanel extends JPanel implements GameListener {
  /** Pixel size of one cell at the preferred size (the original DEFAULT_CELL_SIZE). */
  private static final int DEFAULT_CELL_SIZE = 8;

  private final GameController controller;
  private final BoardRenderer renderer;
  private final GridCoordinateMapper mapper;
  private volatile BoardSnapshot snapshot;

  public BoardPanel(GameController controller, BoardRenderer renderer) {
    this.controller = controller;
    this.renderer = renderer;
    this.snapshot = controller.snapshot();
    this.mapper = GridCoordinateMapper.forSnapshot(snapshot);

    final Dimension preferredSize =
        new Dimension(
            snapshot.columns() * DEFAULT_CELL_SIZE, snapshot.rows() * DEFAULT_CELL_SIZE);
    setBackground(Color.white);
    setPreferredSize(preferredSize);
    setMaximumSize(preferredSize);
    setMinimumSize(preferredSize);
    setOpaque(true);

    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            // Keep every cell the same size: the side must be a
            // multiple of the number of cells across.
            Rectangle bounds = getBounds();
            bounds.height = GridCoordinateMapper.snapToCells(bounds.height, snapshot.rows());
            bounds.width = bounds.height;
            setBounds(bounds);
          }
        });

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            mapper.toCell(e.getPoint(), surface()).ifPresent(controller::toggleCell);
          }
        });

    controller.addListener(this);
  }

  /** Called by the controller after the board has changed. Safe to call from any thread. */
  @Override
  public void boardChanged(BoardSnapshot newSnapshot) {
    snapshot = newSnapshot;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    renderer.render(g, snapshot, surface());
  }

  /** The panel's own area, with the origin at (0,0). */
  private Rectangle surface() {
    return new Rectangle(0, 0, getWidth(), getHeight());
  }
}
