package com.sayai.record.point;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_point")
@Entity
public class UserPoint {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private long balance;

    /** delta 만큼 증감. 잔액이 0 아래로 내려가면 예외. */
    public void adjust(long delta) {
        if (balance + delta < 0) {
            throw new IllegalArgumentException("포인트가 부족합니다. 현재: " + balance + ", 요청: " + delta);
        }
        balance += delta;
    }
}
