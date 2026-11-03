# 기존 파일 인코딩

- 제공: C / 사용: B 및 테스트 / 담당: C
- 변경: 2026-11-03, #24. Storable.load/flush를 LegacyLifeCodec의 정적 메서드로 이전.

```java
void LegacyLifeCodec.load(NeighborhoodState state, InputStream in) throws IOException
void LegacyLifeCodec.flush(NeighborhoodState state, OutputStream out) throws IOException
```

기존 Collection<Point> 객체 직렬화 형식을 유지한다. x=열, y=행. 중복과 순서를 보존한다. load는 Collection 캐스트만 수행하고 원소나 좌표를 추가 검증하지 않는다. null도 저장 객체에 대입되어 복원 시 기존 예외가 발생한다. 전달된 스트림은 호출자가 관리한다. 모델 상태 캡처·복원과 실행 정지는 호출자의 책임이다. 동시 접근 보장은 추가하지 않는다.
