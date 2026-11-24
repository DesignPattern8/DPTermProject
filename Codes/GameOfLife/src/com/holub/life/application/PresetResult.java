package com.holub.life.application;

/** Outcome of {@link GameController#applyPreset}. Only {@link #APPLIED} changes the board. */
public enum PresetResult {
  APPLIED,
  /** At least one preset cell would fall outside the board; nothing was placed. */
  OUT_OF_BOUNDS,
  /** The catalog has no preset with that id; nothing was placed. */
  UNKNOWN_ID
}
