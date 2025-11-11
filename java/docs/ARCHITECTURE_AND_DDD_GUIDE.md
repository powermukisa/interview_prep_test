# Hexagonal Architecture & Domain-Driven Design Guide

## 🏛️ Architecture Overview

This implementation demonstrates **Hexagonal Architecture** (Ports & Adapters) and **Domain-Driven Design** principles that Rillet values.

```
┌────────────────────────────────────────────────────────────────┐
│                     ADAPTERS (Infrastructure)                   │
│  ┌──────────────┐                        ┌─────────────────┐  │
│  │     Web      │                        │      (Future)   │  │
│  │  Controller  │                        │   Persistence   │  │
│  │   Adapter    │                        │     Adapter     │  │
│  └──────────────┘                        └─────────────────┘  │
│         ↓                                          ↓           │
│    [HTTP/JSON]                              [Database]        │
│         ↓                                          ↓           │
├─────────┼──────────────────────────────────────────┼──────────┤
│         ↓              PORTS (Interfaces)          ↓           │
│  ┌──────────────┐                        ┌─────────────────┐  │
│  │   Driving    │                        │     Driven      │  │
│  │     Port     │                        │      Port       │  │
│  │(UseCase IF)  │                        │ (Repository IF) │  │
│  └──────────────┘                        └─────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│                    DOMAIN (Core Business Logic)               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Value Objects:                                         │  │
│  │  - RevenueRecognitionRequest                           │  │
│  │  - MonthlyAllocation                                   │  │
│  │  - RoundingPlacement                                   │  │
│  │                                                         │  │
│  │  Aggregate Root:                                       │  │
│  │  - RevenueAllocation (aggregate)                       │  │
│  │                                                         │  │
│  │  Domain Services:                                      │  │
│  │  - AllocationCalculator                                │  │
│  │                                                         │  │
│  │  Application Services:                                 │  │
│  │  - AllocateRevenueUseCase (implements port)           │  │
│  └────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Domain-Driven Design Concepts

### **Ubiquitous Language**
We use revenue recognition terminology throughout:
- **Allocation** (not "split" or "divide")
- **Recognition Period** (not "time span")
- **Rounding Adjustment** (not "remainder" or "leftover")
- **Revenue Period** (not just "month")

### **Building Blocks**

| DDD Pattern | Our Implementation | Purpose |
|------------|-------------------|---------|
| **Value Object** | `RevenueRecognitionRequest` | Immutable request with validation |
| **Value Object** | `MonthlyAllocation` | Immutable allocation result |
| **Enum (Value Object)** | `RoundingPlacement` | Type-safe strategy |
| **Aggregate Root** | `RevenueAllocation` | Ensures business invariants |
| **Domain Service** | `AllocationCalculator` | Pure domain logic |
| **Application Service** | `AllocateRevenueUseCase` | Orchestrates use case |
| **Port (Interface)** | `AllocateRevenue` | Defines driving port |
| **Adapter** | `AmountsController` | HTTP adapter |

---

## 📐 Hexagonal Architecture Principles

### **1. Dependency Rule**
```
Infrastructure (Adapters) → Application (Ports) → Domain
```
- Domain has NO dependencies on infrastructure
- Domain doesn't know about HTTP, JSON, databases
- Infrastructure depends on domain, never the reverse

### **2. Ports (Interfaces)**

**Driving Ports** (primary, left side):
```java
// What the domain offers to the outside world
public interface AllocateRevenue {
    RevenueAllocation execute(RevenueRecognitionRequest request);
}
```

**Driven Ports** (secondary, right side):
```java
// What the domain needs from infrastructure (future)
public interface RevenueAllocationRepository {
    void save(RevenueAllocation allocation);
    Optional<RevenueAllocation> findById(String id);
}
```

### **3. Adapters**

**Driving Adapters** (call the domain):
- `AmountsController` (HTTP/REST adapter)
- Future: CLI adapter, gRPC adapter, etc.

**Driven Adapters** (called by domain):
- Future: JPA repository, MongoDB repository, etc.

---

## 🎨 Enhanced Implementation with DDD

### **Phase 1: Domain Layer (Pure Business Logic)**

#### **Step 1.1: Value Object - RoundingPlacement**

This is a **Value Object** (enum type) - immutable and type-safe.

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RoundingPlacement.java`

