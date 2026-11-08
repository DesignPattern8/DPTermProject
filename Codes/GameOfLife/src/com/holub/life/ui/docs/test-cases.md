# 화면 테스트

구현 전 정의: [EXT-R01~05, EXT-U01~02](../../docs/domain-extraction-tests.md).

RenderingExtractionTest는 e38ce7a 원본에서 생성한 ARGB SHA-256 기대값을 사용한다. 빈 보드·glider·4세대 부분 갱신·block 안정화의 픽셀을 고정한다. 511px 표면의 정수 나눗셈과 범위 밖 예외도 유지한다.

GameMenuTest는 기존 속도 매핑·메뉴 선택 중 tick 차단을 검증한다. NativeUiSmoke의 EXT-N01 결과와 실행 방법은 [검증 기록](../../application/docs/verification.md)을 참고한다.
