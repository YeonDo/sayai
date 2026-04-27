# Sayai Record — 프로젝트 가이드

## 개요

| 항목 | 내용 |
|------|------|
| 설명 | 야구 통계 기록 + 판타지 야구 게임 플랫폼 |
| 프레임워크 | Spring Boot 3.4.2, Java 21 |
| DB | MySQL (테스트: H2) |
| ORM | JPA/Hibernate + QueryDSL 5.1.0 |
| 인증 | JWT (HTTP-Only 쿠키, 6시간, HS256) |
| 실시간 | WebSocket (STOMP + SockJS) |
| 알림 | Firebase Cloud Messaging (Web Push, iOS PWA) |
| 템플릿 | Thymeleaf + Layout Dialect |
| 도메인 | `teamsayai.com` |
| 배포 | Docker (amazoncorretto:21) |

---

## 패키지 구조

```
com.sayai.record/
├── auth/           JWT 인증, Member 엔티티, SecurityConfig
├── config/         Security, WebSocket, Cache, QueryDSL 설정
├── controller/     야구 통계 API (Player, Game, Ligue, Crawling)
├── model/          Player, Game, Hit, Pitch, Ligue, HitterBoard, PitcherBoard
├── service/        비즈니스 로직
├── firebase/       FcmService, FcmController, FirebaseConfig
├── admin/          관리자 API/뷰
├── viewer/         Episode 뷰어
└── fantasy/        판타지 야구 모듈 (아래 별도 정리)

com.sayai.kbo/      KBO 통계 별도 모듈 (Game, Hit, Pitch, Ligue)
```

### Fantasy 서브모듈 구조

```
fantasy/
├── entity/    FantasyGame, FantasyParticipant, DraftPick, FantasyPlayer
│              FantasyWaiverOrder, FantasyWaiverClaim, RosterTransaction
│              RosterLog, FantasyRotisserieScore, Post
├── dto/       16개 DTO
├── repository/ 11개 Repository
├── service/   FantasyDraftService, FantasyGameService, FantasyRosterService
│              FantasyRankingService, FantasyScoringService, PostService
│              DraftScheduler, WaiverScheduler
├── rule/      DraftRuleValidator, Rule1Validator, Rule2Validator
├── scoring/   ScoringStrategy (interface), PointScoringStrategy, RotisserieScoringStrategy
└── controller/ 8개 Controller
```

---

## 핵심 엔티티 관계

```
Member (app_users)
  └─ playerId → Player.id 연결

FantasyGame (ft_games)  상태: WAITING → DRAFTING → ONGOING → FINISHED
  ├─ FantasyParticipant (ft_participants)  [playerId → Member]
  ├─ DraftPick (ft_draft_picks)
  ├─ FantasyWaiverOrder (ft_games_waiver)
  └─ FantasyRotisserieScore (ft_score_rotisserie)

RosterTransaction (ft_transactions)  타입: WAIVER | TRADE
  └─ 상태: REQUESTED → APPROVED | REJECTED | FA_MOVED

FantasyPlayer (ft_players)  foreignerType: TYPE_1 | TYPE_2 | NONE
```

---

## API 엔드포인트 요약

### 공개 API
```
GET  /apis/v1/player/**              선수 조회
GET  /apis/v1/game/**                경기 조회
POST /apis/v1/auth/login             로그인
POST /apis/v1/auth/signup            회원가입
GET  /apis/v1/fantasy/games/{seq}/details  게임 상세
```

### 인증 필요 (USER)
```
GET  /apis/v1/auth/me                내 정보
GET  /apis/v1/fantasy/my-games       내 게임 목록
POST /apis/v1/fantasy/draft          드래프트 픽
POST /apis/v1/fantasy/roster/waiver  웨이버 신청
POST /apis/v1/fantasy/roster/trade   트레이드 신청
POST /apis/v1/fcm/subscribe          FCM 토픽 구독
```

### 관리자 전용 (ADMIN)
```
/apis/v1/admin/**                    관리자 API
POST /apis/v1/kbo/games/upload       KBO 경기 업로드
POST /apis/v1/fantasy/games/{seq}/start  게임 시작
```

---

## 인증 / Security

