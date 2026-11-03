# 화면 테스트

구현 전 정의: [EXT-R01~05, EXT-U01~02](../../docs/domain-extraction-tests.md).

RenderingExtractionTest는 e38ce7a 원본에서 생성한 ARGB SHA-256 기대값을 사용한다. 빈 보드·glider·4세대 부분 갱신·block 안정화의 픽셀을 고정한다. 511px 표면의 정수 나눗셈과 범위 밖 예외도 유지한다.
