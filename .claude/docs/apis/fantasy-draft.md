# 판타지 드래프트 API

---

### GET `/apis/v1/fantasy/games/{gameSeq}/available-players`
드래프트 가능한 선수 목록 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `team` | 선택 | KBO 팀 이름 필터 |
| `position` | 선택 | 포지션 필터 (`SP`, `RP`, `CL`, `C`, `1B`, `2B`, `3B`, `SS`, `OF`, `DH`) |
| `search` | 선택 | 선수 이름 검색 |
| `sort` | 선택 | 정렬 기준 |
| `foreignerType` | 선택 | `TYPE_1`, `TYPE_2`, `NONE` |

**Response** `200` — `List<FantasyPlayerDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `seq` | `Long` | 선수 식별자 (fantasyPlayerSeq) |
| `name` | `String` | 선수명 |
| `position` | `String` | 포지션 |
| `team` | `String` | KBO 소속팀 |
| `stats` | `String` | 성적 요약 |
| `cost` | `Integer` | 샐러리캡 비용 |
| `foreignerType` | `String` | 외국인 선수 타입 (`TYPE_1`, `TYPE_2`, `NONE`) |
| `discounted` | `Boolean` | 할인 적용 여부 |
| `ownerId` | `Long` | 현재 소유 참가자 ID (없으면 `null`) |
| `pickStatus` | `String` | 선수 상태 (`NORMAL`, `WAIVER_REQ`, `TRADE_PENDING`) |

---

### POST `/apis/v1/fantasy/draft` 🔒
선수 드래프트 픽.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `gameSeq` | `Long` | 필수 | 게임 식별자 |
| `fantasyPlayerSeq` | `Long` | 필수 | 드래프트할 선수 식별자 |

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

**Path Parameters**: `gameSeq`

**Response** `200` — `MyPicksResponseDto`

| 필드 | 타입 | 설명 |
|------|------|------|
| `picks` | `List<FantasyPlayerDto>` | 내 로스터 선수 목록 |
| `picks[].seq` | `Long` | 선수 식별자 |
| `picks[].name` | `String` | 선수명 |
| `picks[].position` | `String` | 포지션 |
| `picks[].team` | `String` | KBO 소속팀 |
| `picks[].cost` | `Integer` | 샐러리캡 비용 |
| `picks[].assignedPosition` | `String` | 배치된 포지션 |
| `picks[].pickStatus` | `String` | 선수 상태 (`NORMAL`, `WAIVER_REQ`, `TRADE_PENDING`) |
| `currentCost` | `Integer` | 현재 팀 총 샐러리 비용 |

---

### GET `/apis/v1/fantasy/games/{gameSeq}/players/{playerId}/picks`
특정 참여자의 드래프트 픽 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`, `playerId` (참가자 ID)

**Response** `200` — `MyPicksResponseDto` (필드 구조 위와 동일)

---

### POST `/apis/v1/fantasy/games/{gameSeq}/my-team/save` 🔒
로스터 포지션 배치 저장.

**Path Parameters**: `gameSeq`

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `entries` | `List` | 필수 | 포지션 배치 목록 |
| `entries[].fantasyPlayerSeq` | `Long` | 필수 | 선수 식별자 |
| `entries[].assignedPosition` | `String` | 필수 | 배치할 포지션 |

```json
{
  "entries": [
    { "fantasyPlayerSeq": 42, "assignedPosition": "SP" },
    { "fantasyPlayerSeq": 55, "assignedPosition": "BENCH" }
  ]
}
```

**포지션 값**: `SP`, `RP`, `CL`, `C`, `1B`, `2B`, `3B`, `SS`, `OF`, `DH`, `BENCH`

**포지션 제한**
| 포지션 | 최대 인원 |
|--------|---------|
| `SP` | 4명 |
| `RP` | 4명 |
| 그 외 각 포지션 | 1명 |

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 포지션 한도 초과 | 400 | `"Position limit exceeded for {pos} (Max {limit})"` |
| 게임 종료 상태 | 500 | `"Cannot update roster when FINISHED."` |
| 샐러리캡 초과 | 500 | `"샐러리캡을 초과하여 저장할 수 없습니다. (현재: {n} / 제한: {n})"` |
