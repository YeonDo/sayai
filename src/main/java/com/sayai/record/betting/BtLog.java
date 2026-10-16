package com.sayai.record.betting;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "bt_log")
@Entity
public class BtLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bt_game_id", nullable = false)
    private Long btGameId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 객관식=문항 index, 주관식=숫자 문자열 */
    @Column(nullable = false, length = 100)
    private String answer;

    @Column(nullable = false)
    private long amount;

    /** 정산 결과 지급 포인트 (적중 배당 또는 환불). 정산 전 null, 낙첨 0. */
    @Setter
    private Long payout;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
