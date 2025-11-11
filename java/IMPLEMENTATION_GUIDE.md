# Rillet Revenue Recognition - TDD Implementation Guide

This guide provides a step-by-step walkthrough for implementing the revenue recognition solution following TDD best practices for the Rillet pair programming interview.

## Table of Contents
1. [Phase 0: Setup Verification](#phase-0-setup-verification)
2. [Phase 1: Domain Modeling](#phase-1-domain-modeling)
3. [Phase 2: Core Algorithm (TDD Cycles)](#phase-2-core-algorithm-tdd-cycles)
4. [Phase 3: Edge Cases & Validation](#phase-3-edge-cases--validation)
5. [Phase 4: API & DTO Layer](#phase-4-api--dto-layer)
6. [Phase 5: UI Enhancement](#phase-5-ui-enhancement)
7. [Phase 6: Final Polish](#phase-6-final-polish)

---

## Phase 0: Setup Verification

### Verify Tests Run
```bash
cd /Users/power/code/rillet/coding-challenge-main/java
./gradlew test
```

Expected: BUILD SUCCESSFUL

---

## Phase 1: Domain Modeling

### Step 1.1: Create RoundingPlacement Enum

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RoundingPlacement.java`

```java
package com.rillet.codingchallenge.accounting.domain;

/**
 * Defines where the rounding difference should be placed when splitting
 * an annual amount into monthly amounts.
 * <p>
 * When dividing amounts that don't split evenly (e.g., $1000 / 12),
 * there will be a remainder that needs to be added to one of the months.
 */
public enum RoundingPlacement {
    /**
     * Add the rounding difference to the first month (January or start month)
     */
    FIRST,
    
    /**
     * Add the rounding difference to the last month (December or end month)
     * This is the default behavior.
     */
    LAST,
    
    /**
     * Add the rounding difference to the middle month (June or 6 months from start)
     */
    MIDDLE
}
```

### Step 1.2: Create MonthlyAllocation Record

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/MonthlyAllocation.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;

/**
 * Represents a single month's allocated revenue amount.
 * 
 * @param month The month this allocation applies to
 * @param amount The monetary amount allocated to this month
 * @param hasRemainder Whether this month received the rounding adjustment
 */
public record MonthlyAllocation(
    Month month,
    MonetaryAmount amount,
    boolean hasRemainder
) {
    /**
     * Creates a monthly allocation.
     */
    public MonthlyAllocation {
        if (month == null) {
            throw new IllegalArgumentException("Month cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
    }
    
    /**
     * Returns the month name for display purposes.
     */
    public String getMonthName() {
        return month.name();
    }
}
```

### Step 1.3: Create RevenueAllocationResult Record

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RevenueAllocationResult.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.util.List;

/**
 * Contains the complete result of splitting an annual amount into monthly allocations.
 * 
 * @param allocations List of 12 monthly allocations
 * @param baseMonthlyAmount The base amount allocated to most months (before adjustment)
 * @param remainder The rounding difference that was allocated
 * @param totalAmount The original total amount (should equal sum of allocations)
 */
public record RevenueAllocationResult(
    List<MonthlyAllocation> allocations,
    MonetaryAmount baseMonthlyAmount,
    MonetaryAmount remainder,
    MonetaryAmount totalAmount
) {
    private static final int MONTHS_IN_YEAR = 12;
    
    /**
     * Creates a revenue allocation result with validation.
     */
    public RevenueAllocationResult {
        if (allocations == null || allocations.size() != MONTHS_IN_YEAR) {
            throw new IllegalArgumentException("Must have exactly 12 monthly allocations");
        }
        if (baseMonthlyAmount == null || remainder == null || totalAmount == null) {
            throw new IllegalArgumentException("All monetary amounts must be non-null");
        }
        
        // Verify sum equals total
        MonetaryAmount sum = allocations.stream()
            .map(MonthlyAllocation::amount)
            .reduce(MonetaryAmount::add)
            .orElseThrow(() -> new IllegalStateException("Failed to calculate sum"));
        
        if (!sum.isEqualTo(totalAmount)) {
            throw new IllegalStateException(
                String.format("Sum of allocations (%s) does not equal total (%s)", sum, totalAmount)
            );
        }
    }
}
```

**Run tests to ensure compilation:**
```bash
./gradlew test
```

---

## Phase 2: Core Algorithm (TDD Cycles)

### Step 2.1: RED - First Test (Neat Division)

**File:** `src/test/java/com/rillet/codingchallenge/accounting/application/AllocateAmountTest.java`

```java
package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocationResult;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;
import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmount;
import java.time.Month;

import static com.rillet.codingchallenge.accounting.Helpers.dollars;
import static org.assertj.core.api.Assertions.assertThat;

class AllocateAmountTest {
    private final AllocateAmount useCase = new AllocateAmount();

    @Test
    void shouldSplitNeatlyDivisibleAmountIntoTwelveEqualMonths() {
        // Given: An amount that divides evenly by 12
        MonetaryAmount annualAmount = dollars(1200);
        
        // When: We split it into monthly amounts
        RevenueAllocationResult result = useCase.execute(annualAmount);
        
        // Then: We get 12 equal amounts of $100
        assertThat(result.allocations()).hasSize(12);
        assertThat(result.allocations())
            .allMatch(allocation -> allocation.amount().isEqualTo(dollars(100)));
        
        // And: Base amount is $100
        assertThat(result.baseMonthlyAmount()).isEqualTo(dollars(100));
        
        // And: No remainder
        assertThat(result.remainder()).isEqualTo(dollars(0));
        
        // And: Total equals original
        assertThat(result.totalAmount()).isEqualTo(annualAmount);
        
        // And: Months are in order starting from January
        assertThat(result.allocations().get(0).month()).isEqualTo(Month.JANUARY);
        assertThat(result.allocations().get(11).month()).isEqualTo(Month.DECEMBER);
    }
}
```

**Run tests (they should FAIL):**
```bash
./gradlew test
```

### Step 2.2: GREEN - Make First Test Pass

**File:** `src/main/java/com/rillet/codingchallenge/accounting/application/AllocateAmount.java`

```java
package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.CurrenciesHelper;
import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocationResult;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;
import org.springframework.stereotype.Service;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Service
public class AllocateAmount {
    private static final int MONTHS_IN_YEAR = 12;

    /**
     * Splits an annual amount into 12 monthly amounts.
     * Default: Remainder goes to the LAST month.
     * 
     * @param amount The total annual amount to split
     * @return RevenueAllocationResult containing 12 monthly allocations
     */
    public RevenueAllocationResult execute(MonetaryAmount amount) {
        return execute(amount, RoundingPlacement.LAST);
    }

    /**
     * Splits an annual amount into 12 monthly amounts with configurable rounding placement.
     * 
     * @param amount The total annual amount to split
     * @param placement Where to place the rounding difference (FIRST, LAST, or MIDDLE)
     * @return RevenueAllocationResult containing 12 monthly allocations
     */
    public RevenueAllocationResult execute(MonetaryAmount amount, RoundingPlacement placement) {
        validateInput(amount, placement);
        
        // Calculate base monthly amount with proper rounding
        MonetaryAmount baseMonthlyAmount = CurrenciesHelper.rounded(
            amount.divide(MONTHS_IN_YEAR)
        );
        
        // Calculate the target index for the rounding difference
        int adjustmentIndex = calculateAdjustmentIndex(placement);
        
        // Calculate allocated amount for all base months (11 months)
        MonetaryAmount totalAllocated = baseMonthlyAmount.multiply(MONTHS_IN_YEAR - 1);
        
        // Remainder goes to the adjustment month
        MonetaryAmount adjustedMonthAmount = amount.subtract(totalAllocated);
        
        // Calculate remainder for metadata
        MonetaryAmount remainder = adjustedMonthAmount.subtract(baseMonthlyAmount);
        
        // Build the result list
        List<MonthlyAllocation> allocations = new ArrayList<>(MONTHS_IN_YEAR);
        Month[] months = Month.values();
        
        for (int i = 0; i < MONTHS_IN_YEAR; i++) {
            if (i == adjustmentIndex) {
                allocations.add(new MonthlyAllocation(
                    months[i],
                    adjustedMonthAmount,
                    !remainder.isZero()
                ));
            } else {
                allocations.add(new MonthlyAllocation(
                    months[i],
                    baseMonthlyAmount,
                    false
                ));
            }
        }
        
        return new RevenueAllocationResult(
            allocations,
            baseMonthlyAmount,
            remainder,
            amount
        );
    }
    
    private void validateInput(MonetaryAmount amount, RoundingPlacement placement) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (placement == null) {
            throw new IllegalArgumentException("Rounding placement cannot be null");
        }
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    private int calculateAdjustmentIndex(RoundingPlacement placement) {
        return switch (placement) {
            case FIRST -> 0;
            case LAST -> MONTHS_IN_YEAR - 1;
            case MIDDLE -> 5; // June (0-indexed)
        };
    }
}
```

**Run tests (should PASS):**
```bash
./gradlew test
```

### Step 2.3: RED - Test Rounding with Remainder (LAST)

Add this test to `AllocateAmountTest.java`:

```java
    @Test
    void shouldHandleRoundingDifferenceByAddingToLastMonth() {
        // Given: An amount that doesn't divide evenly
        MonetaryAmount annualAmount = dollars(1000);
        
        // When: We split it with default LAST placement
        RevenueAllocationResult result = useCase.execute(annualAmount);
        
        // Then: We get 12 months
        assertThat(result.allocations()).hasSize(12);
        
        // And: First 11 months have the base amount
        for (int i = 0; i < 11; i++) {
            assertThat(result.allocations().get(i).amount()).isEqualTo(dollars(83.33));
            assertThat(result.allocations().get(i).hasRemainder()).isFalse();
        }
        
        // And: Last month (December) has the adjusted amount
        MonthlyAllocation lastMonth = result.allocations().get(11);
        assertThat(lastMonth.amount()).isEqualTo(dollars(83.37));
        assertThat(lastMonth.hasRemainder()).isTrue();
        assertThat(lastMonth.month()).isEqualTo(Month.DECEMBER);
        
        // And: Base amount is $83.33
        assertThat(result.baseMonthlyAmount()).isEqualTo(dollars(83.33));
        
        // And: Remainder is $0.04
        assertThat(result.remainder()).isEqualTo(dollars(0.04));
        
        // And: Sum equals original
        assertThat(result.totalAmount()).isEqualTo(annualAmount);
    }
```

**Run tests (should PASS if implementation is correct):**
```bash
./gradlew test
```

### Step 2.4: RED - Test FIRST Placement

Add this test to `AllocateAmountTest.java`:

```java
    @Test
    void shouldPlaceRoundingDifferenceInFirstMonth() {
        // Given: An amount that doesn't divide evenly
        MonetaryAmount annualAmount = dollars(1000);
        
        // When: We specify FIRST placement
        RevenueAllocationResult result = useCase.execute(annualAmount, RoundingPlacement.FIRST);
        
        // Then: First month (January) has the adjusted amount
        MonthlyAllocation firstMonth = result.allocations().get(0);
        assertThat(firstMonth.amount()).isEqualTo(dollars(83.37));
        assertThat(firstMonth.hasRemainder()).isTrue();
        assertThat(firstMonth.month()).isEqualTo(Month.JANUARY);
        
        // And: Remaining 11 months have base amount
        for (int i = 1; i < 12; i++) {
            assertThat(result.allocations().get(i).amount()).isEqualTo(dollars(83.33));
            assertThat(result.allocations().get(i).hasRemainder()).isFalse();
        }
        
        // And: Sum equals original
        assertThat(result.totalAmount()).isEqualTo(annualAmount);
    }
```

**Run tests (should PASS):**
```bash
./gradlew test
```

### Step 2.5: RED - Test MIDDLE Placement

Add this test to `AllocateAmountTest.java`:

```java
    @Test
    void shouldPlaceRoundingDifferenceInMiddleMonth() {
        // Given: An amount that doesn't divide evenly
        MonetaryAmount annualAmount = dollars(1000);
        
        // When: We specify MIDDLE placement
        RevenueAllocationResult result = useCase.execute(annualAmount, RoundingPlacement.MIDDLE);
        
        // Then: Middle month (June, index 5) has the adjusted amount
        MonthlyAllocation middleMonth = result.allocations().get(5);
        assertThat(middleMonth.amount()).isEqualTo(dollars(83.37));
        assertThat(middleMonth.hasRemainder()).isTrue();
        assertThat(middleMonth.month()).isEqualTo(Month.JUNE);
        
        // And: Other months have base amount
        for (int i = 0; i < 12; i++) {
            if (i != 5) {
                assertThat(result.allocations().get(i).amount()).isEqualTo(dollars(83.33));
                assertThat(result.allocations().get(i).hasRemainder()).isFalse();
            }
        }
        
        // And: Sum equals original
        assertThat(result.totalAmount()).isEqualTo(annualAmount);
    }
```

**Run tests (should PASS):**
```bash
./gradlew test
```

---

## Phase 3: Edge Cases & Validation

### Step 3.1: Add Edge Case Tests

Add these tests to `AllocateAmountTest.java`:

```java
    @Test
    void shouldHandleVerySmallAmounts() {
        // Given: A very small amount
        MonetaryAmount annualAmount = dollars(0.05);
        
        // When: We split it
        RevenueAllocationResult result = useCase.execute(annualAmount);
        
        // Then: Sum still equals original (no precision loss)
        MonetaryAmount sum = result.allocations().stream()
            .map(MonthlyAllocation::amount)
            .reduce(MonetaryAmount::add)
            .orElseThrow();
        assertThat(sum).isEqualTo(annualAmount);
    }
    
    @Test
    void shouldHandleZeroAmount() {
        // Given: Zero amount
        MonetaryAmount annualAmount = dollars(0);
        
        // When: We split it
        RevenueAllocationResult result = useCase.execute(annualAmount);
        
        // Then: All months are zero
        assertThat(result.allocations())
            .allMatch(allocation -> allocation.amount().isZero());
        
        // And: No remainder
        assertThat(result.remainder()).isEqualTo(dollars(0));
    }
    
    @Test
    void shouldHandleAmountsWithManyCents() {
        // Given: An amount with cents
        MonetaryAmount annualAmount = dollars(1234.56);
        
        // When: We split it
        RevenueAllocationResult result = useCase.execute(annualAmount);
        
        // Then: Sum equals original exactly
        MonetaryAmount sum = result.allocations().stream()
            .map(MonthlyAllocation::amount)
            .reduce(MonetaryAmount::add)
            .orElseThrow();
        assertThat(sum).isEqualTo(annualAmount);
        
        // And: We have exactly 12 allocations
        assertThat(result.allocations()).hasSize(12);
    }
    
    @Test
    void shouldRejectNullAmount() {
        // When/Then: Null amount throws exception
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> 
            useCase.execute(null)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount cannot be null");
    }
    
    @Test
    void shouldRejectNegativeAmount() {
        // Given: Negative amount
        MonetaryAmount negativeAmount = dollars(-100);
        
        // When/Then: Negative amount throws exception
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> 
            useCase.execute(negativeAmount)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount cannot be negative");
    }
    
    @Test
    void shouldRejectNullPlacement() {
        // Given: Valid amount but null placement
        MonetaryAmount amount = dollars(1000);
        
        // When/Then: Null placement throws exception
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> 
            useCase.execute(amount, null)
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rounding placement cannot be null");
    }
```

**Run tests (should PASS):**
```bash
./gradlew test
```

---

## Phase 4: API & DTO Layer

### Step 4.1: Update RequestDto

**File:** `src/main/java/com/rillet/codingchallenge/accounting/infra/dataclasses/RequestDto.java`

```java
package com.rillet.codingchallenge.accounting.infra.dataclasses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;

import javax.money.MonetaryAmount;

/**
 * Request DTO for revenue allocation.
 * 
 * @param amount The annual amount to allocate
 * @param roundingPlacement Optional placement strategy (defaults to LAST)
 */
public record RequestDto(
    MonetaryAmount amount,
    RoundingPlacement roundingPlacement
) {
    /**
     * Constructor with defaults for backward compatibility.
     */
    @JsonCreator
    public RequestDto(
        @JsonProperty("amount") MonetaryAmount amount,
        @JsonProperty("roundingPlacement") RoundingPlacement roundingPlacement
    ) {
        this.amount = amount;
        this.roundingPlacement = roundingPlacement != null ? roundingPlacement : RoundingPlacement.LAST;
    }
    
    /**
     * Simple constructor with only amount (uses LAST placement).
     */
    public RequestDto(MonetaryAmount amount) {
        this(amount, RoundingPlacement.LAST);
    }
}
```

### Step 4.2: Update ResponseDto

**File:** `src/main/java/com/rillet/codingchallenge/accounting/infra/dataclasses/ResponseDto.java`

```java
package com.rillet.codingchallenge.accounting.infra.dataclasses;

import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocationResult;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for revenue allocation results.
 * 
 * @param allocations List of monthly amounts with metadata
 * @param baseMonthlyAmount The base amount for most months
 * @param remainder The rounding adjustment that was applied
 * @param total The total amount (should equal sum of allocations)
 */
public record ResponseDto(
    List<MonthlyAmountDto> allocations,
    MonetaryAmount baseMonthlyAmount,
    MonetaryAmount remainder,
    MonetaryAmount total
) {
    /**
     * Nested DTO for individual month allocation.
     */
    public record MonthlyAmountDto(
        String month,
        MonetaryAmount amount,
        boolean hasRemainder
    ) {}

    /**
     * Converts domain result to response DTO.
     */
    public static ResponseDto from(RevenueAllocationResult result) {
        List<MonthlyAmountDto> dtoAllocations = result.allocations().stream()
            .map(allocation -> new MonthlyAmountDto(
                allocation.getMonthName(),
                allocation.amount(),
                allocation.hasRemainder()
            ))
            .collect(Collectors.toList());
        
        return new ResponseDto(
            dtoAllocations,
            result.baseMonthlyAmount(),
            result.remainder(),
            result.totalAmount()
        );
    }
}
```

### Step 4.3: Update Controller

**File:** `src/main/java/com/rillet/codingchallenge/accounting/infra/AmountsController.java`

```java
package com.rillet.codingchallenge.accounting.infra;

import com.rillet.codingchallenge.accounting.application.AllocateAmount;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocationResult;
import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto;
import com.rillet.codingchallenge.accounting.infra.dataclasses.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for revenue allocation endpoints.
 */
@RestController
@RequestMapping("/amounts")
public class AmountsController {
    private final AllocateAmount allocateAmount;

    public AmountsController(AllocateAmount allocateAmount) {
        this.allocateAmount = allocateAmount;
    }

    /**
     * Allocates an annual amount into monthly amounts.
     * 
     * @param request Contains the amount and optional rounding placement
     * @return Response with 12 monthly allocations and metadata
     */
    @PostMapping
    public ResponseEntity<ResponseDto> createAmounts(@RequestBody RequestDto request) {
        try {
            RevenueAllocationResult result = allocateAmount.execute(
                request.amount(),
                request.roundingPlacement()
            );
            return ResponseEntity.ok(ResponseDto.from(result));
        } catch (IllegalArgumentException e) {
            // Return 400 for validation errors
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Return 500 for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### Step 4.4: Add Controller Test

**File:** `src/test/java/com/rillet/codingchallenge/accounting/infra/AmountsControllerTest.java`

```java
package com.rillet.codingchallenge.accounting.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AmountsControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldReturnMonthlyAllocationsForValidRequest() throws Exception {
        // Given: A valid request body
        String requestBody = """
            {
                "amount": {
                    "value": 1200,
                    "currency": "USD"
                }
            }
            """;
        
        // When: We POST to /amounts
        mockMvc.perform(post("/amounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            // Then: We get 200 OK
            .andExpect(status().isOk())
            // And: Response contains allocations array with 12 items
            .andExpect(jsonPath("$.allocations").isArray())
            .andExpect(jsonPath("$.allocations.length()").value(12))
            // And: Response contains metadata
            .andExpect(jsonPath("$.baseMonthlyAmount").exists())
            .andExpect(jsonPath("$.remainder").exists())
            .andExpect(jsonPath("$.total").exists());
    }
    
    @Test
    void shouldHandleRoundingPlacementParameter() throws Exception {
        // Given: A request with FIRST placement
        String requestBody = """
            {
                "amount": {
                    "value": 1000,
                    "currency": "USD"
                },
                "roundingPlacement": "FIRST"
            }
            """;
        
        // When: We POST to /amounts
        mockMvc.perform(post("/amounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            // Then: We get 200 OK
            .andExpect(status().isOk())
            // And: First month has remainder
            .andExpect(jsonPath("$.allocations[0].hasRemainder").value(true))
            .andExpect(jsonPath("$.allocations[0].month").value("JANUARY"));
    }
}
```

**Run all tests:**
```bash
./gradlew test
```

---

## Phase 5: UI Enhancement

### Step 5.1: Update index.html

**File:** `src/main/resources/public/index.html`

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <script src="https://cdn.tailwindcss.com"></script>
    <title>Rillet Revenue Recognition</title>
  </head>
  <body class="bg-slate-900">
    <div class="space-y-6 bg-slate-900 text-white mx-12 py-8">
      <div class="mb-8">
        <h1 class="text-3xl font-bold">Revenue Recognition Calculator</h1>
        <p class="text-gray-400 mt-2">Split annual revenue into monthly allocations</p>
      </div>

      <form
        id="calculate-amounts"
        action="http://localhost:8080/amounts"
        method="post"
        class="space-y-6"
      >
        <div class="border border-gray-700 rounded-lg p-6">
          <h2 class="text-xl font-semibold mb-4">Input Parameters</h2>
          
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <!-- Amount Input -->
            <div>
              <label for="amount" class="block text-sm font-medium text-gray-300 mb-2">
                Annual Amount
              </label>
              <div class="flex rounded-md shadow-sm">
                <div class="text-sm bg-gray-800 pt-2 px-3 border border-r-0 border-gray-600 rounded-l">
                  USD
                </div>
                <input
                  required
                  type="number"
                  name="amount"
                  min="0"
                  step="0.01"
                  id="amount"
                  class="block flex-1 border border-gray-600 bg-gray-800 py-2 px-4 text-white placeholder:text-gray-500 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-r [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                  placeholder="1200.00"
                  value="1200"
                />
              </div>
            </div>

            <!-- Rounding Placement -->
            <div>
              <label for="placement" class="block text-sm font-medium text-gray-300 mb-2">
                Rounding Placement
              </label>
              <select
                id="placement"
                name="placement"
                class="block w-full border border-gray-600 bg-gray-800 py-2 px-4 text-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              >
                <option value="LAST">Last Month (December)</option>
                <option value="FIRST">First Month (January)</option>
                <option value="MIDDLE">Middle Month (June)</option>
              </select>
            </div>
          </div>

          <div class="mt-6">
            <button
              type="submit"
              id="submit"
              class="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus:ring-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Calculate Monthly Allocation
            </button>
          </div>
        </div>
      </form>

      <!-- Results Section -->
      <div id="results-container" class="hidden space-y-4">
        <!-- Summary Card -->
        <div class="border border-gray-700 rounded-lg p-6 bg-gray-800">
          <h3 class="text-lg font-semibold mb-4">Allocation Summary</h3>
          <div class="grid grid-cols-3 gap-4 text-center">
            <div>
              <div class="text-gray-400 text-sm">Total Amount</div>
              <div id="summary-total" class="text-xl font-bold text-white mt-1">-</div>
            </div>
            <div>
              <div class="text-gray-400 text-sm">Base Monthly</div>
              <div id="summary-base" class="text-xl font-bold text-white mt-1">-</div>
            </div>
            <div>
              <div class="text-gray-400 text-sm">Remainder Applied</div>
              <div id="summary-remainder" class="text-xl font-bold text-indigo-400 mt-1">-</div>
            </div>
          </div>
        </div>

        <!-- Monthly Allocations Table -->
        <div class="border border-gray-700 rounded-lg p-6">
          <h3 class="text-lg font-semibold mb-4">Monthly Allocations</h3>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-700">
              <thead>
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                    Month
                  </th>
                  <th class="px-4 py-3 text-right text-xs font-medium text-gray-400 uppercase tracking-wider">
                    Amount
                  </th>
                  <th class="px-4 py-3 text-center text-xs font-medium text-gray-400 uppercase tracking-wider">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody id="allocations-table" class="divide-y divide-gray-700">
                <!-- Rows will be inserted here by JavaScript -->
              </tbody>
            </table>
          </div>
        </div>

        <!-- Raw JSON (collapsible) -->
        <div class="border border-gray-700 rounded-lg p-6">
          <details>
            <summary class="cursor-pointer text-sm font-medium text-gray-400 hover:text-white">
              View Raw JSON Response
            </summary>
            <pre id="result-json" class="mt-4 p-4 bg-gray-900 rounded text-xs text-gray-300 overflow-auto max-h-96"></pre>
          </details>
        </div>
      </div>
    </div>

    <script>
      const form = document.querySelector("#calculate-amounts");
      const url = form.getAttribute("action");
      const resultsContainer = document.querySelector("#results-container");
      const allocationsTable = document.querySelector("#allocations-table");
      const resultJson = document.querySelector("#result-json");
      const submitButton = document.querySelector("#submit");

      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        
        // Disable submit button
        submitButton.disabled = true;
        submitButton.textContent = "Calculating...";
        
        const amountValue = document.querySelector("#amount").value;
        const placementValue = document.querySelector("#placement").value;
        
        const body = {
          amount: {
            value: parseFloat(amountValue),
            currency: "USD",
          },
          roundingPlacement: placementValue
        };
        
        try {
          const response = await fetch(url, {
            method: "POST",
            body: JSON.stringify(body),
            headers: {
              "Content-Type": "application/json; charset=utf-8",
            },
          });
          
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
          
          const data = await response.json();
          displayResults(data);
        } catch (error) {
          alert("Error: " + error.message);
          console.error(error);
        } finally {
          // Re-enable submit button
          submitButton.disabled = false;
          submitButton.textContent = "Calculate Monthly Allocation";
        }
      });

      function displayResults(data) {
        // Show results container
        resultsContainer.classList.remove("hidden");
        
        // Update summary
        document.querySelector("#summary-total").textContent = formatMoney(data.total);
        document.querySelector("#summary-base").textContent = formatMoney(data.baseMonthlyAmount);
        document.querySelector("#summary-remainder").textContent = formatMoney(data.remainder);
        
        // Clear and populate table
        allocationsTable.innerHTML = "";
        data.allocations.forEach((allocation) => {
          const row = document.createElement("tr");
          row.className = allocation.hasRemainder 
            ? "bg-indigo-900 bg-opacity-30" 
            : "";
          
          row.innerHTML = `
            <td class="px-4 py-3 text-sm font-medium">${formatMonth(allocation.month)}</td>
            <td class="px-4 py-3 text-sm text-right font-mono">${formatMoney(allocation.amount)}</td>
            <td class="px-4 py-3 text-sm text-center">
              ${allocation.hasRemainder 
                ? '<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-600 text-white">Adjusted</span>' 
                : '<span class="text-gray-500">—</span>'
              }
            </td>
          `;
          
          allocationsTable.appendChild(row);
        });
        
        // Display raw JSON
        resultJson.textContent = JSON.stringify(data, null, 2);
        
        // Scroll to results
        resultsContainer.scrollIntoView({ behavior: "smooth", block: "nearest" });
      }

      function formatMoney(monetaryAmount) {
        const value = monetaryAmount.number || monetaryAmount.amount;
        const currency = monetaryAmount.currency?.currencyCode || "USD";
        return new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: currency
        }).format(value);
      }

      function formatMonth(monthName) {
        return monthName.charAt(0) + monthName.slice(1).toLowerCase();
      }
    </script>
  </body>
</html>
```

---

## Phase 6: Final Polish

### Step 6.1: Add Documentation Comments

Review all files and ensure:
- Classes have JavaDoc
- Complex methods have explanatory comments
- Constants are well-named and documented

### Step 6.2: Final Test Run

```bash
./gradlew clean test
```

### Step 6.3: Run Application

```bash
./gradlew bootRun
```

Visit: http://localhost:8080/

### Step 6.4: Manual Testing Checklist

- [ ] Test with $1200 (neat division)
- [ ] Test with $1000 (rounding needed)
- [ ] Test with $0.05 (very small)
- [ ] Test with $1234.56 (decimal)
- [ ] Test all three placement options
- [ ] Verify UI displays correctly
- [ ] Check JSON structure
- [ ] Verify sum always equals input

---

## Summary

This implementation demonstrates:

✅ **TDD Best Practices**
- Red-Green-Refactor cycles
- Test-first development
- Comprehensive test coverage

✅ **Clean Code**
- Clear domain modeling
- Separation of concerns
- Immutable value objects
- Descriptive naming

✅ **Robustness**
- Input validation
- Edge case handling
- Precision-safe calculations
- Error handling

✅ **Creativity & Initiative**
- Rich response DTOs with metadata
- User-friendly UI enhancements
- Flexible API design
- Clear visual feedback

✅ **Communication**
- Well-documented code
- Clear commit messages
- Structured implementation

---

## Next Steps for Interview

1. **Be ready to explain design decisions:**
   - Why `RevenueAllocationResult` instead of just a list?
   - Why enum for placement instead of string?
   - How does the rounding algorithm guarantee sum accuracy?

2. **Be prepared to extend:**
   - "How would you add quarterly allocation?"
   - "How would you handle different currencies?"
   - "What if we need to start from a month other than January?"

3. **Walk through the TDD process:**
   - Show how each test drove the implementation
   - Explain refactoring decisions
   - Discuss trade-offs made

Good luck with your interview! 🚀

