# 웨이버 시스템

> 참조 코드: `FantasyRosterService.java`, `WaiverScheduler.java`

---

## 상태 흐름

```
REQUESTED → APPROVED  (타 팀이 클레임)
          → FA_MOVED  (클레임 없음 → FA 이동)
```

---

## 단계별 상세

### 1. 웨이버 신청 (`requestWaiver`)

**트리거**: 사용자가 `POST /apis/v1/fantasy/roster/waiver`

**전제조건**: 해당 선수의 `DraftPick.pickStatus == NORMAL`

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | INSERT — type=WAIVER, status=**REQUESTED** |
| `ft_draft_picks` | pick → `WAIVER_REQ`, `assignedPosition=BENCH` |

**FCM**
| 토픽 | 제목 | 내용 |
|------|------|------|
| `game_{gameSeq}` | 웨이버 신청 | `{teamName}팀에서 웨이버를 신청했습니다: {playerName} ({team}, {position})` |

**RosterLog**: `WAIVER_RELEASE` — `"{playerName} - Waiver Requested"`

---

### 2. 클레임 등록 (`claimWaiver`)

**트리거**: 타 팀 참가자가 `POST /apis/v1/fantasy/roster/waiver/{seq}/claim`

**전제조건**
- 자신이 신청한 웨이버는 클레임 불가
- 해당 게임의 참가자여야 함 (FantasyWaiverOrder 존재)

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_waiver_claims` | INSERT — waiverSeq, claimPlayerId |

**FCM**: 없음  
**RosterLog**: 없음

---

### 3. 스케줄러 처리 (`WaiverScheduler.processWaivers`)

**실행 주기**: 30분마다 (10:00~23:30 KST)  
**대상**: status=REQUESTED이고 createdAt 기준 30분 이상 경과한 웨이버

#### 3-A. 클레임 있음 → CLAIM 처리 (`processWaiver`, CLAIM)

클레임한 팀들 중 `ft_games_waiver.orderNum`이 가장 낮은(우선순위 높은) 팀이 낙찰.

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: REQUESTED → **APPROVED**, targetId=낙찰자 playerId |
| `ft_draft_picks` | pick → `playerId=낙찰자`, `NORMAL`, `assignedPosition=BENCH` |
| `ft_games_waiver` | 낙찰자 orderNum → 현재 최대값+1 (우선순위 최하위로 이동) |

**FCM**: 없음  
**RosterLog**: `WAIVER_CLAIM` — `"{playerName} - Claimed by {targetTeamName}"`

#### 3-B. 클레임 없음 → FA 이동 (`processWaiver`, FA)

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_transactions` | UPDATE status: REQUESTED → **FA_MOVED** |
| `ft_draft_picks` | pick **DELETE** |

**FCM**: 없음  
**RosterLog**: `WAIVER_FA` — `"{playerName} - Moved to FA"`

---

## DraftPick 상태 요약

| 웨이버 상태 | pick 상태 |
|---|---|
| 신청 후 (REQUESTED) | `WAIVER_REQ` |
| 클레임 낙찰 (APPROVED) | `NORMAL` (소유자 교체) |
| FA 이동 (FA_MOVED) | 레코드 **삭제** |

---

## 웨이버 우선순위 (`ft_games_waiver`)

- `orderNum`이 낮을수록 우선순위 높음
- 클레임 낙찰 시 해당 팀의 `orderNum`이 현재 최대값+1로 이동 (최하위)
- 동일 게임 내 여러 웨이버가 같은 스케줄러 사이클에 처리될 경우 orderNum 캐시를 통해 순서 보장

---

## FCM 요약

| 시점 | 토픽 | 발송 여부 |
|------|------|-----------|
| 웨이버 신청 | `game_{gameSeq}` | O |
| 클레임 등록 | — | X |
| 클레임 낙찰 | — | X |
| FA 이동 | — | X |
