# Sayai Record — 프로젝트 규칙 문서

이 디렉토리는 Claude Code 가 작업 시 참고하는 프로젝트 규칙 문서 모음입니다.

## 문서 목록

| 파일 | 내용 |
|------|------|
| [01-overview.md](01-overview.md) | 프로젝트 개요, 모듈 구조, 패키지 책임, DB 테이블 맵, game_idx 인코딩 |
| [02-entity-convention.md](02-entity-convention.md) | JPA 엔티티 패턴, Lombok 조합, PK 전략, 타입 규칙, Enum 정의 |
| [03-api-convention.md](03-api-convention.md) | URL 구조, Controller 패턴, Request/Response 규칙, Rate Limit, 정렬/페이징 |
| [04-security-auth.md](04-security-auth.md) | JWT 구조, Security 설정, CORS, 비밀번호 정책, 미인증 처리 |
| [05-repository-query.md](05-repository-query.md) | 네이티브 쿼리 작성법, Projection, Pageable/Sort, 트랜잭션, Upsert 패턴 |
| [06-fantasy-module.md](06-fantasy-module.md) | 게임 상태 흐름, 드래프트, 웨이버, 스코어링 전략, WebSocket |
| [07-stats-calculation.md](07-stats-calculation.md) | 타율/ERA/WHIP 계산식, outs vs inning 구분, 시즌 누적 전략, Excel 파싱 |

## 빠른 참조

### 새 API 추가 시
1. `03-api-convention.md` — URL 구조, 파라미터 규칙
2. `04-security-auth.md` — 공개/인증 경로 등록
3. `05-repository-query.md` — 쿼리 작성 방식

### 새 엔티티 추가 시
1. `02-entity-convention.md` — Lombok 패턴, PK 전략

### KBO 기록 관련 작업 시
1. `07-stats-calculation.md` — 계산식, 필드 매핑
2. `05-repository-query.md` — 집계 쿼리 패턴

### Fantasy 관련 작업 시
1. `06-fantasy-module.md` — 전체 도메인 흐름
