package com.sayai.kbo.dto;

import com.sayai.record.dto.PitcherDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
public class PitcherDetailResponse {
    private PitcherDto summary;
    private Page<PitcherDailyStatDto> dailyStats;
}
