# Fantasy 모듈 규칙

## 게임 상태 흐름

```
WAITING → DRAFTING → ONGOING → FINISHED
```

| 전환 | 트리거 |
|------|--------|
| WAITING → DRAFTING | ADMIN이 `/start` 호출 → `startGame()` |
| DRAFTING → ONGOING | 모든 라운드 픽 완료 |
| ONGOING → FINISHED | 수동 (ADMIN) |

상태 전환 시 `DraftScheduler` 활성화/비활성화:
```java
// 트랜잭션 커밋 후 스케줄러 등록 (TransactionSynchronization 사용)
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() { draftScheduler.addActiveGame(gameSeq); }
});
```

---

## 드래프트 규칙

### 드래프트 순서
- `FantasyParticipant.draftOrder` (1부터 시작)
- 게임 시작 시 참가자 목록을 shuffle → 순서 배정
- 웨이버 우선순위 = 드래프트 역순 (`n - draftOrder + 1`)

### 드래프트 픽
```
POST /apis/v1/fantasy/draft
→ DraftPick 생성
→ WebSocket /topic/draft/{gameSeq} 로 이벤트 브로드캐스트
```

### 외국인 선수 제한 (Rule 2)
`FantasyPlayer.foreignerType`: `TYPE_1` | `TYPE_2` | `NONE`
- Rule2Validator에서 외국인 선수 보유 한도 검증

---

## 웨이버 클레임 흐름

```
사용자 신청 → RosterTransaction(WAIVER, REQUESTED) 생성
           → FantasyWaiverClaim 등록
           → WaiverScheduler 처리
           → APPROVED: 로스터 변경 + FCM 알림
           → REJECTED: 상태만 변경
           → FA_MOVED: 자유계약 처리
```

### 우선순위
`FantasyWaiverOrder.orderNum` 기준 (낮을수록 높은 우선순위)

---

## 스코어링 전략

```java
ScoringStrategy (interface)
├── PointScoringStrategy        // 포인트제: 개인 기록 → 점수
└── RotisserieScoringStrategy   // 로티서리제: 카테고리 랭킹 → 포인트
```

로티서리 카테고리:
- 타자: avg, hr, rbi, so, sb
- 투수: wins, era, whip, saves

`FantasyRotisserieScore` 테이블에 카테고리별 점수 저장.

---

## 실시간 WebSocket

```
엔드포인트: /ws  (SockJS 폴백)
구독 접두사: /topic
앱 접두사:   /app
```

드래프트 이벤트 타입:
- `START` → 드래프트 시작, 순서 공지
- `PICK` → 픽 완료, 다음 순서 공지
- `TIMEOUT` → 제한 시간 초과, 자동 처리

이벤트 전송:
```java
messagingTemplate.convertAndSend("/topic/draft/" + gameSeq, event);
```

---

## FantasyPlayer (ft_players) 역할

`ft_players` 테이블은 **선수 마스터** 역할.

| 컬럼 | 용도 |
|------|------|
| `seq` | PK, `app_users.playerId`와 연결 |
| `position` | SP/RP/CL (투수) 또는 타자 포지션 |
| `team` | KBO 팀명 |
| `cost` | 드래프트 비용 (salaryCap 모드) |
| `foreignerType` | TYPE_1/TYPE_2/NONE |
| `isActive` | 0=비활성, 1=활성 |

투수 판별:
```java
boolean isPitcher = pos.contains("SP") || pos.contains("RP") || pos.contains("CL");
```

타자 필터 (KBO 쿼리):
```sql
AND p.position NOT IN ('SP', 'RP', 'CL')
```

---

## 가용 선수 조회 (Available Players)

IF/OF 포지션 필터링이 있다.  
`/apis/v1/fantasy/players?position=IF` → 내야수만 조회  
`/apis/v1/fantasy/players?position=OF` → 외야수만 조회

---

## RosterTransaction 타입/상태

```
타입: WAIVER | TRADE
상태: REQUESTED → APPROVED | REJECTED | FA_MOVED
```

트레이드는 `RosterTransaction(TRADE)` 로 시작,  
상대방 수락 시 APPROVED, 거절 시 REJECTED.