```java
package com.rillet.codingchallenge.accounting.domain;

/**
 * VALUE OBJECT: Defines rounding adjustment placement strategy.
 * 
 * Part of the Ubiquitous Language for revenue recognition.
 * Immutable and side-effect free.
 */
public enum RoundingPlacement {
    /**
     * Apply rounding adjustment to the first recognition period (January)
     */
    FIRST,
    
    /**
     * Apply rounding adjustment to the last recognition period (December)
     * This is the standard accounting practice.
     */
    LAST,
    
    /**
     * Apply rounding adjustment to the middle recognition period (June)
     * Used for balanced recognition profiles.
     */
    MIDDLE;
    
    /**
     * Calculates the zero-based index for the adjustment month.
     * 
     * @param totalPeriods Total number of periods (typically 12)
     * @return The index where adjustment should be applied
     */
    public int calculateAdjustmentIndex(int totalPeriods) {
        return switch (this) {
            case FIRST -> 0;
            case LAST -> totalPeriods - 1;
            case MIDDLE -> totalPeriods / 2 - 1;  // Middle month (0-indexed)
        };
    }
}
```

**🎯 DDD Benefits:**
- Encapsulates business logic (adjustment index calculation)
- Uses ubiquitous language ("adjustment", "recognition period")
- Type-safe (no string constants)
- Testable in isolation

---

#### **Step 1.2: Value Object - RevenueRecognitionRequest**

This is a **Value Object** - immutable with self-validation.

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RevenueRecognitionRequest.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.util.Objects;

/**
 * VALUE OBJECT: Represents a request to allocate annual revenue across periods.
 * 
 * Immutable and self-validating. Encapsulates all parameters needed for
 * revenue recognition allocation.
 * 
 * Part of the Ubiquitous Language.
 */
public record RevenueRecognitionRequest(
    MonetaryAmount annualAmount,
    RoundingPlacement roundingPlacement
) {
    private static final int PERIODS_IN_YEAR = 12;
    
    /**
     * Compact constructor with validation (DDD: self-validating value object)
     */
    public RevenueRecognitionRequest {
        Objects.requireNonNull(annualAmount, "Annual amount cannot be null");
        Objects.requireNonNull(roundingPlacement, "Rounding placement cannot be null");
        
        if (annualAmount.isNegative()) {
            throw new IllegalArgumentException(
                "Annual amount cannot be negative: " + annualAmount
            );
        }
    }
    
    /**
     * Factory method with default placement (follows DDD patterns)
     */
    public static RevenueRecognitionRequest of(MonetaryAmount annualAmount) {
        return new RevenueRecognitionRequest(annualAmount, RoundingPlacement.LAST);
    }
    
    /**
     * Returns the number of periods for allocation.
     */
    public int recognitionPeriods() {
        return PERIODS_IN_YEAR;
    }
}
```

**🎯 DDD Benefits:**
- Self-validating (fails fast)
- Immutable (thread-safe)
- Factory method for common case
- Encapsulates business rules ("annual amount must be positive")

---

#### **Step 1.3: Value Object - MonthlyAllocation**

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/MonthlyAllocation.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.Objects;

/**
 * VALUE OBJECT: Represents recognized revenue for a single period (month).
 * 
 * Immutable and equality is based on value, not identity.
 * Part of the Ubiquitous Language.
 */
public record MonthlyAllocation(
    Month recognitionPeriod,
    MonetaryAmount recognizedAmount,
    boolean hasRoundingAdjustment
) {
    /**
     * Compact constructor with validation
     */
    public MonthlyAllocation {
        Objects.requireNonNull(recognitionPeriod, "Recognition period cannot be null");
        Objects.requireNonNull(recognizedAmount, "Recognized amount cannot be null");
        
        if (recognizedAmount.isNegative()) {
            throw new IllegalArgumentException(
                "Recognized amount cannot be negative: " + recognizedAmount
            );
        }
    }
    
    /**
     * Factory method for standard allocation (without adjustment)
     */
    public static MonthlyAllocation standard(Month period, MonetaryAmount amount) {
        return new MonthlyAllocation(period, amount, false);
    }
    
    /**
     * Factory method for adjusted allocation
     */
    public static MonthlyAllocation adjusted(Month period, MonetaryAmount amount) {
        return new MonthlyAllocation(period, amount, true);
    }
    
    /**
     * Returns formatted period name for reporting
     */
    public String periodName() {
        return recognitionPeriod.name();
    }
}
```

