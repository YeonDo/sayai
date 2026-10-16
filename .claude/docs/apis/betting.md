# 포인트 / 베팅 API

## 테이블
| 테이블 | 용도 |
|---|---|
| `user_point` | 멤버 포인트 잔액. PK `member_id`, `balance`. 행 없음 = 0 |
| `bt_games` | 베팅 항목. `type` CHOICE/NUMBER, `choices` JSON 배열(객관식만), `deadline` UTC, `status` OPEN/CLOSED/CANCELED, `answer` |
| `bt_log` | 베팅 이력. `answer`(객관식=문항 index, 주관식=숫자), `amount`, `payout`(정산 전 NULL) |

## 멤버 (USER, 토큰 필요)
| Method | Path | Body | 설명 |
|---|---|---|---|
| GET | `/apis/v1/point/me` | - | `{memberId, balance}` |
| GET | `/apis/v1/bet/games` | - | OPEN 항목 목록. 객관식은 `choices[].{index,label,pool,odds}` 포함. `odds` = 전체풀/문항풀 (문항풀 0이면 null) |
| POST | `/apis/v1/bet/games/{gameId}` | `{answer, amount}` | 베팅. 포인트 즉시 차감. 마감 후 409, 잔액 부족/형식 오류 400. 같은 항목 여러 번 가능 |

## 어드민 (ADMIN)
| Method | Path | Body | 설명 |
|---|---|---|---|
| POST | `/apis/v1/admin/users/{memberId}/point` | `{delta}` | 포인트 증감. 잔액이 0 미만이 되면 400 |
| GET | `/apis/v1/admin/users` | - | 기존 목록에 `balance` 컬럼 추가 |
| POST | `/apis/v1/admin/bets` | `{title, content, type, choices[], deadline}` | 항목 생성. `deadline`은 ISO-8601 오프셋 포함(`2026-10-20T21:00:00+09:00`) → UTC 저장 |
| POST | `/apis/v1/admin/bets/{gameId}/close` | `{answer}` | 종료 + 정산. OPEN만 가능 |
| GET | `/apis/v1/admin/bets` | - | 전체 항목 (상태 무관, 최신순) |
| GET | `/apis/v1/admin/bets/{gameId}/logs` | - | 항목별 베팅 내역 `{id, memberId, memberName, answer, amount, payout, createdAt}` |
| POST | `/apis/v1/admin/bets/{gameId}/cancel` | - | 무효 처리: 전액 환불 후 CANCELED. OPEN만 가능 |

## 정산 규칙
- 배당 = 베팅액 × (전체 풀 / 적중 풀), 소수점 버림
- CHOICE: `answer` index와 같은 베팅이 적중
- NUMBER: 정답에 가장 근접한 값이 적중 (동률 전원)
- 적중자 없음: 전액 환불
- 포인트 변동은 `PointService.adjust(memberId, delta)` 단일 경로 (PESSIMISTIC_WRITE 락, 음수 잔액 불허)
