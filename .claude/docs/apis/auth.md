# 인증 API

---

### POST `/apis/v1/auth/login`
로그인. 성공 시 `accessToken` HttpOnly 쿠키를 자동으로 설정함.

**Request Body**
```json
{
  "userId": "string",
  "password": "string"
}
```

**Response** `200`
```json
{
  "name": "홍길동"
}
```

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 아이디/비밀번호 불일치 | 400 | `"Invalid userId or password"` |

---

### POST `/apis/v1/auth/logout`
로그아웃. `accessToken` 쿠키를 삭제함.

**Response** `200` (body 없음)

---

### POST `/apis/v1/auth/signup`
회원가입.

**Request Body**
```json
{
  "userId": "string",
  "password": "string",
  "name": "string",
  "memberId": 1
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 아이디 중복 | 400 | `"User ID already exists"` |
| 비밀번호 비어있음 | 400 | `"Password cannot be empty"` |
| 비밀번호 형식 불일치 | 400 | `"Password must be at least 8 characters long and contain both letters and numbers"` |

---

### GET `/apis/v1/auth/me` 🔒
현재 로그인한 사용자 정보 조회.

**Response** `200`
```json
{
  "memberId": 1,
  "userId": "user123",
  "name": "홍길동",
  "admin": false,
  "kakaoOnly": false
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `memberId` | `Long` | 회원 식별자 |
| `userId` | `String` | 로그인 ID |
| `name` | `String` | 이름 |
| `admin` | `Boolean` | 관리자 여부 |
| `kakaoOnly` | `Boolean` | 카카오로만 가입한 계정 여부. `userId`가 `kakao_`로 시작하면 `true` |

---

---

### POST `/apis/v1/auth/kakao/login`
카카오 인가코드로 로그인/신규가입. **인증 불필요**.  
성공 시 `accessToken` HttpOnly 쿠키(`SameSite=None`) 자동 설정.  
상세 플로우는 `.claude/docs/system/kakao-oauth-system.md` 참조.

| 파라미터 | 위치 | 설명 |
|---------|------|------|
| `code` | Query | 카카오 발급 인가코드 |

**Response** `200`
```json
{ "name": "홍길동" }
```

---

### GET `/apis/v1/auth/kakao/status` 🔒
카카오 연동 여부 조회.

**Response** `200`
```json
{ "linked": true }
```

---

### POST `/apis/v1/auth/kakao/link?code={인가코드}` 🔒
기존 계정에 카카오 연동.

**Response** `200` `"카카오 계정이 연동되었습니다."`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 이미 다른 계정에 연동된 카카오 ID | 409 | `"이미 다른 계정에 연동된 카카오 계정입니다."` |

---

### DELETE `/apis/v1/auth/kakao/unlink` 🔒
카카오 연동 해제.

**Response** `200` `"카카오 연동이 해제되었습니다."`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 연동된 카카오 계정 없음 | 409 | `"연동된 카카오 계정이 없습니다."` |

---

### PATCH `/apis/v1/auth/me/name` 🔒
닉네임 변경.

**Request Body**
```json
{ "name": "새닉네임" }
```

**Response** `200` `"Name changed successfully"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 이름 비어있음 | 400 | `"Name cannot be empty"` |
| 형식 불일치 (한/영/숫자, 2~10자) | 400 | `"Name must be 2-10 characters using Korean, English, or numbers"` |

---

### POST `/apis/v1/auth/password` 🔒
비밀번호 변경.

**Request Body**
```json
{
  "currentPassword": "string",
  "newPassword": "string"
}
```

**Response** `200` `"OK"`

**Error Cases**
| 상황 | 상태코드 | 메시지 |
|------|---------|--------|
| 현재 비밀번호 불일치 | 400 | `"Invalid current password"` |
| 새 비밀번호 형식 불일치 | 400 | `"Password must be at least 8 characters long and contain both letters and numbers"` |