**🎯 DDD Benefits:**
- Uses ubiquitous language ("recognition period", "recognized amount")
- Factory methods express intent
- Self-validating
- Immutable

---

#### **Step 1.4: Aggregate Root - RevenueAllocation**

This is an **Aggregate Root** - maintains invariants and consistency.

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RevenueAllocation.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AGGREGATE ROOT: Represents a complete annual revenue allocation across periods.
 * 
 * Maintains the invariant: Sum of all monthly allocations equals annual amount.
 * Encapsulates business rules for revenue recognition.
 * 
 * This is the consistency boundary for allocation operations.
 */
public class RevenueAllocation {
    private final MonetaryAmount annualAmount;
    private final List<MonthlyAllocation> monthlyAllocations;
    private final MonetaryAmount baseMonthlyAmount;
    private final MonetaryAmount roundingAdjustment;
    private final RoundingPlacement placement;
    
    /**
     * Constructor enforces aggregate invariants.
     * Package-private: Use AllocationCalculator (domain service) to create.
     */
    RevenueAllocation(
        MonetaryAmount annualAmount,
        List<MonthlyAllocation> monthlyAllocations,
        MonetaryAmount baseMonthlyAmount,
        MonetaryAmount roundingAdjustment,
        RoundingPlacement placement
    ) {
        Objects.requireNonNull(annualAmount, "Annual amount cannot be null");
        Objects.requireNonNull(monthlyAllocations, "Monthly allocations cannot be null");
        
        if (monthlyAllocations.size() != 12) {
            throw new IllegalArgumentException(
                "Must have exactly 12 monthly allocations, got: " + monthlyAllocations.size()
            );
        }
        
        // INVARIANT: Sum must equal annual amount
        MonetaryAmount sum = monthlyAllocations.stream()
            .map(MonthlyAllocation::recognizedAmount)
            .reduce(MonetaryAmount::add)
            .orElseThrow(() -> new IllegalStateException("Failed to calculate sum"));
        
        if (!sum.isEqualTo(annualAmount)) {
            throw new IllegalStateException(
                String.format(
                    "Invariant violated: Sum of allocations (%s) must equal annual amount (%s)",
                    sum, annualAmount
                )
            );
        }
        
        this.annualAmount = annualAmount;
        this.monthlyAllocations = List.copyOf(monthlyAllocations);  // Defensive copy
        this.baseMonthlyAmount = baseMonthlyAmount;
        this.roundingAdjustment = roundingAdjustment;
        this.placement = placement;
    }
    
    // Getters (read-only access to aggregate internals)
    
    public MonetaryAmount getAnnualAmount() {
        return annualAmount;
    }
    
    public List<MonthlyAllocation> getMonthlyAllocations() {
        return monthlyAllocations;  // Already unmodifiable
    }
    
    public MonetaryAmount getBaseMonthlyAmount() {
        return baseMonthlyAmount;
    }
    
