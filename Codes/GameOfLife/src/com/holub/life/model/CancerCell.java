package com.holub.life.model;

/** A cell that can be marked as cancerous. */
public interface CancerCell extends Cell {
  /** Return whether this cell is marked as cancerous. */
  boolean isCancer();

  /** Turn this cell into a live cancer cell. */
  void setCancer();

  /** Kill this cell if it is a cancer cell. */
  void cureCancer();
}
