package com.sayai.record.repository;

import com.sayai.record.model.Hit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HitRepository extends JpaRepository<Hit, Long> {
}
