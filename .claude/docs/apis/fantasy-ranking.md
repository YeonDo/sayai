# 판타지 순위/로그 API

---

### GET `/apis/v1/fantasy/games/{gameSeq}/ranking`
게임 순위표 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Response** `200` — `RankingTableDto`

| 필드 | 타입 | 설명 |
|------|------|------|
| `gameSeq` | `Long` | 게임 식별자 |
| `scoringType` | `String` | 스코어링 방식 (`POINT`, `ROTISSERIE`) |
| `rankings` | `List<ParticipantStatsDto>` | 순위별 참가자 통계 목록 |

**rankings[] 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `participantId` | `Long` | 참가자 ID |
| `teamName` | `String` | 팀 이름 |
| `ownerName` | `String` | 팀 소유자 이름 |
| `battingAvg` | `Double` | 타율 |
| `homeruns` | `Long` | 홈런 |
| `rbi` | `Integer` | 타점 |
| `batterStrikeOuts` | `Long` | 타자 삼진 |
| `stolenBases` | `Integer` | 도루 |
| `wins` | `Long` | 승리 |
| `pitcherStrikeOuts` | `Long` | 투수 삼진 |
| `era` | `Double` | 평균자책점 |
| `whip` | `Double` | WHIP |
| `saves` | `Long` | 세이브 |
| `totalPoints` | `Double` | 총점 |
| `rounds` | `List<FantasyScoreDto>` | 라운드별 성적 목록 |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/picks`
게임 전체 드래프트 픽 로그. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Response** `200` — `List<DraftLogDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `pickNumber` | `Integer` | 전체 픽 번호 |
| `playerName` | `String` | 드래프트된 선수 이름 |
| `playerTeam` | `String` | 선수 KBO 소속팀 |
| `playerPosition` | `String` | 선수 포지션 |
| `pickedByTeamName` | `String` | 픽한 팀 이름 |
| `pickedAt` | `LocalDateTime` | 픽 일시 |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/logs`
로스터 변경 로그 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `type` | 선택 | `DRAFT` 전달 시 드래프트 픽만 표시. 미전달 시 드래프트 픽 제외 |

**Response** `200` — `List<RosterLogDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 로그 식별자 |
| `playerName` | `String` | 선수 이름 |
| `playerTeam` | `String` | 선수 KBO 소속팀 |
| `playerPosition` | `String` | 선수 포지션 |
| `participantName` | `String` | 관련 참가자 팀 이름 |
| `actionType` | `String` | 액션 타입 (아래 참조) |
| `details` | `String` | 상세 설명 |
| `timestamp` | `LocalDateTime` | 발생 일시 |

**actionType 값**
| 값 | 설명 |
|----|------|
| `DRAFT_PICK` | 드래프트 픽 |
| `FA_ADD` | FA 영입 |
| `WAIVER_RELEASE` | 웨이버 방출 신청 |
| `WAIVER_FA` | 웨이버 → FA 이동 (클레임 없음) |
| `WAIVER_CLAIM` | 웨이버 클레임 낙찰 |
| `TRADE_REQ` | 트레이드 수락 → 투표 시작 |
| `TRADE_SUCCESS` | 트레이드 성사 |
| `TRADE_REJECT` | 트레이드 투표 기각 |

---

### GET `/apis/v1/fantasy/players/pick-owner`
선수 이름으로 현재 소유 팀 조회. **인증 불필요**.

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `gameSeq` | 필수 | 게임 식별자 |
| `name` | 필수 | 선수 이름 |

**Response** `200`

| 필드 | 타입 | 설명 |
|------|------|------|
| `teamName` | `String` | 소유 팀 이름. 소유자 없으면 빈 문자열 `""` |
