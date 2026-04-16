# 프로젝트 개요 및 모듈 구조

## 기술 스택

| 항목 | 내용 |
|------|------|
| Framework | Spring Boot 3.4.2, Java 21 |
| DB | MySQL (prod) / H2 (test) |
| ORM | JPA + QueryDSL 5.1.0 |
| 인증 | JWT (HTTP-Only 쿠키, 6h, HS256) |
| 실시간 | WebSocket (STOMP + SockJS) |
| 알림 | Firebase Admin SDK 9.2.0 |
| 빌드 | Gradle |
| 배포 | Docker (amazoncorretto:21) |

---

## 패키지 분리 원칙

이 프로젝트는 **두 개의 루트 패키지**가 공존한다.

```
com.sayai.record/   ← 원래 서비스 (인증, Fantasy, Player, Game, 뷰어)
com.sayai.kbo/      ← KBO 통계 별도 모듈 (경기 기록, 시즌 스탯)
```

- `com.sayai.kbo`는 `com.sayai.record` 의 엔티티를 참조할 수 있다 (`FantasyPlayer` 등)
- 반대로 `com.sayai.record`는 `com.sayai.kbo`를 직접 참조하지 않는다

---

## 각 패키지 책임

### com.sayai.record

| 패키지 | 책임 |
|--------|------|
| `auth/` | JWT 필터, Member 엔티티, SecurityConfig |
| `config/` | QueryDSL, WebSocket, Cache, Thymeleaf 설정 |
| `controller/` | Player/Game/Ligue 조회 API |
| `model/` | Player, Game, Hit, Pitch, Ligue, Board 엔티티 |
| `service/` | 기본 통계 비즈니스 로직 |
| `firebase/` | FCM 푸시 알림 |
| `admin/` | 관리자 API/뷰 |
| `viewer/` | 에피소드 뷰어 |
| `fantasy/` | 판타지 야구 (드래프트, 웨이버, 스코어링) |

### com.sayai.kbo

| 패키지 | 책임 |
|--------|------|
| `controller/` | KBO 선수 조회, 관리자 업로드 API |
| `model/` | KboGame, KboHit, KboPitch, 시즌 스탯 엔티티 |
| `repository/` | JPA Repository + 네이티브 쿼리 인터페이스 |
| `service/` | KBO 기록 조회/업로드 비즈니스 로직 |
| `config/` | KBO 전용 WebMvc 설정 (Rate Limit 등) |
| `dto/` | 업로드 요청/응답 DTO |

---

## 핵심 DB 테이블 맵

```
app_users           ← Member 엔티티
ft_players          ← FantasyPlayer 엔티티 (선수 마스터)
ft_games            ← FantasyGame
ft_participants     ← FantasyParticipant
ft_draft_picks      ← DraftPick
ft_games_waiver     ← FantasyWaiverOrder
ft_transactions     ← RosterTransaction
ft_score_rotisserie ← FantasyRotisserieScore

kbo_game            ← KboGame
kbo_hit             ← KboHit (경기별 타자 기록)
kbo_pitch           ← KboPitch (경기별 투수 기록)
kbo_hitter_stats    ← KboHitterStats (시즌 누적 타자 스탯)
kbo_pitcher_stats   ← KboPitcherStats (시즌 누적 투수 스탯)
```

---

## game_idx 인코딩 규칙

`game_idx`는 Long 타입이며 형식은 다음과 같다.

```
yyyyMMddHH{away_code}{home_code}
예) 20260328140301 → 2026-03-28 14시, away=두산(03), home=LG(01)
```

시즌 추출: `season = (int)(game_idx / 10_000_000_000L)`

팀 코드:
```
두산=00, LG=01, 키움=02, KT=03, SSG=04
한화=05, KIA=06, 삼성=07, NC=08, 롯데=09
```
