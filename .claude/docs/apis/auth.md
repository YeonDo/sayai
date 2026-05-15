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
  "playerId": 1
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
  "playerId": 1,
  "userId": "user123",
  "name": "홍길동",
  "admin": false
}
```

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