    public MonetaryAmount getRoundingAdjustment() {
        return roundingAdjustment;
    }
    
    public RoundingPlacement getPlacement() {
        return placement;
    }
    
    /**
     * Domain query: Is this a perfect even allocation?
     */
    public boolean isPerfectAllocation() {
        return roundingAdjustment.isZero();
    }
    
    /**
     * Domain query: Get the adjusted month
     */
    public MonthlyAllocation getAdjustedAllocation() {
        return monthlyAllocations.stream()
            .filter(MonthlyAllocation::hasRoundingAdjustment)
            .findFirst()
            .orElse(null);
    }
}
```

**🎯 DDD Benefits:**
- **Aggregate Root** ensures consistency boundary
- Enforces invariant: sum = annual amount
- Encapsulates allocation as a whole
- Provides domain queries (`isPerfectAllocation`)
- Defensive copying prevents external mutation

---

#### **Step 1.5: Domain Service - AllocationCalculator**

This is a **Domain Service** - stateless domain logic.

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/AllocationCalculator.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * DOMAIN SERVICE: Calculates revenue allocations using domain rules.
 * 
 * Stateless service that encapsulates the core business logic for
 * splitting annual revenue into monthly recognition periods.
 * 
 * This is pure domain logic with no dependencies on infrastructure.
 */
public class AllocationCalculator {
    private static final int MONTHS_IN_YEAR = 12;
    
    /**
     * Calculates monthly revenue allocations based on request parameters.
     * 
     * Algorithm:
     * 1. Calculate base monthly amount (annual / 12, rounded)
     * 2. Calculate remainder (difference from perfect distribution)
     * 3. Allocate remainder to specified month per rounding placement
     * 4. Ensure sum equals annual amount (aggregate invariant)
     * 
     * @param request The revenue recognition request
     * @return A RevenueAllocation aggregate
     */
    public RevenueAllocation calculate(RevenueRecognitionRequest request) {
        MonetaryAmount annualAmount = request.annualAmount();
        RoundingPlacement placement = request.roundingPlacement();
        
        // Step 1: Calculate base monthly amount
        MonetaryAmount baseMonthlyAmount = CurrenciesHelper.rounded(
            annualAmount.divide(MONTHS_IN_YEAR)
        );
        
        // Step 2: Calculate what would be allocated to 11 months
        MonetaryAmount elevenMonthsTotal = baseMonthlyAmount.multiply(MONTHS_IN_YEAR - 1);
        
        // Step 3: Remainder goes to the adjustment month (ensures sum = annual)
        MonetaryAmount adjustedMonthAmount = annualAmount.subtract(elevenMonthsTotal);
        
        // Step 4: Calculate the adjustment (difference from base)
        MonetaryAmount roundingAdjustment = adjustedMonthAmount.subtract(baseMonthlyAmount);
        
        // Step 5: Determine which month gets the adjustment
        int adjustmentIndex = placement.calculateAdjustmentIndex(MONTHS_IN_YEAR);
        
        // Step 6: Build monthly allocations
        List<MonthlyAllocation> allocations = buildAllocations(
            baseMonthlyAmount,
            adjustedMonthAmount,
            adjustmentIndex
        );
        
        // Step 7: Create and return aggregate (validates invariants)
        return new RevenueAllocation(
            annualAmount,
            allocations,
            baseMonthlyAmount,
            roundingAdjustment,
            placement
        );
    }
    
    /**
     * Builds the list of 12 monthly allocations.
     */
    private List<MonthlyAllocation> buildAllocations(
        MonetaryAmount baseAmount,
        MonetaryAmount adjustedAmount,
        int adjustmentIndex
    ) {
        List<MonthlyAllocation> allocations = new ArrayList<>(MONTHS_IN_YEAR);
        Month[] months = Month.values();
        
        for (int i = 0; i < MONTHS_IN_YEAR; i++) {
            if (i == adjustmentIndex) {
                allocations.add(MonthlyAllocation.adjusted(months[i], adjustedAmount));
            } else {
                allocations.add(MonthlyAllocation.standard(months[i], baseAmount));
            }
        }
        
        return allocations;
    }
}
```

