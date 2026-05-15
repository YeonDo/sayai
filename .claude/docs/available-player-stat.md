# 작업 지시서

## 작업 개요
드래프트 대상 조회시 현재 성적 조회 기능 추가

1. 수정 대상
GET /games/{gameSeq}/available-players
FantasyDraftController 참조
2. 수정 사항
FantasyPlayerDto response 의 stat 값을 ft_player 테이블의 값에서 kbo_hitter_stats, kbo_pitcher_stats 의 데이터로 대체
타자인경우: FantasyPlayerDto.stats 의 데이터를 ft_players 데이터 -> avg + ", " + hr + "홈런, " + rbi + "타점, " + sb + "도루"
투수인 경우:  FantasyPlayerDto.stats 의 데이터를 ft_players 데이터 -> win + "승, " + era + " ERA, " + whip + " WHIP"
3. 참고사항
개발 후 500명의 데이터 조회시에 대략적인 성능 저하 체크 필요
막히는 사항이 있다면 멈추고 나에게 물어볼것