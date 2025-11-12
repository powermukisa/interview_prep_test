package com.rillet.codingchallenge.accounting.infra.dataclasses;

import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.stream.Collectors;

public record ResponseDto(
        List<MonthlyAllocation> allocations,
        MonetaryAmount baseMonthlyAmount,
        MonetaryAmount remainder,
        MonetaryAmount total
) {
    public record MonthlyAmountDto(
            String month,
            MonetaryAmount amount
    ) {}

    public static ResponseDto fromDomain(RevenueAllocation allocation) {
        return new ResponseDto(
                allocation.monthlyAllocations(),    // Pass through directly!
                allocation.baseMonthlyAmount(),
                allocation.roundingAdjustment(),
                allocation.annualAmount()
        );
    }
}
