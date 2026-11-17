# model 도메인

셀 계산·상태·활성 블록을 관리하고 클래식·암세포 보드 API를 제공한다.

- [#24 도메인 분리 결정](../../../../../../../../docs/GTY.md#001-모델과-화면-메서드-분리)
- [#49 클래식 보드 Facade 설계 결정](../../../../../../../../docs/GTY.md#002-클래식-보드-facade) · [전후 클래스 다이어그램](https://app.notion.com/p/40a61a0bb83480208e4accb053cf40f7)
- [#52 클래식 생사 판정 분리](../../../../../../../../docs/GTY.md#003-클래식-생사-판정-분리) · [전후 클래스 다이어그램](https://app.notion.com/p/40a61a0bb83480208e4accb053cf40f7)
- [#73 암세포 스냅샷 보존·문서 통합](../../../../../../../../docs/GTY.md#004-암세포-스냅샷-보존-73)
- [#81 legacy Load 경계 활성화](../../../../../../../../docs/GTY.md#006-legacy-load-경계-활성화-81)
- [계약](interfaces.md)
- [테스트](test-cases.md)

- [전체 이전 구조·전후 UML](../../application/docs/002-domain-layout.md)
- [검증 결과](../../application/docs/verification.md)

- [C 저장·패턴 도메인의 공개 계약 및 도메인 간 변경 제안](../../persistence/docs/002-public-contract-and-refactoring.md)

- [C 파일 API·모델 값 연동 (#61)](../../persistence/docs/003-file-api-and-model-values.md): 기존 Memento 저장 경로·암세포 보존, 클래식 Snapshot 변환과 C/B 책임 경계.

A 모델의 설계 결정·검증 결과·코드 전후 비교는 루트 [docs/GTY.md](../../../../../../../../docs/GTY.md)에 작업마다 누적한다. 클래스 다이어그램 이미지는 기존 Notion 개발 기록에 보관한다. 공개 계약과 테스트 기준은 이 폴더의 `interfaces.md`·`test-cases.md`를 갱신한다.
