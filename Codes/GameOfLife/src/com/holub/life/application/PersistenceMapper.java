package com.holub.life.application;

import com.holub.life.model.NeighborhoodState;
import com.holub.life.persistence.PatternData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/** Maps model mementos and file values without traversing or changing the board. */
public final class PersistenceMapper {
  private PersistenceMapper() {}

  public static PatternData capture(NeighborhoodState state) {
    return new PatternData(new ArrayList<>(state.liveCells()), state.cancerCells());
  }

  public static NeighborhoodState restore(PatternData data) {
    NeighborhoodState state = new NeighborhoodState();
    state.replaceCells(new LinkedList<>(data.cells()), new HashSet<>(data.cancerCells()));
    return state;
  }
}
