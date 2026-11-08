# 모델 테스트

[기존 CL-G/E/C 케이스](../../docs/test-cases.md)의 입력·기대값을 유지한다. ClassicCellTest의 초기 셀 토글 호출은 userClicked(null,null)에서 추출된 GridCoordinateMapper.userClicked(r,null,null)로만 바꾼다. 기존 테스트 묶음은 package-private 도우미와 함께 유지한다. [EXT-M01~03](../../docs/domain-extraction-tests.md)도 상태 복원 경계를 검증한다.

## main 병합 후 CI 복구 (#29)

CL-C-08은 모델 내부 계측 필드를 사용하는 `model.NeighborhoodRecomputationTest#onlyBlocksNearChangesAreRecomputed`로 이동한다. 보드 크기, glider 좌표, tick 2~10의 재계산 5개 이하 검증은 유지한다. 나머지 정상·경계·예외 CL-C 케이스는 기존 정의 그대로다.
