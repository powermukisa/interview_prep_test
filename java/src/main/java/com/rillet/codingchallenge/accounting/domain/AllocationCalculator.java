package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public class AllocationCalculator {
    private static final int MONTHS_IN_YEAR = 12;

    public RevenueAllocation calculate(RevenueRecognitionRequest request) {
        MonetaryAmount amount = request.amount();
        RoundingPlacement placement = request.roundingPlacement();

        // Step 1: Calculate base monthly amount with proper rounding
        // WHY ROUND: Ensures amount respects currency precision (e.g., 2 decimals for USD)
        MonetaryAmount baseMonthlyAmount = CurrenciesHelper.rounded(
                amount.divide(MONTHS_IN_YEAR)
        );

        // Step 2: Determine which month gets the adjustment
        int adjustmentIndex = placement.calculateAdjustmentIndex(MONTHS_IN_YEAR);

        // Step 3: Calculate what would be allocated to 11 base months
        // WHY: This tells us how much to allocate to the other months
        MonetaryAmount elevenMonthsTotal = baseMonthlyAmount.multiply(MONTHS_IN_YEAR - 1);

        // Step 4: Remainder goes to the adjustment month
        // WHY: This guarantees sum = annual (no rounding error)
        // The adjusted month gets: annual - (base × 11)
        MonetaryAmount adjustedMonthAmount = amount.subtract(elevenMonthsTotal);

        // Step 5: Calculate the adjustment for metadata
        // This is the "extra" amount the adjusted month receives
        MonetaryAmount roundingAdjustment = adjustedMonthAmount.subtract(baseMonthlyAmount);

        // Step 6: Build monthly allocations
        List<MonthlyAllocation> allocations = buildAllocations(
                baseMonthlyAmount,
                adjustedMonthAmount,
                adjustmentIndex
        );

        // Step 7: Create and return aggregate
        // NOTE: The aggregate constructor validates that sum = annual
        // If our algorithm is wrong, we'll get an exception here!
        return new RevenueAllocation(
                amount,
                allocations,
                baseMonthlyAmount,
                roundingAdjustment,
                placement
        );
    }

    private List<MonthlyAllocation> buildAllocations(
            MonetaryAmount baseAmount,
            MonetaryAmount adjustedAmount,
            int adjustmentIndex
    ) {
        List<MonthlyAllocation> allocations = new ArrayList<>(MONTHS_IN_YEAR);
        Month[] months = Month.values();

        for (int i = 0; i < MONTHS_IN_YEAR; i++) {
            if (i == adjustmentIndex) {
                // This month gets the adjusted amount
                allocations.add(new MonthlyAllocation(months[i], adjustedAmount));
            } else {
                // Standard month gets base amount
                allocations.add(new MonthlyAllocation(months[i], baseAmount));
            }
        }

        return allocations;
    }
}