- JWT: HTTP-Only 쿠키 (`accessToken`) 우선, `Authorization: Bearer` 헤더 폴백
- 클레임: `sub(userId)`, `playerId`, `role`, `name`
- CORS 허용: `teamsayai.com`, `*.teamsayai.com`, `localhost:*`
- 미인증 API → 403 반환, 미인증 페이지 → `/?login=required&redirect=` 리다이렉트
- 비밀번호: BCrypt, 최소 8자 영문+숫자 조합

---

## WebSocket (드래프트 실시간)

```
엔드포인트: /ws  (SockJS 폴백)
구독 접두사: /topic
앱 접두사:   /app
용도: 드래프트 순서 업데이트, 웨이버/거래 알림
```

---

## Firebase FCM (Web Push)

- SDK: firebase-admin 9.2.0 (백엔드), firebase compat 9.2.0 (프론트)
- FCM 키: `record-private/private/fcm-key.json` (Docker COPY 경로: `/etc/conf/fcm-key.json`)
- VAPID Key: `fcm.js` 상단 `VAPID_KEY` 상수 — **Firebase Console에서 설정 필요**
- SW: `static/firebase-messaging-sw.js`

### iOS PWA 알림 조건
- iOS 16.4 이상
- Safari → 홈화면에 추가 → standalone 모드로 실행
- 앱 안에서 사용자가 "허용" 버튼 클릭 → `Notification.requestPermission()` 호출

---

## 주요 파일 경로

| 파일 | 경로 |
|------|------|
| 기본 레이아웃 | `templates/layout/default.html` |
| 인증 JS | `static/js/auth.js` |
| FCM 클라이언트 | `static/js/fcm.js` |
| Service Worker | `static/firebase-messaging-sw.js` |
| PWA Manifest | `static/manifest.json` |
| Security 설정 | `auth/config/SecurityConfig.java` |
| Firebase 설정 | `firebase/FirebaseConfig.java` |
| WebSocket 설정 | `config/WebSocketConfig.java` |
| Dockerfile | `Dockerfile` |

---

## 비즈니스 플로우

### 드래프트
1. ADMIN이 FantasyGame 생성 (WAITING)
2. 사용자들 참가 신청 (FantasyParticipant)
3. ADMIN이 게임 시작 → 상태 DRAFTING, DraftScheduler 실행
4. 순서대로 `/apis/v1/fantasy/draft` POST → DraftPick 생성
5. 전원 완료 → 상태 ONGOING

### 로티서리 스코어링
- 카테고리: avg, hr, rbi, so, sb, wins, era, whip, saves
- 카테고리별 참가자 랭킹 → 포인트 환산 → `FantasyRotisserieScore` 저장
- 총점 기준 순위표 표시

### 웨이버
1. 사용자가 웨이버 신청 → `RosterTransaction(WAIVER)` 생성
2. `FantasyWaiverClaim` 등록 (우선순위: FantasyWaiverOrder)
3. `WaiverScheduler` 처리 → 로스터 변경 → FCM 알림 발송

---

## 스코어링 전략 패턴

```java
ScoringStrategy (interface)
├── PointScoringStrategy   — 포인트제 (개인 성적 → 점수)
└── RotisserieScoringStrategy — 로티서리제 (카테고리 랭킹 기반)
```

드래프트 규칙:
```java
DraftRuleValidator
├── Rule1Validator  — 규칙 1 (type=RULE_1)
└── Rule2Validator  — 규칙 2 (type=RULE_2, 외국인 선수 제한 등)
```

---

## 판타지 데이터 조회 규칙

> **항상 준수** — 판타지 관련 기능 작업 시 아래 테이블을 반드시 참조할 것

| 항목 | 테이블 | 비고 |
|------|--------|------|
| 선수명 | `ft_players` (FantasyPlayer) | `playerName` 컬럼 |
| 팀명 | `ft_participants` (FantasyParticipant) | `teamName` 컬럼 |

---

## 개발 / 배포 참고

- QueryDSL APT 어노테이션 프로세서 사용 → `build/generated` 경로에 Q클래스 생성
- 설정 파일: `record-private/private/` (Git 비공개 서브디렉토리)
- Spring Profile: `prod` (Docker), `dev` (로컬)
- 캐시: `@EnableCaching` — 기본 ConcurrentMapCacheManager
