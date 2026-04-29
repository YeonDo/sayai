# FCM 알림 API

---

### POST `/apis/v1/fcm/subscribe` 🔒
FCM 토픽 구독 등록. 여러 토픽을 한 번에 구독할 수 있다.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `token` | `String` | 필수 | FCM 디바이스 토큰 |
| `topics` | `List<String>` | 필수 | 구독할 토픽 목록 |

```json
{
  "token": "FCM_DEVICE_TOKEN",
  "topics": ["game_1", "user_42_game_1"]
}
```

**토픽 형식**
| 형식 | 용도 |
|------|------|
| `game_{gameSeq}` | 게임 전체 참가자 대상 알림 |
| `user_{playerId}_game_{gameSeq}` | 특정 참가자 개인 알림 |

**Response** `200` `"Subscribed"`

> `token` 또는 `topics`가 없으면 처리 없이 200 반환.

---

## FCM 알림 발송 시점 요약

### 웨이버
| 시점 | 토픽 | 제목 |
|------|------|------|
| 웨이버 신청 | `game_{gameSeq}` | 웨이버 신청 |

### 트레이드
| 시점 | 토픽 | 제목 |
|------|------|------|
| 트레이드 제안 | `user_{targetId}_game_{gameSeq}` | 트레이드 제안 |
| 신청자 취소 | `user_{targetId}_game_{gameSeq}` | 트레이드 취소 |
| 상대팀 거절 | `user_{requesterId}_game_{gameSeq}` | 트레이드 거절 |
| 투표 → 승인 | `game_{gameSeq}` | 트레이드 승인 |
| 투표 → 기각 | `game_{gameSeq}` | 트레이드 기각 |
