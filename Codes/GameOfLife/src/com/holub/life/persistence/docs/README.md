# 저장·패턴 데이터 도메인 (C · 도현)

C는 파일 형식·파일 스트림 수명과 불변 프리셋 데이터를 제공한다. 기존 `.life`와 #60 암세포 확장 파일을 읽고 쓰며, block/blinker/glider 카탈로그를 제공한다. 모델 조작·Clock·프리셋 배치는 B, 화면은 D 책임이다.

- [006 암세포 스냅샷 파일 API·블록 순서 (#92)](006-snapshot-file-api.md): C에서 필터링·정렬, B 연결은 #88 후속 작업.
- [#92 테스트 우선 검증 결과](verification-092.md)

- [005 Legacy 입력 허용 타입·자원 한도 (#65)](005-legacy-input-policy.md)
- [#65 테스트 우선 검증 결과](verification-065.md)
- [004 실패 시 기존 파일 보존·원자적 교체](004-atomic-file-replacement.md)
- [003 구현 결정·후속 작업](003-file-api-and-model-values.md)
- [공개 인터페이스와 계약](interfaces.md)
- [테스트 케이스](test-cases.md)
- [#64 안전한 Store 검증 결과](verification-064.md)
- [#61 검증 결과](verification.md)
- [002 당시 분석·제안 기록](002-public-contract-and-refactoring.md)
- [001 최초 파일 인코딩 추출](001-domain-extraction.md)
- [A 모델 인계 계약](../../model/docs/interfaces.md#후속-작업-인계)

- [B/C 안전한 Load (#63)](../../application/docs/005-safe-load.md): 입력을 닫고 변환한 뒤 보드를 교체하며 실패 시 상태와 Clock을 유지한다.
#61은 #30(종료)의 후속 구현이다. 002는 2026-11-04 코드 분석 기록이며, #50 모델 값 타입과 #60 암세포 저장을 반영한 파일 API 설계는 003, Legacy 입력 허용 범위·한도는 005를 따른다.
