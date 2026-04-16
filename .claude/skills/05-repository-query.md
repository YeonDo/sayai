# Repository / 쿼리 작성 규칙

## Repository 기본 패턴

```java
@Repository
public interface KboHitRepository extends JpaRepository<KboHit, Long> {
    // 네이티브 쿼리 우선 사용
}
```

모든 Repository는 `JpaRepository<Entity, Id>` 상속.  
QueryDSL 설정이 있지만 (`JPAQueryFactory` Bean), 현재는 네이티브 쿼리 위주로 작성됨.

---

## 네이티브 쿼리 작성 규칙

### 기본 구조
```java
@Query(value = "SELECT ... FROM ... WHERE ...",
       countQuery = "SELECT COUNT(*) FROM ...",
       nativeQuery = true)
Page<ProjectionInterface> findByXxx(@Param("xxx") Type xxx, Pageable pageable);
```

### Pagination 사용 시
- `Page<T>` 반환형이면 반드시 `countQuery` 별도 작성
- `Slice<T>` 는 countQuery 불필요

### 정렬 (Sort from Pageable)
- Native 쿼리에서 `Pageable`의 `Sort`는 Spring Data JPA가 자동으로 `ORDER BY` 절 추가
- Sort 컬럼명은 **SQL 쿼리 결과의 alias 이름**과 일치해야 함
- 쿼리 내 `ORDER BY` 가 이미 있는 경우 Pageable Sort와 충돌 가능 → 둘 중 하나만 사용

```java
// Sort.by(Direction.DESC, "hr") → ORDER BY hr DESC 로 추가됨
// alias: "s.hr as hr" 와 "hr" 가 일치해야 함
```

---

## Projection Interface 네이밍

| 용도 | 네이밍 패턴 | 예시 |
|------|-----------|------|
| 기간 집계 쿼리 결과 | `Kbo{Entity}StatInterface` | `KboHitStatInterface` |
| 시즌 스탯 조회 결과 | `Kbo{Entity}SeasonStat[Interface/Projection]` | `KboHitterSeasonStatsProjection` |
| Fantasy 참가자 스탯 | `KboParticipantStatsInterface` | |

Projection getter 메서드명 = SQL alias 이름 (camelCase 일치 필수)

---

## 집계 쿼리 패턴

### 기간 집계 (경기별 Raw 기록 합산)
```sql
SELECT p.seq as id, p.name as name,
       IFNULL(SUM(h.pa), 0) as playerAppearance,
       ...
FROM kbo_hit h
JOIN ft_players p ON h.PLAYER_ID = p.seq
JOIN kbo_game g ON h.game_idx = g.game_idx
WHERE g.game_idx BETWEEN :startIdx AND :endIdx
GROUP BY p.seq
```
- `IFNULL(SUM(...), 0)` 패턴으로 null 방지
- `game_idx BETWEEN startIdx AND endIdx` 로 날짜 범위 필터

### 날짜 → game_idx 변환
```java
// yyyy-MM-dd → 해당 날짜 시작/끝 game_idx
Long startIdx = Long.parseLong(date.format("yyyyMMdd") + "000000");
Long endIdx   = Long.parseLong(date.format("yyyyMMdd") + "239999");
```

### 시즌 범위 (연도 전체)
```java
long startIdx = (long) season * 10_000_000_000L;
long endIdx   = (long) (season + 1) * 10_000_000_000L - 1;
```

---

## 시즌 스탯 테이블 쿼리

`kbo_hitter_stats` / `kbo_pitcher_stats` 는 `ft_players` JOIN 이 필요하다.

```sql
SELECT s.player_id as id, p.name as name, s.pa as pa, ...
FROM kbo_hitter_stats s
JOIN ft_players p ON s.player_id = p.seq
WHERE s.season = :season
AND (:minPa IS NULL OR s.pa >= :minPa)
```

- `:minPa IS NULL OR` 패턴으로 선택적 필터링 처리 (Java `null` 전달 시 필터 무시)
- limit 미입력 시 `null` 전달 → 전체 조회

---

## 트랜잭션 규칙

```java
@Service
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
public class KboHitService {

    @Transactional  // 메서드 레벨: 쓰기 작업에 명시적 오버라이드
    public void saveStats(...) { ... }
}
```

- 읽기 전용 서비스는 클래스에 `@Transactional(readOnly = true)` 선언
- 쓰기 작업은 메서드에 `@Transactional` 명시
- `@Transactional`은 Service 레이어에만 적용, Controller/Repository에는 선언하지 않음

---

## Repository vs Service 경계

| 작업 | 위치 |
|------|------|
| SQL 쿼리 작성 | Repository |
| 비즈니스 로직 (계산, 조건 분기) | Service |
| 여러 Repository 호출 조합 | Service |
| DTO 변환 | Service (private mapToDto 메서드) |
| 트랜잭션 경계 | Service |

---

## Upsert 패턴

JPA `save()`는 ID가 있으면 update, 없으면 insert.  
복합 PK 엔티티의 경우 항상 새 객체를 Builder로 만들어 `save()` 호출.

```java
Optional<KboHitterStats> existing = kboHitterStatsRepository.findByPlayerIdAndSeason(id, season);
if (existing.isPresent()) {
    // 기존값 + 신규값 계산
    kboHitterStatsRepository.save(KboHitterStats.builder()
        .playerId(id).season(season).pa(newPa)...build());
} else {
    // 전체 합산 후 insert
    kboHitterStatsRepository.save(KboHitterStats.builder()
        .playerId(id).season(season).pa(totalPa)...build());
}
```
