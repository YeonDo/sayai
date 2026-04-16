# API 설계 컨벤션

## URL 구조

```
/apis/v1/{domain}/{resource}
```

| 도메인 | 경로 | 인증 |
|--------|------|------|
| KBO 선수 조회 | `/apis/v1/kboplayer/**` | 공개 (Rate Limit 적용) |
| 인증 | `/apis/v1/auth/**` | 일부 공개 |
| Fantasy | `/apis/v1/fantasy/**` | 인증 필요 |
| 관리자 | `/apis/v1/admin/**` | ADMIN 권한 |
| KBO 관리자 | `/apis/v1/admin/kbo/**` | ADMIN 권한 |

---

## Controller 패턴

### 의존성 주입
```java
// KBO 모듈: @RequiredArgsConstructor 사용
@RequiredArgsConstructor
public class KboAdminController { ... }

// 파라미터가 많은 경우: @AllArgsConstructor 도 허용
@AllArgsConstructor
public class KboPlayerController { ... }
```

### 응답 방식
```java
// 단순 데이터 반환: @ResponseBody + 반환 타입 직접 사용
@GetMapping("/hitter/all")
@ResponseBody
public Page<PlayerDto> getAllHitter(...) { ... }

// 에러 처리가 필요한 경우: ResponseEntity<?> 사용
@PostMapping("/game/upload")
public ResponseEntity<?> uploadGameRecords(...) {
    try {
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

- **일반 조회 API**: `@ResponseBody` + 직접 반환
- **업로드/변경 API**: `ResponseEntity<?>` + try-catch

### @CrossOrigin
`@CrossOrigin(origins = "*", allowedHeaders = "*")` 는 KBO 컨트롤러에만 선언됨.  
Record/Fantasy 컨트롤러는 `SecurityConfig`의 전역 CORS 설정을 따른다.

---

## Request Parameter 컨벤션

### 날짜 파라미터
```java
@RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate
```

### 선택적 파라미터
```java
@RequestParam(required = false) Integer season
@RequestParam(value = "sort", required = false) String sort
@RequestParam(value = "page", defaultValue = "0") int page
@RequestParam(value = "size", defaultValue = "20") int size
```

### 필수 파라미터 검증 (컨트롤러 레벨)
Spring Validation 어노테이션보다 **직접 조건 검증**을 사용한다.
```java
if (season != null) {
    // season 모드
} else if (startDate != null && endDate != null) {
    // 기간 모드
} else {
    throw new IllegalArgumentException("season 또는 start+end 파라미터가 필요합니다.");
}
```

---

## 정렬 파라미터 규칙

`sort` 파라미터 형식: `{field}_{direction}`

```
hr_desc   → HR 내림차순
pa_asc    → 타석 오름차순
era_desc  → ERA 내림차순
```

파싱 방식: `lastIndexOf('_')` 로 field/direction 분리.  
필드명에 언더스코어가 없는 단어만 사용한다 (hr, pa, rbi, so, sb, era, whip 등).

---

## Pageable 생성 패턴

```java
// 정렬 없음
PageRequest.of(page, size)

// 정렬 있음 (season 모드)
PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "hr"))
```

---

## limit 파라미터 의미

| 포지션 | limit 의미 | 내부 변환 |
|--------|-----------|----------|
| 타자 | 최소 타석 수 (PA) | `pa >= limit` |
| 투수 | 최소 이닝 수 | `outs >= limit * 3` |

---

## DTO 설계 규칙

### Projection Interface (네이티브 쿼리 결과 매핑)
```java
public interface KboHitterSeasonStatsProjection {
    Long getId();
    String getName();
    Integer getPa();
    // ...
}
```
- getter 메서드명이 SQL 쿼리의 **컬럼 alias와 일치**해야 한다
- alias는 `camelCase`로 작성 (`player_id as id`, `s.pa as pa`)

### 응답 DTO
- `@JsonInclude(JsonInclude.Include.NON_NULL)` 로 null 필드 직렬화 제외
- 계산된 필드(avg, ERA, WHIP)는 DTO에서 String → Double 변환
- 사용하지 않는 필드는 `dto.setXxx(null)` 로 명시적으로 null 처리

```java
dto.setAvgPa(null);
dto.setOnBasePer(null);
// → @JsonInclude(NON_NULL) 로 응답에서 제외됨
```

---

## Rate Limit 규칙

- 적용 대상: `/apis/v1/kboplayer/**`
- 방식: IP 기반 토큰 버킷 (Bucket4j)
- 현재 설정: **IP당 분당 10회** (`KboPlayerRateLimitInterceptor`)
- 초과 시: HTTP 429 + JSON 에러 메시지
- IP 감지 순서: `X-Forwarded-For` → `X-Real-IP` → `RemoteAddr`

설정 변경 위치: `KboPlayerRateLimitInterceptor.REQUESTS_PER_MINUTE`

---

## 관리자 API 업로드 흐름

`POST /apis/v1/admin/kbo/game/upload` (multipart/form-data)

1. Excel 파싱 (Apache POI)
2. `kbo_game` insert (기존 데이터 있으면 덮어씀)
3. `kbo_hit` / `kbo_pitch` insert
4. `kbo_hitter_stats` / `kbo_pitcher_stats` 시즌 누적 업데이트
   - 존재하면: 기존값 + 신규값 계산 후 save (upsert)
   - 없으면: 해당 시즌 전체 합산 후 insert

Excel 시트 구조: Sheet 0 = 원정팀, Sheet 1 = 홈팀
