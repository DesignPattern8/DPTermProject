# [009] 암세포 게임 결과 메시지를 어디서 띄울 것인가

- 상태: 채택
- 작성자: D 담당 / 2026-11-14 / 이슈 #67
- 기준 코드: `refactor/base` `881819f` (#46 + 암세포 기능 #60)
- 관련: [006](006-dialog-injection.md) (대화상자를 `UserDialogs`로 분리)

## 문제

#60이 게임 종료 메시지를 `LifeFrame` 생성자 안에서 직접 띄운다(`LifeFrame.java:23-31`).

```java
controller.addGameListener(change -> {
  if (change != GameListener.Change.GAME_OVER) return;
  String message = controller.gameResultMessage();
  SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
      LifeFrame.this, message, "Game of Life", JOptionPane.INFORMATION_MESSAGE));
});
```

- 006에서 Grid 메뉴의 대화상자(파일 선택, 오류)를 `UserDialogs`로 모았는데, 이 메시지만 `JOptionPane`을 직접 부른다. 대화상자를 띄우는 곳이 다시 두 군데가 됐다.
- 이 코드는 `JFrame` 생성자 안에 있어 headless JUnit에서 실행할 수 없다(`LifeFrame`을 만들면 `MenuSite.establish`와 `setVisible`이 창을 띄운다). 그래서 "`GAME_OVER`면 결과 메시지를 한 번 띄운다", "다른 알림에는 띄우지 않는다"를 확인하는 테스트가 없다. #60의 테스트(`GameControllerCancerTest`)는 `GAME_OVER` 알림까지만 본다.
- 알림은 tick을 실행한 스레드(`UtilTimerTickSource`면 `java.util.Timer` 스레드)에서 오므로 `invokeLater`로 이벤트 스레드에 넘기는 부분도 지켜야 한다.

## 후보

### A. `UserDialogs`에 `showMessage` 추가 + 결과 알림 구독을 작은 클래스로 분리
- `UserDialogs.showMessage(String message)`를 추가한다. `SwingUserDialogs`는 지금 코드와 같은 인자(부모 창, 제목 `"Game of Life"`, `INFORMATION_MESSAGE`)로 띄운다. 부모 창은 생성자로 받는다. 기존 `showError`는 원본대로 부모 없이(`null`) 띄운다.
- `GameResultAnnouncer implements GameListener`: `GAME_OVER`일 때만 `controller.gameResultMessage()`를 읽어 `dialogs.showMessage`를 실행기(`Executor`)에 넘긴다. 제품에서는 `SwingUtilities::invokeLater`, 테스트에서는 `Runnable::run`.
- `LifeFrame`은 `controller.addGameListener(new GameResultAnnouncer(controller, new SwingUserDialogs(this), SwingUtilities::invokeLater))` 한 줄만 남는다.
- 장점: 대화상자를 띄우는 곳이 `SwingUserDialogs` 하나로 다시 모인다. 결과 메시지를 창 없이 테스트한다.
- 단점: 타입 +1(`GameResultAnnouncer`), `UserDialogs` 메서드 +1. 이 인터페이스를 구현한 테스트 대역(`GameMenuTest.RecordingDialogs`)도 메서드를 하나 더 구현해야 한다.

### B. 결과 메시지만 받는 별도 인터페이스 (`GameResultView`)
- 장점: 메뉴용 `UserDialogs`를 건드리지 않는다(인터페이스 분리 원칙).
- 단점: 대화상자 Strategy가 두 개로 갈라지고 Swing 구현도 두 개가 된다. 지금 메시지 종류는 오류·결과 두 개뿐이다.

### C. 그대로 (`LifeFrame` 생성자 안 람다)
- 장점: 변경 없음.
- 단점: 위 문제가 남는다.

### 기타 검토
| 패턴 | 판단 | 이유 |
| --- | --- | --- |
| Observer | 이미 사용 (`GameListener`) | A는 기존 Observer의 구독자를 이름 있는 클래스로 바꿀 뿐이다 |
| Mediator (`LifeFrame`이 화면 조정) | 기각 | 조정할 화면 요소가 패널과 대화상자 둘뿐이다 |
| Command (결과 표시 명령) | 기각 | 실행 취소·기록이 없다. `Runnable` 하나로 충분하다 |

## 오버헤드 비교표

| 항목 | A | B | C |
| --- | --- | --- | --- |
| 구조 비용 | 타입 +1, `UserDialogs` 메서드 +1, `SwingUserDialogs` 생성자 +1, `LifeFrame` 9줄 → 2줄 | 타입 +2(인터페이스, Swing 구현) + 구독 클래스 +1 | 0 |
| 간접성 | 알림 → `GameResultAnnouncer` → 실행기 → `UserDialogs` → `JOptionPane` (+2) | 같음 | 알림 → 람다 → `invokeLater` → `JOptionPane` |
| 결합도 | `LifeFrame`의 `JOptionPane` 의존 제거 | 같음 | 그대로 |
| 런타임 비용 | 게임당 1번 호출. 측정하지 않음 | 같음 | — |
| 테스트 용이성 | 기록용 대역 + 즉시 실행기로 headless 확인 | 같음 | 불가 |
| 확장 비용 (메시지 종류 1개 추가) | `UserDialogs` 메서드 1 | 인터페이스 1개 더 또는 메서드 1 | `LifeFrame` 수정 |

## 결정과 근거

A를 채택한다. 006에서 "대화상자를 띄우는 곳을 하나로"를 근거로 `UserDialogs`를 만들었으므로, 같은 종류의 새 대화상자도 그 인터페이스에 둔다. B는 메시지가 두 종류뿐인 지금 Strategy를 둘로 나눌 이유가 약하다.

동작을 유지하는 부분
- 메시지 문구(`controller.gameResultMessage()`), 제목 `"Game of Life"`(오류 대화상자의 `"The Game of Life"`와 다름, 원본 그대로), 아이콘 `INFORMATION_MESSAGE`, 부모 창 `LifeFrame`.
- 알림을 받은 스레드에서 메시지를 먼저 읽고, 대화상자는 `invokeLater`로 이벤트 스레드에서 띄운다.
- `GAME_OVER`가 아닌 알림(`EDITED`, `GENERATION`)은 무시한다.

## 전/후 비교

- 다이어그램: [diagrams/009-before.puml](diagrams/009-before.puml) / [diagrams/009-after.puml](diagrams/009-after.puml)

## 구현 결과

(구현 후 실측값을 채운다)

## 후속 작업

- #57(취소 시 실패 메시지)을 고칠 때 `UserDialogs.chooseFile`의 취소 표현도 함께 바뀐다.
