# 게임 기록 API

---

### GET `/apis/v1/game/list`
기간별 KBO 게임 목록 조회.

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `start` | 필수 | 시작 날짜 (`yyyy-MM-dd`) |
| `end` | 필수 | 종료 날짜 (`yyyy-MM-dd`) |

**Response** `200` — `List<GameDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `gameDate` | `String` | 경기 날짜 |
| `homeTeam` | `String` | 홈팀 |
| `awayTeam` | `String` | 원정팀 |
| `homeScore` | `Integer` | 홈팀 점수 |
| `awayScore` | `Integer` | 원정팀 점수 |

---

### GET `/apis/v1/game/recent`
마지막으로 기록된 게임 날짜 조회.

**Response** `200` — `"2024-10-05"` (문자열)
