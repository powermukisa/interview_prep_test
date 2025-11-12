package com.rillet.codingchallenge.accounting.domain;

import java.util.List;

public record RevenueAllocation (
        List<MonthlyAllocation> monthlyAllocations
) {
//validations
}
