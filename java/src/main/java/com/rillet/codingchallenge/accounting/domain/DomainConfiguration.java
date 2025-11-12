package com.rillet.codingchallenge.accounting.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    /**
     * Registers AllocationCalculator as a Spring bean.
     *
     * WHY AS A BEAN:
     * - AllocationCalculator is stateless
     * - Can be shared (thread-safe)
     * - Singleton scope is appropriate
     * - Enables dependency injection in use case
     *
     * NOTE: The calculator itself has no Spring dependencies!
     * It's just a regular Java class. We're registering it here
     * so Spring can inject it into our use case.
     */
    @Bean
    public AllocationCalculator allocationCalculator() {
        return new AllocationCalculator();
    }
}