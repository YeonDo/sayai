# 카카오 OAuth 시스템

---

## 개요

카카오 소셜 로그인 및 기존 계정 연동을 지원한다.  
일반 로그인과 카카오 로그인 모두 **HttpOnly 쿠키(`SameSite=None; Secure`)** 방식으로 통일됐다.  
FE(`pandasy.vercel.app`)와 BE(`teamsayai.com`)가 별도 도메인이므로 `SameSite=None`을 사용하며, HTTPS 환경에서만 동작한다.

---

## API 명세

### POST `/apis/v1/auth/kakao/login`
카카오 인가코드로 로그인. **인증 불필요**.

FE의 카카오 콜백 페이지에서 `code`를 추출해 이 엔드포인트를 호출한다.

| 파라미터 | 위치 | 설명 |
|---------|------|------|
| `code` | Query | 카카오 발급 인가코드 |

**동작**
1. 인가코드로 카카오 액세스 토큰 발급
2. 카카오 사용자 정보(ID, 닉네임) 조회
3. `user_social_accounts`에서 카카오 ID 검색
   - 존재하면 → 연동된 기존 계정으로 로그인
   - 없으면 → 신규 `app_users` + `user_social_accounts` 생성 후 로그인
4. JWT 반환

성공 시 `accessToken` HttpOnly 쿠키(`SameSite=None; Secure`) 자동 설정.

**Response** `200`
```json
{ "name": "홍길동" }
```

---

### GET `/apis/v1/auth/kakao/status` 🔒
현재 로그인 계정의 카카오 연동 여부 조회.

**Response** `200`
```json
{ "linked": true }
```

---

### POST `/apis/v1/auth/kakao/link?code={인가코드}` 🔒
기존 계정에 카카오 연동.

| 파라미터 | 위치 | 설명 |
|---------|------|------|
| `code` | Query | 카카오 발급 인가코드 |

**동작**
1. 인가코드로 카카오 사용자 ID 조회
2. 해당 카카오 ID가 이미 다른 계정에 연동됐으면 에러
3. 현재 로그인 계정 + 카카오 ID를 `user_social_accounts`에 저장

**Response** `200` `"카카오 계정이 연동되었습니다."`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 이미 다른 계정에 연동된 카카오 ID | 500 | `"이미 다른 계정에 연동된 카카오 계정입니다."` |

---

### DELETE `/apis/v1/auth/kakao/unlink` 🔒
카카오 연동 해제.

**동작**: `user_social_accounts`에서 해당 레코드 삭제. 계정 자체는 유지됨.

**Response** `200` `"카카오 연동이 해제되었습니다."`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 연동된 카카오 계정 없음 | 500 | `"연동된 카카오 계정이 없습니다."` |

---

## 카카오 인가 URL

로그인과 연동 모두 **동일한 `redirect_uri`** (`/auth/kakao/callback`)를 사용한다.  
`state` 파라미터로 로그인/연동을 구분하며, 카카오 콘솔에 등록할 URI도 하나만 필요하다.

**로그인용**
```
https://kauth.kakao.com/oauth/authorize
  ?client_id={REST_API_KEY}
  &redirect_uri=https://pandasy.vercel.app/auth/kakao/callback
  &response_type=code
```

**연동용** (마이페이지에서 연동하기 클릭 시)
```
https://kauth.kakao.com/oauth/authorize
  ?client_id={REST_API_KEY}
  &redirect_uri=https://pandasy.vercel.app/auth/kakao/callback
  &response_type=code
  &state=link
```

> `redirect_uri`를 하나로 통일하면 카카오 콘솔에 URI 1개만 등록하면 된다.  
> `/auth/kakao/callback` 페이지에서 `state=link` 여부에 따라 `/login` 또는 `/link` API로 분기한다.

---

## 호출 플로우

### 카카오 로그인

```
[FE] 카카오 로그인 버튼 클릭
    │
    ▼
[FE] 카카오 인가 URL로 리다이렉트
    │  redirect_uri = https://pandasy.vercel.app/auth/kakao/callback
    ▼
[카카오] 로그인 후 인가코드 발급
    │  https://pandasy.vercel.app/auth/kakao/callback?code=xxx
    ▼
[FE] /auth/kakao/callback 페이지에서 code 추출
    │
    ▼
[FE] POST https://teamsayai.com/apis/v1/auth/kakao/login?code=xxx
    │
    ▼
[BE] accessToken 쿠키 설정 + { "name": "..." } 반환
    │
    ▼
[FE] 홈으로 이동
```

### 기존 계정에 카카오 연동