**🎯 DDD Benefits:**
- Pure domain logic (no infrastructure dependencies)
- Stateless (can be shared/singleton)
- Clear business algorithm
- Creates aggregate root (ensures invariants)
- Uses ubiquitous language

---

### **Phase 2: Application Layer (Use Cases)**

#### **Step 2.1: Port Interface (Driving Port)**

**File:** `src/main/java/com/rillet/codingchallenge/accounting/application/AllocateRevenue.java`

```java
package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;

/**
 * DRIVING PORT: Defines the use case for revenue allocation.
 * 
 * This is the hexagonal architecture port that adapters call into.
 * The domain implements this; adapters depend on it.
 */
public interface AllocateRevenue {
    /**
     * Allocates annual revenue into monthly recognition periods.
     * 
     * @param request The revenue recognition parameters
     * @return The complete allocation aggregate
     * @throws IllegalArgumentException if request is invalid
     */
    RevenueAllocation execute(RevenueRecognitionRequest request);
}
```

---

#### **Step 2.2: Application Service (Port Implementation)**

**File:** `src/main/java/com/rillet/codingchallenge/accounting/application/AllocateRevenueUseCase.java`

```java
package com.rillet.codingchallenge.accounting.application;

import com.rillet.codingchallenge.accounting.domain.AllocationCalculator;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import org.springframework.stereotype.Service;

/**
 * APPLICATION SERVICE: Orchestrates the revenue allocation use case.
 * 
 * This implements the port interface and coordinates domain services.
 * Thin orchestration layer - business logic is in domain.
 */
@Service
public class AllocateRevenueUseCase implements AllocateRevenue {
    private final AllocationCalculator calculator;
    
    public AllocateRevenueUseCase(AllocationCalculator calculator) {
        this.calculator = calculator;
    }
    
    /**
     * Executes the revenue allocation use case.
     * 
     * Orchestrates: Validation → Calculation → (Future: Persistence)
     */
    @Override
    public RevenueAllocation execute(RevenueRecognitionRequest request) {
        // Request is self-validating (DDD value object)
        // Delegate to domain service for calculation
        RevenueAllocation allocation = calculator.calculate(request);
        
        // Future: Could save to repository here
        // allocationRepository.save(allocation);
        
        return allocation;
    }
}
```

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/DomainConfiguration.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for domain services.
 * Domain services are stateless and can be singletons.
 */
@Configuration
public class DomainConfiguration {
    
    @Bean
    public AllocationCalculator allocationCalculator() {
        return new AllocationCalculator();
    }
}
```

**🎯 Hexagonal Architecture:**
- Port (`AllocateRevenue`) defines what domain offers
- Application service implements port
- Domain service (calculator) has no Spring dependencies
- Clear separation: Application orchestrates, Domain executes

---

### **Phase 3: Infrastructure Layer (Adapters)**

#### **Step 3.1: Web Adapter (REST Controller)**

**File:** `src/main/java/com/rillet/codingchallenge/accounting/infra/web/AmountsController.java`

```java
package com.rillet.codingchallenge.accounting.infra.web;

import com.rillet.codingchallenge.accounting.application.AllocateRevenue;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import com.rillet.codingchallenge.accounting.infra.web.dto.AllocationRequestDto;
import com.rillet.codingchallenge.accounting.infra.web.dto.AllocationResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WEB ADAPTER (Driving Adapter): HTTP/REST interface to the domain.
 * 
 * This is a hexagonal architecture adapter that:
 * - Receives HTTP requests
 * - Converts DTOs to domain objects
 * - Calls the port (AllocateRevenue)
 * - Converts domain objects back to DTOs
 * - Returns HTTP responses
 * 
 * The controller knows about HTTP and JSON, but domain doesn't.
 */
