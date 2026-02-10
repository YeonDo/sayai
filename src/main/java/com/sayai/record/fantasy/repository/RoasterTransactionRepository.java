package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.RoasterTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoasterTransactionRepository extends JpaRepository<RoasterTransaction, Long> {
    List<RoasterTransaction> findByFantasyGameSeqAndStatus(Long fantasyGameSeq, RoasterTransaction.TransactionStatus status);
}
