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
| `botCount` | `Integer` | 봇 참가자 수 (0이면 미사용) |
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
| `isBot` | `Boolean` | 봇 참가자 여부 |
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

### POST `/apis/v1/admin/games` 🔒 (Admin only)
판타지 게임 생성.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | `String` | 필수 | 게임 제목 |
| `ruleType` | `String` | 필수 | 드래프트 규칙 (`RULE_1`, `RULE_2`) |
| `scoringType` | `String` | 필수 | 스코어링 방식 (`POINT`, `ROTISSERIE`) |
| `scoringSettings` | `String` | 선택 | 스코어링 상세 설정 (JSON 문자열) |
| `maxParticipants` | `Integer` | 필수 | 최대 참가 인원 |
| `draftDate` | `LocalDateTime` | 선택 | 드래프트 예정 일시 |
| `gameDuration` | `String` | 선택 | 게임 기간 |
| `draftTimeLimit` | `Integer` | 선택 | 1픽 제한 시간 (분, 기본값 10) |
| `useFirstPickRule` | `Boolean` | 선택 | 1라운드 선호팀 우선 픽 규칙 적용 여부 |
| `salaryCap` | `Integer` | 선택 | 샐러리캡 한도 (`null`이면 미적용) |
| `useTeamRestriction` | `Boolean` | 선택 | 팀 제한 규칙 적용 여부 |
| `rounds` | `Integer` | 필수 | 총 라운드 수 |
| `botCount` | `Integer` | 선택 | 봇 참가자 수 (0~9, 기본값 0) |

**Response** `200` — `FantasyGame` (생성된 게임 엔티티)

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| `botCount` > 봇 풀 크기 | 500 | `"봇 풀이 부족합니다"` |

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

### GET `/apis/v1/admin/fantasy/games/{gameSeq}/participants` 🔒 (Admin only)
게임 참가자 목록 조회 (Admin 전용, 봇 포함).

**Path Parameters**: `gameSeq`

**Response** `200` — `List<ParticipantDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 참가자 seq |
| `memberId` | `Long` | 회원 ID |
| `userName` | `String` | 회원 이름 |
| `teamName` | `String` | 팀 이름 |
| `preferredTeam` | `String` | 선호 KBO 팀 |
| `isBot` | `Boolean` | 봇 참가자 여부 |

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
| `run` | `run` | 득점 |
| `era` | `er / inning * 27` | inning은 아웃카운트. inning=0이면 0.0 |
| `wins` | `win` | |
| `soPitcher` | `pSo` | 투수 삼진 |
| `whip` | `(pHit + bb) / inning * 3` | inning은 아웃카운트. inning=0이면 0.0 |
| `saves` | `save` | |
| `hold` | `hold` | 홀드 |

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
| `run` | `Integer` | 득점 |
| `wins` | `Integer` | 승리 |
| `era` | `Double` | 평균자책점 |
| `soPitcher` | `Integer` | 투수 삼진 |
| `whip` | `Double` | WHIP |
| `saves` | `Integer` | 세이브 |
| `hold` | `Integer` | 홀드 |
| `totalPoints` | `Double` | 라운드 총점 |

---

### GET `/apis/v1/admin/fantasy/players` 🔒 (Admin only)
`ft_players` 전체 선수 조회 (active/inactive 모두 포함). 어드민 선수 데이터 관리 화면용. **페이징 지원**.

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `page` | 선택 | 페이지 번호 (0부터 시작, 기본값 `0`) |
| `size` | 선택 | 페이지 크기 (기본값 `20`) |

**Response** `200` — `Page<FantasyPlayerDto>`

선수 목록은 `content` 배열에 담기며, Spring `Page` 표준 필드(`totalElements`, `totalPages`, `number`, `size` 등)가 함께 반환된다.

```json
{
  "content": [
    {
      "seq": 100,
      "name": "홍길동",
      "position": "SS",
      "team": "KIA",
      "stats": "{}",
      "cost": 10,
      "foreignerType": "NONE",
      "isActive": 0
    }
  ],
  "totalElements": 512,
  "totalPages": 26,
  "number": 0,
  "size": 20
}
```

**`content[]` (FantasyPlayerDto) 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 선수 seq (`ft_players.seq`) |
| `name` | `String` | 선수 이름 |
| `position` | `String` | 포지션 (예: `SS`, `SP`, 멀티 `SS,2B`) |
| `team` | `String` | KBO 소속 팀 |
| `stats` | `String` | 성적 정보 (JSON 문자열) |
| `cost` | `Integer` | 샐러리캡 비용 (0 이상) |
| `foreignerType` | `String` | 외국인 구분 (`TYPE_1`, `TYPE_2`, `NONE`) |
| `isActive` | `Integer` | 활성 여부 (`0`=활성, 그 외=비활성) |

---

### POST `/apis/v1/admin/fantasy/players` 🔒 (Admin only)
`ft_players` 신규 선수 등록. `seq`는 현재 최대 `seq + 1`로 자동 채번되며, `isActive`는 항상 `0`(활성)으로 등록된다.

**Request Body** (`FantasyPlayerCreateRequest`)
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | `String` | 필수 | 선수 이름 |
| `position` | `String` | 필수 | 포지션 (예: `SS`, `SP`, 멀티 `SS,2B`) |
| `team` | `String` | 필수 | KBO 소속 팀 |
| `cost` | `Integer` | 선택 | 샐러리캡 비용 (기본값 `0`, 음수 불가) |
| `foreignerType` | `String` | 선택 | 외국인 구분 `TYPE_1`, `TYPE_2`, `NONE` (기본값 `NONE`) |

```json
{ "name": "홍길동", "position": "SS", "team": "KIA", "cost": 10, "foreignerType": "NONE" }
```

**Response** `200` — 등록된 `FantasyPlayerDto` (필드 구조는 위 조회 API와 동일)

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| `name`/`position`/`team` 누락 | 400 | `"name, position, team are required"` |
| `cost` 음수 | 400 | `"Cost must be non-negative"` |

---

### PUT `/apis/v1/admin/fantasy/players/{seq}` 🔒 (Admin only)
`ft_players` 선수 정보 수정. **전달한 필드만 반영**되는 부분 업데이트 (`null` 필드는 기존 값 유지).

**Path Parameters**: `seq` — 수정할 선수 seq

**Request Body** (`FantasyPlayerUpdateRequest`, 모든 필드 선택)
| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | `String` | 선수 이름 |
| `position` | `String` | 포지션 |
| `team` | `String` | KBO 소속 팀 |
| `stats` | `String` | 성적 정보 (JSON 문자열) |
| `cost` | `Integer` | 샐러리캡 비용 (0 이상, 음수 불가) |
| `foreignerType` | `String` | 외국인 구분 (`TYPE_1`, `TYPE_2`, `NONE`) |
| `isActive` | `Integer` | 활성 여부 (`0`=활성) |

```json
{ "team": "삼성", "cost": 25, "isActive": 0 }
```

**Response** `200` — 수정된 `FantasyPlayerDto` (필드 구조는 위 조회 API와 동일)

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 존재하지 않는 선수 | 400 | `"Player not found"` |
| `cost` 음수 | 400 | `"Cost must be non-negative"` |
