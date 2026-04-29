# 판타지 게임 API

---

### GET `/apis/v1/fantasy/games` 🔒
대시보드용 게임 목록 조회.

**Response** `200` — `List<FantasyGameDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 게임 식별자 |
| `title` | `String` | 게임 제목 |
| `status` | `String` | 게임 상태 (`WAITING`, `DRAFTING`, `FA_SIGNING`, `ONGOING`, `FINISHED`) |
| `ruleType` | `String` | 드래프트 규칙 타입 |
| `scoringType` | `String` | 스코어링 방식 (`POINT`, `ROTISSERIE`) |
| `scoringSettings` | `String` | 스코어링 상세 설정 (JSON 문자열) |
| `maxParticipants` | `Integer` | 최대 참가 인원 |
| `draftDate` | `LocalDateTime` | 드래프트 예정 일시 |
| `draftTimeLimit` | `Integer` | 드래프트 1픽 제한 시간 (초) |
| `gameDuration` | `String` | 게임 기간 |
| `useFirstPickRule` | `Boolean` | 첫 픽 규칙 사용 여부 |
| `salaryCap` | `Integer` | 샐러리캡 한도 (없으면 `null`) |
| `useTeamRestriction` | `Boolean` | 팀 제한 사용 여부 |
| `participantCount` | `Integer` | 현재 참가 인원 |
| `isJoined` | `Boolean` | 내가 참여했는지 여부 |
| `myTeamName` | `String` | 내 팀 이름 (참여하지 않았으면 `null`) |

---

### GET `/apis/v1/fantasy/my-games` 🔒
내가 참여한 게임 목록 조회.

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `status` | 선택 | 게임 상태 필터 (`WAITING`, `DRAFTING`, `FA_SIGNING`, `ONGOING`, `FINISHED`) |
| `type` | 선택 | 게임 타입 필터 |

**Response** `200` — `List<FantasyGameDto>` (필드 구조 위와 동일)

---

### GET `/apis/v1/fantasy/games/{gameSeq}/details`
게임 상세 정보 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Response** `200` — `FantasyGameDetailDto`

| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 게임 식별자 |
| `title` | `String` | 게임 제목 |
| `ruleType` | `String` | 드래프트 규칙 타입 |
| `scoringType` | `String` | 스코어링 방식 |
| `scoringSettings` | `String` | 스코어링 상세 설정 |
| `status` | `String` | 게임 상태 |
| `gameDuration` | `String` | 게임 기간 |
| `draftTimeLimit` | `Integer` | 1픽 제한 시간 (초) |
| `useFirstPickRule` | `Boolean` | 첫 픽 규칙 사용 여부 |
| `salaryCap` | `Integer` | 샐러리캡 한도 |
| `useTeamRestriction` | `Boolean` | 팀 제한 사용 여부 |
| `rounds` | `Integer` | 총 라운드 수 |
| `participantCount` | `Integer` | 현재 참가 인원 |
| `maxParticipants` | `Integer` | 최대 참가 인원 |
| `nextPickerId` | `Long` | 다음 픽 차례 참가자 ID (드래프트 중일 때) |
| `nextPickDeadline` | `ZonedDateTime` | 다음 픽 마감 시각 |
| `roundNum` | `Integer` | 현재 라운드 번호 |
| `pickInRound` | `Integer` | 현재 라운드 내 픽 번호 |
| `participants` | `List<ParticipantRosterDto>` | 참가자 목록 (아래 참조) |

**participants[] 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `participantId` | `Long` | 참가자 ID (playerId) |
| `teamName` | `String` | 팀 이름 |
| `preferredTeam` | `String` | 선호 KBO 팀 |
| `draftOrder` | `Integer` | 드래프트 순서 |
| `roster` | `List<FantasyPlayerDto>` | 현재 로스터 |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/participants`
게임 참여자 목록 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Response** `200` — `List<ParticipantDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `participantId` | `Long` | 참가자 ID |
| `teamName` | `String` | 팀 이름 |
| `draftOrder` | `Integer` | 드래프트 순서 |

---

### POST `/apis/v1/fantasy/games/{gameSeq}/join` 🔒
게임 참가 신청.

**Path Parameters**: `gameSeq`

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `teamName` | `String` | 필수 | 사용할 팀 이름 |

```json
{ "teamName": "홍길동팀" }
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

**Path Parameters**: `gameSeq`

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

**Path Parameters**: `gameSeq`

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `startDt` | 필수 | 집계 시작일 (`yyyy-MM-dd`) |
| `endDt` | 필수 | 집계 종료일 (`yyyy-MM-dd`) |

**Response** `200` — `List<ParticipantKboStatsDto>`

---

### POST `/apis/v1/admin/fantasy/games/{gameSeq}/scores/{round}/upload-from-snapshot` 🔒 (Admin only)
KBO 로스터 스냅샷을 조회하여 해당 선수들의 판타지 라운드 성적을 자동 계산 및 저장.

**Path Parameters**
| 파라미터 | 설명 |
|---------|------|
| `gameSeq` | 판타지 게임 seq |
| `round` | 업데이트할 라운드 번호 |

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `startDt` | 필수 | 라운드 시작일 (`yyyy-MM-dd`) |
| `endDt` | 필수 | 라운드 종료일 (`yyyy-MM-dd`) |

**성적 변환 공식**
| 판타지 성적 | 계산식 | 비고 |
|-----------|--------|------|
| `avg` | `hit / ab` | ab=0이면 0.0 |
| `hr` | `hr` | |
| `rbi` | `rbi` | |
| `sb` | `sb` | |
| `soBatter` | `so` | 타자 삼진 |
| `era` | `er / inning * 27` | inning은 아웃카운트. inning=0이면 0.0 |
| `wins` | `win` | |
| `soPitcher` | `pSo` | 투수 삼진 |
| `whip` | `(pHit + bb) / inning * 3` | inning은 아웃카운트. inning=0이면 0.0 |
| `saves` | `save` | |

**Response** `200` — `List<FantasyScoreDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 성적 식별자 |
| `fantasyGameSeq` | `Long` | 게임 seq |
| `playerId` | `Long` | 참가자 ID |
| `round` | `Integer` | 라운드 번호 |
| `avg` | `Double` | 타율 |
| `hr` | `Integer` | 홈런 |
| `rbi` | `Integer` | 타점 |
| `soBatter` | `Integer` | 타자 삼진 |
| `sb` | `Integer` | 도루 |
| `wins` | `Integer` | 승리 |
| `era` | `Double` | 평균자책점 |
| `soPitcher` | `Integer` | 투수 삼진 |
| `whip` | `Double` | WHIP |
| `saves` | `Integer` | 세이브 |
| `totalPoints` | `Double` | 라운드 총점 |
