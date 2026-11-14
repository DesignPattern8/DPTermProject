package com.holub.life.persistence;

import java.util.Objects;

/** Named immutable relative coordinates; placement belongs to the application domain. */
public record PatternPreset(String id, String displayName, PatternData data) {
  public PatternPreset {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(data, "data");
  }
}
