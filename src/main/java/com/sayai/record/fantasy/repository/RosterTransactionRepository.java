package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.RosterTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RosterTransactionRepository extends JpaRepository<RosterTransaction, Long> {
    List<RosterTransaction> findByFantasyGameSeqAndStatus(Long fantasyGameSeq, RosterTransaction.TransactionStatus status);
}
