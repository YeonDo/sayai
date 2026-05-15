# 공통 사항

> **Base URL**: `https://teamsayai.com` (로컬: `http://localhost:8080`)  
> **인증**: HTTP-Only 쿠키 `accessToken` (JWT, 6시간 유효)  
> **대상**: sayai-frontend 개발자 참조용

---

## 날짜 형식

모든 날짜 파라미터는 `yyyy-MM-dd` 형식 사용.

---

## 인증 (🔒)

🔒 표시 API는 `accessToken` 쿠키 필요. 없으면 `403` 반환.

---

## 공통 에러 응답

```
400 Bad Request    — 잘못된 파라미터 또는 비즈니스 규칙 위반
401 Unauthorized   — 인증 토큰 없음 또는 만료
403 Forbidden      — 권한 없음 (Admin 전용 API 등)
500 Internal Error — 서버 오류
```

> `IllegalStateException`은 `500`으로 반환. 클라이언트에서 500 수신 시 `response.body`에 에러 메시지가 포함되어 있으므로 파싱해서 사용자에게 표시 권장.

---

## 자주 만날 에러 메시지

| 메시지 | 원인 |
|--------|------|
| `"Invalid userId or password"` | 로그인 실패 |
| `"이미 참여 신청을 완료했습니다"` | 게임 중복 참여 시도 |
| `"당신의 차례가 아닙니다"` | 드래프트 순서 아닐 때 픽 시도 |
| `"이미 뽑힌 선수입니다"` | 이미 드래프트된 선수 픽 시도 |
| `"Roster full"` | 로스터 최대 인원 초과 |
| `"샐캡 초과"` / `"Salary Cap Exceeded"` | 샐러리캡 한도 초과 |
| `"Position limit exceeded"` | 포지션 배치 한도 초과 |
| `"Only Admin can start the draft"` | 일반 유저가 게임 시작 시도 |

---

## GameStatus 참조

| 값 | 설명 |
|----|------|
| `WAITING` | 참가 신청 대기 중 |
| `DRAFTING` | 드래프트 진행 중 |
| `FA_SIGNING` | FA 서명 기간 |
| `ONGOING` | 시즌 진행 중 |
| `FINISHED` | 종료 |
