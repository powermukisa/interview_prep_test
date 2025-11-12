package com.rillet.codingchallenge.accounting.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;


public class AllocationCalculator {
    static int MONTHS_IN_YEAR = 12;

    public RevenueAllocation calculate(MonetaryAmount amount, RoundingPlacement placement) {

        MonetaryAmount baseMonthlyAmount = CurrenciesHelper.rounded(amount.divide(MONTHS_IN_YEAR));

        MonetaryAmount elevenMonthsTotal = baseMonthlyAmount.multiply(MONTHS_IN_YEAR - 1);
        //916.63

        MonetaryAmount adjustedAmount = amount.subtract(elevenMonthsTotal);
        //83.37

        List<MonthlyAllocation> allocations = buildAllocations(baseMonthlyAmount, adjustedAmount, placement);
        return new RevenueAllocation(allocations);
    }

    private List<MonthlyAllocation> buildAllocations(MonetaryAmount baseMonthlyAmount, MonetaryAmount adjustedAmount, RoundingPlacement placement) {
        int adjustmentIndex = placement.calculateAdjustmentIndex();

        List<MonthlyAllocation> allocations = new ArrayList<>(MONTHS_IN_YEAR);
        Month[] months = Month.values();

        for (int i = 0; i < MONTHS_IN_YEAR; i++) {
            if (i == adjustmentIndex) {
                allocations.add(new MonthlyAllocation(months[i], adjustedAmount));
            }
            else {
                allocations.add(new MonthlyAllocation(months[i], baseMonthlyAmount));
            }
        }
        return allocations;
    }
}
