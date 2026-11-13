# 포인트 / 베팅 API

> 공통 사항(Base URL, 인증 쿠키, 에러 형식)은 `common.md` 참조.
> 🔒 = `accessToken` 쿠키 필요(USER 이상). 🔑 = ADMIN 권한 필요.

---

## 개요

- 멤버는 **포인트**를 보유하고, 어드민이 만든 **베팅 항목**에 포인트를 건다.
- 베팅 방식은 **패리뮤추얼**: 적중자들이 전체 풀을 베팅액 비율로 나눠 가진다. 배당률은 베팅이 들어올 때마다 변하므로 화면에는 **"예상 배당"** 으로 표기할 것.
- 모든 시각은 **UTC ISO-8601** 문자열(`2026-10-20T12:00:00Z`)로 주고받는다. 표시할 때 KST로 변환.

---

## 공통 타입

### BtGameType
| 값 | 설명 |
|----|------|
| `CHOICE` | 객관식. `choices` 배열에서 하나를 고름. `answer`는 문항 **index**(0부터) |
| `NUMBER` | 주관식(숫자). `answer`는 숫자 문자열. 정답에 **가까울수록** 많이 받는 가중 분배 ([정산 규칙](#정산-규칙)) |

### BtGameStatus
| 값 | 설명 |
|----|------|
| `OPEN` | 진행 중. `deadline` 전이면 베팅 가능 |
| `CLOSED` | 정답 입력 + 정산 완료 |
| `CANCELED` | 무효 처리. 전액 환불됨 |

### BtGame 객체
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 항목 ID |
| `title` | `String` | 제목 |
| `content` | `String` | 설명 (nullable) |
| `type` | `BtGameType` | 문제 타입 |
| `deadline` | `String` | 마감 시각 (UTC ISO-8601) |
| `status` | `BtGameStatus` | 상태 |
| `answer` | `String` | 정답. `CLOSED` 전에는 `null` |
| `bettable` | `boolean` | 지금 베팅 가능한지 (`status == OPEN && now < deadline`) |
| `totalPool` | `Long` | 이 항목에 걸린 전체 포인트 |
| `finalOdds` | `Double` | **CHOICE의 CLOSED만**. 최종 배당 = `totalPool / 적중 풀` (소수 2자리). 전액 환불이면 `1.0`. OPEN/CANCELED/베팅 없음/NUMBER면 `null` |
| `choices` | `Choice[]` | 객관식만. 주관식은 `null` |

### Choice 객체
| 필드 | 타입 | 설명 |
|------|------|------|
| `index` | `int` | 문항 index. 베팅 시 `answer`로 보내는 값 |
| `label` | `String` | 문항 텍스트 |
| `pool` | `Long` | 이 문항에 걸린 포인트 |
| `odds` | `Double` | 예상 배당 = `totalPool / pool` (소수 2자리). 아직 아무도 안 걸었으면 `null` |

```json
{
  "id": 3,
  "title": "10/20 LG vs 두산 승자",
  "content": "잠실 19:00 경기",
  "type": "CHOICE",
  "deadline": "2026-10-20T09:30:00Z",
  "status": "OPEN",
  "answer": null,
  "bettable": true,
  "totalPool": 1500,
  "finalOdds": null,
  "choices": [
    { "index": 0, "label": "LG", "pool": 1000, "odds": 1.5 },
    { "index": 1, "label": "두산", "pool": 500, "odds": 3.0 },
    { "index": 2, "label": "무승부", "pool": 0, "odds": null }
  ]
}
```

### MyBet 객체 (내 베팅 내역)
| 필드 | 타입 | 설명 |
|------|------|------|
| `logId` | `Long` | 베팅 ID |
| `gameId` | `Long` | 항목 ID |
| `title` | `String` | 항목 제목 |
| `type` | `BtGameType` | 문제 타입 |
| `status` | `BtGameStatus` | 항목 상태 |
| `deadline` | `String` | 마감 시각 (UTC) |
| `correctAnswer` | `String` | 정답. `CLOSED` 전에는 `null` |
| `answer` | `String` | 내가 건 값 (객관식 index / 주관식 숫자) |
| `answerLabel` | `String` | 객관식이면 문항 텍스트, 주관식이면 `answer`와 동일 |
| `amount` | `Long` | 베팅 포인트 |
| `payout` | `Long` | 정산 결과. 정산 전 `null`, 낙첨 `0`, 적중/환불이면 지급액 |
| `createdAt` | `String` | 베팅 시각 (UTC) |

### Ranking 객체
| 필드 | 타입 | 설명 |
|------|------|------|
| `rank` | `int` | 순위. 동점은 같은 순위(1, 1, 3) |
| `memberId` | `Long` | 멤버 ID |
| `name` | `String` | 멤버 이름 |
| `balance` | `Long` | 현재 포인트 |

### BtLog 객체 (어드민 조회용)
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 베팅 ID |
| `memberId` | `Long` | 멤버 ID |
| `memberName` | `String` | 멤버 이름 |
| `answer` | `String` | 선택값 (객관식 index / 주관식 숫자) |
| `amount` | `Long` | 베팅 포인트 |
| `payout` | `Long` | 정산 결과. 정산 전 `null`, 낙첨 `0`, 적중/환불이면 지급액 |
| `createdAt` | `String` | 베팅 시각 (UTC) |

---

## 멤버 API

### GET `/apis/v1/point/me` 🔒
내 현재 포인트.

**Response** `200`
```json
{ "memberId": 42, "balance": 2500 }
```
> 포인트 레코드가 없는 신규 멤버는 `balance: 0`.

---

### GET `/apis/v1/point/ranking` 🔒
포인트 잔액 순 리더보드.

**Query**
| 이름 | 기본값 | 설명 |
|------|--------|------|
| `limit` | `50` | 최대 인원 (1~200) |

**Response** `200` `Ranking[]`
```json
[
  { "rank": 1, "memberId": 42, "name": "홍길동", "balance": 5200 },
  { "rank": 1, "memberId": 7,  "name": "김철수", "balance": 5200 },
  { "rank": 3, "memberId": 13, "name": "이영희", "balance": 3100 }
]
```
> 포인트 레코드가 없는 멤버(한 번도 지급/베팅 안 함)는 목록에 없다.

---

### GET `/apis/v1/bet/games` 🔒
베팅 항목 목록. 상태별 조회.

**Query**
| 이름 | 기본값 | 설명 |
|------|--------|------|
| `status` | `OPEN` | `OPEN` / `CLOSED` / `CANCELED` |

| status | 정렬 | 비고 |
|--------|------|------|
| `OPEN` | 마감 임박순 | 마감은 지났지만 정답 미입력 항목도 포함. `bettable: false`면 "결과 대기" 표시 |
| `CLOSED` | 최신순 | `answer`(정답)와 `finalOdds`(최종 배당) 채워짐. 객관식은 `choices[].pool`로 문항별 최종 풀 확인 가능 |
| `CANCELED` | 최신순 | 무효 처리된 항목. 전액 환불됨 |

**Response** `200` `BtGame[]`

```json
[
  {
    "id": 2,
    "title": "10/18 KIA vs 삼성 승자",
    "content": null,
    "type": "CHOICE",
    "deadline": "2026-10-18T09:30:00Z",
    "status": "CLOSED",
    "answer": "1",
    "bettable": false,
    "totalPool": 900,
    "finalOdds": 3.0,
    "choices": [
      { "index": 0, "label": "KIA", "pool": 600, "odds": 1.5 },
      { "index": 1, "label": "삼성", "pool": 300, "odds": 3.0 }
    ]
  }
]
```

---

### GET `/apis/v1/bet/my` 🔒
내 베팅 내역. 최신순. 항목 제목·상태·정답·정산 결과 포함.

**Response** `200` `MyBet[]`
```json
[
  {
    "logId": 17, "gameId": 3, "title": "10/20 LG vs 두산 승자",
    "type": "CHOICE", "status": "OPEN", "deadline": "2026-10-20T09:30:00Z",
    "correctAnswer": null, "answer": "1", "answerLabel": "두산",
    "amount": 300, "payout": null, "createdAt": "2026-10-19T03:12:45Z"
  },
  {
    "logId": 9, "gameId": 2, "title": "10/18 KIA vs 삼성 승자",
    "type": "CHOICE", "status": "CLOSED", "deadline": "2026-10-18T09:30:00Z",
    "correctAnswer": "1", "answer": "1", "answerLabel": "삼성",
    "amount": 100, "payout": 300, "createdAt": "2026-10-17T11:00:00Z"
  }
]
```

> 적중 여부는 `status == CLOSED && payout > 0`. `CANCELED`면 `payout == amount`(환불).

---

### POST `/apis/v1/bet/games/{gameId}` 🔒
베팅. 성공 시 즉시 포인트가 차감된다. 같은 항목에 여러 번(다른 문항 포함) 베팅 가능.

**Path**
| 이름 | 설명 |
|------|------|
| `gameId` | 베팅 항목 ID |

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `answer` | `String` | 필수 | `CHOICE`: 문항 index(`"0"`), `NUMBER`: 숫자(`"3.5"`) |
| `amount` | `Long` | 필수 | 베팅 포인트. 1 이상 |

```json
{ "answer": "1", "amount": 300 }
```

**Response** `200`
```json
{ "logId": 17, "answer": "1", "amount": 300, "balance": 2200 }
```

**에러**
| 코드 | 메시지 | 원인 |
|------|--------|------|
| `400` | `베팅 포인트는 1 이상이어야 합니다.` | amount 누락/0 이하 |
| `400` | `포인트가 부족합니다. 현재: N, 요청: -M` | 잔액 부족 |
| `400` | `answer는 필수입니다.` | answer 누락 |
| `400` | `answer 형식이 올바르지 않습니다: X` | 숫자가 아님 |
| `400` | `문항 index 범위 밖: N` | 객관식 index 초과 |
| `400` | `베팅 항목을 찾을 수 없습니다: N` | 없는 gameId |
| `409` | `마감된 베팅입니다.` | `deadline` 경과 또는 `OPEN`이 아님 |

---

## 어드민 API

### POST `/apis/v1/admin/users/{memberId}/point` 🔑
특정 멤버 포인트 증감.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `delta` | `Long` | 필수 | 양수 = 지급, 음수 = 차감. 0 불가 |

```json
{ "delta": 1000 }
```

**Response** `200`
```json
{ "memberId": 42, "balance": 3200 }
```

**에러**
| 코드 | 메시지 |
|------|--------|
| `400` | `User not found` |
| `400` | `delta는 0이 아닌 정수여야 합니다.` |
| `400` | `포인트가 부족합니다. ...` (차감 결과가 0 미만) |

---

### GET `/apis/v1/admin/users` 🔑
기존 유저 목록 응답에 `balance`(현재 포인트) 필드가 추가됨.

```json
{ "memberId": 42, "userId": "tester", "name": "홍길동", "role": "USER", "balance": 2500 }
```

---

### POST `/apis/v1/admin/bets` 🔑
베팅 항목 생성.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | `String` | 필수 | 제목 |
| `content` | `String` | 선택 | 설명 |
| `type` | `BtGameType` | 필수 | `CHOICE` / `NUMBER` |
| `choices` | `String[]` | CHOICE만 필수 | 문항 2개 이상. NUMBER면 생략 |
| `deadline` | `String` | 필수 | 마감 시각. ISO-8601 **오프셋 포함** (`2026-10-20T18:30:00+09:00` 또는 `...09:30:00Z`). 서버가 UTC로 저장 |

```json
{
  "title": "10/20 LG vs 두산 승자",
  "content": "잠실 19:00 경기",
  "type": "CHOICE",
  "choices": ["LG", "두산", "무승부"],
  "deadline": "2026-10-20T18:30:00+09:00"
}
```
```json
{
  "title": "10/20 LG vs 두산 총 득점",
  "type": "NUMBER",
  "deadline": "2026-10-20T18:30:00+09:00"
}
```

**Response** `200` `BtGame`

**에러**
| 코드 | 메시지 |
|------|--------|
| `400` | `title은 필수입니다.` |
| `400` | `type은 CHOICE 또는 NUMBER여야 합니다.` |
| `400` | `deadline은 필수입니다.` / `deadline이 이미 지났습니다.` |
| `400` | `객관식은 문항이 2개 이상 필요합니다.` |

---

### GET `/apis/v1/admin/bets` 🔑
전체 베팅 항목. 상태 무관, 최신순(`id` 내림차순).

**Response** `200` `BtGame[]`

---

### GET `/apis/v1/admin/bets/{gameId}/logs` 🔑
항목별 베팅 내역.

**Response** `200` `BtLog[]`
```json
[
  { "id": 17, "memberId": 42, "memberName": "홍길동", "answer": "1", "amount": 300, "payout": null, "createdAt": "2026-10-19T03:12:45Z" }
]
```

---

### POST `/apis/v1/admin/bets/{gameId}/close` 🔑
정답 입력 → 종료 → 정산. `OPEN` 상태만 가능.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `answer` | `String` | 필수 | `CHOICE`: 정답 문항 index, `NUMBER`: 정답 숫자 |

```json
{ "answer": "0" }
```

**Response** `200` `BtGame` (status `CLOSED`, `answer` 채워짐)

**에러**
| 코드 | 메시지 |
|------|--------|
| `400` | answer 관련 (베팅 API와 동일) |
| `409` | `이미 종료/취소된 베팅입니다: CLOSED` |

---

### POST `/apis/v1/admin/bets/{gameId}/cancel` 🔑
무효 처리. 모든 베팅을 전액 환불하고 상태를 `CANCELED`로 변경. `OPEN` 상태만 가능. Body 없음.

**Response** `200` `BtGame` (status `CANCELED`)

**에러**
| 코드 | 메시지 |
|------|--------|
| `409` | `이미 종료/취소된 베팅입니다: CLOSED` |

---

## 정산 규칙

| 항목 | 규칙 |
|------|------|
| CHOICE 지급액 | `베팅액 × (전체 풀 / 적중 풀)`, 소수점 버림 |
| CHOICE 적중 | `answer` index가 정답과 같은 베팅 |
| CHOICE 적중자 없음 | 전액 환불 (`payout = amount`) |
| NUMBER 지급액 | 근접도 가중 분배 (아래). 지급 합계 = 전체 풀 |
| NUMBER 전원 같은 오차 | 전액 환불 |
| 무효 처리 | 전액 환불 |
| 낙첨 | `payout = 0` |

예) 전체 풀 300, 정답 문항에 100(50+50) → 적중자 각각 150 지급.

### NUMBER 분배 (베팅 단위, 수수료 0)

정답에 가까운 베팅일수록 많이 받고, 오차순 상위 구간의 베팅만 받는다. 구현은 `BettingService.numberPayouts`에 있다.

#### 설정값

| 상수 | 값 | 뜻 |
|------|----|----|
| `NUMBER_CUTOFF` (P) | `0.5` | 당첨 구간. 오차순으로 쌓은 베팅액 기준 상위 비율. 이 위치에서 가중치가 0이 된다 |
| `NUMBER_SHARPNESS` (k) | `2` | 가중치 감소 지수. 1은 선형, 클수록 정답 근처에 몰아준다 |

#### 알고리즘

1. 베팅마다 오차 `|값 - 정답|`을 구한다.
2. 오차 오름차순으로 정렬한다. 같은 오차는 한 그룹으로 묶는다.
3. 그룹마다 위치를 구한다. `p = (앞 그룹 누적액 + 그룹액 / 2) / 풀`.
4. 그룹마다 가중치를 구한다. `w = max(0, 1 - p / P)^k`. 그룹 안의 베팅은 모두 같은 `w`를 받는다.
5. 베팅마다 점수를 구한다. `점수 = 베팅액 × w`.
6. 지급액을 구한다. `floor(풀 × 점수 / 점수 합)`.
7. 내림으로 남은 자투리를 점수가 가장 큰 베팅에 더한다. 지급 합계는 항상 풀과 같다.

#### 세부 규칙

| 상황 | 처리 |
|------|------|
| 동률 판정 | 오차를 `BigDecimal`로 정확히 비교한다. 정답 0.3에서 0.1과 0.5는 같은 그룹이다 |
| 자투리 수령자 동률 | 오차가 작은 베팅, 그다음 `id`가 작은(먼저 건) 베팅이 받는다. 입력 순서와 무관하다 |
| 점수 합 = 0 | 전원 베팅액 환불. 모든 베팅이 같은 오차일 때만 생긴다 (베팅 1건 포함) |
| 한 사람의 여러 베팅 | 각 베팅을 따로 계산한다 |
| 입력값 | 유한한 숫자만 받는다. `NaN`·`Infinity`는 400 |
| `finalOdds` | 베팅마다 배당이 달라 항상 `null` |

#### 받는 조건

`p < P` 조건을 P=0.5에서 풀어 쓰면 아래와 같다.

> **나보다 가까운 베팅 총액 < 나보다 먼 베팅 총액**이면 받는다.

- 내 그룹의 금액은 양쪽에서 지워진다. 그래서 내가 거는 금액은 받는지 여부에 영향이 없다. 받는 양에만 영향이 있다.
- 경계 근처는 가중치가 거의 0이다. 그래서 당첨 구간 안이라도 지급액이 베팅액보다 적을 수 있다.
- 정확히 맞혀도 보너스는 없다. 오차 0은 "가장 가까운 베팅"과 똑같이 처리한다.

#### 계산 예시

정답 15.2, 풀 100.

| 베팅값 | 베팅액 | 오차 | p | w | 점수 | 지급액 |
|--------|--------|------|---|---|------|--------|
| 15 | 10 | 0.2 | 0.05 | 0.81 | 8.1 | 52 (50 + 자투리 2) |
| 17 | 20 | 1.8 | 0.20 | 0.36 | 7.2 | 45 |
| 13 | 5 | 2.2 | 0.325 | 0.1225 | 0.6125 | 3 |
| 20 | 50 | 4.8 | 0.60 | 0 | 0 | 0 |
| 6 | 15 | 9.2 | 0.925 | 0 | 0 | 0 |

- 이 판의 받는 경계는 오차 4.8 미만, 즉 값 10.4 초과 20.0 미만이다.
- 이 판에 정답 15.2로 새로 걸면 베팅액의 약 3~6배를 받는다. 적게 걸수록 배수가 높다 (1 → 6, 10 → 52, 50 → 146).

#### 참가자가 적을 때

베팅액이 비슷하면 사실상 1등 독식이 된다. 1등 한 명이 풀의 큰 몫을 차지해서 2등의 위치가 50% 근처로 밀리기 때문이다.

| 10 / 10 / 10 세 명 | 1등 | 2등 | 3등 |
|------|-----|-----|-----|
| 현재 (P=0.5, k=2) | 30 | 0 | 0 |
| P=0.8, k=1 | 21 | 9 | 0 |
| P=1.0, k=1 | 17 | 10 | 3 |

소규모 판에서 2등에게도 몫을 주려면 P를 올리고 k를 낮춘다. 현재는 모든 판이 같은 상수를 쓴다.

---

## FE 구현 메모

- 베팅 가능 여부는 `bettable`만 보면 된다. 클라이언트 시계로 `deadline`을 비교하지 말 것 (서버 시각 기준으로 409 처리됨).
- `odds`는 베팅 시점의 예상값. 최종 배당은 마감 시 풀 기준으로 결정됨을 UI에 명시.
- `NUMBER` 타입은 다른 사람의 입력값이 노출되지 않는다. `totalPool`만 표시.
- `409`는 마감/상태 충돌, `400`은 입력값·잔액 문제. 에러 본문은 `{ "error": "메시지" }`.
