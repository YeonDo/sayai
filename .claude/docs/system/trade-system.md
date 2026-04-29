# 트레이드 시스템

> 참조 코드: `FantasyRosterService.java`, `WaiverScheduler.java`

---

## 상태 흐름

```
SUGGESTED → REQUESTED → APPROVED
                      ↘ REJECTED
         ↘ REJECTED
```

---

## 단계별 상세

### 1. 트레이드 제안 (`requestTrade`)

**트리거**: 신청자가 `POST /apis/v1/fantasy/roster/trade`

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | INSERT — type=TRADE, status=**SUGGESTED** |
| `ft_draft_picks` | giving picks → `TRADE_PENDING` |

> receiving picks는 이 단계에서 잠그지 않음 (상대가 수락 전이므로)

**FCM**
| 토픽 | 제목 | 내용 |
|------|------|------|
| `user_{targetId}_game_{gameSeq}` | 트레이드 제안 | `{requesterTeam}팀이 트레이드를 제안했습니다. 주는 선수: ... 받는 선수: ...` |

**RosterLog**: 없음

---

### 2-A. 상대팀 수락 (`respondToTrade`, accept=true)

**트리거**: 상대팀이 `POST /apis/v1/fantasy/roster/trade/{seq}/respond` (accept=true)

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: SUGGESTED → **REQUESTED** |
| `ft_draft_picks` | receiving picks → `TRADE_PENDING` |

> REQUESTED 상태에서는 giving/receiving 양쪽 모두 TRADE_PENDING

**FCM**: 없음

**RosterLog**: `TRADE_REQ` — `"Trade Accepted by {targetTeam} - voting started"`

---

### 2-B. 상대팀 거절 (`respondToTrade`, accept=false, isTarget)

**트리거**: 상대팀이 `respond` (accept=false)

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: SUGGESTED → **REJECTED** |
| `ft_draft_picks` | giving picks → `NORMAL` |

**FCM**
| 토픽 | 제목 | 내용 |
|------|------|------|
| `user_{requesterId}_game_{gameSeq}` | 트레이드 거절 | `{targetTeam}팀이 트레이드 제안을 거절했습니다.` |

**RosterLog**: 없음

---

### 2-C. 신청자 취소 (`respondToTrade`, accept=false, isRequester)

**트리거**: 신청자가 `respond` (accept=false)

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: SUGGESTED → **REJECTED** |
| `ft_draft_picks` | giving picks → `NORMAL` |

**FCM**
| 토픽 | 제목 | 내용 |
|------|------|------|
| `user_{targetId}_game_{gameSeq}` | 트레이드 취소 | `{requesterTeam}팀이 트레이드 제안을 취소했습니다.` |

**RosterLog**: 없음

---

### 3. 참가자 투표 (`voteOnTrade`)

**트리거**: 제3자 참가자가 `POST /apis/v1/fantasy/roster/trade/{seq}/vote`

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_trade_board` | INSERT 또는 UPDATE (재투표: 이전 투표 1분 후 가능) |

**FCM**: 없음  
**RosterLog**: 없음

**임계치 계산** (총 참가자 n명, 당사자 2명 제외)
- 승인: `(n-2)/2 + 1` 표 이상 찬성 → `processTrade("APPROVE")`
- 기각: `ceil((n-2)/2)` 표 이상 반대 → `processTrade("REJECT")`

---

### 4-A. 투표 승인 (`processTrade`, APPROVE)

**트리거**: 투표 임계치 도달 또는 WaiverScheduler 24시간 자동 처리

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: REQUESTED → **APPROVED** |
| `ft_draft_picks` | giving picks → `playerId=targetId`, `NORMAL`, `BENCH` |
| `ft_draft_picks` | receiving picks → `playerId=requesterId`, `NORMAL`, `BENCH` |

**FCM**
| 토픽 | 제목 | 내용 |
|------|------|------|
| `game_{gameSeq}` | 트레이드 승인 | `{reqTeam} ↔ {tgtTeam} 트레이드가 승인되었습니다. ({givingNames} ↔ {receivingNames})` |

**RosterLog**: `TRADE_SUCCESS` — `"Trade Approved (Swapped {givingNames} <-> {receivingNames})"`

---

### 4-B. 투표 기각 (`processTrade`, REJECT)

**트리거**: 투표 임계치 도달

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: REQUESTED → **REJECTED** |
| `ft_draft_picks` | giving picks → `NORMAL` |
| `ft_draft_picks` | receiving picks → `NORMAL` |

**FCM**
| 토픽 | 제목 | 내용 |
|------|------|------|
| `game_{gameSeq}` | 트레이드 기각 | `{reqTeam}팀의 트레이드 신청이 기각되었습니다. (주는 선수: {givingNames})` |

**RosterLog**: `TRADE_REJECT` — `"Trade Rejected"`

---

## 스케줄러 자동 처리 (`WaiverScheduler.processTrades`)

30분마다 실행 (10:00~23:30 KST)

| 조건 | 처리 |
|------|------|
| status=SUGGESTED이고 createdAt 기준 24시간 경과 | `respondToTrade(tx.getSeq(), tx.getTargetId(), false)` → 자동 거절 |
| status=REQUESTED이고 updatedAt 기준 24시간 경과 | `processTrade(tx.getSeq(), "APPROVE")` → 미투표 찬성 간주 자동 승인 |

---

## DraftPick 상태 요약

| 트레이드 상태 | giving picks | receiving picks |
|---|---|---|
| SUGGESTED | `TRADE_PENDING` | `NORMAL` |
| REQUESTED | `TRADE_PENDING` | `TRADE_PENDING` |
| APPROVED | `NORMAL` (소유자 교체) | `NORMAL` (소유자 교체) |
| REJECTED | `NORMAL` | `NORMAL` |

---

## FCM 토픽 규칙

| 토픽 형식 | 용도 |
|-----------|------|
| `game_{gameSeq}` | 게임 전체 참가자 |
| `user_{playerId}_game_{gameSeq}` | 특정 참가자 개인 |
