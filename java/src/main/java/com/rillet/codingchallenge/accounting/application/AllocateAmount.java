package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.AllocationCalculator;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;
import org.springframework.stereotype.Service;

import javax.money.MonetaryAmount;

@Service
public class AllocateAmount {
    private final AllocationCalculator calculator;

    public AllocateAmount(AllocationCalculator calculator) {
        this.calculator = calculator;
    }

    public RevenueAllocation execute(MonetaryAmount amount, RoundingPlacement roundingPlacement) {
        return calculator.calculate(amount, roundingPlacement);
    }
}
