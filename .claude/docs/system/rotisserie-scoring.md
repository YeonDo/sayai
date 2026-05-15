# 로티세리 스코어링 시스템

> 참조 코드: `FantasyScoringService.java`, `FantasyRankingService.java`  
> 저장 테이블: `ft_score_rotisserie` (FantasyRotisserieScore)

---

## 개요

로티세리(Rotisserie)는 카테고리별로 참가자 간 순위를 매겨 포인트를 부여하고, 전체 포인트 합산으로 최종 순위를 결정하는 방식이다. 라운드 단위로 성적을 입력하며, 라운드별 totalPoints를 누적하여 최종 순위를 결정한다.

---

## 카테고리 구성

| 카테고리 | 필드 | 유형 | 정렬 방향 |
|---------|------|------|-----------|
| 타율 | `avg` | 타자 | 높을수록 좋음 |
| 홈런 | `hr` | 타자 | 높을수록 좋음 |
| 타점 | `rbi` | 타자 | 높을수록 좋음 |
| 타자 삼진 | `soBatter` | 타자 | **낮을수록 좋음** |
| 도루 | `sb` | 타자 | 높을수록 좋음 |
| 승리 | `wins` | 투수 | 높을수록 좋음 |
| 방어율 | `era` | 투수 | **낮을수록 좋음** |
| 투수 삼진 | `soPitcher` | 투수 | 높을수록 좋음 |
| WHIP | `whip` | 투수 | **낮을수록 좋음** |
| 세이브 | `saves` | 투수 | 높을수록 좋음 |

총 10개 카테고리 (타자 5개, 투수 5개)

---

## 포인트 배정 공식

참가자 수 n명 기준, 각 카테고리에서:

```
포인트 = (n - rank + 1) * 10
```

| 순위 (n=8 예시) | 포인트 |
|----------------|--------|
| 1위 | 80점 |
| 2위 | 70점 |
| 3위 | 60점 |
| ... | ... |
| 8위 | 10점 |

---

## 동률(Tie) 처리

같은 값이 여러 참가자에게 발생하면, 해당 순위대의 포인트를 평균하여 동일하게 배분한다.

**예시** (n=8, 3~4위 동률):
```
3위 포인트 = (8-3+1)*10 = 60
4위 포인트 = (8-4+1)*10 = 50
동률 포인트 = (60 + 50) / 2 = 55점 (공동 3위 전원에게 적용)
```

---

## null 값 처리

해당 카테고리 값이 `null`인 참가자는:
- 순위 = n (최하위)
- 포인트 = 0

---

## totalPoints 계산

```
totalPoints = pointsAvg + pointsHr + pointsRbi + pointsSoBatter + pointsSb
            + pointsWins + pointsEra + pointsSoPitcher + pointsWhip + pointsSaves
```

---

## 라운드별 성적 관리

- Admin이 `POST /apis/v1/admin/fantasy/games/{gameSeq}/scores/{round}/upload-from-snapshot` 호출 시 `saveAndCalculateScores` 실행
- 해당 라운드의 `FantasyRotisserieScore` upsert (없으면 INSERT, 있으면 UPDATE)
- 포인트는 라운드 내 참가자 간 상대 순위 기준으로 계산
- **최종 순위**: 전체 라운드 `totalPoints` 합산 기준 내림차순 정렬

---

## 순위표 집계 방식 (`getRotisserieRanking`)

각 참가자의 라운드별 성적을 집계:

| 통계 유형 | 집계 방식 |
|---------|---------|
| hr, rbi, sb, soBatter, wins, soPitcher, saves | 라운드 합산 (sum) |
| avg, era, whip | 라운드 단순 평균 (avg) — 원시 데이터(타수/이닝) 없으므로 근사값 |

> avg/era/whip은 원시 denominator(타수, 이닝) 없이 라운드 평균으로 표시하므로 참고용 수치임. 실제 포인트는 라운드별 개별 계산이 정확함.

---

## DB 테이블 구조 (`ft_score_rotisserie`)

| 컬럼 | 설명 |
|------|------|
| `seq` | PK |
| `fantasyGameSeq` | 게임 식별자 |
| `playerId` | 참가자 ID |
| `round` | 라운드 번호 |
| `avg`, `hr`, `rbi`, `soBatter`, `sb` | 타자 성적 |
| `wins`, `era`, `soPitcher`, `whip`, `saves` | 투수 성적 |
| `rankAvg`, `rankHr`, ... | 카테고리별 순위 |
| `pointsAvg`, `pointsHr`, ... | 카테고리별 포인트 |
| `totalPoints` | 라운드 총점 |
