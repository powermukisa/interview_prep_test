# 🎯 START HERE - Complete Overview

## 🤔 Not Sure What The Problem Is?

**👉 Read [THE_PROBLEM_EXPLAINED.md](THE_PROBLEM_EXPLAINED.md) first!**

It explains in simple terms:
- What the actual problem is (with examples)
- What the UI currently shows
- What needs to be built
- Step-by-step walkthrough of how it should work

**Then come back here for the technical implementation.**

---

## ✅ What You Asked For - Status

### 1. ✅ MoneyModule Configuration Explained
**See: [MONEY_MODULE_EXPLAINED.md](MONEY_MODULE_EXPLAINED.md)**

Your questions:
- **`.withAmountFieldName("value")`** - Why JSON has "value" not "amount"
- **`.withQuotedDecimalNumbers()`** - How this prevents floating-point precision loss

**Answer Summary:**

#### `.withAmountFieldName("value")` 
**Without it:**
```json
{"amount": {"amount": 100, "currency": "USD"}}  ← Confusing! "amount.amount"
```

**With it:**
```json
{"amount": {"value": 100, "currency": "USD"}}   ← Clear! "amount.value"
```

#### `.withQuotedDecimalNumbers()`
**Without it (DANGEROUS ⚠️):**
```json
{"value": 83.33, "currency": "USD"}  ← JavaScript Number (loses precision)
```
```javascript
83.33 * 100 = 8332.999999999999  ← BUG! Should be 8333
0.1 + 0.2 = 0.30000000000000004  ← WRONG!
```

**With it (SAFE ✅):**
```json
{"value": "83.33", "currency": "USD"}  ← String (exact decimal)
```
```javascript
new Decimal("83.33").times(100) = "8333.00"  ← CORRECT!
```

**Why Rillet cares:** Accounting requires EXACT precision. One $0.01 error in a million-dollar system = audit failure.

---

### 2. ✅ Hexagonal Architecture & DDD Implementation
**See: [ARCHITECTURE_AND_DDD_GUIDE.md](ARCHITECTURE_AND_DDD_GUIDE.md)**

The implementation guide has been **completely restructured** to follow Rillet's architectural pillars:

#### 🏛️ Hexagonal Architecture (Ports & Adapters)

**Structure:**
```
Infrastructure → Application (Ports) → Domain
```

**Key changes from original plan:**

| Original (Basic) | Enhanced (Hexagonal + DDD) |
|-----------------|---------------------------|
| `AllocateAmount` does everything | **Separated layers:** Calculator (domain), UseCase (app), Controller (infra) |
| Domain calls Spring | **Domain has ZERO infrastructure dependencies** |
| No interfaces | **Port interface** (`AllocateRevenue`) defines contract |
| Direct coupling | **Adapter pattern** with explicit DTO conversions |
| Returns lists | **Aggregate Root** ensures business invariants |

**Files organized by layer:**

**Domain Layer** (no Spring, no HTTP, pure business logic):
- `RoundingPlacement.java` - Value Object (enum)
- `RevenueRecognitionRequest.java` - Value Object (immutable, validated)
- `MonthlyAllocation.java` - Value Object
- `RevenueAllocation.java` - **Aggregate Root** (maintains sum = annual)
- `AllocationCalculator.java` - **Domain Service** (pure calculation logic)

**Application Layer** (orchestration):
- `AllocateRevenue.java` - **Port Interface** (hexagonal boundary)
- `AllocateRevenueUseCase.java` - **Application Service** (implements port)

**Infrastructure Layer** (adapters):
- `AmountsController.java` - **Web Adapter** (HTTP → domain)
- `AllocationRequestDto.java` - Infrastructure DTO
- `AllocationResponseDto.java` - Infrastructure DTO
- `index.html` - UI

#### 🎯 Domain-Driven Design Patterns

**1. Ubiquitous Language**
```java
// Before (technical)
MonthlyAmount, split, divide, remainder

// After (domain language)
MonthlyAllocation, recognitionPeriod, recognizedAmount, roundingAdjustment
```

