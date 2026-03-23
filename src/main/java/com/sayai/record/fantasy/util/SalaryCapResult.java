package com.sayai.record.fantasy.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SalaryCapResult {
    private int totalCost;
    private Long discountedPlayerSeq;
    private int originalCost;
    private int discountedCost;
}
