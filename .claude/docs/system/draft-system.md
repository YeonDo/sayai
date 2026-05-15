# 드래프트 시스템

> 참조 코드: `FantasyGameService.java`, `FantasyDraftService.java`, `DraftScheduler.java`

---

## 전체 흐름

```
게임 생성(WAITING) → 참가 신청 → 게임 시작(DRAFTING) → 드래프트 진행 → 완료(ONGOING) → FA 영입
```

---

## 1. 게임 생성

Admin이 `FantasyGame` 생성 (status=`WAITING`).

**주요 설정값**
| 필드 | 설명 |
|------|------|
| `ruleType` | 드래프트 규칙 (`RULE_1`, `RULE_2` 등) |
| `scoringType` | 스코어링 방식 (`POINTS`, `ROTISSERIE`) |
| `maxParticipants` | 최대 참가 인원 |
| `draftTimeLimit` | 1픽 제한 시간 (분, 기본값 10분) |
| `useFirstPickRule` | 1라운드 선호팀 우선 픽 규칙 적용 여부 |
| `salaryCap` | 샐러리캡 한도 (null이면 미적용) |
| `useTeamRestriction` | 팀 제한 규칙 적용 여부 |
| `rounds` | 총 라운드 수 |

---

## 2. 참가 신청 (`joinGame`)

- status=`WAITING`인 게임만 참가 가능
- 중복 참가 불가
- `FantasyParticipant` INSERT (draftOrder는 아직 미배정)

---

## 3. 게임 시작 (`startGame`) — Admin only

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_participants` | 참가자 shuffle → `draftOrder` 1~n 배정 |
| `ft_games_waiver` | `FantasyWaiverOrder` 생성 — 드래프트 역순으로 웨이버 우선순위 배정 |
| `ft_games` | status → `DRAFTING`, `nextPickDeadline` = 현재 + draftTimeLimit |

**웨이버 우선순위 배정**
```
드래프트 1순위 → 웨이버 orderNum = n (최하위)
드래프트 2순위 → 웨이버 orderNum = n-1
...
드래프트 n순위 → 웨이버 orderNum = 1 (최우선)
```
> 드래프트에서 먼저 뽑은 팀일수록 웨이버 우선순위가 낮음

**WebSocket 이벤트** (`/topic/draft/{gameSeq}`)
```json
{
  "type": "START",
  "fantasyGameSeq": 1,
  "message": "Draft Started",
  "draftOrder": [...],
  "nextPickerId": 42,
  "nextPickDeadline": "...",
  "round": 1,
  "pickInRound": 1
}
```

**DraftScheduler**: 서버 시작 시 `DRAFTING` 상태 게임을 `activeGameSeqs`에 자동 등록. `startGame` 이후에도 등록.

---

## 4. 드래프트 순서 계산 — 스네이크 드래프트

총 픽 수(totalPicks)와 참가자 수(n)로 현재 차례를 계산:

```java
round = (totalPicks / n) + 1
index = totalPicks % n  // 0 ~ n-1

