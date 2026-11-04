# [001] 기존 실행 조정 추출

- 상태: 채택
- 작성: Codex / 2026-11-03 / #24

## 문제
기준 e38ce7a Universe.java:126~188은 패널 안에서 세대 진행과 파일 작업 순서를 조정한다. Clock.java:81/171은 메뉴 생성·선택 상태를 직접 참조한다.

## 후보
A. Universe 전체를 ui로 이동: 파일·모델 조정이 여전히 UI에 남는다.
B. 해당 본문을 구체 GameController로 추출: 채택. 새 Facade/Mediator/Command/State 계층은 도입하지 않는다. 기존 Clock의 Publisher 알림만 유지한다.

## 결정과 전/후
Clock을 application으로 이동하고 메뉴 코드를 GameMenu로 추출한다. 기존 Singleton과 java.util.Timer.scheduleAtFixedRate(task,0,period)를 유지한다. UI가 메뉴 선택 상태를 확인한 뒤 GameController.tick을 호출한다. 반환값에 따른 부분 갱신 조건도 원본과 같다.

GameController.load는 파일 열기 → stop → clear → createMemento → load → transfer → close 순서를 유지한다. store는 파일 열기 → stop → createMemento → 추가 transfer → flush → close 순서다. 실패 시 스트림 정리나 보드 보존을 개선하지 않는다. 원본의 실패 경로는 별도 버그 수정 범위다.

## 오버헤드
GameController 1개 추가, 기존 Clock 이동. UI에서 제어 메서드 호출 1단계가 추가된다. B의 Swing 의존은 제거되지만 기존 Point/IO 타입은 유지한다. 타이머·주기·스레드 변경은 없다. Clock(Timer)은 동일 구체 Timer를 전달하는 테스트 진입점이며 테스트에서 sleep 없이 예약과 취소를 검증한다. 성능은 측정하지 않았다.

## 후속 작업
Undo/Redo·새 명령·TickSource 인터페이스·Swing Timer·스냅샷·안전한 로딩은 추가하지 않았다. 기존 실행 동작을 바꾸는 작업은 별도 이슈/PR로 진행한다.

## 2026-11-04 병합 반영

위 내용은 최초 추출 시점의 결정이다. main의 #16·#17 병합 후 중복 저장과 형식 오류 계약은 [현재 인터페이스](interfaces.md) 및 [CI 복구 기록](../../application/docs/verification.md)에 맞게 갱신했다(#29).
