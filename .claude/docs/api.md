# Sayai API 문서

> **대상**: sayai-frontend 개발자 참조용  
> **Base URL**: `https://teamsayai.com` (로컬: `http://localhost:8080`)  
> **인증**: HTTP-Only 쿠키 `accessToken` (JWT, 6시간 유효)

---

## 목차

1. [인증 API](#1-인증-api)
2. [선수 통계 API](#2-선수-통계-api)
3. [게임 기록 API](#3-게임-기록-api)
4. [판타지 게임 API](#4-판타지-게임-api)
5. [판타지 드래프트 API](#5-판타지-드래프트-api)
6. [판타지 로스터 API](#6-판타지-로스터-api)
7. [판타지 순위/로그 API](#7-판타지-순위로그-api)
8. [FCM 알림 API](#8-fcm-알림-api)
9. [에러 코드 정리](#9-에러-코드-정리)

---

## 공통 사항

### 날짜 형식
모든 날짜 파라미터는 `yyyy-MM-dd` 형식 사용.

### 인증이 필요한 API
아래 API들은 로그인 후 `accessToken` 쿠키가 있어야 함. 없으면 `403` 반환.

### 공통 에러 응답
```
400 Bad Request    — 잘못된 파라미터 또는 비즈니스 규칙 위반
401 Unauthorized   — 인증 토큰 없음 또는 만료
403 Forbidden      — 권한 없음 (Admin 전용 API 등)
500 Internal Error — 서버 오류
```

---

## 1. 인증 API

### POST `/apis/v1/auth/login`
로그인. 성공 시 `accessToken` HttpOnly 쿠키를 자동으로 설정함.

**Request Body**
```json
{
  "userId": "string",
  "password": "string"
}
```

**Response** `200`
```json
{
  "name": "홍길동"
}
```

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 아이디/비밀번호 불일치 | 400 | `"Invalid userId or password"` |

---

### POST `/apis/v1/auth/logout`
로그아웃. `accessToken` 쿠키를 삭제함.

**Response** `200` (body 없음)

---

### POST `/apis/v1/auth/signup`
회원가입.

**Request Body**
```json
{
  "userId": "string",
  "password": "string",
  "name": "string",
  "playerId": 1
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 아이디 중복 | 400 | `"User ID already exists"` |
| 비밀번호 비어있음 | 400 | `"Password cannot be empty"` |
| 비밀번호 형식 불일치 | 400 | `"Password must be at least 8 characters long and contain both letters and numbers"` |

---

### GET `/apis/v1/auth/me` 🔒
현재 로그인한 사용자 정보 조회.

**Response** `200`
```json
{
  "playerId": 1,
  "userId": "user123",
  "name": "홍길동",
  "admin": false
}
```

---

### POST `/apis/v1/auth/password` 🔒
비밀번호 변경.

**Request Body**
```json
{
  "currentPassword": "string",
  "newPassword": "string"
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 현재 비밀번호 불일치 | 400 | `"Invalid current password"` |
| 새 비밀번호 형식 불일치 | 400 | `"Password must be at least 8 characters long and contain both letters and numbers"` |

---

## 2. 선수 통계 API

> KBO 선수 통계는 `/apis/v1/kboplayer`, 기본 선수는 `/apis/v1/player`
> 사용 가능한 CORS: `*` (모든 origin 허용)

### GET `/apis/v1/kboplayer/hitter/all`
전체 타자 목록 조회 (페이징).

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `season` | season 또는 start+end 중 하나 필수 | 시즌 연도 (예: `2024`) |
| `start` | ^^ | 시작 날짜 (`yyyy-MM-dd`) |
| `end` | ^^ | 종료 날짜 (`yyyy-MM-dd`) |
| `sort` | 선택 | `{field}_{direction}` 형식 (예: `hr_desc`, `avg_asc`) |
| `page` | 선택 | 페이지 번호 (기본값: `0`) |
| `size` | 선택 | 페이지 크기 (기본값: `20`) |

**Response** `200` — `Page<PlayerDto>`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| season/start+end 미전달 | 400 | `"season 또는 start+end 파라미터가 필요합니다."` |

---

### GET `/apis/v1/kboplayer/pitcher/all`
전체 투수 목록 조회 (페이징). 파라미터 구조는 `/hitter/all`과 동일.

---

### GET `/apis/v1/kboplayer/hitter/{playerId}`
특정 타자 성적 조회.

**Path Parameters**: `playerId`  
**Query Parameters**: `start` (필수), `end` (필수)

**Response** `200` — `PlayerDto`

---

### GET `/apis/v1/kboplayer/pitcher/{playerId}`
특정 투수 성적 조회. 파라미터 구조는 `/hitter/{playerId}`와 동일.

---

### GET `/apis/v1/kboplayer/hitter/{playerId}/period`
타자 기간별 성적 조회.

**Query Parameters**
| 파라미터 | 설명 |
|---------|------|
| `list` | 복수 전달 가능. `"total"`, `"2024"`, `"202404"` 형식 |

**Response** `200` — `List<PlayerDto>`

---

## 3. 게임 기록 API

### GET `/apis/v1/game/list`
기간별 게임 목록 조회.

**Query Parameters**: `start` (필수), `end` (필수)

**Response** `200` — `List<GameDto>`

---

### GET `/apis/v1/game/recent`
마지막으로 기록된 게임 날짜 조회.

**Response** `200` — `"2024-10-05"` (문자열)

---

## 4. 판타지 게임 API

### GET `/apis/v1/fantasy/games` 🔒
대시보드용 게임 목록 조회.

**Response** `200` — `List<FantasyGameDto>`

---

### GET `/apis/v1/fantasy/my-games` 🔒
내가 참여한 게임 목록 조회.

**Query Parameters**
| 파라미터 | 설명 |
|---------|------|
| `status` | 선택. `WAITING`, `DRAFTING`, `FA_SIGNING`, `ONGOING`, `FINISHED` |
| `type` | 선택. 게임 타입 필터 |

**Response** `200` — `List<FantasyGameDto>`

---

### GET `/apis/v1/fantasy/games/{gameSeq}/details`
게임 상세 정보 조회. **인증 불필요**.

**Response** `200` — `FantasyGameDetailDto`

---

### GET `/apis/v1/fantasy/games/{gameSeq}/participants`
게임 참여자 목록 조회. **인증 불필요**.

**Response** `200` — `List<ParticipantDto>`

---

### POST `/apis/v1/fantasy/games/{gameSeq}/join` 🔒
게임 참가 신청.

**Request Body**
```json
{
  "teamName": "string"
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 잘못된 게임 상태 | 500 | `"Cannot join game. Status is {status}"` |
| 이미 참여 신청함 | 500 | `"이미 참여 신청을 완료했습니다"` |

---

### POST `/apis/v1/fantasy/games/{gameSeq}/start` 🔒 (Admin only)
게임 시작. Admin만 호출 가능.

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| Admin 아님 | 403 | `"Only Admin can start the draft"` |
| 게임 상태가 WAITING이 아님 | 500 | `"Game must be WAITING status to start."` |
| 참여자 없음 | 500 | `"Cannot start game with no participants."` |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/kbo-stats`
게임 참여자별 KBO 성적 집계.

**Query Parameters**: `startDt` (필수, `yyyy-MM-dd`), `endDt` (필수, `yyyy-MM-dd`)

**Response** `200` — `List<ParticipantKboStatsDto>`

---

## 5. 판타지 드래프트 API

### GET `/apis/v1/fantasy/games/{gameSeq}/available-players`
드래프트 가능한 선수 목록 조회.

**Query Parameters**
| 파라미터 | 설명 |
|---------|------|
| `team` | 선택. 팀 이름 필터 |
| `position` | 선택. 포지션 필터 |
| `search` | 선택. 이름 검색 |
| `sort` | 선택. 정렬 기준 |
| `foreignerType` | 선택. `TYPE_1`, `TYPE_2`, `NONE` |

**Response** `200` — `List<FantasyPlayerDto>`

---

### POST `/apis/v1/fantasy/draft` 🔒
선수 드래프트 픽.

**Request Body**
```json
{
  "gameSeq": 1,
  "fantasyPlayerSeq": 42
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 드래프트 미활성 상태 | 500 | `"Drafting or FA signing is not active (Status: {status})"` |
| 내 차례 아님 | 500 | `"당신의 차례가 아닙니다: {pickerId}"` |
| 이미 뽑힌 선수 | 500 | `"이미 뽑힌 선수입니다"` |
| 로스터 가득 참 | 500 | `"Roster full (Max {limit})"` |
| 샐러리캡 초과 | 500 | `"샐캡 초과: {amount} / {cap}"` |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/my-picks` 🔒
내 드래프트 픽 목록 조회.

**Response** `200` — `MyPicksResponseDto`

---

### GET `/apis/v1/fantasy/games/{gameSeq}/players/{playerId}/picks`
특정 참여자의 드래프트 픽 조회. **인증 불필요**.

**Response** `200` — `MyPicksResponseDto`

---

### POST `/apis/v1/fantasy/games/{gameSeq}/my-team/save` 🔒
로스터(포지션 배치) 저장.

**Request Body**
```json
{
  "entries": [
    {
      "fantasyPlayerSeq": 42,
      "assignedPosition": "SP"
    }
  ]
}
```

**포지션 값**: `SP`, `RP`, `CL`, `C`, `1B`, `2B`, `3B`, `SS`, `OF`, `DH`, `BENCH`  
**포지션 제한**: SP/RP 최대 4명, 나머지 포지션 1명

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 포지션 한도 초과 | 400 | `"Position limit exceeded for {pos} (Max {limit})"` |
| 게임 종료 상태 | 500 | `"Cannot update roster when FINISHED."` |
| 샐러리캡 초과 | 500 | `"샐러리캡을 초과하여 저장할 수 없습니다. (현재: {n} / 제한: {n})"` |

---

## 6. 판타지 로스터 API

### POST `/apis/v1/fantasy/roster/waiver` 🔒
웨이버 신청 (선수 방출 또는 FA 영입).

**Request Body**
```json
{
  "gameSeq": 1,
  "releasePlayerSeq": 10,
  "targetPlayerSeq": 20
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 본인 선수 클레임 | 400 | `"Cannot claim your own waived player"` |
| 소유하지 않은 선수 | 400 | `"Player does not belong to you"` |
| 이미 처리된 거래 | 500 | `"Transaction is already processed"` |
| 선수가 BENCH 아님 | 500 | `"Player {seq} must be on BENCH"` |

---

### POST `/apis/v1/fantasy/roster/trade` 🔒
트레이드 신청.

**Request Body**
```json
{
  "gameSeq": 1,
  "givingPlayerSeqs": [10],
  "receivingPlayerSeqs": [20],
  "targetParticipantId": 5
}
```

**제약조건**
- 주는 선수: 최소 1명, 최대 2명
- 받는 선수: 최소 1명, 최대 2명

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 주는 선수 없음 | 400 | `"Must give at least one player"` |
| 받는 선수 없음 | 400 | `"Must receive at least one player"` |
| 한 쪽에 3명 이상 | 400 | `"Max 2 players per side"` |
| 신청자 샐캡 초과 | 500 | `"Trade failed: Requester Salary Cap Exceeded ({cost} > {cap})"` |
| 상대방 샐캡 초과 | 500 | `"Trade failed: Target Salary Cap Exceeded ({cost} > {cap})"` |
| 소유권 변경됨 | 500 | `"Player {seq} ownership changed"` |

---

### GET `/apis/v1/fantasy/roster/games/{gameSeq}/waivers`
웨이버 보드 조회. **인증 불필요**.

**Response** `200` — `List<WaiverBoardDto>`

---

### GET `/apis/v1/fantasy/roster/games/{gameSeq}/waiver-orders`
웨이버 우선순위 조회. **인증 불필요**.

**Response** `200` — `List<WaiverOrderDto>`

---

### POST `/apis/v1/fantasy/roster/games/{gameSeq}/waivers/{transactionSeq}/claim` 🔒
웨이버 클레임 (방출된 선수 획득 신청).

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 유효하지 않은 웨이버 거래 | 400 | `"Invalid waiver transaction"` |
| Waiver 거래가 아님 | 400 | `"Not a Waiver transaction"` |

---

## 7. 판타지 순위/로그 API

### GET `/apis/v1/fantasy/games/{gameSeq}/ranking`
게임 순위표 조회. **인증 불필요**.

**Response** `200` — `RankingTableDto`

---

### GET `/apis/v1/fantasy/games/{gameSeq}/picks`
게임의 전체 드래프트 픽 로그. **인증 불필요**.

**Response** `200` — `List<DraftLogDto>`

---

### GET `/apis/v1/fantasy/games/{gameSeq}/logs`
로스터 변경 로그 조회. **인증 불필요**.

**Query Parameters**
| 파라미터 | 설명 |
|---------|------|
| `type` | 선택. `DRAFT`만 전달 시 드래프트 픽만 표시. 미전달 시 드래프트 픽 제외한 나머지 |

**로그 액션 타입**: `DRAFT_PICK`, `FA_ADD`, `WAIVER_RELEASE`, `WAIVER_FA`, `WAIVER_CLAIM`, `TRADE`

**Response** `200` — `List<RosterLogDto>`

---

### GET `/apis/v1/fantasy/players/pick-owner`
선수 이름으로 현재 소유 팀 조회. **인증 불필요**.

**Query Parameters**: `gameSeq` (필수), `name` (필수, 선수 이름)

**Response** `200`
```json
{
  "teamName": "홍길동팀"
}
```
> 소유자 없을 경우 `teamName`이 빈 문자열 `""` 로 반환됨.

---

## 8. FCM 알림 API

### POST `/apis/v1/fcm/subscribe` 🔒
FCM 토픽 구독 등록.

**Request Body**
```json
{
  "token": "FCM_DEVICE_TOKEN",
  "topic": "TOPIC_NAME"
}
```

**Response** `200` `"OK"` (token 또는 topic이 없으면 처리 없이 200 반환)

---

## 9. 에러 코드 정리

### HTTP 상태코드 요약

| 코드 | 의미 | 발생 상황 |
|------|------|---------|
| `200` | 성공 | - |
| `400` | 잘못된 요청 | 파라미터 오류, 비즈니스 규칙 위반 (`IllegalArgumentException`) |
| `401` | 인증 필요 | 🔒 API 호출 시 `accessToken` 쿠키 없음 |
| `403` | 권한 없음 | Admin 전용 API를 일반 사용자가 호출 |
| `500` | 서버 오류 | 비즈니스 로직 예외 (`IllegalStateException`) |

> **참고**: 현재 서버는 `IllegalStateException`을 `500`으로 반환함. 클라이언트에서 500 에러 수신 시에도 `response.body` 에 에러 메시지가 포함되어 있으니 파싱해서 사용자에게 보여주는 것을 권장.

### 자주 만날 에러 메시지 빠른 참조

| 메시지 | 원인 |
|--------|------|
| `"Invalid userId or password"` | 로그인 실패 |
| `"이미 참여 신청을 완료했습니다"` | 게임 중복 참여 시도 |
| `"당신의 차례가 아닙니다"` | 드래프트 순서 아닐 때 픽 시도 |
| `"이미 뽑힌 선수입니다"` | 이미 드래프트된 선수 픽 시도 |
| `"Roster full"` | 로스터 최대 인원 초과 |
| `"샐캡 초과"` / `"Salary Cap Exceeded"` | 샐러리캡 한도 초과 |
| `"Position limit exceeded"` | 포지션 배치 한도 초과 |
| `"Only Admin can start the draft"` | 일반 유저가 게임 시작 시도 |

---

## GameStatus 참조

| 값 | 설명 |
|----|------|
| `WAITING` | 참가 신청 대기 중 |
| `DRAFTING` | 드래프트 진행 중 |
| `FA_SIGNING` | FA 서명 기간 |
| `ONGOING` | 시즌 진행 중 |
| `FINISHED` | 종료 |