**2. Value Objects** (immutable, self-validating)
```java
public record RevenueRecognitionRequest(
    MonetaryAmount annualAmount,
    RoundingPlacement roundingPlacement
) {
    public RevenueRecognitionRequest {
        // Self-validating!
        Objects.requireNonNull(annualAmount, "Annual amount cannot be null");
        if (annualAmount.isNegative()) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}
```

**3. Aggregate Root** (consistency boundary)
```java
public class RevenueAllocation {
    // Package-private constructor - can only be created by domain service
    RevenueAllocation(...) {
        // Enforces INVARIANT: Sum of allocations = annual amount
        MonetaryAmount sum = allocations.stream()...;
        if (!sum.isEqualTo(annualAmount)) {
            throw new IllegalStateException("Invariant violated!");
        }
    }
}
```

**4. Domain Service** (stateless business logic)
```java
public class AllocationCalculator {
    // Pure domain logic - NO Spring dependencies
    // Can be tested without any framework
    public RevenueAllocation calculate(RevenueRecognitionRequest request) {
        // Business rules here
    }
}
```

**5. Application Service** (orchestration)
```java
@Service
public class AllocateRevenueUseCase implements AllocateRevenue {
    private final AllocationCalculator calculator;
    
    public RevenueAllocation execute(RevenueRecognitionRequest request) {
        // Thin orchestration layer
        return calculator.calculate(request);
        // Future: could add repository.save(allocation) here
    }
}
```

---

## 📚 Your Complete Guide System

I've created **4 comprehensive guides** for you:

### 1. **[README_GUIDES.md](README_GUIDES.md)** ⬅️ Read this first!
**Overview of all guides and how to use them**
- Quick reference to all documents
- Architecture diagram
- Interview talking points
- Implementation checklist

### 2. **[IMPLEMENTATION_GUIDE.md](OLD_IMPLEMENTATION_GUIDE.md)** ⬅️ Copy-paste code here!
**Step-by-step implementation with all code**
- Phase-by-phase walkthrough
- Complete code for every file
- Test commands after each step
- Ready for pair programming

### 3. **[ARCHITECTURE_AND_DDD_GUIDE.md](ARCHITECTURE_AND_DDD_GUIDE.md)** ⬅️ Understand the WHY!
**Deep dive into Hexagonal Architecture and DDD**
- Hexagonal Architecture principles
- DDD tactical patterns explained
- Value Objects, Aggregates, Domain Services
- Why domain has no infrastructure dependencies
- Interview discussion points

### 4. **[MONEY_MODULE_EXPLAINED.md](MONEY_MODULE_EXPLAINED.md)** ⬅️ Technical deep dive!
**Jackson MoneyModule configuration explained**
- `.withAmountFieldName("value")` examples
- `.withQuotedDecimalNumbers()` precision safety
- Real-world examples of $0.01 bugs
- Why this matters for financial software

---

## 🎯 Interview Strategy

### Before the Interview
1. ✅ Read **README_GUIDES.md** (overview)
2. ✅ Study **ARCHITECTURE_AND_DDD_GUIDE.md** (understand WHY)
3. ✅ Skim **IMPLEMENTATION_GUIDE.md** (know the structure)

### During Pair Programming
1. Open **IMPLEMENTATION_GUIDE.md** side-by-side with IDE
2. Follow TDD phases (Red-Green-Refactor)
3. Explain architectural decisions as you code
4. Copy code, run tests, discuss trade-offs

### When Second Interviewer Joins
Walk them through:
1. **Hexagonal Architecture**: "Domain → Application → Infrastructure"
2. **DDD Patterns**: "Value Objects are immutable, Aggregate Root maintains invariants"
3. **Why domain is pure**: "AllocationCalculator has no Spring dependencies - pure business logic"
4. **Testing strategy**: "Domain tested with plain JUnit, adapters with Spring MockMvc"

---

## 💎 Key Differentiators (Staff-Level Engineering)

What makes this implementation stand out:

### 1. **True Hexagonal Architecture**
```java
// Domain has NO infrastructure dependencies
public class AllocationCalculator {
    // No @Service annotation
    // No Spring imports
    // No HTTP knowledge
    // Pure business logic
}
```

