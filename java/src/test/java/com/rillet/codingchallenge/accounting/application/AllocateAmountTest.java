package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.AllocationCalculator;
import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;
import org.junit.jupiter.api.Test;

import java.time.Month;

import static com.rillet.codingchallenge.accounting.Helpers.dollars;
import static org.assertj.core.api.Assertions.assertThat;

class AllocateAmountTest {
    final private AllocationCalculator calculator = new AllocationCalculator();
    final private AllocateAmount useCase = new AllocateAmount(calculator);

    @Test
    public void shouldReturnAmountsCalculatedOverAYear() {
        //given: an amount that divides by 12

        //when the useCase is executed
        RevenueAllocation allocation = useCase.execute(dollars(1200), RoundingPlacement.LAST);
        // {
        //monthlyAllocations:
        //[
        //    {"month" : "JANUARY", "amount" : {"value": "100", "currency" : "USD"}},
        //    {"month" : "DECEMBER", "amount" : {"value": "100", "currency" : "USD"}}
        //]
        // }

        //then: all 12 months get exactly $100
        assertThat(allocation.monthlyAllocations()).hasSize(12).allMatch(
                monthlyAllocation -> monthlyAllocation.amount().isEqualTo(dollars(100))
        );

        //and: all months are included in the result
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++) {
            //check that the allocation exists in allocation.monthlyAllocations()
            assertThat(allocation.monthlyAllocations().get(i).month()).isEqualTo(months[i]);
        }
    }

    @Test
    public void shouldHandleRoundingDifferenceByAddingToTheLastMonth() {
        //given: an amount that doesnt divide by 12

        //when the useCase is executed
        RevenueAllocation allocation = useCase.execute(dollars(1000), RoundingPlacement.LAST);
        // {
        //monthlyAllocations:
        //[
        //    {"month" : "JANUARY", "amount" : {"value": "83.33", "currency" : "USD"}},
        //    {"month" : "DECEMBER", "amount" : {"value": "83.37", "currency" : "USD"}}
        //]
        // }

        //then: the first 11 months get exactly 83.33
        for (int i = 0; i < 11; i++) {
            MonthlyAllocation monthlyAllocation = allocation.monthlyAllocations().get(i);
            assertThat(monthlyAllocation.amount()).isEqualTo(dollars(83.33));
        }

        //and: the last month has the adjusted amount
        MonthlyAllocation lastAllocation = allocation.monthlyAllocations().get(11);
        assertThat(lastAllocation.amount()).isEqualTo(dollars(83.37));


        //and: all months are included in the result
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++) {
            //check that the allocation exists in allocation.monthlyAllocations()
            assertThat(allocation.monthlyAllocations().get(i).month()).isEqualTo(months[i]);
        }
    }

    @Test
    public void shouldPlaceRoundingDifferenceInTheFirstMonth() {
        //given: an amount that doesnt divide by 12

        //when the useCase is executed
        RevenueAllocation allocation = useCase.execute(dollars(1000), RoundingPlacement.FIRST);
        // {
        //monthlyAllocations:
        //[
        //    {"month" : "JANUARY", "amount" : {"value": "83.37", "currency" : "USD"}},
        //    {"month" : "DECEMBER", "amount" : {"value": "83.33", "currency" : "USD"}}
        //]
        // }

        //then: the first month has the adjusted amount
        MonthlyAllocation lastAllocation = allocation.monthlyAllocations().get(0);
        assertThat(lastAllocation.amount()).isEqualTo(dollars(83.37));

        //and: the next 11 months get exactly 83.33
        for (int i = 1; i < 11; i++) {
            MonthlyAllocation monthlyAllocation = allocation.monthlyAllocations().get(i);
            assertThat(monthlyAllocation.amount()).isEqualTo(dollars(83.33));
        }


        //and: all months are included in the result
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++) {
            //check that the allocation exists in allocation.monthlyAllocations()
            assertThat(allocation.monthlyAllocations().get(i).month()).isEqualTo(months[i]);
        }
    }
}