package com.holub.life.twoplayer.ui;

import com.holub.life.twoplayer.TwoPlayerSession;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executor;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * 2인용 화면: 보드 + 상태 표시줄 한 줄 + 확정·패스·새 게임 버튼 (rules.md 5장·8장). 클릭은 현재 플레이어의 임시 배치, Enter는
 * 확정이다. 세션이 바뀌면 상태 표시줄과 보드를 다시 그린다(Observer, docs/007 결정 2).
 */
final class TwoPlayerPanel extends JPanel {
  private static final int DEFAULT_CELL_SIZE = 8;
  private static final String CONFIRM = "confirm";

  private final TwoPlayerSession session;
  private final JComponent boardView = new BoardView();
  private final JLabel statusLabel = new JLabel();
  private final JButton confirmButton = new JButton("Confirm");
  private final JButton passButton = new JButton("Pass");
  private final JButton newGameButton = new JButton("New Game");

  /** @param ui 세션 알림 뒤 화면 갱신을 실행할 곳 (앱은 EDT, 테스트는 바로 실행) */
  TwoPlayerPanel(TwoPlayerSession session, Executor ui) {
    super(new BorderLayout());
    this.session = session;

    Dimension boardSize =
        new Dimension(
            session.columns() * DEFAULT_CELL_SIZE, session.rows() * DEFAULT_CELL_SIZE);
    boardView.setPreferredSize(boardSize);
    boardView.setOpaque(true);
    boardView.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            pressed(e.getPoint());
          }
        });

    confirmButton.addActionListener(e -> session.confirm());
    passButton.addActionListener(e -> session.pass());
    newGameButton.addActionListener(e -> session.newGame());
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
    for (JButton button : new JButton[] {confirmButton, passButton, newGameButton}) {
      button.setFocusable(false); // Enter 키 바인딩을 버튼이 가져가지 않도록
      buttons.add(button);
    }
    statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
    // 상태 표시줄 한 줄 + 버튼 줄. 너비는 보드에 맞춰, 긴 문구가 창을 넓히지 않게 한다.
    JPanel bar =
        new JPanel(new BorderLayout()) {
          @Override
          public Dimension getPreferredSize() {
            return new Dimension(boardSize.width, super.getPreferredSize().height);
          }
        };
    bar.add(statusLabel, BorderLayout.NORTH);
    bar.add(buttons, BorderLayout.SOUTH);

    add(boardView, BorderLayout.CENTER);
    add(bar, BorderLayout.SOUTH);

    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), CONFIRM);
    getActionMap()
        .put(
            CONFIRM,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                session.confirm();
              }
            });

    session.addListener(() -> ui.execute(this::refresh));
    refresh();
  }

  JComponent boardView() {
    return boardView;
  }

  JLabel statusLabel() {
    return statusLabel;
  }

  JButton confirmButton() {
    return confirmButton;
  }

  JButton passButton() {
    return passButton;
  }

  JButton newGameButton() {
    return newGameButton;
  }

  private void refresh() {
    statusLabel.setText(session.status());
    boardView.repaint();
  }

  /** 칸 한 변의 픽셀 수. 보드 영역의 짧은 변을 칸 수로 나눈 몫이다. */
  private int cellSize() {
    return Math.min(boardView.getWidth() / session.columns(), boardView.getHeight() / session.rows());
  }

  /** 격자 밖(오른쪽·아래 남는 픽셀)을 누르면 보드 밖 좌표가 되어 세션이 거부 이유를 남긴다. */
  private void pressed(Point point) {
    int size = cellSize();
    if (size == 0) return;
    session.click(point.y / size, point.x / size);
  }

  private final class BoardView extends JComponent {
    @Override
    protected void paintComponent(Graphics g) {
      int size = cellSize();
      g.setColor(getParent() == null ? TwoPlayerRenderer.EMPTY_COLOR : getParent().getBackground());
      g.fillRect(0, 0, getWidth(), getHeight());
      if (size == 0) return;
      TwoPlayerRenderer.render(
          g,
          new Rectangle(0, 0, size * session.columns(), size * session.rows()),
          session.snapshot());
    }
  }
}
