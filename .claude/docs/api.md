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

> `team` 필드 포함. `ft_players`에 등록되지 않은 선수는 `null`.

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| season/start+end 미전달 | 400 | `"season 또는 start+end 파라미터가 필요합니다."` |

---

### GET `/apis/v1/kboplayer/pitcher/all`
전체 투수 목록 조회 (페이징). 파라미터 구조는 `/hitter/all`과 동일.

> `team` 필드 포함. `ft_players`에 등록되지 않은 선수는 `null`.

---

### GET `/apis/v1/kboplayer/hitter/{playerId}`
특정 타자 기간 합산 성적 + 일자별 성적 목록 조회.

**Path Parameters**: `playerId`  
**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `start` | 필수 | 시작 날짜 (`yyyy-MM-dd`) |
| `end` | 필수 | 종료 날짜 (`yyyy-MM-dd`) |
| `page` | 선택 | 일자별 성적 페이지 번호 (기본값: `0`) |
| `size` | 선택 | 일자별 성적 페이지 크기 (기본값: `10`) |

**Response** `200` — `HitterDetailResponse`
```json
{
  "summary": {
    "id": 123,
    "name": "홍길동",
    "team": "KIA",
    "totalGames": 50,
    "battingAvg": 0.312,
    "playerAppearance": 210,
    "atBat": 185,
    "totalHits": 58,
    "homeruns": 10,
    "rbi": 42,
    "runs": 30,
    "sb": 5,
    "strikeOut": 40
  },
  "dailyStats": {
    "content": [
      {
        "gameDate": "20240418",
        "opponent": "LG",
        "pa": 4,
        "ab": 3,
        "hit": 2,
        "hr": 1,
        "rbi": 3,
        "run": 2,
        "sb": 0,
        "so": 1,
        "battingAvg": 0.667
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "number": 0,
    "size": 10
  }
}
```

> `dailyStats.content`는 최근 날짜 순(내림차순) 정렬.  
> `team`은 `ft_players`에 등록된 현재 소속팀. 미등록 선수는 `null`.

---

### GET `/apis/v1/kboplayer/pitcher/{playerId}`
특정 투수 기간 합산 성적 + 일자별 성적 목록 조회.

**Path Parameters**: `playerId`  
**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `start` | 필수 | 시작 날짜 (`yyyy-MM-dd`) |
| `end` | 필수 | 종료 날짜 (`yyyy-MM-dd`) |
| `page` | 선택 | 일자별 성적 페이지 번호 (기본값: `0`) |
| `size` | 선택 | 일자별 성적 페이지 크기 (기본값: `10`) |

**Response** `200` — `PitcherDetailResponse`
```json
{
  "summary": {
    "id": 456,
    "name": "김투수",
    "team": "두산",
    "innings": 120.2,
    "wins": 10,
    "loses": 5,
    "saves": 0,
    "era": 3.45,
    "whip": 1.12,
    "stOut": 130,
    "baseOnBall": 40,
    "pHit": 105,
    "selfLossScore": 46
  },
  "dailyStats": {
    "content": [
      {
        "gameDate": "20240418",
        "opponent": "삼성",
        "innings": 6.0,
        "win": 1,
        "lose": 0,
        "save": 0,
        "er": 2,
        "bb": 3,
        "hbp": 0,
        "pHit": 7,
        "so": 8,
        "era": 3.0
      }
    ],
    "totalElements": 20,
    "totalPages": 2,
    "number": 0,
    "size": 10
  }
}
```

> `dailyStats.content`는 최근 날짜 순(내림차순) 정렬.  
> `innings`는 `이닝수/3 + (나머지아웃수)*0.1` 포맷 (예: 6이닝 1/3 → `6.1`).  
> `era`는 해당 등판 단독 ERA.  
> `team`은 `ft_players`에 등록된 현재 소속팀. 미등록 선수는 `null`.

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

### POST `/apis/v1/admin/fantasy/games/{gameSeq}/scores/{round}/upload-from-snapshot` 🔒 (Admin only)
KBO 로스터 스냅샷의 조회하여 해당 선수들의 판타지 라운드 성적을 자동 계산 및 저장.  
`kbo-stats` API로 집계한 데이터를 판타지 성적(FantasyScoreDto)으로 변환한 뒤 `saveAndCalculateScores`를 호출한다.

