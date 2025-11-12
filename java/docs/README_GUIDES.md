# 📘 Rillet Implementation Guides - Overview

This folder contains comprehensive guides for implementing the revenue recognition challenge using **Hexagonal Architecture** and **Domain-Driven Design** principles.

---

## 📂 Guide Structure

### 0️⃣ **[THE_PROBLEM_EXPLAINED.md](THE_PROBLEM_EXPLAINED.md)** - UNDERSTAND THE PROBLEM FIRST
**Simple explanation of what needs to be built (with examples).**

- What is the actual problem? (revenue recognition)
- What does the current UI show? (nothing!)
- What needs to be built?
- Step-by-step example with $1,000
- Why this matters for accounting

**Alternative:** [QUICK_VISUAL_SUMMARY.md](QUICK_VISUAL_SUMMARY.md) - Same info in visual format

**Use this to:** Understand what you're building before diving into code.

---

### 1️⃣ **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** - THEN BUILD IT
**Your step-by-step implementation checklist with copy-paste code.**

- Complete TDD walkthrough (Red-Green-Refactor)
- All code organized by phase
- Test commands after each step
- Ready for pair programming interview

**Use this to:** Actually build the solution step by step.

---

### 2️⃣ **[ARCHITECTURE_AND_DDD_GUIDE.md](ARCHITECTURE_AND_DDD_GUIDE.md)** - UNDERSTAND THE DESIGN
**Deep dive into Hexagonal Architecture and Domain-Driven Design.**

**Topics covered:**
- 🏛️ **Hexagonal Architecture (Ports & Adapters)**
  - Why domain has no infrastructure dependencies
  - Driving vs Driven ports
  - Adapter pattern in practice

- 🎯 **Domain-Driven Design Patterns**
  - Value Objects (immutable, self-validating)
  - Aggregate Root (RevenueAllocation maintains invariants)
  - Domain Services (pure business logic)
  - Application Services (orchestration)
  - Ubiquitous Language

**Use this to:** Understand WHY the code is structured this way (for interview discussions).

---

### 3️⃣ **[MONEY_MODULE_EXPLAINED.md](MONEY_MODULE_EXPLAINED.md)** - TECHNICAL DEEP DIVE
**Explains critical Jackson MoneyModule configuration.**

**Topics covered:**
- `.withAmountFieldName("value")` - Why JSON uses "value" not "amount"
- `.withQuotedDecimalNumbers()` - How this prevents $0.01 bugs
- Real examples of floating-point precision loss
- Why this matters for accounting/financial software

**Use this to:** Explain technical decisions about money handling (staff engineer level).

---

## 🎯 How to Use These Guides

### For the Interview

**Phase 1: Preparation (Before Interview)**
1. Read **ARCHITECTURE_AND_DDD_GUIDE.md** to understand the design philosophy
2. Skim **IMPLEMENTATION_GUIDE.md** to see the structure
3. Review **MONEY_MODULE_EXPLAINED.md** if you want to dive into precision handling

**Phase 2: During Interview (Pairing)**
1. Open **IMPLEMENTATION_GUIDE.md** side-by-side with your IDE
2. Follow the TDD phases step by step
3. Copy code blocks, run tests, explain your thinking
4. Reference architectural decisions from the DDD guide when discussing design

**Phase 3: Handoff Discussion (Second Interviewer)**
When the second interviewer joins:
- Explain the Hexagonal Architecture (show the diagram from DDD guide)
- Walk through domain layer → application layer → infrastructure layer
- Discuss why domain has no Spring dependencies
- Explain the aggregate root and its invariants

---

## 🏗️ Architecture Quick Reference

```
┌─────────────────────────────────────────────────────────┐
│              INFRASTRUCTURE LAYER                        │
│  (Adapters: Web Controller, DTOs, Future: Repositories) │
│                                                          │
│  Files:                                                  │
│  - AmountsController.java (Web Adapter)                 │
│  - AllocationRequestDto.java                            │
│  - AllocationResponseDto.java                           │
│  - index.html (UI)                                      │
└─────────────────────────────────────────────────────────┘
                          ↓ depends on
┌─────────────────────────────────────────────────────────┐
│              APPLICATION LAYER                           │
│  (Use Cases, Ports, Orchestration)                      │
│                                                          │
│  Files:                                                  │
│  - AllocateRevenue.java (Port Interface)                │
│  - AllocateRevenueUseCase.java (Implements Port)        │
└─────────────────────────────────────────────────────────┘
                          ↓ depends on
┌─────────────────────────────────────────────────────────┐
│              DOMAIN LAYER (Core Business Logic)          │
│  (Value Objects, Aggregates, Domain Services)           │
│  ⚠️  NO dependencies on Spring, HTTP, JSON, Database    │
│                                                          │
│  Files:                                                  │
│  - RoundingPlacement.java (Value Object - Enum)         │
│  - RevenueRecognitionRequest.java (Value Object)        │
│  - MonthlyAllocation.java (Value Object)                │
│  - RevenueAllocation.java (Aggregate Root)              │
│  - AllocationCalculator.java (Domain Service)           │
│  - CurrenciesHelper.java (Domain Helper)                │
└─────────────────────────────────────────────────────────┘
```

---

## 💡 Key Interview Talking Points

### 1. **Hexagonal Architecture**
> "I'm using Hexagonal Architecture to keep the domain independent of infrastructure. Notice how `AllocationCalculator` has no Spring dependencies? That's intentional - it's pure business logic that can be tested without any framework."

### 2. **Domain-Driven Design**
> "I'm using DDD patterns: `RevenueRecognitionRequest` is a Value Object - it's immutable and self-validating. `RevenueAllocation` is an Aggregate Root that maintains the invariant that all monthly amounts sum to the annual total."

