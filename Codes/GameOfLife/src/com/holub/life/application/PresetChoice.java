package com.holub.life.application;

import java.util.Objects;

/** A preset the UI can offer: its catalog id and menu label, without persistence types. */
public record PresetChoice(String id, String displayName) {
  public PresetChoice {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
  }
}