@RestController
@RequestMapping("/amounts")
public class AmountsController {
    private final AllocateRevenue allocateRevenue;  // ← Depends on PORT, not implementation

    public AmountsController(AllocateRevenue allocateRevenue) {
        this.allocateRevenue = allocateRevenue;
    }

    /**
     * HTTP POST endpoint for revenue allocation.
     * 
     * Adapter responsibilities:
     * 1. Receive HTTP request
     * 2. Validate DTO structure
     * 3. Convert DTO → Domain object
     * 4. Call domain through port
     * 5. Convert Domain object → DTO
     * 6. Return HTTP response
     */
    @PostMapping
    public ResponseEntity<AllocationResponseDto> allocateRevenue(
        @RequestBody AllocationRequestDto requestDto
    ) {
        try {
            // Convert infrastructure DTO → Domain value object
            RevenueRecognitionRequest domainRequest = requestDto.toDomain();
            
            // Call domain through port
            RevenueAllocation allocation = allocateRevenue.execute(domainRequest);
            
            // Convert domain aggregate → Infrastructure DTO
            AllocationResponseDto responseDto = AllocationResponseDto.fromDomain(allocation);
            
            return ResponseEntity.ok(responseDto);
            
        } catch (IllegalArgumentException e) {
            // Domain validation failed
            return ResponseEntity
                .badRequest()
                .build();
        } catch (Exception e) {
            // Unexpected error
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}
```

**🎯 Hexagonal Architecture:**
- Controller is an **adapter** (infrastructure)
- Depends on **port** (`AllocateRevenue`), not implementation
- Handles HTTP concerns (status codes, JSON)
- Domain is unaware of HTTP

---

#### **Step 3.2: DTOs (Adapter Layer)**

**File:** `src/main/java/com/rillet/codingchallenge/accounting/infra/web/dto/AllocationRequestDto.java`

```java
package com.rillet.codingchallenge.accounting.infra.web.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;

import javax.money.MonetaryAmount;

/**
 * ADAPTER DTO: HTTP request structure.
 * 
 * This is NOT a domain object - it's infrastructure.
 * Knows about JSON serialization but domain doesn't.
 */
public record AllocationRequestDto(
    MonetaryAmount amount,
    RoundingPlacement roundingPlacement
) {
    @JsonCreator
    public AllocationRequestDto(
        @JsonProperty("amount") MonetaryAmount amount,
        @JsonProperty("roundingPlacement") RoundingPlacement roundingPlacement
    ) {
        this.amount = amount;
        // Default to LAST if not specified
        this.roundingPlacement = roundingPlacement != null 
            ? roundingPlacement 
            : RoundingPlacement.LAST;
    }
    
    /**
     * Converts this infrastructure DTO to domain value object.
     * Adapter pattern: translates between layers.
     */
    public RevenueRecognitionRequest toDomain() {
        return new RevenueRecognitionRequest(amount, roundingPlacement);
    }
}
```

**File:** `src/main/java/com/rillet/codingchallenge/accounting/infra/web/dto/AllocationResponseDto.java`

```java
package com.rillet.codingchallenge.accounting.infra.web.dto;

import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ADAPTER DTO: HTTP response structure.
 * 
 * Infrastructure concern - knows about JSON but domain doesn't.
 */
public record AllocationResponseDto(
    List<MonthlyAllocationDto> allocations,
    MonetaryAmount baseMonthlyAmount,
    MonetaryAmount remainder,
    MonetaryAmount total
) {
    /**
     * Nested DTO for monthly amounts.
     */
    public record MonthlyAllocationDto(
        String month,
        MonetaryAmount amount,
        boolean hasRemainder
    ) {}

    /**
     * Converts domain aggregate to infrastructure DTO.
     * Adapter pattern: translates between layers.
     */
    public static AllocationResponseDto fromDomain(RevenueAllocation allocation) {
        List<MonthlyAllocationDto> dtoAllocations = allocation.getMonthlyAllocations().stream()
            .map(ma -> new MonthlyAllocationDto(
                ma.periodName(),
                ma.recognizedAmount(),
                ma.hasRoundingAdjustment()
            ))
            .collect(Collectors.toList());
        
        return new AllocationResponseDto(
            dtoAllocations,
            allocation.getBaseMonthlyAmount(),
            allocation.getRoundingAdjustment(),
            allocation.getAnnualAmount()
        );
    }
}
```

**🎯 Hexagonal Architecture:**
- DTOs are in infrastructure layer
- Explicit conversion methods (`toDomain()`, `fromDomain()`)
- Domain objects never converted to/from JSON directly
- Clear adapter pattern

---

## 🧪 Testing Strategy (Hexagonal + DDD)

### **Domain Layer Tests (Pure Unit Tests)**

```java
// Test domain service (no Spring, no mocks)
class AllocationCalculatorTest {
    private AllocationCalculator calculator = new AllocationCalculator();
    
    @Test
    void shouldCalculatePerfectAllocation() {
        var request = RevenueRecognitionRequest.of(dollars(1200));
        var allocation = calculator.calculate(request);
        
        assertThat(allocation.isPerfectAllocation()).isTrue();
        assertThat(allocation.getRoundingAdjustment()).isEqualTo(dollars(0));
    }
}
```

### **Application Layer Tests (Integration)**

```java
@SpringBootTest
class AllocateRevenueUseCaseTest {
    @Autowired
    private AllocateRevenue useCase;  // ← Test through port
    
    @Test
    void shouldAllocateRevenue() {
        var request = RevenueRecognitionRequest.of(dollars(1000));
        var allocation = useCase.execute(request);
        
        assertThat(allocation.getMonthlyAllocations()).hasSize(12);
    }
}
```

### **Adapter Layer Tests (Spring MVC)**

```java
@WebMvcTest(AmountsController.class)
class AmountsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AllocateRevenue allocateRevenue;  // ← Mock the port
    
    @Test
    void shouldAcceptValidRequest() throws Exception {
        mockMvc.perform(post("/amounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"amount": {"value": 1200, "currency": "USD"}}
                """))
            .andExpect(status().isOk());
    }
}
```

---

## 📚 Summary: Architecture Benefits

### **Hexagonal Architecture**
✅ **Testability**: Domain can be tested without HTTP/database  
✅ **Flexibility**: Easy to add new adapters (CLI, gRPC)  
✅ **Independence**: Domain doesn't depend on frameworks  
✅ **Clear boundaries**: Ports define contracts  

### **Domain-Driven Design**
✅ **Ubiquitous Language**: Revenue recognition terminology throughout  
✅ **Value Objects**: Immutable, self-validating  
✅ **Aggregate Root**: Maintains invariants (sum = annual)  
✅ **Domain Services**: Pure business logic  
✅ **Rich Domain Model**: Domain objects have behavior, not just data  

### **Interview Discussion Points**

1. **"Why is AllocationCalculator not a Spring @Service?"**
   - It's pure domain logic with no infrastructure dependencies
   - Can be instantiated and tested without Spring
   - Shows understanding of clean architecture

2. **"Why the port interface?"**
   - Decouples domain from adapters
   - Makes it easy to add new adapters (CLI, gRPC, message queue)
   - Follows Dependency Inversion Principle

3. **"Why is RevenueAllocation an aggregate?"**
   - It's the consistency boundary (sum must equal annual)
   - Package-private constructor ensures it's only created by calculator
   - Encapsulates business rules

This architecture demonstrates staff-level understanding of:
- Clean Architecture / Hexagonal Architecture
- Domain-Driven Design tactical patterns
- Separation of concerns
- Testability and maintainability

