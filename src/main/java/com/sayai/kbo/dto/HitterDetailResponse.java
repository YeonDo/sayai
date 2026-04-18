package com.sayai.kbo.dto;

import com.sayai.record.dto.PlayerDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
public class HitterDetailResponse {
    private PlayerDto summary;
    private Page<HitterDailyStatDto> dailyStats;
}
