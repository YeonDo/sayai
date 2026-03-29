package com.sayai.kbo.repository;

import com.sayai.kbo.model.KboGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KboGameRepository extends JpaRepository<KboGame, Long> {
    @Query("SELECT MAX(g.id) FROM KboGame g")
    Long findMaxId();
}