### 3. **Ports & Adapters**
> "`AllocateRevenue` is a port - it's the interface between our application and domain. The controller depends on this port, not on the concrete implementation. This makes it easy to add new adapters like a CLI or gRPC interface later."

### 4. **Ubiquitous Language**
> "I'm using revenue recognition terminology throughout: 'recognition period' instead of 'month', 'recognized amount' instead of 'allocated amount'. This creates a shared language between developers and domain experts."

### 5. **Value Objects**
> "All our domain objects are immutable value objects. `MonthlyAllocation` uses factory methods like `standard()` and `adjusted()` to express intent clearly. They're self-validating, so invalid states can't exist."

### 6. **Aggregate Root**
> "`RevenueAllocation` is an aggregate root - it's the consistency boundary. Its constructor validates that the sum of allocations equals the annual amount. If that invariant is violated, the aggregate can't be created."

### 7. **Testability**
> "Because the domain has no infrastructure dependencies, I can test `AllocationCalculator` with pure unit tests - no Spring, no mocks. The adapter layer (controller) is tested separately with Spring MockMvc."

---

## 🧪 Testing Layers

```
┌─────────────────────────────────────────────────┐
│  ADAPTER TESTS (@WebMvcTest)                    │
│  - Test HTTP contracts                          │
│  - Mock the port (AllocateRevenue)              │
│  - Verify JSON serialization                    │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  APPLICATION TESTS (@SpringBootTest)            │
│  - Test use case orchestration                  │
│  - Integration between layers                   │
│  - Real beans, no mocks                         │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  DOMAIN TESTS (Plain JUnit)                     │
│  - Pure unit tests                              │
│  - No Spring, no mocks                          │
│  - Fast, isolated, business logic focused       │
└─────────────────────────────────────────────────┘
```

---

## 📋 Implementation Checklist

Use this to track your progress:

- [ ] **Phase 0:** Verify project setup and tests run
- [ ] **Phase 1:** Domain Layer
  - [ ] RoundingPlacement enum (Value Object)
  - [ ] RevenueRecognitionRequest (Value Object)
  - [ ] MonthlyAllocation (Value Object)
  - [ ] RevenueAllocation (Aggregate Root)
  - [ ] AllocationCalculator (Domain Service)
- [ ] **Phase 2:** Application Layer
  - [ ] AllocateRevenue interface (Port)
  - [ ] AllocateRevenueUseCase (Application Service)
  - [ ] DomainConfiguration (Bean setup)
- [ ] **Phase 3:** TDD Implementation
  - [ ] Test: Perfect allocation (1200/12)
  - [ ] Test: Rounding with LAST placement
  - [ ] Test: FIRST placement
  - [ ] Test: MIDDLE placement
  - [ ] Test: Edge cases (zero, small amounts, validation)
- [ ] **Phase 4:** Infrastructure Layer
  - [ ] AllocationRequestDto (Adapter DTO)
  - [ ] AllocationResponseDto (Adapter DTO)
  - [ ] AmountsController (Web Adapter)
  - [ ] Controller tests (@WebMvcTest)
- [ ] **Phase 5:** UI Enhancement
  - [ ] Update index.html with placement dropdown
  - [ ] Add results table with highlighting
  - [ ] Display summary metadata
- [ ] **Phase 6:** Polish
  - [ ] Run all tests
  - [ ] Manual UI testing
  - [ ] Code review for clarity

---

## 🚀 Quick Start Commands

```bash
# Navigate to Java project
cd /Users/power/code/rillet/coding-challenge-main/java

# Verify setup
./gradlew test

# During implementation - run tests frequently
./gradlew test --tests AllocateAmountTest

# Run application to test UI
./gradlew bootRun
# Then visit: http://localhost:8080

# Clean build
./gradlew clean test
```

---

## 🎓 Learning Outcomes

By following these guides, you'll demonstrate:

✅ **Hexagonal Architecture** - Clean separation of concerns  
✅ **Domain-Driven Design** - Rich domain model with ubiquitous language  
✅ **Test-Driven Development** - Red-Green-Refactor cycles  
✅ **SOLID Principles** - Especially Dependency Inversion  
✅ **Value Objects** - Immutability and self-validation  
✅ **Aggregate Patterns** - Consistency boundaries  
✅ **Clean Code** - Readable, maintainable, well-documented  
✅ **Financial Precision** - Understanding decimal arithmetic  
✅ **API Design** - Clear contracts with DTOs  
✅ **Staff Engineer Thinking** - Architecture for long-term maintainability  

---

## ❓ Questions for Discussion

Be ready to discuss:

1. **"Why did you choose Hexagonal Architecture?"**
   - Testability, flexibility, domain independence

2. **"What's the difference between AllocateRevenueUseCase and AllocationCalculator?"**
   - Application service vs domain service
   - Orchestration vs business logic

3. **"Why is RevenueAllocation package-private constructor?"**
   - Aggregate can only be created by domain service
   - Ensures invariants are always maintained

4. **"How would you add quarterly allocation?"**
   - Extend `RevenueRecognitionRequest` with period type
   - Update calculator to handle different periods
   - Domain logic remains isolated

5. **"Why do you convert DTOs to domain objects?"**
   - DTOs are infrastructure concerns (JSON)
   - Domain objects are business concepts
   - Clear separation between layers

---

## 📞 Need Help?

- Review the **ARCHITECTURE_AND_DDD_GUIDE.md** for design rationale
- Check **MONEY_MODULE_EXPLAINED.md** for precision handling details
- Follow **IMPLEMENTATION_GUIDE.md** step by step

Good luck with your interview! 🎉

