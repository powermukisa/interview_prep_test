package com.rillet.codingchallenge.accounting.application;

import org.junit.jupiter.api.Test;

import static com.rillet.codingchallenge.accounting.Helpers.dollars;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AllocateAmountTest {
    final private AllocateAmount useCase = new AllocateAmount();

    @Test
    public void shouldReturnAmountsCalculatedOverAYear() {
        var result = useCase.execute(dollars(120));

        assertThat(result).isNull();
    }
}