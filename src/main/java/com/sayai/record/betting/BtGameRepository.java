package com.sayai.record.betting;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BtGameRepository extends JpaRepository<BtGame, Long> {
    List<BtGame> findByStatusOrderByDeadlineAsc(BtGame.Status status);
    List<BtGame> findByStatusOrderByIdDesc(BtGame.Status status);
    List<BtGame> findAllByOrderByIdDesc();

    /** 종료/취소 정산용: 동시 호출 시 이중 지급 방지 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from BtGame g where g.id = :id")
    Optional<BtGame> findForUpdate(@Param("id") Long id);
}
