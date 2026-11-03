package com.holub.life.ui;

import com.holub.life.model.BoardSnapshot;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Draws a board snapshot. Implementations are stacked as Decorators: the base layer draws the cells
 * and each decorator adds one kind of overlay on top of what its inner renderer drew.
 *
 * @see CellLayerRenderer
 * @see BlockBorderDecorator
 * @see ActiveBlockDecorator
 */
public interface BoardRenderer {
  void render(Graphics g, BoardSnapshot snapshot, Rectangle surface);

  /** The renderer that reproduces the original Life display. */
  static BoardRenderer standard() {
    return new ActiveBlockDecorator(new BlockBorderDecorator(new CellLayerRenderer()));
  }
}
