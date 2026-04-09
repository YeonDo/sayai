package com.sayai.record.fantasy.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WaiverOrderDto {
    private String teamName;
    private Integer orderNum;
}
