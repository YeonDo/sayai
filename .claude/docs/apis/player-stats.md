# 선수 통계 API

> KBO 선수 통계: `/apis/v1/kboplayer`  
> CORS: `*` (모든 origin 허용)

---

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

**Response 주요 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | `List` | 타자 목록 |
| `content[].id` | `Long` | 선수 ID |
| `content[].name` | `String` | 선수명 |
| `content[].team` | `String` | 소속팀 (`ft_players` 기준, 미등록 시 `null`) |
| `content[].battingAvg` | `Double` | 타율 |
| `content[].homeruns` | `Integer` | 홈런 |
| `content[].rbi` | `Integer` | 타점 |
| `content[].sb` | `Integer` | 도루 |
| `content[].strikeOut` | `Integer` | 삼진 |
| `totalElements` | `Long` | 전체 선수 수 |
| `totalPages` | `Integer` | 전체 페이지 수 |
| `number` | `Integer` | 현재 페이지 번호 |

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| season/start+end 미전달 | 400 | `"season 또는 start+end 파라미터가 필요합니다."` |

---

### GET `/apis/v1/kboplayer/pitcher/all`
전체 투수 목록 조회 (페이징). 파라미터 구조는 `/hitter/all`과 동일.

**Response 주요 필드** (`content[]`)
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 선수 ID |
| `name` | `String` | 선수명 |
| `team` | `String` | 소속팀 (`ft_players` 기준, 미등록 시 `null`) |
| `era` | `Double` | 평균자책점 |
| `wins` | `Integer` | 승리 |
| `saves` | `Integer` | 세이브 |
| `whip` | `Double` | WHIP |
| `stOut` | `Integer` | 삼진 |

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

**summary 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 선수 ID |
| `name` | `String` | 선수명 |
| `team` | `String` | 소속팀 (`ft_players` 기준, 미등록 시 `null`) |
| `totalGames` | `Integer` | 총 경기 수 |
| `battingAvg` | `Double` | 타율 |
| `playerAppearance` | `Integer` | 타석 수 (PA) |
| `atBat` | `Integer` | 타수 (AB) |
| `totalHits` | `Integer` | 안타 |
| `homeruns` | `Integer` | 홈런 |
| `rbi` | `Integer` | 타점 |
| `runs` | `Integer` | 득점 |
| `sb` | `Integer` | 도루 |
| `strikeOut` | `Integer` | 삼진 |

**dailyStats.content 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `gameDate` | `String` | 경기 날짜 (`yyyyMMdd`) |
| `opponent` | `String` | 상대 팀 |
| `pa` | `Integer` | 타석 |
| `ab` | `Integer` | 타수 |
| `hit` | `Integer` | 안타 |
| `hr` | `Integer` | 홈런 |
| `rbi` | `Integer` | 타점 |
| `run` | `Integer` | 득점 |
| `sb` | `Integer` | 도루 |
| `so` | `Integer` | 삼진 |
| `battingAvg` | `Double` | 해당 경기 타율 |

> `dailyStats.content`는 최근 날짜 순(내림차순) 정렬.

---

### GET `/apis/v1/kboplayer/pitcher/{playerId}`
특정 투수 기간 합산 성적 + 일자별 성적 목록 조회.

**Path Parameters**: `playerId`

**Query Parameters**: `start` (필수), `end` (필수), `page` (선택), `size` (선택)

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

**summary 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `innings` | `Double` | 이닝 (`이닝수 + 나머지아웃수*0.1`, 예: 6이닝 1/3 → `6.1`) |
| `wins` | `Integer` | 승리 |
| `loses` | `Integer` | 패배 |
| `saves` | `Integer` | 세이브 |
| `era` | `Double` | 평균자책점 |
| `whip` | `Double` | WHIP |
| `stOut` | `Integer` | 삼진 |
| `baseOnBall` | `Integer` | 볼넷 |
| `pHit` | `Integer` | 피안타 |
| `selfLossScore` | `Integer` | 자책점 |

**dailyStats.content 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `gameDate` | `String` | 경기 날짜 (`yyyyMMdd`) |
| `opponent` | `String` | 상대 팀 |
| `innings` | `Double` | 이닝 |
| `win` / `lose` / `save` | `Integer` | 승/패/세이브 |
| `er` | `Integer` | 자책점 |
| `bb` | `Integer` | 볼넷 |
| `hbp` | `Integer` | 사구 |
| `pHit` | `Integer` | 피안타 |
| `so` | `Integer` | 삼진 |
| `era` | `Double` | 해당 등판 단독 ERA |

> `dailyStats.content`는 최근 날짜 순(내림차순) 정렬.

---

### GET `/apis/v1/kboplayer/hitter/{playerId}/period`
타자 기간별 성적 조회.

**Query Parameters**
| 파라미터 | 설명 |
|---------|------|
| `list` | 복수 전달 가능. `"total"`, `"2024"`, `"202404"` 형식 |

**Response** `200` — `List<PlayerDto>`
