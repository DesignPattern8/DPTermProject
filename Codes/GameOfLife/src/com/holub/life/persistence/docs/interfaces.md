# 기존 파일 인코딩

- 제공: C / 사용: B 및 테스트 / 담당: C
- 변경: 2026-11-03, #24. Storable.load/flush를 LegacyLifeCodec의 정적 메서드로 이전.

```java
void LegacyLifeCodec.load(NeighborhoodState state, InputStream in) throws IOException
void LegacyLifeCodec.flush(NeighborhoodState state, OutputStream out) throws IOException
```

기존 Collection<Point> 객체 직렬화 형식을 유지한다. x=열, y=행. 중복과 순서를 보존한다. load는 Collection 여부와 각 원소의 Point 여부를 검사하고, null을 포함한 잘못된 형식은 IOException으로 보고한다. 좌표 범위 검증은 추가하지 않는다. 전달된 스트림은 호출자가 관리한다. 모델 상태 캡처·복원과 실행 정지는 호출자의 책임이다. 동시 접근 보장은 추가하지 않는다.

파일 열기는 BoardFileStore.openForLoad(File)/openForStore(File)가 수행한다. 반환 스트림의 수명과 원본 동작 순서는 GameController가 소유한다. 실패 경로를 바꾸지 않기 위해 파일 열기와 디코딩을 단일 원자 연산으로 합치지 않았다.

- 변경: 2026-11-04, #29. main의 #17 형식 검증을 추출된 Codec에 반영. 시그니처 변경 없음.
