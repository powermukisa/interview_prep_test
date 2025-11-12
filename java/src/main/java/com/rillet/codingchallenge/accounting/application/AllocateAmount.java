package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;

public interface AllocateAmount {
    /**
     * Allocates annual revenue into monthly recognition periods.
     *
     * @param request The revenue recognition parameters
     * @return The complete allocation aggregate
     * @throws IllegalArgumentException if request is invalid
     */
    RevenueAllocation execute(RevenueRecognitionRequest request);
}