**Path Parameters**
| 파라미터 | 설명 |
|---------|------|
| `gameSeq` | 판타지 게임 seq |
| `round` | 업데이트할 라운드 번호 |

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `startDt` | 필수 | 라운드 시작 일자 (`yyyy-MM-dd`) |
| `endDt` | 필수 | 라운드 종료 일자 (`yyyy-MM-dd`) |

**성적 변환 공식**
| 판타지 성적 | 계산식 | 비고 |
|-----------|--------|------|
| `avg` | `hit / ab` | ab = 0이면 0.0 |
| `hr` | `hr` | |
| `rbi` | `rbi` | |
| `sb` | `sb` | |
| `soBatter` | `so` | 타자 삼진 |
| `era` | `er / inning * 27` | inning은 아웃카운트. inning = 0이면 0.0 |
| `wins` | `win` | |
| `soPitcher` | `pSo` | 투수 삼진 |
| `whip` | `(pHit + bb) / inning * 3` | inning은 아웃카운트. inning = 0이면 0.0 |
| `saves` | `save` | |

**Response** `200` — `List<FantasyScoreDto>` (저장 후 해당 라운드 성적 전체 반환)
```json
[
  {
    "seq": 1,
    "fantasyGameSeq": 10,
    "playerId": 100,
    "round": 1,
    "avg": 0.312,
    "hr": 5,
    "rbi": 20,
    "soBatter": 30,
    "sb": 3,
    "wins": 4,
    "era": 3.45,
    "soPitcher": 40,
    "whip": 1.12,
    "saves": 2,
    "totalPoints": 85.0
  }
]
```

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
트레이드 제안. 성공 시 상태 `SUGGESTED`로 생성되며, 상대방이 수락해야 투표 단계로 진행됨.

**Request Body**
```json
{
  "gameSeq": 1,
  "targetId": 5,
  "givingPlayerSeqs": [10],
  "receivingPlayerSeqs": [20],
  "comment": "트레이드 제안 메시지 (선택)"
}
```

**제약조건**
- 주는 선수: 최소 1명, 최대 2명 (BENCH 아닌 선수도 가능)
- 받는 선수: 최소 1명, 최대 2명 (BENCH 아닌 선수도 가능)
- 신청자가 내보내는 선수들은 `TRADE_PENDING` 상태로 변경됨
- 24시간 내 상대방 미수락 시 자동 거절 (WaiverScheduler)

**Response** `200` `"Trade suggested"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 주는 선수 없음 | 400 | `"Must give at least one player"` |
| 받는 선수 없음 | 400 | `"Must receive at least one player"` |
| 한 쪽에 3명 이상 | 400 | `"Max 2 players per side"` |
| 선수가 NORMAL 상태 아님 | 500 | `"Player {seq} is not in NORMAL status"` |
| 소유하지 않은 선수 포함 | 400 | `"Not all players found in roster for user {id}"` |

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

### POST `/apis/v1/fantasy/roster/games/{gameSeq}/trades/{transactionSeq}/respond` 🔒
트레이드 제안 수락/거절. 트레이드 `target`만 호출 가능.

**Request Body**
```json
{
  "accept": true
}
```

**동작**
- `accept: true` → 상태 `REQUESTED`로 변경, 참가자 투표 단계 진입. FCM 알림 없음
- `accept: false` (상대팀 거절) → 상태 `REJECTED`로 변경, 신청자의 `TRADE_PENDING` 선수들 `NORMAL`로 복원. 신청자에게 개인 FCM 알림 발송
- `accept: false` (신청자 취소) → 상태 `REJECTED`로 변경, 신청자의 `TRADE_PENDING` 선수들 `NORMAL`로 복원. 상대팀에게 개인 FCM 알림 발송

**Response** `200` `"Trade accepted"` 또는 `"Trade rejected"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| target이 아닌 사용자 호출 | 400 | `"Only the trade target can respond"` |
| SUGGESTED 상태가 아님 | 500 | `"Trade is not in SUGGESTED status"` |

---

### GET `/apis/v1/fantasy/roster/games/{gameSeq}/trades`
진행 중인 트레이드 목록 조회. 로그인 시 내 투표/수락 정보 포함.

- `REQUESTED` 상태 트레이드: 모든 참가자에게 노출 (투표 단계)
- `SUGGESTED` 상태 트레이드: 트레이드 당사자(신청자, 상대방)에게만 노출

