# 엔티티 / 모델 컨벤션

## Lombok 어노테이션 패턴

엔티티마다 용도에 따라 다른 패턴을 사용한다.

### 불변 엔티티 (주로 기록/통계)
```java
@Entity
@Table(name = "kbo_hit")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 필수, 외부 생성 차단
@Getter                                               // setter 없음 → 불변
@Builder
public class KboHit { ... }
```

### 가변 엔티티 (상태가 바뀌는 도메인)
```java
@Entity
@Table(name = "ft_participants")
@Getter
@Setter                         // 상태 변경이 필요한 필드에만
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FantasyParticipant { ... }
```

### 규칙
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 는 모든 엔티티에 필수
- 외부에서 직접 `new`로 생성하지 않음 → 반드시 `Builder` 사용
- ID가 없는 변경 로직은 엔티티 내 메서드로 캡슐화 (`changePassword`, `setCost` 등)

---

## PK 전략

| 상황 | 전략 |
|------|------|
| 단일 Long PK | `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| 복합 PK (시즌 스탯 등) | `@IdClass` + 별도 ID 클래스 |
| 비즈니스 키 (game_idx 등) | `@Id` 직접 지정, 자동 생성 없음 |

복합 PK 예시 (`kbo_hitter_stats`):
```java
@Entity
@IdClass(KboHitterStatsId.class)
public class KboHitterStats {
    @Id @Column(name = "player_id") private Long playerId;
    @Id @Column(name = "season")    private Integer season;
}
```

---

## 컬럼 네이밍

- DB 컬럼: `snake_case`
- Java 필드: `camelCase`
- `@Column(name = "...")` 로 명시적으로 연결
- 컬럼 이름이 Java 컨벤션과 다를 때 반드시 `@Column(name)` 작성

---

## @ManyToOne / 연관관계

- 이 프로젝트는 **연관관계 매핑을 최소화**한다
- 대부분 `@Column`으로 FK 값(Long ID)만 보관
- 필요한 조인은 Repository 네이티브 쿼리에서 직접 처리
- `@ManyToOne`/`@OneToMany` 는 거의 사용하지 않음 → N+1 리스크 방지

---

## Enum 정의

엔티티 내부에 `inner enum`으로 정의하는 것이 기본 패턴.

```java
public class Member {
    public enum Role { USER, ADMIN }
}

public class FantasyGame {
    public enum GameStatus { WAITING, DRAFTING, ONGOING, FINISHED }
    public enum RuleType   { RULE_1, RULE_2 }
    public enum ScoringType { POINT, ROTISSERIE }
}
```

DB 저장: `@Enumerated(EnumType.STRING)` (숫자 저장 금지)

---

## 통계 수치 타입 규칙

| 필드 유형 | 타입 | 이유 |
|----------|------|------|
| 경기별 누적 수치 (pa, ab, hit ...) | `Long` | KboHit 등 raw 기록 |
| 시즌 누적 수치 (pa, ab, hit ...) | `Integer` | KboHitterStats 저장 |
| 타율/ERA/WHIP (문자 저장) | `String` (VARCHAR 10) | 소수점 포맷 유지 |
| 이닝 (outs 기준) | `Integer` outs = 아웃카운트, inning = 이닝 수 |
| 계산된 ERA/WHIP (응답용) | `Double` | DTO에서 파싱 후 반환 |

> **outs vs inning**: `kbo_pitcher_stats.outs` = 아웃카운트(이닝×3),  
> `kbo_pitch.inning` = 정수 이닝 수. 혼용 주의.

---

## @PrePersist 활용

기본값 설정은 `@PrePersist`에서 처리한다.

```java
@PrePersist
public void prePersist() {
    if (this.role == null) this.role = Role.USER;
}
```

`@Column(columnDefinition = "int default 0")` 와 병행 사용 가능하지만,  
Java 레벨 기본값은 `@PrePersist` 또는 필드 초기화로 처리한다.
