package com.sayai.record.service;

import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class HitServiceTest {
    @Autowired
    private HitService hitService;
    @Test
    void findOne() {
        PlayerDto one = hitService.findOne(LocalDate.of(2023, 01, 01), LocalDate.of(2023, 06, 01), 2L);

    }

    @Test
    void findAllByPeriod() {
        List<PlayerDto> allByPeriod = hitService.findAllByPeriod(LocalDate.of(2023, 01, 01), LocalDate.of(2023, 06, 01));
        for(PlayerDto dto: allByPeriod){
            System.out.println(dto.toString());
        }
    }
}