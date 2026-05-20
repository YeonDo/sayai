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

---

### GET `/apis/v1/fantasy/games/{gameSeq}/kbo-stats`
게임 전체 참가자의 KBO 성적 집계 조회 (팀 단위). **인증 불필요**.

**Path Parameters**: `gameSeq`

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `startDt` | 필수 | 조회 시작일 (`yyyy-MM-dd`) |
| `endDt` | 필수 | 조회 종료일 (`yyyy-MM-dd`) |

**Response** `200` — `List<ParticipantKboStatsDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `participantSeq` | `Long` | 참가자 식별자 |
| `playerId` | `Long` | 참가자 유저 ID |
| `teamName` | `String` | 판타지 팀 이름 |
| `pa` | `long` | 타석 |
| `ab` | `long` | 타수 |
| `hit` | `long` | 안타 |
| `rbi` | `long` | 타점 |
| `run` | `long` | 득점 |
| `sb` | `long` | 도루 |
| `so` | `long` | 삼진 (타자) |
| `hr` | `long` | 홈런 |
| `win` | `long` | 승 |
| `lose` | `long` | 패 |
| `save` | `long` | 세이브 |
| `inning` | `long` | 이닝 (아웃카운트 단위) |
| `formattedInning` | `String` | 이닝 표시 (예: `"7 2/3"`) |
| `batter` | `long` | 상대 타자 수 |
| `pitchCnt` | `long` | 투구 수 |
| `pHit` | `long` | 피안타 |
| `bb` | `long` | 볼넷 |
| `pSo` | `long` | 탈삼진 |
| `er` | `long` | 자책점 |
| `hbp` | `long` | 사구 |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/my-roster/stats` 🔒
내 로스터 선수별 KBO 성적 조회. 벤치 포함 전체 집계 포함.

**Path Parameters**: `gameSeq`

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `startDt` | 필수 | 조회 시작일 (`yyyy-MM-dd`) |
| `endDt` | 필수 | 조회 종료일 (`yyyy-MM-dd`) |

**Response** `200` — `MyRosterStatDto`

| 필드 | 타입 | 설명 |
|------|------|------|
| `hitters` | `List<HitterStat>` | 타자 목록 (벤치 포함) |
| `pitchers` | `List<PitcherStat>` | 투수 목록 (벤치 포함) |
| `hitterTotal` | `HitterStat` | 전체 타자 집계 (`fantasyPlayerSeq`, `playerName`, `kboTeam`, `assignedPosition` 는 `null`) |
| `pitcherTotal` | `PitcherStat` | 전체 투수 집계 (`fantasyPlayerSeq`, `playerName`, `kboTeam`, `assignedPosition` 는 `null`) |

**HitterStat 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `fantasyPlayerSeq` | `Long` | ft_players 식별자 |
| `playerName` | `String` | 선수 이름 |
| `kboTeam` | `String` | KBO 소속팀 |
| `assignedPosition` | `String` | 배치 포지션 (예: `1B`, `BENCH`) |
| `pa` | `long` | 타석 |
| `ab` | `long` | 타수 |
| `hit` | `long` | 안타 |
| `hr` | `long` | 홈런 |
| `rbi` | `long` | 타점 |
| `run` | `long` | 득점 |
| `sb` | `long` | 도루 |
| `so` | `long` | 삼진 |
| `avg` | `String` | 타율 (예: `".318"`, 타수 0이면 `".000"`) |

**PitcherStat 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `fantasyPlayerSeq` | `Long` | ft_players 식별자 |
| `playerName` | `String` | 선수 이름 |
| `kboTeam` | `String` | KBO 소속팀 |
| `assignedPosition` | `String` | 배치 포지션 (예: `SP`, `BENCH`) |
| `win` | `long` | 승 |
| `lose` | `long` | 패 |
| `save` | `long` | 세이브 |
| `inning` | `long` | 이닝 (아웃카운트 단위) |
| `formattedInning` | `String` | 이닝 표시 (예: `"7 2/3"`) |
| `er` | `long` | 자책점 |
| `bb` | `long` | 볼넷 |
| `pHit` | `long` | 피안타 |
| `pSo` | `long` | 탈삼진 |
| `hbp` | `long` | 사구 |
| `era` | `String` | 평균자책점 (예: `"3.24"`, 이닝 0이면 `"-.--"`) |
| `whip` | `String` | WHIP (예: `"1.12"`, 이닝 0이면 `"-.--"`) |

**Error**
| 코드 | 설명 |
|------|------|
| `401` | 미인증 |