### 2. **Rich Domain Model**
```java
// Domain objects have BEHAVIOR, not just data
public enum RoundingPlacement {
    FIRST, LAST, MIDDLE;
    
    // Business logic belongs in domain!
    public int calculateAdjustmentIndex(int totalPeriods) {
        return switch (this) { ... };
    }
}
```

### 3. **Aggregate Root with Invariants**
```java
// RevenueAllocation is a consistency boundary
RevenueAllocation(...) {
    // INVARIANT: sum must equal annual
    if (!sum.isEqualTo(annualAmount)) {
        throw new IllegalStateException(...);
    }
}
```

### 4. **Explicit Ports**
```java
// Port interface decouples layers
public interface AllocateRevenue {
    RevenueAllocation execute(RevenueRecognitionRequest request);
}

// Controller depends on port, not implementation
@RestController
public class AmountsController {
    private final AllocateRevenue allocateRevenue;  // ← Port, not UseCase
}
```

### 5. **Value Object Self-Validation**
```java
public record RevenueRecognitionRequest(...) {
    public RevenueRecognitionRequest {
        // Fails fast - invalid objects cannot exist
        Objects.requireNonNull(annualAmount, "...");
        if (annualAmount.isNegative()) throw new IllegalArgumentException(...);
    }
}
```

---

## 🧪 Testing Pyramid

```
┌─────────────────────────────────────┐
│  UI Tests (Manual)                  │  ← Verify browser works
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Adapter Tests (@WebMvcTest)        │  ← HTTP, JSON contracts
│  - Mock the port                    │
│  - Test controller only             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Application Tests (@SpringBootTest)│  ← Integration between layers
│  - Real beans, wired together       │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Domain Tests (Plain JUnit)         │  ← Pure unit tests (MAJORITY)
│  - No Spring, no mocks              │  ← Fast, focused, isolated
│  - Test business logic              │
└─────────────────────────────────────┘
```

---

## 🚦 How to Proceed

**Option A: Deep Understanding First (Recommended)**
```
1. Read ARCHITECTURE_AND_DDD_GUIDE.md (20 mins)
2. Read MONEY_MODULE_EXPLAINED.md (10 mins)
3. Follow IMPLEMENTATION_GUIDE.md step-by-step (2-3 hours)
```

**Option B: Dive Right In**
```
1. Skim README_GUIDES.md (5 mins)
2. Start IMPLEMENTATION_GUIDE.md immediately
3. Reference architecture guide when needed
```

---

## 💬 Questions You Can Confidently Answer

After following these guides, you'll be ready to discuss:

✅ "Why did you separate AllocationCalculator from the use case?"  
✅ "What's the purpose of the AllocateRevenue interface?"  
✅ "Why is RevenueAllocation's constructor package-private?"  
✅ "How would you add persistence to this system?"  
✅ "Why convert DTOs to domain objects instead of using them directly?"  
✅ "What's the difference between a domain service and application service?"  
✅ "How does the aggregate root maintain invariants?"  
✅ "Why use quoted decimal numbers for monetary amounts?"  
✅ "How would you extend this to support quarterly allocation?"  
✅ "What testing strategy did you use and why?"  

---

## 🎓 What This Demonstrates

By implementing this solution, you show understanding of:

🏆 **Hexagonal Architecture** - Ports, adapters, dependency inversion  
🏆 **Domain-Driven Design** - Value objects, aggregates, ubiquitous language  
🏆 **SOLID Principles** - Especially Single Responsibility and Dependency Inversion  
🏆 **Test-Driven Development** - Red-Green-Refactor discipline  
🏆 **Financial Software** - Decimal precision, audit-safe calculations  
🏆 **Clean Architecture** - Separation of concerns, testability  
🏆 **Staff Engineer Thinking** - Long-term maintainability over quick hacks  

---

## 🚀 Ready to Start?

```bash
cd /Users/power/code/rillet/coding-challenge-main/java

# Verify setup
./gradlew test

# Open your IDE and start with:
# OLD_IMPLEMENTATION_GUIDE.md - Phase 1, Step 1.1
```

**Good luck with your Rillet interview! 🎉**

You've got comprehensive guides that demonstrate both technical depth and architectural maturity. Rillet will be impressed! 💪

