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
        System.out.println(one.toString());
    }

    @Test
    void findAllByPeriod() {
        List<PlayerDto> allByPeriod = hitService.findAllByPeriod(LocalDate.of(2023, 01, 01), LocalDate.of(2023, 06, 01));
        for(PlayerDto dto: allByPeriod){
            System.out.println(dto.toString());
        }
    }

    @Test
    void findAllByPeriodWithName() {
        LocalDate start = LocalDate.of(2023, 01, 01);
        LocalDate end = LocalDate.of(2023, 12, 31);

        List<PlayerDto> all = hitService.findAllByPeriod(start, end);
        if (all.isEmpty()) {
            return;
        }

        String targetName = all.get(0).getName();
        List<PlayerDto> filtered = hitService.findAllByPeriod(start, end, targetName);

        assertFalse(filtered.isEmpty());
        for (PlayerDto p : filtered) {
            assertTrue(p.getName().contains(targetName));
        }
    }

}