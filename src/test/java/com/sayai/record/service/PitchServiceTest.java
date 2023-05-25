package com.sayai.record.service;

import com.sayai.record.dto.PitcherDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@Transactional
class PitchServiceTest {
    @Autowired
    private PitchService pitchService;
    @Test
    void select() {
        List<PitcherDto> pitcherDtos = pitchService.select(LocalDate.of(2023,01,01),LocalDate.of(2023,8,01));
        System.out.println("=======================");
    }

    @Test
    void selectOne(){
        PitcherDto pitcher = pitchService.selectOne(LocalDate.of(2023,01,01),LocalDate.of(2023,8,01), 11L);
        System.out.println(pitcher);
    }
}