package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.DraftPickSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DraftPickSnapshotRepository extends JpaRepository<DraftPickSnapshot, Long> {
    List<DraftPickSnapshot> findByFantasyGameSeq(Long fantasyGameSeq);

    @Transactional
    void deleteByFantasyGameSeq(Long fantasyGameSeq);
}
