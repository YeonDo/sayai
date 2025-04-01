package com.sayai.record.repository;

import com.sayai.record.model.PitcherBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PitcherBoardRepository extends JpaRepository<PitcherBoard, Long> {
}
