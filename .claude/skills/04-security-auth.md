# 보안 / 인증 규칙

## JWT 토큰

| 항목 | 내용 |
|------|------|
| 알고리즘 | HS256 (매 서버 시작 시 랜덤 키 생성) |
| 유효 시간 | 6시간 (21,600,000ms) |
| 전달 방식 | HTTP-Only 쿠키 (`accessToken`) 우선, `Authorization: Bearer` 폴백 |

### 클레임 구조
```
sub     → userId (로그인 ID)
playerId → Long (ft_players.seq 연결)
role    → "USER" | "ADMIN"
name    → 표시명
```

### 토큰 해석 순서 (JwtTokenProvider.resolveToken)
1. 쿠키 `accessToken` 확인
2. 없으면 `Authorization: Bearer xxx` 헤더 확인

---

## Spring Security 설정

### 공개 경로 (인증 없이 접근 가능)
```
/apis/v1/player/**
/apis/v1/auth/login
/apis/v1/auth/signup
/apis/v1/fantasy/players/import
/css/**, /js/**, /images/**, /favicon.ico
/
그 외 모든 경로 (anyRequest().permitAll())
```

### 인증 필요
```
/apis/v1/fantasy/**
/fantasy/**
/apis/v1/auth/password
```

### ADMIN 전용
```
/apis/v1/admin/**
```

> 주의: `/apis/v1/kboplayer/**` 는 `anyRequest().permitAll()` 에 포함되어 있어  
> Security 레벨 인증은 없지만 **Rate Limit 인터셉터**가 걸려 있다.

---

## 미인증 처리

| 요청 종류 | 처리 방식 |
|----------|----------|
| `/apis/` 로 시작하는 API | HTTP 403 반환 |
| 일반 페이지 요청 | `/?login=required&redirect={원래경로}` 리다이렉트 |

---

## CORS 허용 출처

```
http(s)://localhost:*
http(s)://127.0.0.1:*
http(s)://teamsayai.com
http(s)://*.teamsayai.com
https://*.vercel.app
```

- `allowCredentials = true` → 쿠키 포함 요청 허용
- 허용 메서드: GET, POST, PUT, DELETE, OPTIONS

---

## 비밀번호 정책

- 인코더: `BCryptPasswordEncoder`
- 최소 조건: 8자 이상, 영문+숫자 조합 (프론트 Validation 기준)
- 변경: `Member.changePassword(String)` 메서드 사용

---

## Member 엔티티 구조

```java
@Table(name = "app_users")
public class Member {
    @Id
    private Long playerId;        // ft_players.seq 와 동일한 ID 사용

    private String userId;        // 로그인 ID (unique)
    private String name;          // 표시명
    private String password;      // BCrypt 해시
    private Role role;            // USER | ADMIN
}
```

> `playerId`가 PK이며, `ft_players`의 `seq`와 **같은 값**을 공유한다.  
> 선수 마스터와 회원이 1:1로 연결되는 구조.

---

## SecurityConfig 수정 시 주의사항

새로운 공개 API 추가 시:
```java
.requestMatchers("/apis/v1/new-endpoint/**").permitAll()
```

새로운 인증 필요 API 추가 시:
```java
.requestMatchers("/apis/v1/protected/**").authenticated()
```

`anyRequest().permitAll()` 이 마지막에 있으므로, 명시적으로 등록하지 않은 경로는  
인증 없이도 접근 가능하다. 보안이 필요한 경로는 반드시 명시 등록할 것.
