# 야구 통계 계산 규칙

## 타자 통계

### 타율 (AVG)
```
AVG = hit / ab
저장: String.format("%.3f", (double) hit / ab)
예시: "0.289"
```
- `ab == 0` 이면 `"0.000"` 저장

### 응답 DTO에서의 타율
```java
// KboHitterStats (String) → PlayerDto (Double)
dto.setBattingAvg(Double.parseDouble(stat.getAvg()));

// 기간 집계 (KboHitStatInterface) → 직접 계산
double battingAvg = (double) stat.getTotalHits() / stat.getPlayerAppearance();
battingAvg = Math.round(battingAvg * 1000.0) / 1000.0;
```

### 타자 시즌 스탯 필드 매핑

| kbo_hitter_stats 컬럼 | 의미 | PlayerDto 필드 |
|-----------------------|------|----------------|
| pa | 타석 | playerAppearance |
| ab | 타수 | atBat |
| hit | 안타 | totalHits |
| hr | 홈런 | homeruns |
| rbi | 타점 | rbi |
| so | 삼진 | strikeOut |
| sb | 도루 | sb |
| avg | 타율 (String) | battingAvg (Double) |

---

## 투수 통계

### ERA (평균자책점)
```
ERA = er / outs * 27
저장: String.format("%.2f", (double) er / outs * 27)
예시: "3.14"
```
- `outs == 0` 이면 `"0.00"` 저장
- `outs` = 아웃카운트 (이닝 × 3)

### WHIP
```
WHIP = (bb + phit) / outs * 3
저장: String.format("%.2f", (double)(bb + phit) / outs * 3)
예시: "1.23"
```
- `outs == 0` 이면 `"0.00"` 저장

### 이닝 표시 (응답용)
```java
// outs(아웃카운트) → 이닝 표시
double inn = outs / 3 + (outs % 3) * 0.1;
// 10아웃 → 3.1이닝
// 11아웃 → 3.2이닝
```

### 투수 시즌 스탯 필드 매핑

| kbo_pitcher_stats 컬럼 | 의미 | PitcherDto 필드 |
|------------------------|------|-----------------|
| outs | 아웃카운트 | innings (변환 후) |
| er | 자책점 | selfLossScore |
| win | 승 | wins |
| so | 삼진 | stOut |
| save | 세이브 | saves |
| bb | 볼넷 | baseOnBall |
| phit | 피안타 | pHit |
| era | 평균자책점 (String) | era (Double) |
| whip | WHIP (String) | whip (Double) |

---

## outs vs inning 구분 주의

| 테이블/필드 | 타입 | 의미 |
|------------|------|------|
| `kbo_pitch.inning` | Long | 이닝 수 (정수) |
| `kbo_pitcher_stats.outs` | Integer | 아웃카운트 = inning × 3 |

변환:
```java
// kbo_pitch → kbo_pitcher_stats 저장 시
int newOuts = stats.getOuts() + (int)(pitch.getInning() * 3);

// kbo_pitch 기간 집계 쿼리 내
IFNULL(SUM(pi.inning * 3), 0) as outs
```

---

## 선수 포지션 분류

```java
// 투수
position IN ('SP', 'RP', 'CL')

// 타자 (투수가 아닌 모든 포지션)
position NOT IN ('SP', 'RP', 'CL')
```

포지션 예시: C, 1B, 2B, 3B, SS, LF, CF, RF, DH (타자)  
IF (내야수 필터), OF (외야수 필터) 는 `ft_players.position` 기준으로 파생

---

## 시즌 누적 업데이트 전략

경기 업로드 시 시즌 스탯 업데이트 로직:

```
1. kbo_hitter_stats에 (player_id, season) 존재?
   ├── YES: 기존값 + 해당 경기 기록 덧셈 → save
   └── NO:  kbo_hit 테이블에서 해당 시즌 전체 합산 → insert

2. kbo_pitcher_stats에 (player_id, season) 존재?
   ├── YES: 기존값 + 해당 경기 기록 덧셈 → save
   └── NO:  kbo_pitch 테이블에서 해당 시즌 전체 합산 → insert
```

avg/ERA/WHIP 은 매번 재계산해서 저장 (누적 합산 값 기준).

---

## 타석수 파싱 (Excel 업로드)

이닝 컬럼(숫자 헤더 "1", "2", "3"...)의 셀값에서 타석 추출:
```java
// "/" 구분자로 분리하여 각 타석 카운트
String[] atBats = cellVal.split("/");
for (String atBat : atBats) {
    if (!atBat.trim().isEmpty()) pa++;
}

// 삼진/홈런 파싱
if (cellVal.contains("삼진") || cellVal.contains("스낫")) so++;
if (cellVal.contains("홈")) hr++;
```

대주자 처리: `pa == 0 && sb == 0` 인 행은 건너뜀.