홀수 라운드: draftOrder = index + 1        // 1→2→3→...→n
짝수 라운드: draftOrder = n - index        // n→n-1→...→1
```

**예시** (n=4)
| 픽 번호 | 라운드 | 순서 |
|---------|--------|------|
| 1~4 | 1라운드 | 1→2→3→4 |
| 5~8 | 2라운드 | 4→3→2→1 |
| 9~12 | 3라운드 | 1→2→3→4 |

---

## 5. 드래프트 픽 (`draftPlayer`)

**검증 순서**
1. 게임 status = `DRAFTING` 또는 `ONGOING`(FA) 확인
2. 내 차례인지 확인 (DRAFTING 시에만)
3. 이미 픽된 선수 여부 확인
4. 로스터 최대 인원 확인 (FA: 최대 21명)
5. 샐러리캡 초과 여부 확인
6. 드래프트 규칙 검증 `DraftRuleValidator` (DRAFTING 시에만)

**DB 변화**
| 테이블 | 변화 |
|--------|------|
| `ft_draft_picks` | INSERT — `pickNumber`, `assignedPosition` 자동 배정 |
| `ft_roster_log` | INSERT — actionType=`DRAFT_PICK` 또는 `FA_ADD` |
| `ft_games` | `nextPickDeadline` 갱신 (현재 + draftTimeLimit) |

**포지션 자동 배정 (`determineInitialPosition`)**

| 선수 유형 | 배정 로직 |
|---------|---------|
| SP | SP 슬롯 < 4개이면 SP, 아니면 BENCH |
| RP | RP 슬롯 < 4개이면 RP, 아니면 BENCH |
| CL | CL 슬롯 < 1개이면 CL, 아니면 BENCH |
| 타자 | 주포지션 비어있으면 → 주포지션, 아니면 멀티 포지션인 경우 부포지션 순서대로 시도, 전부 차있으면 DH, 아니면 BENCH |

> 멀티 포지션 예시: `position = "SS,2B"` → SS 차있으면 2B 시도 → 2B도 차있으면 DH → BENCH

**WebSocket 이벤트** (픽 완료 후)
```json
{
  "type": "PICK",
  "fantasyGameSeq": 1,
  "playerId": 42,
  "fantasyPlayerSeq": 100,
  "playerName": "이정후",
  "playerTeam": "키움",
  "pickNumber": 5,
  "nextPickerId": 33,
  "nextPickDeadline": "...",
  "round": 2,
  "pickInRound": 1
}
```

---

## 6. 드래프트 완료

```java
totalPicks >= participants.size() * totalPlayerCount  →  status = ONGOING
```

- `DraftScheduler.activeGameSeqs`에서 해당 게임 제거
- `nextPickDeadline` = null

**WebSocket 이벤트**
```json
{ "type": "FINISH", "message": "Draft Completed!" }
```

---

## 7. 자동 픽 (`autoPick`) — 시간 초과 시

**DraftScheduler**: 10초마다 만료된 드래프트 게임 확인
```
nextPickDeadline < now → autoPick 실행
```

**autoPick 선수 선택 로직**
1. 아직 픽되지 않은 선수 목록 추출
2. `useFirstPickRule=true` + 1라운드이면 → 선호팀 선수 우선 필터
3. 후보 목록 shuffle
4. 샐러리캡 + 드래프트 규칙 통과하는 첫 번째 선수 선택
5. `draftPlayer` 호출 (autoPick=true로 로그 기록)

---

## 8. FA 영입 (ONGOING 상태)

- `draftPlayer` 동일 로직, 단 차례 검증 없음
- 로스터 최대 21명 제한
- 21번째 선수 추가 시 샐러리캡 페널티 +5 적용
- actionType = `FA_ADD`로 로그 기록
- WebSocket 이벤트 미발송

---

## 드래프트 규칙별 슬롯 구성

### RULE_1 (18픽)

| 포지션 | 슬롯 수 |
|--------|---------|
| C, 1B, 2B, SS, 3B, LF, CF, RF, DH | 각 1 |
| SP | 4 |
| RP | 4 |
| CL | 1 |
| BENCH | 1 |
| **합계** | **19** |

> 슬롯(19) > 픽(18) → CL 슬롯을 비워도 로스터 구성 가능 → **CL 선택적 (0~1명)**

### RULE_2 (20픽)

| 포지션 | 슬롯 수 |
|--------|---------|
| C, 1B, 2B, SS, 3B, LF, CF, RF, DH | 각 1 |
| SP | 4 |
| RP | 4 |
| CL | 1 |
| BENCH | 3 |
| **합계** | **21** |

> 슬롯(21) > 픽(20) → CL 슬롯을 비워도 로스터 구성 가능 → **CL 선택적 (0~1명)**

### CL 제약 공통 규칙

- CL 포지션 선수는 최대 1명까지만 선발 가능 (`clCount > 1` 이면 예외 발생)
- 백트래킹 시 모든 선수(투수 포함)는 BENCH 슬롯에 배치될 수 있음
- 타자는 자신의 포지션 외에도 DH, BENCH에 배치 가능

---

## 포지션 제한 (저장 시)

`updateRoster` (포지션 배치 저장) 시 검증:

| 포지션 | 최대 |
|--------|------|
| SP | 4명 |
| RP | 4명 |
| 그 외 | 1명 |

---

## RosterLog actionType 요약

| actionType | 발생 시점 |
|-----------|---------|
| `DRAFT_PICK` | 드래프트 중 픽 |
| `FA_ADD` | ONGOING 중 FA 영입 |
