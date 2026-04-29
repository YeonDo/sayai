# 판타지 로스터 API

> 웨이버/트레이드 흐름 상세는 `.claude/docs/system/waiver-system.md`, `trade-system.md` 참조

---

## 웨이버

### POST `/apis/v1/fantasy/roster/waiver` 🔒
웨이버 신청 (보유 선수 방출).

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `gameSeq` | `Long` | 필수 | 게임 식별자 |
| `releasePlayerSeq` | `Long` | 필수 | 방출할 선수의 fantasyPlayerSeq |
| `targetPlayerSeq` | `Long` | 선택 | 영입할 FA 선수의 fantasyPlayerSeq |

```json
{
  "gameSeq": 1,
  "releasePlayerSeq": 10,
  "targetPlayerSeq": 20
}
```

**동작**: 방출 선수 `DraftPick.pickStatus` → `WAIVER_REQ`, 트랜잭션 생성(status=`REQUESTED`). 30분 후 스케줄러가 클레임 처리.

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 본인 선수 클레임 | 400 | `"Cannot claim your own waived player"` |
| 소유하지 않은 선수 | 400 | `"Player does not belong to you"` |
| 이미 처리된 거래 | 500 | `"Transaction is already processed"` |
| 선수가 NORMAL 상태 아님 | 500 | `"Player is already in {status} state"` |

---

### GET `/apis/v1/fantasy/roster/games/{gameSeq}/waivers`
웨이버 보드 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Response** `200` — `List<WaiverBoardDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `transactionSeq` | `Long` | 트랜잭션 식별자 |
| `requesterId` | `Long` | 방출 신청자 ID |
| `requesterTeamName` | `String` | 방출 신청 팀 이름 |
| `playerName` | `String` | 방출 선수 이름 |
| `playerTeam` | `String` | 방출 선수 KBO 소속팀 |
| `playerPosition` | `String` | 방출 선수 포지션 |
| `playerCost` | `Integer` | 방출 선수 샐러리캡 비용 |
| `waiverDate` | `LocalDateTime` | 웨이버 신청 일시 |
| `claimPlayerIds` | `List<Long>` | 클레임 신청한 참가자 ID 목록 |

---

### GET `/apis/v1/fantasy/roster/games/{gameSeq}/waiver-orders`
웨이버 우선순위 조회. **인증 불필요**.

**Path Parameters**: `gameSeq`

**Response** `200` — `List<WaiverOrderDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `teamName` | `String` | 팀 이름 |
| `orderNum` | `Integer` | 웨이버 우선순위 번호 (낮을수록 높은 우선순위) |

---

### POST `/apis/v1/fantasy/roster/games/{gameSeq}/waivers/{transactionSeq}/claim` 🔒
웨이버 클레임 (방출된 선수 획득 신청).

**Path Parameters**: `gameSeq`, `transactionSeq`

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 유효하지 않은 웨이버 거래 | 400 | `"Invalid waiver transaction"` |
| Waiver 거래가 아님 | 400 | `"Not a Waiver transaction"` |

---

## 트레이드

### POST `/apis/v1/fantasy/roster/trade` 🔒
트레이드 제안.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `gameSeq` | `Long` | 필수 | 게임 식별자 |
| `targetId` | `Long` | 필수 | 상대 참가자 ID |
| `givingPlayerSeqs` | `List<Long>` | 필수 | 내가 주는 선수 fantasyPlayerSeq 목록 (최대 2명) |
| `receivingPlayerSeqs` | `List<Long>` | 필수 | 내가 받는 선수 fantasyPlayerSeq 목록 (최대 2명) |
| `comment` | `String` | 선택 | 트레이드 제안 메시지 |

```json
{
  "gameSeq": 1,
  "targetId": 5,
  "givingPlayerSeqs": [10],
  "receivingPlayerSeqs": [20],
  "comment": "트레이드 제안합니다"
}
```

**동작**: 생성 시 status=`SUGGESTED`. 신청자의 giving 선수 → `TRADE_PENDING`. 24시간 내 미수락 시 자동 거절(WaiverScheduler).

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

### POST `/apis/v1/fantasy/roster/games/{gameSeq}/trades/{transactionSeq}/respond` 🔒
트레이드 수락/거절 또는 신청자 취소.

**Path Parameters**: `gameSeq`, `transactionSeq`

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `accept` | `Boolean` | 필수 | 수락 여부 |

```json
{ "accept": true }
```

