# P-Rank 시스템

> 선수별 판타지 가치 점수 (`p_rank`). 로티세리 성적 데이터를 기반으로 산출한 선형회귀 모델로, "이 선수를 6경기(또는 18아웃) 뛰게 하면 판타지 점수가 얼마나 나오는가"를 단일 숫자로 표현한다.

---

## 개요

| 항목 | 내용 |
|------|------|
| 저장 위치 | `kbo_hitter_stats.p_rank`, `kbo_pitcher_stats.p_rank` |
| 타입 | `DOUBLE NULL` |
| 갱신 시점 | KBO 경기 업로드(`POST /apis/v1/kbo/games/upload`) 완료 직후 시즌 전체 일괄 재산출 |
| 노출 API | `GET /apis/v1/kboplayer/hitter/all` (season 파라미터), `GET /apis/v1/kboplayer/pitcher/all` (season 파라미터) |
| null 조건 | 출전 기록이 없는 선수 (타자 `games = 0`, 투수 `outs = 0`) |

---

## 타자 p_rank

### 정규화 기준: 6경기

시즌 누적 스탯을 선수의 실제 출전 경기 수(`games`)로 나눠 **6경기 기준 스탯**으로 환산한 뒤 가중치를 적용한다.

```
hr6  = hr  / games × 6
rbi6 = rbi / games × 6
sb6  = sb  / games × 6
so6  = so  / games × 6
avg  = 타율 (그대로 사용, 경기 수 무관)
```

### 계산식

```
p_rank = (775.75 × avg  - 147.89)
       + (  8.83 × hr6  +  22.34)
       + (  2.13 × rbi6 +  12.07)
       + ( 13.00 × sb6  +  15.74)
       + ( -2.50 × so6  + 135.69)
```

> 삼진(`so`)의 계수 `a`가 **음수**(-2.50)이므로 삼진이 많을수록 점수가 낮아진다.

### 카테고리별 가중치

| 카테고리 | 변수 | a (기울기) | b (절편) |
|---------|------|-----------|---------|
| 타율 | `avg` | 775.75 | -147.89 |
| 홈런 | `hr6` | 8.83 | 22.34 |
| 타점 | `rbi6` | 2.13 | 12.07 |
| 도루 | `sb6` | 13.00 | 15.74 |
| 삼진(역) | `so6` | **-2.50** | 135.69 |

---

## 투수 p_rank

### 정규화 기준: 18아웃 (= 6이닝)

시즌 누적 스탯을 선수의 실제 아웃카운트(`outs`)로 나눠 **18아웃 기준 스탯**으로 환산한다.  
`outs`는 DB의 `kbo_pitcher_stats.outs` 컬럼(아웃카운트 원시값, 3 = 1이닝)이다.

```
win18  = win  / outs × 18
so18   = so   / outs × 18
save18 = save / outs × 18
era    = ERA  (그대로 사용)
whip   = WHIP (그대로 사용)
```

### 계산식

```
p_rank = ( 17.83 × win18  +  17.56)
       + (-14.84 × era    + 120.79)
       + (  2.76 × so18   -  35.98)
       + (-88.71 × whip   + 185.47)
       + ( 24.96 × save18 +  27.04)
```

> ERA(`era`)와 WHIP(`whip`)의 계수 `a`가 **음수**이므로 두 값이 낮을수록 점수가 높아진다.

### 카테고리별 가중치

| 카테고리 | 변수 | a (기울기) | b (절편) |
|---------|------|-----------|---------|
| 승리 | `win18` | 17.83 | 17.56 |
| 평균자책점(역) | `era` | **-14.84** | 120.79 |
| 삼진 | `so18` | 2.76 | -35.98 |
| WHIP(역) | `whip` | **-88.71** | 185.47 |
| 세이브 | `save18` | 24.96 | 27.04 |

---

## 갱신 흐름

```
POST /apis/v1/kbo/games/upload
  └─ KboAdminService.uploadGame()
       ├─ updateHitterStats()   ← games 컬럼도 여기서 +1
       ├─ updatePitcherStats()  ← outs 컬럼도 여기서 갱신
       └─ PRankService.updatePRank(season)
            ├─ kbo_hitter_stats 시즌 전체 조회 → 타자 p_rank 일괄 재산출 → saveAll
            └─ kbo_pitcher_stats 시즌 전체 조회 → 투수 p_rank 일괄 재산출 → saveAll
```

- 경기 1건 업로드당 **1회** 재산출. 복수 선수 갱신이 있어도 추가 연산 없음.
- `games = 0` 또는 `outs = 0`인 선수는 `p_rank = null` 처리.

---

## API 응답 필드

### 타자 (`/hitter/all?season=...`)

```json
{
  "content": [
    {
      "id": 123,
      "name": "홍길동",
      "team": "KIA",
      "battingAvg": 0.312,
      "homeruns": 10,
      "rbi": 42,
      "sb": 5,
      "strikeOut": 40,
      "games": 50,
      "pRank": 312.45
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `games` | `Integer` | 출전 경기 수 |
| `pRank` | `Double` | 6경기 기준 판타지 예상 점수 (`null` = 미출전) |

### 투수 (`/pitcher/all?season=...`)

```json
{
  "content": [
    {
      "id": 456,
      "name": "김투수",
      "team": "두산",
      "era": 3.45,
      "wins": 10,
      "saves": 2,
      "whip": 1.12,
      "stOut": 130,
      "games": 20,
      "pRank": 287.30
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `games` | `Integer` | 출전 경기 수 |
| `pRank` | `Double` | 18아웃 기준 판타지 예상 점수 (`null` = 미출전) |

---

## 해석 가이드

- **p_rank는 절대 점수가 아닌 상대 비교용 지표**다. 리그 평균 수준의 선수는 약 200~300점대, 에이스급은 350점 이상이 나오는 경향이 있다.
- 타자 삼진, 투수 ERA·WHIP은 낮을수록 좋은 스탯이므로 계수가 음수로 설계되어 있다.
- 경기 수가 극히 적은 선수(예: 1~2경기)는 정규화 효과로 인해 과대 평가될 수 있다. 필터링 시 `games` 필드를 함께 참고할 것.
- `season` 파라미터 사용 시에만 노출된다. 날짜 기간(`start`/`end`) 파라미터로 조회하는 경우 `pRank`는 응답에 포함되지 않는다.
