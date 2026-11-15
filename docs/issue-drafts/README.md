# 이슈 초안

GitHub 이슈로 올리기 전의 초안이다. 형식은 [`.github/ISSUE_TEMPLATE/custom.md`](../../.github/ISSUE_TEMPLATE/custom.md)를 따른다. 올린 뒤에는 각 파일 맨 위에 이슈 번호를 적는다.

| 파일 | 제목 | 근거 | 대상 |
| --- | --- | --- | --- |
| [006a-renderer-draws-resident-only.md](006a-renderer-draws-resident-only.md) | [REFACTOR] BoardRenderer가 Resident만 그린다 | 006 이슈 (a) | ui (리팩토링 팀) |
| [006b-cell-interface-mode-methods.md](006b-cell-interface-mode-methods.md) | [REFACTOR] Cell 인터페이스에 모드별(암) 메서드가 쌓인다 | 006 이슈 (b) | model (리팩토링 팀) |
| [006c-model-api-exposes-awt-point.md](006c-model-api-exposes-awt-point.md) | [REFACTOR] model API가 java.awt.Point를 노출한다 | 006 이슈 (c) | model (리팩토링 팀) |
| [006d-two-player-entry-point.md](006d-two-player-entry-point.md) | [FEAT] 2인용 진입점: GameController 대신 TwoPlayerGame을 부르는 경로 (#7) | 006 이슈 (d) | application (B 도메인) |

근거 문서: [006-two-player-cell.md](../../Codes/GameOfLife/src/com/holub/life/twoplayer/docs/006-two-player-cell.md) "리팩토링 팀에 이슈로 남길 것". 줄 번호는 `feature/5-two-player-cell` 기준이다.
