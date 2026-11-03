package com.holub.life.ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

public class NativeUiSmoke {
  static JFrame frame;
  static JComponent panel;
  static Object board;

  static void check(boolean b, String message) {
    if (!b) throw new AssertionError(message);
  }

  static void edt(Runnable action) throws Exception {
    SwingUtilities.invokeAndWait(action);
  }

  static JMenuItem item(String menu, String name) {
    JMenuBar bar = frame.getJMenuBar();
    for (int i = 0; i < bar.getMenuCount(); i++) {
      JMenu m = bar.getMenu(i);
      if (m != null && menu.equals(m.getText()))
        for (int j = 0; j < m.getItemCount(); j++) {
          JMenuItem v = m.getItem(j);
          if (v != null && name.equals(v.getText())) return v;
        }
    }
    throw new AssertionError("Missing menu: " + menu + " / " + name);
  }

  static Set<Point> cells() throws Exception {
    Object state = board.getClass().getMethod("createMemento").invoke(board);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      Method flush = state.getClass().getMethod("flush", OutputStream.class);
      flush.setAccessible(true);
      flush.invoke(state, bytes);
    } catch (NoSuchMethodException e) {
      Class<?> codec = Class.forName("com.holub.life.persistence.LegacyLifeCodec");
      codec.getMethod("flush", state.getClass(), OutputStream.class).invoke(null, state, bytes);
    }
    Collection<?> points =
        (Collection<?>)
            new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
    Set<Point> result = new HashSet<>();
    for (Object p : points) result.add((Point) p);
    return result;
  }

  static void click(int row, int column) throws Exception {
    edt(
        () ->
            panel.dispatchEvent(
                new MouseEvent(
                    panel,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    column * 8 + 4,
                    row * 8 + 4,
                    1,
                    false)));
  }

  static <T> T descendant(Component c, Class<T> type) {
    if (type.isInstance(c)) return type.cast(c);
    if (c instanceof Container container)
      for (Component child : container.getComponents()) {
        T found = descendant(child, type);
        if (found != null) return found;
      }
    return null;
  }

  static <T> T waitFor(Class<T> type) throws Exception {
    long deadline = System.nanoTime() + 10_000_000_000L;
    while (System.nanoTime() < deadline) {
      Object[] result = {null};
      edt(
          () -> {
            for (Window w : Window.getWindows())
              if (w.isShowing()) {
                T c = descendant(w, type);
                if (c != null) result[0] = c;
              }
          });
      if (result[0] != null) return type.cast(result[0]);
      Thread.sleep(30);
    }
    throw new AssertionError("No visible " + type.getSimpleName());
  }

  static void fileMenu(String name, File file, boolean cancel, boolean errorExpected)
      throws Exception {
    SwingUtilities.invokeLater(() -> item("Grid", name).doClick());
    JFileChooser chooser = waitFor(JFileChooser.class);
    edt(
        () -> {
          if (cancel) chooser.cancelSelection();
          else {
            chooser.setSelectedFile(file);
            chooser.approveSelection();
          }
        });
    if (errorExpected) {
      JOptionPane error = waitFor(JOptionPane.class);
      check(
          error.getMessage().equals(name.equals("Load") ? "Read Failed!" : "Write Failed!"),
          "error text");
      edt(() -> error.setValue(JOptionPane.OK_OPTION));
    }
    edt(() -> {});
  }

  public static void main(String[] args) {
    int exit = 1;
    try {
      Class<?> life = Class.forName("com.holub.life.Life");
      edt(
          () -> {
            try {
              life.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      for (Frame f : Frame.getFrames()) if (f instanceof JFrame jf && jf.isVisible()) frame = jf;
      check(frame != null, "visible frame");
      panel = (JComponent) frame.getContentPane().getComponent(0);
      Field field = panel.getClass().getDeclaredField("outermostCell");
      field.setAccessible(true);
      board = field.get(panel);
      check(panel.getWidth() == 512 && panel.getHeight() == 512, "panel dimensions");
      for (String name : new String[] {"Clear", "Load", "Store", "Exit"}) item("Grid", name);
      for (String name :
          new String[] {"Halt", "Tick (Single Step)", "Agonizing", "Slow", "Medium", "Fast"})
        item("Go", name);
      click(10, 9);
      click(10, 10);
      click(10, 11);
      check(
          cells().equals(Set.of(new Point(9, 10), new Point(10, 10), new Point(11, 10))),
          "mouse toggles");
      edt(() -> item("Go", "Tick (Single Step)").doClick());
      check(
          cells().equals(Set.of(new Point(10, 9), new Point(10, 10), new Point(10, 11))),
          "single tick");
      File save = Path.of(args[0], "smoke.life").toFile();
      fileMenu("Store", save, false, false);
      try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(save))) {
        check(((Collection<?>) in.readObject()).size() == 6, "legacy duplicate coordinates");
      }
      edt(() -> item("Grid", "Clear").doClick());
      check(cells().isEmpty(), "clear menu");
      fileMenu("Load", save, false, false);
      check(cells().size() == 3, "load menu");
      fileMenu("Load", save, true, true);
      check(cells().size() == 3, "cancel before clear");
      File bad = Path.of(args[0], "bad.life").toFile();
      Files.write(bad.toPath(), new byte[] {1, 2, 3, 4});
      fileMenu("Load", bad, false, true);
      check(cells().isEmpty(), "read failure after clear");
      System.out.println(
          "PASS: launch, 10 menu items, mouse, Tick, Store duplicates, Clear, Load, cancel error,"
              + " invalid-load clear");
      System.out.println("EXIT_MENU_REQUESTED");
      System.out.flush();
      edt(() -> item("Grid", "Exit").doClick());
      throw new AssertionError("Exit menu did not exit");
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      for (Window w : Window.getWindows()) w.dispose();
    }
    System.exit(exit);
  }
}
