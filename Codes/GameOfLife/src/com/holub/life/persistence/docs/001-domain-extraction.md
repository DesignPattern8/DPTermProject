# [001] 기존 파일 인코딩 추출

- 상태: 채택
- 작성: Codex / 2026-11-03 / #24

## 문제
기준 e38ce7a의 Neighborhood.java:409 내부 NeighborhoodState는 상태 보관과 load/flush를 함께 수행한다. Cell.Memento가 Storable을 상속하여 셀 계약까지 파일 스트림에 의존한다.

## 후보
A. Storable과 NeighborhoodState를 그대로 다른 패키지에 이동: 파일과 모델의 결합은 유지된다.
B. 기존 load/flush 본문만 구체 클래스 LegacyLifeCodec으로 추출: 채택. Memento 데이터는 모델에 남긴다.
이전 계획의 Adapter/Strategy 인터페이스 추가는 사용자의 새 패턴 미도입 요청에 따라 제외한다. 기존 Memento 구조만 보존한다.

## 오버헤드
이 단계: LegacyLifeCodec 최상위 타입 1개 추가, 내부 NeighborhoodState를 최상위 타입으로 이동, Storable 인터페이스 1개 삭제. 인코딩 진입점은 기존 state.load/flush 대신 정적 함수 호출이며 추가 위임 계층은 없다. 최종 파일/LOC 수는 [전체 결정](../../application/docs/002-domain-layout.md)에 기록했다. 실행 시간은 측정하지 않았다.

## 전/후
전: Cell.Memento extends Storable → NeighborhoodState.load/flush.
후: Cell.Memento는 좌표 기록·조회만 제공. LegacyLifeCodec.load/flush가 NeighborhoodState 컬렉션을 읽고 쓴다.

## 후속 작업
파일 경로 입출력은 BoardFileStore로 추출했다. #10 중복 저장과 #11 형식 오류는 그대로 유지한다. 불변 스냅샷·검증 강화·새 형식은 이번 범위에 포함하지 않는다.

## 파일 경로 추출 결과
Universe의 FileInputStream/FileOutputStream 생성을 구체 BoardFileStore로 이동했다. 스트림 close는 복원/저장 성공 뒤 수행하는 원본 위치인 GameController에 남겼다. 파일 열기 단계가 구체 함수 1단계를 거친다.
