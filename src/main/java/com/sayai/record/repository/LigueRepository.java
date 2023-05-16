package com.sayai.record.repository;

import com.sayai.record.model.Ligue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LigueRepository extends JpaRepository<Ligue, Long> {

    Optional<Ligue> findByName(String name);
}