**동작**
| 호출자 | accept | 결과 | FCM |
|--------|--------|------|-----|
| 상대팀(target) | `true` | status → `REQUESTED`, receiving 선수 `TRADE_PENDING`, 투표 시작 | 없음 |
| 상대팀(target) | `false` | status → `REJECTED`, giving 선수 `NORMAL` 복원 | 신청자에게 개인 FCM |
| 신청자(requester) | `false` | status → `REJECTED`, giving 선수 `NORMAL` 복원 | 상대팀에게 개인 FCM |

**Response** `200` `"Trade accepted"` 또는 `"Trade rejected"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 트레이드 당사자가 아님 | 400 | `"Only trade parties can respond"` |
| SUGGESTED 상태가 아님 | 500 | `"Trade is not in SUGGESTED status"` |
| 신청자가 수락 시도 | 400 | `"Requester cannot accept their own trade"` |

---

### GET `/apis/v1/fantasy/roster/games/{gameSeq}/trades`
진행 중인 트레이드 목록 조회.

**Path Parameters**: `gameSeq`

- `REQUESTED` 상태: 모든 참가자에게 노출 (투표 단계)
- `SUGGESTED` 상태: 트레이드 당사자(신청자/상대방)에게만 노출

**Response** `200` — `List<TradeBoardDto>`

| 필드 | 타입 | 설명 |
|------|------|------|
| `transactionSeq` | `Long` | 트레이드 트랜잭션 식별자 |
| `status` | `String` | `SUGGESTED`=수락 대기, `REQUESTED`=투표 진행 중 |
| `comment` | `String` | 제안 메시지 (`null` 가능) |
| `requesterTeamName` | `String` | 신청 팀 이름 |
| `targetTeamName` | `String` | 상대 팀 이름 |
| `givingPlayers` | `List` | 신청 팀이 내보내는 선수 목록 |
| `givingPlayers[].playerName` | `String` | 선수 이름 (`ft_players.name`) |
| `givingPlayers[].teamName` | `String` | 선수 KBO 소속팀 (`ft_players.team`) |
| `receivingPlayers` | `List` | 신청 팀이 받아오는 선수 목록 (구조 동일) |
| `agreeCount` | `Integer` | 찬성 투표 수 (`SUGGESTED` 상태에서는 0) |
| `disagreeCount` | `Integer` | 반대 투표 수 (`SUGGESTED` 상태에서는 0) |
| `myVote` | `Boolean` | 내 투표 (`null`=미투표, `true`=찬성, `false`=반대). `isParty=true`면 항상 `null` |
| `isParty` | `Boolean` | 내가 트레이드 당사자인지 여부 |
| `canRespond` | `Boolean` | 내가 수락/거절 가능 여부 (`SUGGESTED` 상태이고 내가 상대팀인 경우만 `true`) |
| `createdAt` | `LocalDateTime` | 트레이드 제안 시각 |

**myVote / isParty 조합**
| `isParty` | `myVote` | 의미 |
|-----------|----------|------|
| `true` | `null` | 당사자 — 투표 불가 |
| `false` | `null` | 미투표 — 찬성/반대 가능 |
| `false` | `true` | 찬성 투표함 |
| `false` | `false` | 반대 투표함 |

> 비로그인 조회 시 `myVote: null`, `isParty: false` 고정.

---

### POST `/apis/v1/fantasy/roster/games/{gameSeq}/trades/{transactionSeq}/vote` 🔒
트레이드 찬성/반대 투표.

**Path Parameters**: `gameSeq`, `transactionSeq`

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `voteAgree` | `Boolean` | 필수 | `true`=찬성, `false`=반대 |

```json
{ "voteAgree": true }
```

**투표 임계치** (참가자 n명, 당사자 2명 제외)
| 결과 | 조건 |
|------|------|
| 즉시 승인 | 찬성 `floor((n-2)/2) + 1`개 이상 |
| 즉시 기각 | 반대 `ceil((n-2)/2)`개 이상 |
| 24시간 경과 시 | 미투표를 찬성으로 간주하여 자동 승인 |

**Response** `200` `"Vote registered"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 트레이드 당사자가 투표 시도 | 400 | `"Trade parties cannot vote"` |
| 1분 이내 재투표 | 500 | `"재투표는 이전 투표로부터 1분 후에 가능합니다"` |
| 이미 처리된 트레이드 | 500 | `"Trade is already processed"` |
| 게임 참여자가 아님 | 400 | `"Not a participant of this game"` |
