package com.holub.life.persistence;

import java.io.*;

/** Opens the legacy file streams; the controller retains their original operation order. */
public final class BoardFileStore {
  private BoardFileStore() {}

  public static InputStream openForLoad(File file) throws FileNotFoundException {
    return new FileInputStream(file);
  }

  public static OutputStream openForStore(File file) throws FileNotFoundException {
    return new FileOutputStream(file);
  }
}
