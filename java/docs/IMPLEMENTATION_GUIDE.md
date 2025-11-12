# Rillet Revenue Recognition - TDD Implementation Guide

## 📚 How to Use This Guide

**This guide includes:**
- 🎯 **Reasoning** - Why we're doing each step
- ✅ **Benefits** - What this achieves
- 💬 **What to Say** - How to explain during pairing
- 🏛️ **Architecture Notes** - Design principles in action
- 📝 **Code** - Ready to copy/paste

**Philosophy: Start Simple, Add As Needed**

This guide follows a **natural, incremental approach** like real-world coding:
- ✅ Start with bare minimum (just what you need right now)
- ✅ Make tests pass first
- ✅ Add convenience methods/features when you see repetition
- ✅ Refactor when it adds clear value
- ❌ Don't add "nice-to-have" features upfront

**Examples:**
- We start with basic constructors, not factory methods
- We add validation when needed, not everything upfront
- We introduce helper methods when we see duplication
- Optional improvements are marked clearly

**During the interview:**
1. Read the reasoning section out loud (paraphrase naturally)
2. Explain the benefits
3. Then copy the code
4. Run tests immediately after
5. Discuss any trade-offs
6. Mention optional improvements if there's time

---

## Table of Contents

1. [Phase 0: Setup Verification](#phase-0-setup-verification)
2. [Phase 1: Domain Layer - Value Objects](#phase-1-domain-layer---value-objects)
3. [Phase 2: Domain Layer - Aggregate Root](#phase-2-domain-layer---aggregate-root)
4. [Phase 3: Domain Service (TDD)](#phase-3-domain-service-tdd)
5. [Phase 4: Application Layer](#phase-4-application-layer)
6. [Phase 5: Infrastructure Layer](#phase-5-infrastructure-layer)
7. [Phase 6: UI Enhancement](#phase-6-ui-enhancement)

---

## Phase 0: Setup Verification

### 🎯 Reasoning

Before writing any code, we need to verify:
- Project compiles
- Tests run
- Java 21 is configured
- Dependencies are available

This establishes a known-good baseline.

### 💬 What to Say

> "Let me first verify the project setup. I want to ensure everything compiles and the test infrastructure works before we start implementing. This gives us a stable baseline."

### ✅ Command

```bash
cd /Users/power/code/rillet/coding-challenge-main/java
./gradlew test
```

**Expected output:**
```
BUILD SUCCESSFUL
```

If it fails, troubleshoot Java version or dependencies before proceeding.

---

## Phase 1: Domain Layer - Value Objects

### 🎯 Why Start with Domain?

**Hexagonal Architecture Principle:**
- Domain is the core - it has NO dependencies on infrastructure
- We build from the inside out: Domain → Application → Infrastructure
- Domain should be testable without Spring, HTTP, or JSON

**Benefits:**
- ✅ Business logic is isolated and pure
- ✅ Can test without framework overhead
- ✅ Easy to reason about and maintain
- ✅ Follows Dependency Inversion Principle

### 💬 What to Say

> "I'm going to start with the domain layer, following Hexagonal Architecture principles. The domain represents our core business logic and should have no dependencies on infrastructure. We'll build value objects first - these are immutable, self-validating objects that use our ubiquitous language from revenue recognition."

---

### Step 1.1: RoundingPlacement Enum

#### 🎯 Reasoning

**Why an enum?**
- Type-safe (can't pass invalid strings)
- Self-documenting (shows all valid options)
- Can contain behavior (not just constants)
- Part of domain language

**Why these three options?**
- From requirements: "let the user decide whether that rounding goes to the **first** monthly amount, the **last**, or in the **middle**"
- These are domain concepts, not technical details

#### ✅ Benefits

- Type safety prevents bugs
- Clear API - users see their options
- Encapsulates placement logic
- Makes tests readable

#### 💬 What to Say

> "First, I'll create the RoundingPlacement enum. This is a value object that represents where we put the rounding adjustment. I'm using an enum for type safety - users can only choose FIRST, LAST, or MIDDLE, not invalid strings. I'm also putting the index calculation logic here because it's domain logic that belongs with this concept."

#### 📝 Code

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RoundingPlacement.java`

```java
package com.rillet.codingchallenge.accounting.domain;

/**
 * VALUE OBJECT: Defines rounding adjustment placement strategy.
 * 
 * In revenue recognition, when an amount doesn't divide evenly, we need
 * to allocate the rounding difference to one specific period.
 * 
 * This is part of our ubiquitous language - "rounding placement" is a
 * domain concept that accountants understand.
 */
public enum RoundingPlacement {
    /**
     * Apply rounding adjustment to the first recognition period (January)
     * Common when recognizing revenue at contract start.
     */
    FIRST,
    
    /**
     * Apply rounding adjustment to the last recognition period (December)
     * This is standard accounting practice - adjustments at period end.
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
     * DESIGN NOTE: This logic belongs here (in the enum) rather than in
     * a service because it's intrinsic to the meaning of each placement.
     * This is the "Tell, Don't Ask" principle - the enum knows its behavior.
     * 
     * @param totalPeriods Total number of periods (typically 12 for monthly)
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

#### 🏛️ Architecture Notes

- **Domain Layer** - No Spring, no HTTP, pure Java
- **Value Object Pattern** - Immutable (enums are inherently immutable)
- **Rich Domain Model** - Has behavior, not just data
- **Ubiquitous Language** - Uses terms from revenue recognition domain

#### ✅ Test Compilation

```bash
./gradlew compileJava
```

---

### Step 1.2: RevenueRecognitionRequest

#### 🎯 Reasoning

**Why this value object?**
- Encapsulates all input parameters for the use case
- Self-validating (invalid requests can't exist)
- Immutable (thread-safe, no side effects)
- Part of domain, not infrastructure

**Why validate in constructor?**
- Fail fast - catch errors immediately
- Domain objects enforce business rules
- Prevents invalid data from entering the system

#### ✅ Benefits

- Impossible to create invalid requests
- Clear contract for what's needed
- Business rules centralized
- No null checks scattered everywhere

#### 💬 What to Say

> "Next is RevenueRecognitionRequest - this is a value object that represents the input to our use case. I'm making it a record for immutability and using a compact constructor for validation. This ensures that if this object exists, it's valid - we can't create an invalid request. This is the 'make invalid states unrepresentable' principle."

#### 📝 Code

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RevenueRecognitionRequest.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.util.Objects;

/**
 * VALUE OBJECT: Represents a request to allocate annual revenue across periods.
 * 
 * This is the input to our revenue recognition use case. It's immutable and
 * self-validating - if this object exists, it's guaranteed to be valid.
 * 
 * DESIGN DECISIONS:
 * - Record type: Immutable by default, minimal boilerplate
 * - Compact constructor: Validation happens at construction time
 * - Domain object: Not tied to HTTP/JSON - that's the infrastructure layer's job
 * 
 * Part of the ubiquitous language.
 */
public record RevenueRecognitionRequest(
    MonetaryAmount amount,
    RoundingPlacement roundingPlacement
) {
    /**
     * Compact constructor with validation (DDD: self-validating value object)
     * 
     * WHY: By validating here, we ensure invalid objects can never exist.
     * This is the "make invalid states unrepresentable" principle.
     * 
     * NOTE: We use "amount" (not "annualAmount") for consistency with RequestDto.
     * The context (RevenueRecognitionRequest for annual allocation) makes it clear
     * this is the total annual amount to be split.
     * 
     * START SIMPLE: We're just adding basic validation here. No factory methods
     * or helper methods yet - we'll add those later if we find we need them.
     */
    public RevenueRecognitionRequest {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(roundingPlacement, "Rounding placement cannot be null");
        
        if (amount.isNegative()) {
            throw new IllegalArgumentException(
                "Amount cannot be negative: " + amount
            );
        }
    }
}
```

#### 🏛️ Architecture Notes

- **Value Object** - Immutable, validated, equality by value
- **Domain-Driven Design** - Self-validating, uses ubiquitous language
- **Fail Fast** - Validation at construction, not at use
- **Factory Method** - Convenient creation for common case

---

### Step 1.3: MonthlyAllocation

#### 🎯 Reasoning

**Why this value object?**
- Represents one month's allocated revenue
- Simple structure: just month and amount
- Immutable - once created, can't be changed
- Minimal - contains only what's essential

**Why not include extra flags?**
- Start simple - we can always add more later
- UI can figure out which month is adjusted by comparing amounts
- Less complexity = fewer bugs
- YAGNI principle (You Aren't Gonna Need It... yet)

#### ✅ Benefits

- Simple, clear structure
- Type-safe (uses Month enum, not strings)
- Easy to understand
- Minimal surface area for bugs

#### 💬 What to Say

> "MonthlyAllocation is another value object - it represents the revenue recognized in a single period. I'm keeping it simple: just the month and the amount. We can always add more fields later if we need them, but right now this is all we need to return the allocation data."

#### 📝 Code

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/MonthlyAllocation.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.Objects;

/**
 * VALUE OBJECT: Represents recognized revenue for a single period (month).
 * 
 * This is NOT just data - it's a domain concept with meaning. In revenue
 * recognition, we allocate annual revenue across periods, and this represents
 * one period's allocation.
 * 
 * DESIGN DECISIONS:
 * - Simple structure: Just month and amount
 * - Uses java.time.Month: Type-safe, prevents invalid months
 * - No adjustment flag: Keep it simple, UI can compare amounts if needed
 * 
 * Immutable - equality is based on value, not identity.
 */
public record MonthlyAllocation(
    Month month,
    MonetaryAmount amount
) {
    /**
     * Compact constructor with validation
     * 
     * WHY: Ensures this value object can never be in an invalid state.
     */
    public MonthlyAllocation {
        Objects.requireNonNull(month, "Month cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.isNegative()) {
            throw new IllegalArgumentException(
                "Amount cannot be negative: " + amount
            );
        }
    }
}
```

#### 🏛️ Architecture Notes

- **Value Object** - Immutable, validated, simple structure
- **Type Safety** - Uses `Month` enum, not strings or integers
- **Start Simple** - Just what's needed, can enhance later
- **Domain Concept** - Represents a business idea, not just data

---

## Phase 2: Domain Layer - Aggregate Root

### 🎯 Why an Aggregate?

**DDD Concept:**
- Aggregate Root is the consistency boundary
- Enforces business invariants
- Only way to access/modify its internals

**Our Invariant:**
- Sum of all monthly allocations MUST equal annual amount
- This is a critical business rule in accounting

#### 💬 What to Say

> "Now I'll create the RevenueAllocation aggregate root. This is the consistency boundary for our allocation operation. Its key responsibility is enforcing the invariant that all monthly amounts sum to exactly the annual amount - no rounding errors allowed. I'm making the record public so other layers can use it, but the constructor is package-private - only the domain service can create it. This ensures the invariant is always validated during creation, while still allowing other layers to read the data."

### Step 2.1: RevenueAllocation Aggregate

#### 🎯 Reasoning

**Why PUBLIC record with PACKAGE-PRIVATE constructor?**
- **Record is public**: Application and infrastructure layers can USE it (read data)
- **Constructor is package-private**: Only domain service can CREATE it
- This separates concerns: reading vs creating
- Enforces that only domain service creates aggregates (which validates invariants)

**Key insight:**
```java
public record RevenueAllocation(...)     // ← PUBLIC: everyone can use
    RevenueAllocation { ... }            // ← PACKAGE-PRIVATE: only domain can create
```

Record visibility ≠ Constructor visibility!

**Why validate sum in constructor?**
- Fail fast if invariant is violated
- Aggregate can't exist in invalid state
- Makes bugs impossible, not just unlikely

#### ✅ Benefits

- **Controlled Access**: Public for reading, package-private for creation
- **Business rule enforcement**: Invariant always validated
- **Impossible to have sum != annual**: Enforced at construction
- **Clear ownership**: Calculator creates it, others just use it
- **Less boilerplate**: Record generates accessors automatically
- **Testable invariants**: Constructor validation catches bugs early

#### 📝 Code

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/RevenueAllocation.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.Objects;

/**
 * AGGREGATE ROOT: Represents a complete annual revenue allocation across periods.
 * 
 * This is the consistency boundary for allocation operations. The critical
 * invariant it maintains is:
 *   Sum of all monthly allocations = annual amount (EXACTLY)
 * 
 * DESIGN DECISIONS:
 * 
 * 1. Public record + package-private constructor:
 *    - Record is PUBLIC: Other layers can use it (read data)
 *    - Constructor is PACKAGE-PRIVATE: Only domain service can create it
 *    - This gives controlled access: readable everywhere, creatable only in domain
 * 
 * 2. Record type:
 *    - Immutable by default
 *    - Less boilerplate than class
 *    - Automatic accessor methods (no "get" prefix)
 * 
 * 3. Naming: annualAmount vs amount
 *    - We use "annualAmount" here (not just "amount") because this aggregate
 *      contains BOTH the annual total AND monthly amounts
 *    - Being explicit prevents confusion
 * 
 * 4. Validates sum in constructor:
 *    - Fail fast if invariant is violated
 *    - Impossible to have invalid state
 *    - Makes bugs unrepresentable
 */
public record RevenueAllocation(
    MonetaryAmount annualAmount,
    List<MonthlyAllocation> monthlyAllocations,
    MonetaryAmount baseMonthlyAmount,
    MonetaryAmount roundingAdjustment,
    RoundingPlacement placement
) {
    /**
     * Compact constructor with validation and defensive copying.
     * Package-private (no modifier) - only accessible within domain package.
     * 
     * WHY PACKAGE-PRIVATE CONSTRUCTOR:
     * This is intentional! We don't want clients creating this directly.
     * Only AllocationCalculator (same package) can create this, ensuring
     * the allocation logic and invariant validation always happen together.
     * 
     * IMPORTANT: The RECORD is public, but the CONSTRUCTOR is package-private!
     * 
     * This means:
     * ✅ Other packages can USE it: allocation.annualAmount()
     * ❌ Other packages can't CREATE it: new RevenueAllocation(...)
     * 
     * Example:
     * // In AllocationCalculator (same package):
     * return new RevenueAllocation(...);  // ✅ ALLOWED
     * 
     * // In AmountsController (different package):
     * allocation.annualAmount();          // ✅ ALLOWED - can read
     * new RevenueAllocation(...);         // ❌ ERROR - can't create
     * 
     * This is a key DDD pattern - the aggregate root controls its own consistency.
     */
    RevenueAllocation {
        Objects.requireNonNull(annualAmount, "Annual amount cannot be null");
        Objects.requireNonNull(monthlyAllocations, "Monthly allocations cannot be null");
        
        if (monthlyAllocations.size() != 12) {
            throw new IllegalArgumentException(
                "Must have exactly 12 monthly allocations, got: " + monthlyAllocations.size()
            );
        }
        
        // INVARIANT ENFORCEMENT: Sum must equal annual amount
        // This is THE critical business rule for revenue recognition.
        // If this fails, we have a bug in the allocation logic.
        MonetaryAmount sum = monthlyAllocations.stream()
            .map(MonthlyAllocation::amount)
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
        
        // Defensive copy - ensure immutability
        // List.copyOf creates an unmodifiable list
        monthlyAllocations = List.copyOf(monthlyAllocations);
    }
    
    // Records automatically generate accessors (no "get" prefix):
    // - annualAmount()           ← Not getAnnualAmount()
    // - monthlyAllocations()     ← Not getMonthlyAllocations()
    // - baseMonthlyAmount()      ← Not getBaseMonthlyAmount()
    // - roundingAdjustment()     ← Not getRoundingAdjustment()
    // - placement()              ← Not getPlacement()
    
    // This is more concise and follows modern Java conventions!
    // NOTE: We could add domain query methods later if useful!
}
```

#### 🏛️ Architecture Notes

- **Aggregate Root Pattern** - Consistency boundary for allocation
- **Two-Level Access Control**:
  - Public record → Other layers can read it
  - Package-private constructor → Only domain can create it
- **Invariant Enforcement** - Sum validation in constructor
- **Immutability** - Defensive copying with `List.copyOf()`
- **Record Benefits** - Auto-generated accessors (no "get" prefix)
- **DDD Pattern** - Aggregate controls its own consistency

---

## Phase 3: Domain Service (TDD)

### 🎯 Why a Domain Service?

**When to use Domain Services:**
- Logic doesn't naturally belong to one entity
- Operates on multiple domain objects
- Stateless operations

**Our case:**
- Allocation logic involves multiple MonthlyAllocations
- Creates an aggregate
- Pure calculation, no state needed

#### 💬 What to Say

> "Now we'll implement the core allocation logic as a domain service. I'm using TDD - we'll write a failing test first, then make it pass, then refactor. The AllocationCalculator is a stateless service that encapsulates the allocation algorithm. It's pure domain logic with no Spring or infrastructure dependencies, which means we can test it without any framework overhead."

### Step 3.1: First Test (RED) - Perfect Division

#### 🎯 Reasoning

**Why start with the simplest case?**
- TDD principle: Start simple, add complexity gradually
- Builds confidence
- Clear success criteria

**Why test domain service directly?**
- No framework needed
- Fast execution
- Clear what we're testing

#### ✅ Benefits

- Documents expected behavior
- Catches regressions
- Guides implementation
- No mocking needed

#### 💬 What to Say

> "I'll start with the simplest test case - an amount that divides evenly by 12. This is $1,200, which should give us exactly $100 per month. I'm testing the domain service directly without any Spring infrastructure because it's pure domain logic. This test will fail initially since we haven't implemented the calculator yet - that's the 'Red' in Red-Green-Refactor."

#### 📝 Code

**File:** `src/test/java/com/rillet/codingchallenge/accounting/domain/AllocationCalculatorTest.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmount;

import static com.rillet.codingchallenge.accounting.Helpers.dollars;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AllocationCalculator domain service.
 * 
 * TESTING STRATEGY:
 * - Test domain service directly (no Spring needed)
 * - Start with simplest case, add complexity
 * - Follow TDD: Red-Green-Refactor
 * - Focus on business rules and edge cases
 */
class AllocationCalculatorTest {
    
    // No @Autowired - this is pure unit testing!
    private final AllocationCalculator calculator = new AllocationCalculator();

    /**
     * TEST 1: Perfect division (easiest case)
     * 
     * BUSINESS RULE: $1,200 / 12 months = $100/month exactly
     * 
     * WHY THIS TEST:
     * - Simplest case to start with
     * - No rounding issues
     * - Clear expected outcome
     * - Builds confidence before tackling harder cases
     */
    @Test
    void shouldCalculatePerfectAllocationWhenDivisibleByTwelve() {
        // Given: An amount that divides evenly by 12
        RevenueRecognitionRequest request = new RevenueRecognitionRequest(
            dollars(1200),
            RoundingPlacement.LAST
        );
        
        // When: We calculate the allocation
        RevenueAllocation allocation = calculator.calculate(request);
        
        // Then: All 12 months get exactly $100
        assertThat(allocation.monthlyAllocations())
            .hasSize(12)
            .allMatch(monthAlloc -> monthAlloc.amount().isEqualTo(dollars(100)));
        
        // And: Base amount is $100
        assertThat(allocation.baseMonthlyAmount()).isEqualTo(dollars(100));
        
        // And: No rounding adjustment needed
        assertThat(allocation.roundingAdjustment()).isEqualTo(dollars(0));
        
        // And: Total equals original (invariant checked by aggregate)
        assertThat(allocation.annualAmount()).isEqualTo(dollars(1200));
        
        // And: First month is JANUARY, last is DECEMBER
        assertThat(allocation.monthlyAllocations().get(0).month()).isEqualTo(Month.JANUARY);
        assertThat(allocation.monthlyAllocations().get(11).month()).isEqualTo(Month.DECEMBER);
    }
}
```

#### ✅ Run Test (Should FAIL)

```bash
./gradlew test --tests AllocationCalculatorTest
```

**Expected:** Test fails because `AllocationCalculator` doesn't exist yet.

---

### Step 3.2: Implementation (GREEN) - Make Test Pass

#### 🎯 Reasoning

**Implementation strategy:**
1. Calculate base amount (annual / 12, rounded)
2. Calculate what 11 months would total
3. Give remainder to adjustment month
4. Build list of MonthlyAllocations
5. Create aggregate (validates sum)

**Why this algorithm?**
- Guarantees sum = annual (mathematically sound)
- Simple and understandable
- Efficient (no iteration needed)

#### ✅ Benefits

- Exact precision (no accumulated rounding errors)
- Clear algorithm
- Single source of truth for adjustment
- Aggregate validates result

#### 💬 What to Say

> "Now I'll implement the calculator to make the test pass. The algorithm is straightforward: divide the annual amount by 12 and round to currency precision for the base amount. Then I calculate what 11 months would total, and give the remainder to the adjustment month. This mathematically guarantees the sum equals the annual amount. The aggregate's constructor will validate this invariant."

#### 📝 Code

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
 * This is a stateless service that encapsulates the core business logic for
 * splitting annual revenue into monthly recognition periods.
 * 
 * WHY A DOMAIN SERVICE:
 * - This logic doesn't naturally belong to any one entity
 * - It operates on multiple domain objects
 * - It's stateless - no instance variables needed
 * - It's pure domain logic - no infrastructure dependencies
 * 
 * DESIGN DECISIONS:
 * 
 * 1. Stateless:
 *    - No instance variables
 *    - Thread-safe by design
 *    - Can be singleton
 * 
 * 2. No Spring annotations:
 *    - Pure domain logic
 *    - Testable without Spring
 *    - Framework-agnostic
 * 
 * 3. Package visibility:
 *    - Public because it's used by application layer
 *    - But logically lives in domain package
 * 
 * ALGORITHM:
 * 1. Calculate base monthly amount (annual / 12, rounded)
 * 2. Calculate total for 11 base months
 * 3. Remainder = annual - (base * 11)
 * 4. One month gets base + remainder
 * 5. Result: sum always equals annual (mathematically guaranteed)
 */
public class AllocationCalculator {
    private static final int MONTHS_IN_YEAR = 12;

    /**
     * Calculates monthly revenue allocations based on request parameters.
     * 
     * ALGORITHM EXPLANATION:
     * 
     * Example: $1,000 annual
     * 1. Base = $1,000 / 12 = $83.33 (rounded)
     * 2. 11 months = $83.33 × 11 = $916.63
     * 3. Adjusted month = $1,000 - $916.63 = $83.37
     * 4. Result: 11 × $83.33 + 1 × $83.37 = $1,000 ✓
     * 
     * WHY THIS WORKS:
     * - No accumulated rounding errors
     * - Mathematically guaranteed to sum correctly
     * - Simple to understand and verify
     * - Works for any currency with any decimal precision
     * 
     * @param request The revenue recognition request
     * @return A RevenueAllocation aggregate (validates sum = annual)
     */
    public RevenueAllocation calculate(RevenueRecognitionRequest request) {
        MonetaryAmount amount = request.amount();
        RoundingPlacement placement = request.roundingPlacement();
        
        // Step 1: Calculate base monthly amount with proper rounding
        // WHY ROUND: Ensures amount respects currency precision (e.g., 2 decimals for USD)
        MonetaryAmount baseMonthlyAmount = CurrenciesHelper.rounded(
            amount.divide(MONTHS_IN_YEAR)
        );
        
        // Step 2: Determine which month gets the adjustment
        int adjustmentIndex = placement.calculateAdjustmentIndex(MONTHS_IN_YEAR);
        
        // Step 3: Calculate what would be allocated to 11 base months
        // WHY: This tells us how much to allocate to the other months
        MonetaryAmount elevenMonthsTotal = baseMonthlyAmount.multiply(MONTHS_IN_YEAR - 1);
        
        // Step 4: Remainder goes to the adjustment month
        // WHY: This guarantees sum = annual (no rounding error)
        // The adjusted month gets: annual - (base × 11)
        MonetaryAmount adjustedMonthAmount = amount.subtract(elevenMonthsTotal);
        
        // Step 5: Calculate the adjustment for metadata
        // This is the "extra" amount the adjusted month receives
        MonetaryAmount roundingAdjustment = adjustedMonthAmount.subtract(baseMonthlyAmount);
        
        // Step 6: Build monthly allocations
        List<MonthlyAllocation> allocations = buildAllocations(
            baseMonthlyAmount,
            adjustedMonthAmount,
            adjustmentIndex
        );
        
        // Step 7: Create and return aggregate
        // NOTE: The aggregate constructor validates that sum = annual
        // If our algorithm is wrong, we'll get an exception here!
        return new RevenueAllocation(
            amount,
            allocations,
            baseMonthlyAmount,
            roundingAdjustment,
            placement
        );
    }
    
    /**
     * Builds the list of 12 monthly allocations.
     * 
     * WHY A SEPARATE METHOD:
     * - Keeps main algorithm readable
     * - Single responsibility
     * - Easy to test independently if needed
     * 
     * @param baseAmount The standard monthly amount
     * @param adjustedAmount The amount for the adjustment month
     * @param adjustmentIndex Which month (0-11) gets the adjustment
     * @return List of 12 MonthlyAllocation objects
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
                // This month gets the adjusted amount
                allocations.add(new MonthlyAllocation(months[i], adjustedAmount));
            } else {
                // Standard month gets base amount
                allocations.add(new MonthlyAllocation(months[i], baseAmount));
            }
        }
        
        return allocations;
    }
}
```

#### ✅ Run Test (Should PASS)

```bash
./gradlew test --tests AllocationCalculatorTest
```

**Expected:** Test passes! ✅

---

### Step 3.3: More Tests (RED) - Rounding Cases

#### 💬 What to Say

> "Now that the basic case works, I'll add tests for the rounding scenarios. The real challenge is when amounts don't divide evenly - like $1,000 divided by 12. I'll test each placement option: LAST (default), FIRST, and MIDDLE."

#### 📝 Add to Test File

Add these tests to `AllocationCalculatorTest.java`:

```java
    /**
     * TEST 2: Division with rounding (the interesting case!)
     * 
     * BUSINESS RULE: $1,000 / 12 = $83.333...
     * - Rounded to cents: $83.33
     * - But $83.33 × 12 = $999.96 (lost $0.04!)
     * - Solution: One month gets $83.37 (has the extra $0.04)
     * 
     * WHY THIS TEST:
     * - Tests the core rounding logic
     * - Ensures sum = annual (critical invariant)
     * - Tests default placement (LAST)
     */
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
    
    /**
     * TEST 3: FIRST placement
     * 
     * BUSINESS RULE: User chooses to put rounding in first month
     * 
     * WHY THIS TEST:
     * - Tests configurable placement
     * - Ensures placement logic works correctly
     * - Same math, different month gets adjustment
     */
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
    
    /**
     * TEST 4: MIDDLE placement
     * 
     * BUSINESS RULE: User chooses to put rounding in middle month (June)
     * 
     * WHY THIS TEST:
     * - Tests all three placement options
     * - Ensures middle calculation is correct (index 5 = June)
     */
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
```

#### ✅ Run Tests

```bash
./gradlew test --tests AllocationCalculatorTest
```

**Expected:** All tests pass! ✅

---

### Step 3.4: Edge Case Tests

#### 💬 What to Say

> "Let me add some edge case tests to ensure robustness. These cover very small amounts, zero, amounts with many cents, and validation of invalid inputs. In financial software, edge cases are where bugs hide, so it's important to test them explicitly."

#### 📝 Add to Test File

```java
    /**
     * TEST 5: Very small amount
     * 
     * EDGE CASE: What if the amount is tiny? Like $0.05?
     * 
     * WHY THIS TEST:
     * - Tests precision at small scales
     * - Ensures algorithm works for any amount
     * - Catches potential division by zero or precision issues
     */
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
    
    /**
     * TEST 6: Zero amount
     * 
     * EDGE CASE: Zero revenue allocation (maybe a $0 contract?)
     * 
     * WHY THIS TEST:
     * - Ensures algorithm handles zero gracefully
     * - No division errors
     * - All months should be zero
     */
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
    
    /**
     * TEST 7: Amount with many cents
     * 
     * REALISTIC CASE: Not round numbers, realistic financial amounts
     * 
     * WHY THIS TEST:
     * - Tests real-world scenarios
     * - Ensures precision is maintained
     * - Sum must still equal original exactly
     */
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
    
    /**
     * TEST 8: Negative amount validation
     * 
     * VALIDATION TEST: Should reject invalid inputs
     * 
     * WHY THIS TEST:
     * - Ensures domain objects validate themselves
     * - Negative revenue doesn't make business sense
     * - Fail fast principle
     */
    @Test
    void shouldRejectNegativeAmount() {
        // When/Then: Negative amount throws exception
        assertThatThrownBy(() -> 
            new RevenueRecognitionRequest(dollars(-100), RoundingPlacement.LAST)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be negative");
    }
    
    /**
     * TEST 9: Null validation
     * 
     * VALIDATION TEST: Should reject nulls
     * 
     * WHY THIS TEST:
     * - Ensures no NullPointerExceptions later
     * - Fail fast principle
     * - Makes invalid states unrepresentable
     */
    @Test
    void shouldRejectNullAmount() {
        // When/Then: Null amount throws exception
        assertThatThrownBy(() -> 
            new RevenueRecognitionRequest(null, RoundingPlacement.LAST)
        )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("cannot be null");
    }
```

#### ✅ Run All Tests

```bash
./gradlew test --tests AllocationCalculatorTest
```

**Expected:** All 9 tests pass! ✅

---

## Phase 4: Application Layer

### 🎯 Why Application Layer?

**Hexagonal Architecture:**
- Application layer orchestrates use cases
- Sits between infrastructure and domain
- Defines ports (interfaces)
- Thin layer - no business logic

#### 💬 What to Say

> "Now I'll create the application layer. This is where we define the port interface and the use case implementation. The port defines what our application offers to the outside world, and the use case orchestrates the domain service. This layer is thin - it just coordinates, the actual business logic is in the domain."

### Step 4.1: Port Interface

#### 🎯 Reasoning

**Why an interface?**
- Hexagonal architecture pattern
- Adapters depend on port, not implementation
- Easy to swap implementations
- Clear contract

#### ✅ Benefits

- Dependency Inversion Principle
- Testable (can mock interface)
- Multiple adapters can use same port
- Clear API boundary

#### 📝 Code

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
 * 
 * WHY AN INTERFACE:
 * - Follows Dependency Inversion Principle
 * - Adapters depend on this abstraction, not concrete implementation
 * - Makes it easy to add new adapters (CLI, gRPC, message queue)
 * - Testable (can mock for adapter tests)
 * 
 * DESIGN NOTES:
 * - Lives in application package (not domain)
 * - Uses domain types (RevenueRecognitionRequest, RevenueAllocation)
 * - Simple, focused contract (Single Responsibility)
 */
public interface AllocateRevenue {
    /**
     * Allocates annual revenue into monthly recognition periods.
     * 
     * This is the primary use case for revenue recognition allocation.
     * 
     * @param request The revenue recognition parameters
     * @return The complete allocation aggregate
     * @throws IllegalArgumentException if request is invalid
     */
    RevenueAllocation execute(RevenueRecognitionRequest request);
}
```

---

### Step 4.2: Use Case Implementation

#### 🎯 Reasoning

**Why thin application service?**
- Orchestrates domain service
- No business logic here
- Just coordinates and delegates

**Why @Service?**
- Spring manages lifecycle
- Allows dependency injection
- Port implementation

#### ✅ Benefits

- Single Responsibility
- Easy to test
- Clear separation
- Extensible (can add logging, transactions, etc.)

#### 💬 What to Say

> "The use case implementation is intentionally thin. It just takes the request and delegates to the domain service. In the future, this is where we'd add cross-cutting concerns like transaction management, logging, or persistence. But the core business logic stays in the domain where it belongs."

#### 📝 Code

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
 * This is a THIN layer - business logic is in domain, not here.
 * 
 * WHY THIN:
 * - Application layer orchestrates, doesn't contain business logic
 * - Easy to understand and maintain
 * - Domain service does the heavy lifting
 * - This just coordinates and adds infrastructure concerns
 * 
 * RESPONSIBILITIES:
 * - Validate inputs (domain objects do this)
 * - Call domain service
 * - Handle transactions (if needed - not yet)
 * - Log operations (if needed - not yet)
 * - Persist results (if needed - not yet)
 * 
 * DESIGN NOTES:
 * - Implements the port interface
 * - Depends on domain service (AllocationCalculator)
 * - Uses Spring @Service for lifecycle management
 * - Constructor injection (testable, immutable)
 */
@Service
public class AllocateRevenueUseCase implements AllocateRevenue {
    private final AllocationCalculator calculator;
    
    /**
     * Constructor injection (preferred over field injection)
     * 
     * WHY CONSTRUCTOR INJECTION:
     * - Testable (can pass mock in tests)
     * - Immutable (field is final)
     * - Clear dependencies
     * - Fails fast if dependency missing
     */
    public AllocateRevenueUseCase(AllocationCalculator calculator) {
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
     * 
     * FUTURE ENHANCEMENTS:
     * - Add @Transactional when we have persistence
     * - Add logging: logger.info("Allocating {} for {}", request.annualAmount(), ...)
     * - Add metrics: metrics.record("allocation.executed", ...)
     * - Save to repository: repository.save(allocation)
     */
    @Override
    public RevenueAllocation execute(RevenueRecognitionRequest request) {
        // Request is self-validating (domain value object validates in constructor)
        
        // Delegate to domain service for calculation
        // This is where the actual business logic happens
        RevenueAllocation allocation = calculator.calculate(request);
        
        // Future: Could persist here
        // allocationRepository.save(allocation);
        
        return allocation;
    }
}
```

---

### Step 4.3: Domain Configuration

#### 🎯 Reasoning

**Why configuration class?**
- Domain service has no Spring annotations
- Need to register it as a bean
- Keeps domain pure

#### ✅ Benefits

- Domain stays framework-agnostic
- Explicit bean registration
- Easy to test without Spring

#### 📝 Code

**File:** `src/main/java/com/rillet/codingchallenge/accounting/domain/DomainConfiguration.java`

```java
package com.rillet.codingchallenge.accounting.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for domain services.
 * 
 * WHY THIS CLASS:
 * Our domain services (like AllocationCalculator) don't have Spring
 * annotations because they're pure domain logic. But we need them as
 * beans for dependency injection. This configuration class bridges that gap.
 * 
 * BENEFITS:
 * - Keeps domain pure (no Spring in domain classes)
 * - Explicit about what's a bean
 * - Easy to configure differently for tests
 * - Clear separation between domain and infrastructure
 */
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
```

#### ✅ Verify Application Layer

```bash
./gradlew compileJava
```

---

## Phase 5: Infrastructure Layer

### 🎯 Why Infrastructure Last?

**Hexagonal Architecture:**
- Infrastructure adapts the outside world to domain
- DTOs translate between HTTP/JSON and domain
- Controller orchestrates conversion

#### 💬 What to Say

> "Now I'll build the infrastructure layer - this is where HTTP and JSON come in. I'll create DTOs that adapt between the web layer and our domain. The DTOs know about JSON serialization, but the domain doesn't. This keeps our domain pure and testable without any HTTP concerns."

---

### Step 5.1: Request DTO

#### 🎯 Reasoning

**Why separate DTO from domain object?**
- HTTP concerns vs business concerns
- Different validation rules
- API evolution (backward compatibility)
- Clear adapter pattern

**Why conversion method?**
- Explicit translation between layers
- Clear responsibility
- Easy to test

#### ✅ Benefits

- Domain independent of JSON
- API can evolve separately
- Validation at boundary
- Clear layering

#### 📝 Code

**File:** Update `src/main/java/com/rillet/codingchallenge/accounting/infra/dataclasses/RequestDto.java`

```java
package com.rillet.codingchallenge.accounting.infra.dataclasses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;

import javax.money.MonetaryAmount;

/**
 * ADAPTER DTO: HTTP request structure for revenue allocation.
 * 
 * This is NOT a domain object - it's infrastructure.
 * It knows about JSON serialization, but domain doesn't.
 * 
 * WHY A SEPARATE DTO:
 * 1. Multiple parameters needed (amount + placement)
 * 2. API evolution (can add fields without breaking domain)
 * 3. Different validation rules (HTTP vs business)
 * 4. Clear separation of concerns
 * 
 * DESIGN DECISIONS:
 * - Record type: Immutable, minimal boilerplate
 * - @JsonCreator: Explicit deserialization control
 * - Default values: Handles optional fields
 * - Conversion method: Translates to domain object
 * 
 * JSON FORMAT:
 * {
 *   "amount": {
 *     "value": 1000,
 *     "currency": "USD"
 *   },
 *   "roundingPlacement": "LAST"  // Optional, defaults to LAST
 * }
 */
public record RequestDto(
    MonetaryAmount amount,
    RoundingPlacement roundingPlacement
) {
    /**
     * Jackson constructor for JSON deserialization.
     * 
     * WHY @JsonCreator:
     * - Explicit control over deserialization
     * - Can handle defaults
     * - Clear about what JSON fields we expect
     * 
     * DEFAULT HANDLING:
     * If roundingPlacement is not provided in JSON, we default to LAST.
     * This is standard accounting practice - adjustments at period end.
     */
    @JsonCreator
    public RequestDto(
        @JsonProperty("amount") MonetaryAmount amount,
        @JsonProperty("roundingPlacement") RoundingPlacement roundingPlacement
    ) {
        this.amount = amount;
        // Default to LAST if not specified (backwards compatible!)
        this.roundingPlacement = roundingPlacement != null 
            ? roundingPlacement 
            : RoundingPlacement.LAST;
    }
    
    /**
     * Converts this infrastructure DTO to domain value object.
     * 
     * WHY THIS METHOD:
     * - Explicit adapter pattern
     * - Clear translation between layers
     * - One place to change mapping if needed
     * - Makes the conversion testable
     * 
     * NAMING CONSISTENCY:
     * Both RequestDto and RevenueRecognitionRequest use "amount" (not "annualAmount").
     * This makes the conversion simple and clear. The context (revenue recognition
     * for annual allocation) makes it obvious this is the annual amount.
     * 
     * This is where HTTP concerns end and domain begins.
     */
    public RevenueRecognitionRequest toDomain() {
        return new RevenueRecognitionRequest(amount, roundingPlacement);
    }
}
```

---

### Step 5.2: Response DTO

#### 🎯 Reasoning

**Why include metadata?**
- UI needs to show base amount, remainder
- Users want to understand the calculation
- Transparency in financial software

**Why conversion method?**
- Translates domain → JSON
- Explicit adapter pattern
- Easy to change format

#### 📝 Code

**File:** Update `src/main/java/com/rillet/codingchallenge/accounting/infra/dataclasses/ResponseDto.java`

```java
package com.rillet.codingchallenge.accounting.infra.dataclasses;

import com.rillet.codingchallenge.accounting.domain.MonthlyAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;

import javax.money.MonetaryAmount;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ADAPTER DTO: HTTP response structure for revenue allocation.
 * 
 * This is infrastructure - knows about JSON, but domain doesn't.
 * 
 * WHY THIS STRUCTURE:
 * - UI needs monthly breakdown (allocations)
 * - UI needs summary info (base, remainder, total)
 * - Users want transparency (see how calculation was done)
 * - Makes API self-documenting
 * 
 * JSON FORMAT:
 * {
 *   "allocations": [
 *     {"month": "JANUARY", "amount": {...}, "hasRemainder": false},
 *     ...
 *   ],
 *   "baseMonthlyAmount": {...},
 *   "remainder": {...},
 *   "total": {...}
 * }
 */
public record ResponseDto(
    List<MonthlyAmountDto> allocations,
    MonetaryAmount baseMonthlyAmount,
    MonetaryAmount remainder,
    MonetaryAmount total
) {
    /**
     * Nested DTO for individual month allocation.
     * 
     * WHY NESTED:
     * - Keeps related structures together
     * - Clear namespace
     * - Only used in response context
     * 
     * START SIMPLE: Just month and amount. No flags or extra metadata yet.
     */
    public record MonthlyAmountDto(
        String month,
        MonetaryAmount amount
    ) {}

    /**
     * Converts domain aggregate to infrastructure DTO.
     * 
     * WHY STATIC FACTORY:
     * - Clear intent (creating FROM domain)
     * - One place for conversion logic
     * - Easy to test
     * - Follows adapter pattern
     * 
     * TRANSLATION:
     * - Domain MonthlyAllocation → DTO MonthlyAmountDto
     * - Month enum → String (simpler for JSON)
     * - All monetary amounts pass through (MoneyModule handles JSON)
     */
    public static ResponseDto fromDomain(RevenueAllocation allocation) {
        // Convert list of domain objects to list of DTOs
        List<MonthlyAmountDto> dtoAllocations = allocation.monthlyAllocations().stream()
            .map(ma -> new MonthlyAmountDto(
                ma.month().name(),     // Domain: Month enum → DTO: String
                ma.amount()            // Pass through MonetaryAmount
            ))
            .collect(Collectors.toList());
        
        // Build response with summary info
        return new ResponseDto(
            dtoAllocations,
            allocation.baseMonthlyAmount(),
            allocation.roundingAdjustment(),
            allocation.annualAmount()
        );
    }
}
```

---

### Step 5.3: Controller

#### 🎯 Reasoning

**Controller responsibilities:**
1. Receive HTTP request
2. Validate DTO structure
3. Convert DTO → domain
4. Call use case through port
5. Convert domain → DTO
6. Return HTTP response

**Why depend on port, not implementation?**
- Dependency Inversion Principle
- Can swap implementations
- Testable (mock the port)

#### ✅ Benefits

- Clear adapter pattern
- HTTP concerns isolated
- Easy to test
- Error handling centralized

#### 💬 What to Say

> "The controller is our web adapter. It depends on the port interface, not the concrete implementation. This makes it easy to test and follows the Dependency Inversion Principle. The controller handles HTTP concerns - status codes, error responses - but delegates business logic to the use case."

#### 📝 Code

**File:** Update `src/main/java/com/rillet/codingchallenge/accounting/infra/AmountsController.java`

```java
package com.rillet.codingchallenge.accounting.infra;

import com.rillet.codingchallenge.accounting.application.AllocateRevenue;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto;
import com.rillet.codingchallenge.accounting.infra.dataclasses.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WEB ADAPTER (Driving Adapter): HTTP/REST interface to the domain.
 * 
 * This is a hexagonal architecture adapter that:
 * 1. Receives HTTP requests
 * 2. Converts DTOs to domain objects
 * 3. Calls the port (AllocateRevenue)
 * 4. Converts domain objects back to DTOs
 * 5. Returns HTTP responses
 * 
 * DESIGN DECISIONS:
 * 
 * 1. Depends on PORT not implementation:
 *    - Field type is AllocateRevenue (interface)
 *    - Not AllocateRevenueUseCase (concrete class)
 *    - Follows Dependency Inversion Principle
 * 
 * 2. Explicit DTO conversion:
 *    - RequestDto → Domain (via toDomain())
 *    - Domain → ResponseDto (via fromDomain())
 *    - Clear adapter pattern
 * 
 * 3. HTTP error handling:
 *    - Domain validation errors → 400 Bad Request
 *    - Unexpected errors → 500 Internal Server Error
 *    - Could be enhanced with @ControllerAdvice
 * 
 * The controller knows about HTTP and JSON, but domain doesn't.
 * This is the boundary between infrastructure and application.
 */
@RestController
@RequestMapping("/amounts")
public class AmountsController {
    // NOTE: Type is AllocateRevenue (PORT), not AllocateRevenueUseCase (implementation)!
    // This is Dependency Inversion - depend on abstraction, not concrete class.
    private final AllocateRevenue allocateRevenue;

    /**
     * Constructor injection (preferred over field injection)
     * 
     * WHY CONSTRUCTOR INJECTION:
     * - Testable (can pass mock in tests)
     * - Immutable (field is final)
     * - Clear dependencies
     */
    public AmountsController(AllocateRevenue allocateRevenue) {
        this.allocateRevenue = allocateRevenue;
    }

    /**
     * HTTP POST endpoint for revenue allocation.
     * 
     * ADAPTER PATTERN IN ACTION:
     * 
     * 1. Receive HTTP request with JSON body
     *    ↓
     * 2. Jackson deserializes JSON → RequestDto (infrastructure)
     *    ↓
     * 3. Convert RequestDto → RevenueRecognitionRequest (domain)
     *    ↓
     * 4. Call domain through port interface
     *    ↓
     * 5. Receive RevenueAllocation (domain)
     *    ↓
     * 6. Convert RevenueAllocation → ResponseDto (infrastructure)
     *    ↓
     * 7. Jackson serializes ResponseDto → JSON
     *    ↓
     * 8. Return HTTP 200 OK with JSON response
     * 
     * ERROR HANDLING:
     * - IllegalArgumentException: Domain validation failed → 400 Bad Request
     * - Other exceptions: Unexpected error → 500 Internal Server Error
     * 
     * FUTURE IMPROVEMENTS:
     * - Use @Valid for bean validation
     * - Use @ControllerAdvice for global error handling
     * - Add logging
     * - Add metrics
     */
    @PostMapping
    public ResponseEntity<ResponseDto> allocateRevenue(
        @RequestBody RequestDto requestDto
    ) {
        try {
            // Step 1: Convert infrastructure DTO → Domain value object
            // This is the adapter pattern - translating between layers
            RevenueRecognitionRequest domainRequest = requestDto.toDomain();
            
            // Step 2: Call domain through port interface
            // We don't know (or care) what concrete implementation handles this
            // That's the beauty of Dependency Inversion!
            RevenueAllocation allocation = allocateRevenue.execute(domainRequest);
            
            // Step 3: Convert domain aggregate → Infrastructure DTO
            // Again, adapter pattern - translating layers
            ResponseDto responseDto = ResponseDto.fromDomain(allocation);
            
            // Step 4: Return HTTP 200 OK with JSON response
            return ResponseEntity.ok(responseDto);
            
        } catch (IllegalArgumentException e) {
            // Domain validation failed (e.g., negative amount)
            // Return 400 Bad Request (client error)
            // FUTURE: Could include error details in response body
            return ResponseEntity
                .badRequest()
                .build();
                
        } catch (Exception e) {
            // Unexpected error (should rarely happen)
            // Return 500 Internal Server Error
            // FUTURE: Log this error, alert monitoring
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}
```

#### ✅ Verify Compilation

```bash
./gradlew compileJava
./gradlew test
```

---

## Phase 6: UI Enhancement

### 🎯 What to Add

Current UI just shows JSON blob. Let's make it user-friendly:
1. Dropdown to select rounding placement
2. Table showing 12 months
3. Highlight adjusted month
4. Summary showing total, base, remainder

#### 💬 What to Say

> "Finally, I'll enhance the UI to make it user-friendly. I'll add a dropdown for placement selection and display the results in a table instead of raw JSON. The UI will highlight which month received the rounding adjustment, and show summary information so users understand how the calculation was done."

#### 📝 Code

**File:** Update `src/main/resources/public/index.html`

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
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold">Revenue Recognition Calculator</h1>
        <p class="text-gray-400 mt-2">Split annual revenue into monthly allocations</p>
      </div>

      <!-- Form -->
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
                  class="block flex-1 border border-gray-600 bg-gray-800 py-2 px-4 text-white placeholder:text-gray-500 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-r"
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
        
        // Find base amount to identify which month is adjusted
        const baseAmount = data.baseMonthlyAmount.number || data.baseMonthlyAmount.amount;
        
        data.allocations.forEach((allocation) => {
          const allocAmount = allocation.amount.number || allocation.amount.amount;
          const isAdjusted = Math.abs(allocAmount - baseAmount) > 0.001; // Small epsilon for comparison
          
          const row = document.createElement("tr");
          row.className = isAdjusted ? "bg-indigo-900 bg-opacity-30" : "";
          
          row.innerHTML = `
            <td class="px-4 py-3 text-sm font-medium">${formatMonth(allocation.month)}</td>
            <td class="px-4 py-3 text-sm text-right font-mono">${formatMoney(allocation.amount)}</td>
            <td class="px-4 py-3 text-sm text-center">
              ${isAdjusted
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
        // Handle both formats: {number: value} and {amount: value}
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

## 🔄 Optional Enhancements (If Time Permits)

### 💬 What to Say

> "The core functionality is working. If we have time, there are a couple of enhancements we could discuss. For example, we could add a flag to explicitly mark which month received the adjustment, rather than having the UI figure it out by comparing amounts. What do you think?"

### Enhancement 1: Add Adjustment Flag to MonthlyAllocation

**Why you might add this:**
- Makes it explicit which month was adjusted
- UI doesn't need to compare amounts
- More self-documenting

**Trade-off:**
- More complexity in domain model
- Adds a field that can be derived
- Current approach works fine

```java
public record MonthlyAllocation(
    Month month,
    MonetaryAmount amount,
    boolean hasRemainder  // Optional enhancement
) {
    // ... validation ...
}
```

### Enhancement 2: Add Helper Methods

**If you see repetitive code in tests:**

```java
public record MonthlyAllocation(...) {
    // Factory methods for clearer intent
    public static MonthlyAllocation standard(Month month, MonetaryAmount amount) {
        return new MonthlyAllocation(month, amount, false);
    }
    
    public static MonthlyAllocation adjusted(Month month, MonetaryAmount amount) {
        return new MonthlyAllocation(month, amount, true);
    }
}
```

**When to add:**
- ✅ If interviewer asks for more expressiveness
- ✅ If you see duplication
- ✅ As part of refactoring discussion
- ❌ Not needed for core functionality

---

## 🎉 Final Verification

### Run All Tests

```bash
./gradlew test
```

**Expected:** All tests pass ✅

### Run Application

```bash
./gradlew bootRun
```

Visit: http://localhost:8080

### Manual Test Cases

1. **Perfect division:** Enter `1200`, click Calculate
   - Should show 12 × $100
   
2. **Rounding (LAST):** Enter `1000`, select "Last Month", click Calculate
   - Should show 11 × $83.33 + December $83.37
   
3. **Rounding (FIRST):** Enter `1000`, select "First Month", click Calculate
   - Should show January $83.37 + 11 × $83.33
   
4. **Rounding (MIDDLE):** Enter `1000`, select "Middle Month", click Calculate
   - Should show June $83.37 + 11 × $83.33

---

## 🎓 Interview Summary

**What you've demonstrated:**

✅ **Hexagonal Architecture**
- Clear separation: Domain → Application → Infrastructure
- Port interface pattern
- Dependency Inversion Principle

✅ **Domain-Driven Design**
- Value Objects (immutable, self-validating)
- Aggregate Root (enforces invariants)
- Domain Service (stateless business logic)
- Ubiquitous Language

✅ **Test-Driven Development**
- Red-Green-Refactor cycles
- Domain tests without framework
- Clear test structure

✅ **Clean Code**
- Readable, well-documented
- Explicit conversions
- Single Responsibility
- Meaningful names

✅ **Financial Precision**
- Exact decimal arithmetic
- Sum validation
- No rounding errors

🚀 **You're ready for the Rillet interview!**