**Response** `200` — `List<TradeBoardDto>`
```json
[
  {
    "transactionSeq": 1,
    "requesterTeamName": "홍길동팀",
    "targetTeamName": "김철수팀",
    "givingPlayers": [
      { "playerName": "이정후", "teamName": "키움" }
    ],
    "receivingPlayers": [
      { "playerName": "류현진", "teamName": "한화" }
    ],
    "agreeCount": 2,
    "disagreeCount": 1,
    "myVote": null,
    "isParty": false,
    "createdAt": "2025-04-27T10:00:00
  }
]
```

**Response 필드 설명**

| 필드 | 타입 | 설명 |
|------|------|------|
| `transactionSeq` | `Long` | 트레이드 거래 식별자 |
| `status` | `String` | 트레이드 상태. `SUGGESTED`=수락 대기 중, `REQUESTED`=투표 진행 중 |
| `comment` | `String` | 트레이드 제안 메시지 (없으면 `null`) |
| `requesterTeamName` | `String` | 트레이드를 신청한 팀 이름 (`ft_participants.teamName`) |
| `targetTeamName` | `String` | 트레이드 상대 팀 이름 (`ft_participants.teamName`) |
| `givingPlayers` | `List` | 신청 팀이 내보내는 선수 목록 |
| `givingPlayers[].playerName` | `String` | 선수 이름 (`ft_players.name`) |
| `givingPlayers[].teamName` | `String` | 선수 소속 KBO 팀 (`ft_players.team`) |
| `receivingPlayers` | `List` | 신청 팀이 받아오는 선수 목록 (구조 동일) |
| `agreeCount` | `int` | 현재까지 누적된 찬성 투표 수 (`SUGGESTED` 상태에서는 항상 0) |
| `disagreeCount` | `int` | 현재까지 누적된 반대 투표 수 (`SUGGESTED` 상태에서는 항상 0) |
| `myVote` | `Boolean` | 내 투표 여부. `null`=미투표, `true`=찬성, `false`=반대. `isParty=true`면 항상 `null` |
| `isParty` | `boolean` | 내가 이 트레이드의 당사자(신청자 또는 상대방)인지 여부 |
| `canRespond` | `boolean` | 내가 수락/거절 가능한지 여부 (`SUGGESTED` 상태이고 내가 상대방인 경우만 `true`) |
| `createdAt` | `LocalDateTime` | 트레이드 제안 시각 |

**`myVote` / `isParty` 조합 설명**

| `isParty` | `myVote` | 의미 |
|-----------|----------|------|
| `true` | `null` | 트레이드 당사자 — 투표 불가 |
| `false` | `null` | 미투표 — 찬성/반대 가능 |
| `false` | `true` | 찬성 투표함 |
| `false` | `false` | 반대 투표함 |

> 비로그인 조회 시 `myVote: null`, `isParty: false` 고정.

---

### POST `/apis/v1/fantasy/roster/games/{gameSeq}/trades/{transactionSeq}/vote` 🔒
트레이드 찬성/반대 투표.

**제약 조건**
- 트레이드 당사자(requester, target)는 투표 불가
- 이전 투표로부터 1분 이내 재투표 불가
- n명 참가 게임: `ceil((n-2)/2)`개 반대 → 즉시 REJECT, `floor((n-2)/2)+1`개 찬성 → 즉시 APPROVE
- 트레이드 신청 24시간 경과 시 미투표는 찬성으로 간주하여 자동 승인

**Request Body**
```json
{
  "voteAgree": true
}
```

**Response** `200` `"Vote registered"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 트레이드 당사자가 투표 시도 | 400 | `"Trade parties cannot vote"` |
| 1분 이내 재투표 시도 | 500 | `"재투표는 이전 투표로부터 1분 후에 가능합니다"` |
| 이미 처리된 트레이드 | 500 | `"Trade is already processed"` |
| 게임 참여자가 아님 | 400 | `"Not a participant of this game"` |

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
FCM 토픽 구독 등록. 여러 토픽을 한 번에 구독할 수 있다.

**Request Body**
```json
{
  "token": "FCM_DEVICE_TOKEN",
  "topics": ["game_1", "user_42_game_1"]
}
```

**Response** `200` `"Subscribed"` (token 또는 topics이 없으면 처리 없이 200 반환)

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
