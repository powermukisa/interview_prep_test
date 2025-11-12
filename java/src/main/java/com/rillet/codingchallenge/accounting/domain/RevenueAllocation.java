package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.List;
import java.util.Objects;

/**
 * Represents a request to allocate annual revenue across periods.
 *
 */
public record RevenueAllocation(
        MonetaryAmount annualAmount,
        List<MonthlyAllocation> monthlyAllocations,
        MonetaryAmount baseMonthlyAmount,
        MonetaryAmount roundingAdjustment,
        RoundingPlacement placement
) {

    public RevenueAllocation {
        Objects.requireNonNull(annualAmount, "Annual amount cannot be null");
        Objects.requireNonNull(monthlyAllocations, "Monthly allocations cannot be null");

        if (monthlyAllocations.size() != 12) {
            throw new IllegalArgumentException("Must have exactly 12 monthly allocations, got: " + monthlyAllocations.size());
        }

        //invariant enforcement
        MonetaryAmount sum = monthlyAllocations.stream()
                .map(MonthlyAllocation::amount)
                .reduce(MonetaryAmount::add)
                .orElseThrow(() -> new IllegalArgumentException("Failed to calculate sum"));

        if (!sum.isEqualTo(annualAmount)) {
            throw new IllegalStateException("Invariant violated: Sum of allocation.. ");
        }
    }

}