```
[FE] 마이페이지에서 "카카오 연동" 버튼 클릭
    │
    ▼
[FE] 카카오 인가 URL로 리다이렉트 (state=link 포함)
    │  redirect_uri = https://pandasy.vercel.app/auth/kakao/callback&state=link
    ▼
[카카오] 인가코드 발급
    │  https://pandasy.vercel.app/auth/kakao/callback?code=xxx&state=link
    ▼
[FE] /auth/kakao/callback 페이지에서 state=link 확인 후 분기
    POST /apis/v1/auth/kakao/link?code=xxx  (Authorization: Bearer 헤더 포함)
    │
    ▼
[BE] 200 응답
    │
    ▼
[FE] 마이페이지로 복귀 + 연동 완료 UI 갱신
```

### 카카오 연동 해제

```
[FE] 마이페이지에서 "연동 해제" 버튼 클릭
    │
    ▼
[FE] 확인 다이얼로그 표시
    │
    ▼
[FE] DELETE /apis/v1/auth/kakao/unlink
    │
    ▼
[BE] user_social_accounts 레코드 삭제
    │
    ▼
[FE] "연동하기" 상태로 UI 전환
```

---

## 개발해야 할 화면 작업

### 1. 로그인 페이지 — 카카오 로그인 버튼

- 기존 ID/PW 로그인 폼 하단에 "카카오로 로그인" 버튼 추가
- 클릭 시 카카오 인가 URL(로그인용)로 리다이렉트

```
[ ID 입력          ]
[ PW 입력          ]
[ 로그인 버튼       ]
────────────────────
[ 카카오로 로그인   ]   ← 추가
```

---

### 2. 카카오 로그인 콜백 페이지 (`/auth/kakao/callback`)

URL에서 `code` 파라미터 추출 후 BE 호출, 토큰 저장 후 홈으로 이동하는 처리 전용 페이지.  
사용자에게는 로딩 스피너만 보여주면 된다.

```
처리 흐름:
1. URL params에서 code 추출
2. POST /apis/v1/auth/kakao/login?code={code} 호출
3. 성공: accessToken 저장 → 홈(/) 이동
4. 실패: 로그인 페이지로 이동 + 에러 토스트
```

---

### 3. 마이페이지 — 카카오 연동 관리 섹션

페이지 진입 시 `GET /apis/v1/auth/kakao/status` 호출 후 상태에 따라 UI 분기.

**연동 안 된 경우**
```
소셜 계정   카카오   [ 연동하기 ]
```

**연동된 경우**
```
소셜 계정   카카오   ✓ 연동됨   [ 연동 해제 ]
```

#### "연동하기" 클릭 흐름
1. 카카오 인가 URL(`state=link` 포함)로 리다이렉트
2. `/auth/kakao/callback?code=xxx&state=link` 로 돌아옴
3. 콜백 페이지에서 `state=link` 확인 → `POST /apis/v1/auth/kakao/link?code=xxx` 호출 (`Authorization: Bearer` 헤더 포함)
4. 성공 시 마이페이지로 복귀 + "카카오 연동이 완료되었습니다." 토스트

#### "연동 해제" 클릭 흐름
1. 확인 다이얼로그 표시
2. `DELETE /apis/v1/auth/kakao/unlink` 호출
3. 성공 시 "연동하기" 상태로 UI 전환

---

### 4. 카카오 콜백 페이지 (`/auth/kakao/callback`) — state 분기

로그인과 연동 모두 이 페이지 하나에서 처리한다.

```
처리 흐름:
1. URL params에서 code, state 추출
2. state === 'link'
   → POST /apis/v1/auth/kakao/link?code={code}  (Authorization: Bearer 헤더 포함)
   → 성공: 마이페이지로 이동 + "연동 완료" 토스트
   → 실패: 마이페이지로 이동 + 에러 토스트
3. state 없음 (로그인)
   → POST /apis/v1/auth/kakao/login?code={code}
   → 성공: accessToken 저장 → 홈(/) 이동
   → 실패: 로그인 페이지로 이동 + 에러 토스트
```

---

## 카카오 콘솔 설정 체크리스트

| 항목 | 값 |
|------|-----|
| 플랫폼 | Web — `https://pandasy.vercel.app` 등록 |
| Redirect URI | `https://pandasy.vercel.app/auth/kakao/callback` (1개만 등록) |
| 카카오 로그인 활성화 | ON |
| 동의항목 | 닉네임 (필수) |
| Client Secret | 보안 탭에서 활성화 후 값 복사 |

---

## BE application.yml 설정

```yaml
kakao:
  client-id: {REST API 키}
  client-secret: {Client Secret}
  redirect-uri: https://pandasy.vercel.app/auth/kakao/callback
```

> `redirect-uri`는 카카오 토큰 발급 시 검증용으로 사용되며, 로그인 콜백 페이지 URI와 일치해야 한다.  
> 연동용 콜백(`/mypage/kakao-callback`)은 `/link` 엔드포인트 호출 시 FE에서 직접 code만 전달하므로 BE 설정 불필요.
