package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.AllocationCalculator;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import org.springframework.stereotype.Service;

@Service
public class AllocateAmountUseCase implements AllocateAmount {
    private final AllocationCalculator calculator;

    /**
     * Constructor injection (preferred over field injection)
     *
     * WHY CONSTRUCTOR INJECTION:
     * - Testable (can pass dependencies in tests)
     * - Immutable (field is final)
     * - Clear dependencies
     * - Fails fast if dependency missing
     */
    public AllocateAmountUseCase(AllocationCalculator calculator) {
        this.calculator = calculator;
    }

    /**
     * Executes the revenue allocation use case.
     *
     * ORCHESTRATION:
     * 1. Request validation (done by domain object)
     * 2. Calculate allocation (delegate to domain service)
     * 3. (Future: Save to repository)
     * 4. Return result
     *
     * WHY SO SIMPLE:
     * This is intentional! Application services should orchestrate,
     * not contain business logic. The calculator has the business rules.
     */
    @Override
    public RevenueAllocation execute(RevenueRecognitionRequest request) {
        // Request is self-validating (domain value object)

        // Delegate to domain service for business logic
        return calculator.calculate(request);
    }
}