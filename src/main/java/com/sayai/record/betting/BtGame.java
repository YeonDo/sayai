package com.sayai.record.betting;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "bt_games")
@Entity
public class BtGame {

    public enum Type { CHOICE, NUMBER }
    public enum Status { OPEN, CLOSED, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** 객관식 문항 (JSON 배열 문자열). 주관식은 null. */
    @Column(columnDefinition = "TEXT")
    private String choices;

    /** 마감 시각 (UTC). */
    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /** 정답. 객관식=문항 index, 주관식=숫자 문자열. 종료 전 null. */
    @Column(length = 100)
    private String answer;

    private Instant createdAt;
    private Instant closedAt;

    public boolean isBettable(Instant now) {
        return status == Status.OPEN && now.isBefore(deadline);
    }

    public void close(String answer, Instant now) {
        if (status != Status.OPEN) {
            throw new IllegalStateException("이미 종료/취소된 베팅입니다: " + status);
        }
        this.answer = answer;
        this.status = Status.CLOSED;
        this.closedAt = now;
    }

    public void cancel(Instant now) {
        if (status != Status.OPEN) {
            throw new IllegalStateException("이미 종료/취소된 베팅입니다: " + status);
        }
        this.status = Status.CANCELED;
        this.closedAt = now;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = Status.OPEN;
    }
}
