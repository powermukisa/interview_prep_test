package com.rillet.codingchallenge.accounting.domain;

import org.junit.jupiter.api.Test;

import java.time.Month;

import static com.rillet.codingchallenge.accounting.Helpers.dollars;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AllocationCalculatorTest {
    private final AllocationCalculator calculator = new AllocationCalculator();

    @Test
    void shouldCalculatePerfectAllocationWhenDivisibleByTwelve() {
        //Given: An amount that divides evenly by 12
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(1200),
                RoundingPlacement.LAST
        );

        //When: we calculate the allocation
        RevenueAllocation allocation = calculator.calculate(request);

        //Then: All 12 months get exactly $100
        assertThat(allocation.monthlyAllocations())
                .hasSize(12)
                .allMatch(monthlyAlloc -> monthlyAlloc.amount().isEqualTo(dollars(100)));

        //And: Base amount is $100
        assertThat(allocation.baseMonthlyAmount()).isEqualTo(dollars(100));

        //And no rounding adjustment needed
        assertThat(allocation.roundingAdjustment()).isEqualTo(dollars(0));

        //And total equals original (invariant check by aggregate)
        assertThat(allocation.annualAmount()).isEqualTo(dollars(1200));

        // And: First month is JANUARY, last is DECEMBER
        assertThat(allocation.monthlyAllocations().get(0).month()).isEqualTo(Month.JANUARY);
        assertThat(allocation.monthlyAllocations().get(11).month()).isEqualTo(Month.DECEMBER);
    }


    @Test
    void shouldHandleRoundingDifferenceByAddingToLastMonth() {
        // Given: An amount that doesn't divide evenly
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(1000),
                RoundingPlacement.LAST
        );

        // When: We calculate the allocation
        RevenueAllocation allocation = calculator.calculate(request);

        // Then: We get 12 months
        assertThat(allocation.monthlyAllocations()).hasSize(12);

        // And: First 11 months have the base amount ($83.33)
        for (int i = 0; i < 11; i++) {
            MonthlyAllocation monthAlloc = allocation.monthlyAllocations().get(i);
            assertThat(monthAlloc.amount()).isEqualTo(dollars(83.33));
        }

        // And: Last month (December) has the adjusted amount ($83.37)
        MonthlyAllocation lastMonth = allocation.monthlyAllocations().get(11);
        assertThat(lastMonth.amount()).isEqualTo(dollars(83.37));
        assertThat(lastMonth.month()).isEqualTo(Month.DECEMBER);

        // And: Base amount is $83.33
        assertThat(allocation.baseMonthlyAmount()).isEqualTo(dollars(83.33));

        // And: Rounding adjustment is $0.04
        assertThat(allocation.roundingAdjustment()).isEqualTo(dollars(0.04));

        // And: CRITICAL - Sum equals original (aggregate validates this!)
        assertThat(allocation.annualAmount()).isEqualTo(dollars(1000));
    }

    @Test
    void shouldPlaceRoundingDifferenceInFirstMonth() {
        // Given: Request with FIRST placement
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(1000),
                RoundingPlacement.FIRST
        );

        // When: We calculate
        RevenueAllocation allocation = calculator.calculate(request);

        // Then: First month (January) has the adjusted amount
        MonthlyAllocation firstMonth = allocation.monthlyAllocations().get(0);
        assertThat(firstMonth.amount()).isEqualTo(dollars(83.37));
        assertThat(firstMonth.month()).isEqualTo(Month.JANUARY);

        // And: Remaining 11 months have base amount
        for (int i = 1; i < 12; i++) {
            MonthlyAllocation monthAlloc = allocation.monthlyAllocations().get(i);
            assertThat(monthAlloc.amount()).isEqualTo(dollars(83.33));
        }

        // And: Sum still equals original
        assertThat(allocation.annualAmount()).isEqualTo(dollars(1000));
    }

    @Test
    void shouldPlaceRoundingDifferenceInMiddleMonth() {
        // Given: Request with MIDDLE placement
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(1000),
                RoundingPlacement.MIDDLE
        );

        // When: We calculate
        RevenueAllocation allocation = calculator.calculate(request);

        // Then: Middle month (June, index 5) has the adjusted amount
        MonthlyAllocation middleMonth = allocation.monthlyAllocations().get(5);
        assertThat(middleMonth.amount()).isEqualTo(dollars(83.37));
        assertThat(middleMonth.month()).isEqualTo(Month.JUNE);

        // And: Other months have base amount
        for (int i = 0; i < 12; i++) {
            if (i != 5) {
                MonthlyAllocation monthAlloc = allocation.monthlyAllocations().get(i);
                assertThat(monthAlloc.amount()).isEqualTo(dollars(83.33));
            }
        }

        // And: Sum equals original
        assertThat(allocation.annualAmount()).isEqualTo(dollars(1000));
    }

    @Test
    void shouldHandleVerySmallAmounts() {
        // Given: A very small amount
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(0.05),
                RoundingPlacement.LAST
        );

        // When: We calculate
        RevenueAllocation allocation = calculator.calculate(request);

        // Then: Sum still equals original (no precision loss)
        assertThat(allocation.annualAmount()).isEqualTo(dollars(0.05));

        // And: We get 12 allocations (even if most are zero)
        assertThat(allocation.monthlyAllocations()).hasSize(12);
    }

    @Test
    void shouldHandleZeroAmount() {
        // Given: Zero amount
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(0),
                RoundingPlacement.LAST
        );

        // When: We calculate
        RevenueAllocation allocation = calculator.calculate(request);

        // Then: All months are zero
        assertThat(allocation.monthlyAllocations())
                .allMatch(month -> month.amount().isZero());

        // And: No rounding adjustment
        assertThat(allocation.roundingAdjustment()).isEqualTo(dollars(0));
    }

    @Test
    void shouldHandleAmountsWithManyCents() {
        // Given: An amount with cents
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
                dollars(1234.56),
                RoundingPlacement.LAST
        );

        // When: We calculate
        RevenueAllocation allocation = calculator.calculate(request);

        // Then: Sum equals original exactly (CRITICAL for accounting)
        assertThat(allocation.annualAmount()).isEqualTo(dollars(1234.56));

        // And: We have exactly 12 allocations
        assertThat(allocation.monthlyAllocations()).hasSize(12);
    }

    @Test
    void shouldRejectNegativeAmount() {
        // When/Then: Negative amount throws exception
        assertThatThrownBy(() ->
                new RevenueRecognitionRequest(dollars(-100), RoundingPlacement.LAST)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void shouldRejectNullAmount() {
        // When/Then: Null amount throws exception
        assertThatThrownBy(() ->
                new RevenueRecognitionRequest(null, RoundingPlacement.LAST)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }
}
