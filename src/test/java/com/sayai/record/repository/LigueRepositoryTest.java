package com.sayai.record.repository;

import com.sayai.record.model.Ligue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class LigueRepositoryTest {
    @Autowired
    LigueRepository ligueRepository;
    @Test
    void findByNameOrNameSec() {
        Optional<Ligue> lig = ligueRepository.findBySeasonAndNameOrNameSec(2022L,"토요독립루키");
        System.out.println(lig.get());
    }
}