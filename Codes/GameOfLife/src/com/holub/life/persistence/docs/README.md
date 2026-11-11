# 저장·패턴 데이터 도메인 (C · 도현)

C는 “상태를 어떤 형식으로 보관하는가”를 맡는다. 현재 구현은 파일 읽기·쓰기, 형식 검증, 기존 `.life` 호환이며 프리셋 데이터는 아직 없다.

- [현재 공개 인터페이스와 호출 도메인](interfaces.md)
- [추가 계약·GoF 23개 비교·리팩토링 제안](002-public-contract-and-refactoring.md)
- [최초 파일 인코딩 추출 결정](001-domain-extraction.md)
- [현재 테스트 계약](test-cases.md)
- [전체 도메인 구조](../../application/docs/002-domain-layout.md)
- [기존 검증 결과](../../application/docs/verification.md)

분석 기준은 `19bd091`, 관련 이슈는 [#31](https://github.com/DesignPattern8/DPTermProject/issues/31)이다. 002의 API와 그림은 제안이며 production 구현은 변경하지 않았다.
