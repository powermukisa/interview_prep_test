# 🏗️ Rillet System Design Interview - Complete Preparation Guide

## 📋 Interview Overview

**Company Context:** [Rillet](https://www.rillet.com/careers) is revolutionizing financial decision-making for high-growth companies, focusing on "zero-day close" and real-time financial reporting.

**Interview Format:**
- **Duration:** 60 minutes
- **Medium:** Miro (collaborative whiteboarding)
- **Focus:** Hexagonal Architecture + Domain-Driven Design
- **Assessment:**
  - Problem interpretation (how you understand requirements)
  - Architecture decisions (how you structure systems)
  - Trade-off discussions (why you choose one approach over another)
  - Technical depth (object-oriented design, SOLID principles)
  - Communication (can you explain clearly?)

**Your Advantage:** You just completed pair programming implementing hexagonal architecture - use that experience!

---

## 🎯 Key Resources to Study

### 1. Domain-Driven Hexagon ⭐ (Primary Resource)
**Link:** https://github.com/Sairyss/domain-driven-hexagon

**Essential sections:**
- Architecture overview (layers, dependencies)
- Application layer (ports, use cases)
- Domain layer (entities, aggregates, value objects)
- Infrastructure layer (adapters)
- Folder structure rationale

**Time:** 2-3 hours deep study

### 2. Thoughtworks Architecture Patterns
**Link:** https://www.thoughtworks.com/en-us/insights/blog/architecture/demystify-software-architecture-patterns

**Key patterns:**
- Layered Architecture
- Hexagonal Architecture
- Event-Driven Architecture
- Microservices

**Time:** 1 hour

### 3. Your Pair Programming Code
**Location:** `coding-challenge-main/java/`

**Why:** You already implemented hexagonal architecture with DDD - this is your reference implementation!

**Review:**
- Your domain layer (pure business logic)
- Application layer (ports & use cases)
- Infrastructure layer (controllers, DTOs)

**Time:** 30 minutes review

---

## 📚 Study Plan (10-12 hours)

### Day 1: Foundations (4-5 hours)

**Morning Session (2-3 hours):**
- [ ] Read [Domain-Driven Hexagon](https://github.com/Sairyss/domain-driven-hexagon) README thoroughly
- [ ] Focus on: Why hexagonal? What problems does it solve?
- [ ] Study dependency rules (domain has no dependencies)
- [ ] Understand ports (driving vs driven)

**Afternoon Session (2 hours):**
- [ ] Review your revenue recognition code
- [ ] Draw your architecture on paper
- [ ] Practice explaining it out loud (record yourself)
- [ ] Write down 3 alternative approaches you could have taken

### Day 2: Patterns & Practice (4-5 hours)

**Morning Session (2 hours):**
- [ ] Study DDD tactical patterns (Value Objects, Aggregates, Domain Services)
- [ ] Review SOLID principles with examples
- [ ] Study bounded contexts and context mapping

**Afternoon Session (2-3 hours):**
- [ ] Practice whiteboarding 3 systems (see practice questions below)
- [ ] Time yourself: 10 min clarify, 20 min design, 10 min discuss
- [ ] Practice explaining trade-offs

### Day 3: Mock Interview (2 hours)

- [ ] Pick a question from below
- [ ] Set 60-minute timer
- [ ] Use Miro or paper
- [ ] Present to friend or record yourself
- [ ] Review and identify gaps

---

## 🎬 Complete 60-Minute Interview Simulation

### Sample Question: "Design Rillet's Revenue Recognition Platform"

Let me walk you through a complete interview with **senior/staff-level responses**:

---

### **Minutes 0-5: Problem Clarification**

**Interviewer:** "We want to design Rillet's revenue recognition platform. Customers integrate with us, send contract data, and we automatically calculate revenue recognition schedules. How would you design this?"

**You (Senior/Staff Response):**
> "Great, let me make sure I understand the requirements and constraints before jumping into design. Can I ask a few clarifying questions?"
>
> **Scale:**
> "How many customers are we talking about? How many contracts per customer? Are we looking at thousands of calculations per day or millions?"
>
> **Integration:**
> "How do customers send data? REST API? File upload? Do they push to us or do we pull from their systems?"
>
> **Performance:**
> "What's the latency requirement? Is this real-time as they create contracts, or batch processing overnight?"
>
> **Compliance:**
> "Are there regulatory requirements? GAAP vs IFRS? Do we need audit trails?"
>
> **Scope:**
> "Should we design the calculation engine only, or also the customer-facing API, reporting, integrations, etc.?"

**Interviewer:** "Let's say 500 customers, each with 100-1000 contracts. REST API for integration. Near real-time - within a few seconds is fine. Yes, audit trails are critical. Let's focus on the core calculation and API, we can discuss other components as needed."

**You:**
> "Perfect, that helps me frame this. So we're looking at roughly 50-500K contracts, manageable scale. Real-time calculation with audit trails. Got it."

---

### **Minutes 5-15: High-Level Architecture**

**You (drawing on Miro):**
> "Let me start with the high-level architecture. I see this as several bounded contexts..."

**Draw:**
```
┌─────────────┐        ┌─────────────┐        ┌─────────────┐
│  Contract   │───────│  Revenue    │───────│  Reporting  │
│ Management  │        │ Recognition │        │             │
└─────────────┘        └─────────────┘        └─────────────┘
       │                      │                      │
       │                      │                      │
┌─────────────┐        ┌─────────────┐              │
│  Customer   │        │   Audit     │──────────────┘
│ Integration │        │     Log     │
└─────────────┘        └─────────────┘
```

**You:**
> "I'm identifying five main bounded contexts:
>
> 1. **Contract Management** - stores contract data (amount, term, start date)
> 2. **Revenue Recognition** - calculates allocation schedules (our core domain!)
> 3. **Customer Integration** - handles API, authentication, data validation
> 4. **Audit Log** - immutable event log for compliance
> 5. **Reporting** - queries for financial reports
>
> The **Revenue Recognition context** is the heart of the system - similar to what I built in the pair programming. Let me zoom into that."

**Interviewer:** "Good start. Let's drill into Revenue Recognition."

---

### **Minutes 15-30: Detailed Architecture (Hexagonal)**

**You (drawing hexagonal architecture):**

```
┌──────────────────────────────────────────────────────────┐
│              INFRASTRUCTURE LAYER                         │
│                                                           │
│  Driving Adapters:           Driven Adapters:            │
│  ┌──────────────┐           ┌──────────────┐            │
│  │ REST API     │           │  Contract    │            │
│  │ Controller   │           │  Repository  │            │
│  └──────────────┘           └──────────────┘            │
│  ┌──────────────┐           ┌──────────────┐            │
│  │ Message      │           │  Audit       │            │
│  │ Listener     │           │  Logger      │            │
│  └──────────────┘           └──────────────┘            │
└──────────────────────────────────────────────────────────┘
           ↓                           ↑
┌──────────────────────────────────────────────────────────┐
│              APPLICATION LAYER (Ports)                    │
│                                                           │
│  Driving Ports:              Driven Ports:               │
│  ┌──────────────┐           ┌──────────────┐            │
│  │ Calculate    │           │  Load        │            │
│  │ Recognition  │           │  Contract    │            │
│  └──────────────┘           └──────────────┘            │
│  ┌──────────────┐           ┌──────────────┐            │
│  │ Update       │           │  Publish     │            │
│  │ Schedule     │           │  Event       │            │
│  └──────────────┘           └──────────────┘            │
└──────────────────────────────────────────────────────────┘
           ↓                           ↑
┌──────────────────────────────────────────────────────────┐
│              DOMAIN LAYER (Pure Business Logic)          │
│                                                           │
│  Aggregates:                                             │
│  - RecognitionSchedule (root)                            │
│  - maintains invariant: sum = contract amount            │
│                                                           │
│  Value Objects:                                          │
│  - RecognitionPeriod                                     │
│  - AllocationStrategy                                    │
│  - ContractTerm                                          │
│                                                           │
│  Domain Services:                                        │
│  - RecognitionCalculator                                 │
│  - AllocationAlgorithm                                   │
└──────────────────────────────────────────────────────────┘
```

**You:**
> "This follows hexagonal architecture, same pattern I used in the pair programming challenge. Let me walk through each layer:
>
> **Domain Layer** - This is the core. RecognitionSchedule is an aggregate root that maintains the critical invariant: the sum of all period allocations must equal the contract amount. The RecognitionCalculator has the business logic for different allocation algorithms - straight-line, percentage of completion, etc. This layer has ZERO infrastructure dependencies - it's pure business logic.
>
> **Application Layer** - This defines ports. 'CalculateRecognition' is a driving port - what the application offers to the outside world. 'LoadContract' is a driven port - what the application needs from infrastructure. The use case implementations are thin - they orchestrate domain services and coordinate with repositories.
>
> **Infrastructure Layer** - These are the adapters. REST Controller is a driving adapter that calls into our application. Database Repository is a driven adapter that the application calls out to. Notice the dependency arrows - everything points inward toward the domain."

**Interviewer:** "Why not just use a traditional layered architecture?"

**You (Staff-level response):**
> "Great question. There are several trade-offs to consider:
>
> **Layered architecture** would be simpler upfront - Controller → Service → Repository. For a simple CRUD app, that's fine. But for Rillet's revenue recognition, we have complex business rules:
> - Different recognition methods (ASC 606 compliance)
> - Contract amendments and modifications
> - Multi-currency, multi-period calculations
> - Audit requirements
>
> **Hexagonal architecture** gives us:
> 1. **Testability** - I can test RecognitionCalculator with zero infrastructure. No database, no HTTP. Pure unit tests on business logic.
> 2. **Flexibility** - If we need to support GraphQL or gRPC later, we just add new adapters. Domain doesn't change.
> 3. **Domain focus** - The business logic is isolated and clear. New developers can understand the revenue recognition rules without knowing about Spring or PostgreSQL.
>
> The trade-off is **more indirection** - we have interfaces (ports) and extra layers. For a 10-year system with complex accounting rules, that's worth it. For a quick prototype, maybe not.
>
> Given Rillet's focus on financial accuracy and compliance, I'd choose hexagonal. The domain is complex enough to warrant the extra structure."

---

### **Minutes 30-40: Domain Model Deep Dive**

**Interviewer:** "Let's talk about the domain model. What would be your aggregates?"

**You (drawing):**

```
RecognitionSchedule (Aggregate Root)
├── scheduleId: UUID
├── contractId: ContractId
├── totalAmount: MonetaryAmount
├── recognitionPeriods: List<RecognitionPeriod>
├── strategy: RecognitionStrategy
└── status: ScheduleStatus

RecognitionPeriod (Value Object)
├── period: YearMonth
├── amount: MonetaryAmount
├── recognized: boolean
└── recognizedDate: LocalDate (nullable)

ContractTerm (Value Object)
├── startDate: LocalDate
├── endDate: LocalDate
└── duration() method

RecognitionStrategy (Value Object - enum)
├── STRAIGHT_LINE
├── PERCENTAGE_OF_COMPLETION
└── MILESTONE_BASED
```

**You:**
> "RecognitionSchedule is my aggregate root. It's the consistency boundary. Key invariants it maintains:
> 1. Sum of all period amounts equals contract total amount
> 2. Periods don't overlap
> 3. Can't modify a period that's already recognized
>
> I made it an aggregate because these invariants span multiple periods - they need to be enforced together, atomically.
>
> **RecognitionPeriod** is a value object, not an entity. Why? Two periods with the same values are interchangeable - there's no identity. If I have $100 for January, any $100 January period is the same.
>
> **ContractTerm** is a value object that encapsulates date logic. It can answer questions like 'how many months?' or 'is this date within the term?' This keeps that logic in one place."

**Interviewer:** "Why not make RecognitionPeriod an entity with its own ID?"

**You (Staff-level response):**
> "Good challenge! I considered that. Here's my reasoning:
>
> **As Entity:**
> - ✅ Could track history of changes to individual periods
> - ✅ Could have relationships to other entities
> - ❌ Adds complexity (IDs, repositories, lifecycle management)
> - ❌ Implies periods can be modified independently (they can't - aggregate controls them)
>
> **As Value Object:**
> - ✅ Immutable - can't be changed accidentally
> - ✅ Part of aggregate - lifecycle managed by RecognitionSchedule
> - ✅ Simpler - no IDs to manage
> - ✅ Matches the domain - periods are descriptions, not tracked entities
>
> The key insight is: we never work with a period in isolation. We always work with the entire schedule. If we need to modify a period, we do it through the aggregate, which validates invariants. This is the aggregate pattern - the root controls all access to its internals.
>
> If requirements change and we need to track individual period history, I'd reconsider. But for now, value object is simpler and matches the domain better."

---

### **Minutes 40-50: Integration & Scaling**

**Interviewer:** "How does this integrate with other systems? And how does it scale?"

**You (drawing integration):**

```
External Systems:
┌─────────────┐
│   CRM       │──── Contract Created event ────┐
│  (Salesforce)│                                │
└─────────────┘                                ▼
                                    ┌──────────────────┐
┌─────────────┐                    │  Event Bus       │
│   ERP       │──── Contract data ─│  (Kafka/SQS)     │
│             │                    └──────────────────┘
└─────────────┘                           │
                                          ▼
                                ┌─────────────────────┐
                                │  Revenue            │
                                │  Recognition        │
                                │  Service            │
                                └─────────────────────┘
                                          │
                                          ▼
                                ┌─────────────────────┐
                                │  Event Store/       │
                                │  Database           │
                                └─────────────────────┘
                                          │
                                          ▼
                                ┌─────────────────────┐
                                │  Analytics/         │
                                │  Reporting          │
                                └─────────────────────┘
```

**You:**
> "I see this as an event-driven architecture. Here's my thinking:
>
> **Integration Pattern:**
> When a contract is created in the CRM (Salesforce), it publishes a 'ContractCreated' event to our event bus. Our Revenue Recognition service subscribes to these events and processes them asynchronously.
>
> **Why async?**
> 1. **Decoupling** - CRM doesn't wait for recognition calculation
> 2. **Reliability** - If our service is down, events queue up
> 3. **Scalability** - Can process events at our own pace
>
> **Processing Flow:**
> 1. Event arrives → Message Listener (driving adapter)
> 2. Validate event → Application service
> 3. Calculate schedule → Domain service
> 4. Persist schedule → Repository (driven adapter)
> 5. Publish 'ScheduleCalculated' event → Event publisher
> 6. Analytics subscribes and updates reports
>
> **Scaling Strategy:**
>
> **At current scale (500 customers, 500K contracts):**
> - Single service instance handles this easily
> - PostgreSQL for persistence
> - Simple message queue (SQS or RabbitMQ)
>
> **At 10x scale (5M contracts):**
> - Horizontal scaling: multiple service instances
> - Sharding: partition by customer or contract ID
> - Read replicas for queries
> - Caching frequently-accessed schedules (Redis)
>
> **At 100x scale (50M contracts):**
> - Event sourcing: store all state changes as events
> - CQRS: separate write model (calculation) from read model (queries)
> - Materialized views for reports
> - Distributed cache
> - Consider microservices if team grows (separate by bounded context)
>
> **Key principle:** Start simple, add complexity only when needed. Right now, a modular monolith with hexagonal architecture would serve well."

**Interviewer:** "What about consistency? What if the calculation fails?"

**You (Staff-level response):**
> "Excellent question - this is about failure handling and consistency models.
>
> **Consistency Guarantees:**
>
> **Within the aggregate** (RecognitionSchedule):
> - Strong consistency - single database transaction
> - If calculation fails, nothing is persisted
> - ACID guarantees
>
> **Across contexts** (Contract Management → Revenue Recognition):
> - Eventual consistency - via events
> - If calculation fails, we retry with exponential backoff
> - Dead letter queue for persistent failures
>
> **Failure Scenarios:**
>
> **Scenario 1: Calculation fails (validation error)**
> ```
> 1. Receive ContractCreated event
> 2. Validate contract data
> 3. Validation fails (e.g., negative amount)
> 4. Publish ContractValidationFailed event
> 5. CRM receives event, notifies user
> 6. Original event goes to dead letter queue for investigation
> ```
>
> **Scenario 2: Database unavailable**
> ```
> 1. Calculation succeeds
> 2. Database persistence fails
> 3. Event is NOT acknowledged
> 4. Message queue redelivers
> 5. We calculate again (idempotent calculation)
> 6. Retry until success or max attempts
> ```
>
> **Idempotency Strategy:**
> ```java
> // Use contract ID + version as idempotency key
> @Transactional
> public void processContractEvent(ContractCreatedEvent event) {
>     String idempotencyKey = event.contractId() + "_" + event.version();
>     
>     if (processedEvents.contains(idempotencyKey)) {
>         log.info("Already processed, skipping");
>         return; // Idempotent!
>     }
>     
>     // Process...
>     processedEvents.add(idempotencyKey);
> }
> ```
>
> **Trade-offs:**
> - **Strong consistency everywhere:** Simple but tightly coupled, hard to scale
> - **Eventual consistency:** Complex but scalable and resilient
> - **My choice:** Eventual consistency via events. For financial systems, we need audit trails anyway, and events give us that for free."

---

### **Minutes 50-55: Domain Model Details**

**Interviewer:** "Show me the domain model for handling contract amendments."

**You (Staff-level response):**
> "Great question - this is where aggregate design gets interesting. Contract amendments are tricky because they affect past, present, and future periods.
>
> **Domain Challenge:**
> ```
> Original Contract: $12,000 annual, Jan-Dec
> Amendment in July: Increase to $15,000
> 
> Question: How do we recognize the extra $3,000?
> - Restate past months? (usually no - already in books)
> - Spread over remaining months? (yes - prospective recognition)
> - One-time catch-up? (depends on accounting policy)
> ```
>
> **Domain Model:**

```java
public class RecognitionSchedule {  // Aggregate Root
    private final ScheduleId id;
    private final ContractId contractId;
    private MonetaryAmount totalAmount;  // Can change with amendments
    private List<RecognitionPeriod> periods;  // Immutable once recognized
    private List<Amendment> amendments;  // Audit trail
    
    // Domain method
    public void amendContract(
        MonetaryAmount newAmount, 
        LocalDate effectiveDate,
        AmendmentStrategy strategy
    ) {
        // 1. Validate: can't amend closed periods
        // 2. Calculate impact: newAmount - totalAmount
        // 3. Allocate difference to future periods
        // 4. Record amendment for audit
        // 5. Emit ScheduleAmended event
    }
}

public enum AmendmentStrategy {
    PROSPECTIVE,     // Spread over remaining periods
    RETROSPECTIVE,   // Adjust past periods (rare)
    CATCH_UP        // One-time adjustment
}
```

**You:**
> "Key design decisions:
>
> 1. **Amendment is part of the aggregate** - it affects the schedule's consistency
> 2. **Periods that are recognized are immutable** - can't change what's in the books
> 3. **Strategy pattern** for different amendment approaches - accounting policies vary
> 4. **Domain events** for audit trail - every amendment emits an event
>
> **Invariant enforcement:**
> ```java
> public void amendContract(...) {
>     // Find first unrecognized period
>     LocalDate firstFuturePeriod = findFirstUnrecognizedPeriod();
>     
>     if (effectiveDate.isBefore(firstFuturePeriod)) {
>         throw new IllegalStateException(
>             "Cannot amend before " + firstFuturePeriod + 
>             " - periods already recognized"
>         );
>     }
>     
>     // Calculate new allocation for remaining periods
>     // Ensure sum still equals new total
> }
> ```
>
> This is **rich domain modeling** - the aggregate understands the business rules and enforces them."

**Interviewer:** "How do you handle concurrency? Two amendments at the same time?"

**You (Staff-level response):**
> "Excellent catch! This is a concurrency control problem. Several approaches:
>
> **Option 1: Optimistic Locking (My preference)**
> ```java
> public class RecognitionSchedule {
>     private Long version;  // Increments on each update
> }
> 
> // In repository
> UPDATE recognition_schedule 
> SET ... , version = version + 1
> WHERE schedule_id = ? AND version = ?
> ```
> If version doesn't match → conflict → retry or fail
>
> **Pros:** Works well at low contention, simple
> **Cons:** User might see "someone else modified this"
>
> **Option 2: Pessimistic Locking**
> ```sql
> SELECT ... FROM recognition_schedule 
> WHERE schedule_id = ? 
> FOR UPDATE
> ```
> **Pros:** Guarantees exclusive access
> **Cons:** Can cause waits, deadlocks
>
> **Option 3: Event Sourcing**
> All changes are events in order
> **Pros:** Natural audit trail, no concurrency issues
> **Cons:** Complexity, eventual consistency for queries
>
> **My choice for Rillet:**
> Start with **optimistic locking**. It's simple, handles 99% of cases (amendments are rare), and we can detect conflicts. If we see high contention, we can add **saga pattern** to coordinate amendments, or move to event sourcing for the audit trail benefits.
>
> Key principle: **Choose the simplest thing that works, measure, then optimize**."

---

### **Minutes 55-60: Wrap-up & Questions**

**Interviewer:** "Great! Any questions for us?"

**You (Staff-level questions):**
> "Yes, I have a few about Rillet's architecture:
>
> 1. **Current Architecture:** Are you using hexagonal architecture in production now, or is this aspirational? If so, what's working well and what challenges have you hit?
>
> 2. **Team Structure:** How are teams organized? By bounded context? Shared domain team?
>
> 3. **Event-Driven:** I proposed event-driven integration. Are you using events? What's your event bus? How do you handle versioning?
>
> 4. **Testing Strategy:** For domain-heavy logic like revenue recognition, how do you balance unit tests vs integration tests? Do you use consumer-driven contracts for service boundaries?
>
> 5. **Migration Path:** If you're not fully hexagonal yet, what's the migration strategy? Big bang rewrite or strangler fig pattern?"

**Why these questions show seniority:**
- They're technical and specific
- Show you're thinking about real implementation challenges
- Demonstrate interest in their actual architecture
- Open discussion about trade-offs they've faced

---

## 🎯 More Practice Questions with Answers

### Question: "Design a rate limiting system"

**Senior/Staff Answer Structure:**

**Clarify:**
- Per user? Per API key? Per IP?
- What limits? (requests/sec, requests/day)
- Distributed or single instance?

**Design:**
```
Domain:
- RateLimiter (domain service)
- RateLimit (value object): limit + window
- ThrottleStrategy (sliding window, token bucket, fixed window)

Infrastructure:
- Redis for shared state (distributed rate limiting)
- Local cache for common cases
```

**Implementation Discussion:**
> "For distributed rate limiting, I'd use Redis with Lua scripts for atomic increment-and-check. The domain service defines the algorithm, Redis is just a driven adapter for state storage. This way we can swap Redis for Hazelcast or even database without changing business logic."

**Scaling:**
- Low scale: Local memory (per instance)
- Medium scale: Redis (shared state)
- High scale: Redis cluster + local cache (2-tier)

---

### Question: "Design an ETL pipeline for financial data"

**Senior/Staff Answer:**

**Clarify:**
- Data volume? (GB vs TB vs PB)
- Frequency? (real-time vs batch)
- Transformations? (simple mapping vs complex business rules)
- SLA? (must complete by 6am, or best-effort)

**Architecture:**
```
┌─────────────────────────────────────────────────┐
│  Extract (Driving Adapter)                      │
│  - API Poller                                   │
│  - File Watcher                                 │
│  - Database CDC (Change Data Capture)           │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Application (Orchestration)                    │
│  - ETL Orchestrator (port)                      │
│  - ProcessBatch use case                        │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Domain (Transformation Logic)                  │
│  - DataTransformer (domain service)             │
│  - ValidationRules (domain knowledge)           │
│  - TransformedRecord (aggregate)                │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Load (Driven Adapter)                          │
│  - Database Bulk Loader                         │
│  - S3 Writer                                    │
│  - Data Warehouse Connector                     │
└─────────────────────────────────────────────────┘
```

**Key Discussion Points:**

**Batch Processing:**
> "For financial data, I'd use batch processing with checkpoints. Process in chunks of 1000 records, checkpoint after each chunk. If failure occurs, resume from last checkpoint. Store checkpoint state in database."

**Error Handling:**
> "Financial data needs careful error handling:
> - **Validation errors:** Skip record, log to error table, continue processing
> - **Transformation errors:** Retry with exponential backoff
> - **Load errors:** Transaction per batch, rollback on failure
> - **Critical:** Preserve source data, never lose records"

**Monitoring:**
> "Key metrics to track:
> - Records processed per minute
> - Error rate by type
> - Data freshness (lag behind source)
> - Pipeline execution time
> - Alert if: error rate >1%, execution time >SLA, no data for 2x expected interval"

---

### Question: "How do you handle data migrations in production?"

**Senior/Staff Answer:**

> "Data migrations in production, especially for financial systems, need careful planning. Here's my approach:
>
> **Phase 1: Expand (No Breaking Changes)**
> ```sql
> -- Add new column, keep old one
> ALTER TABLE recognition_schedule 
> ADD COLUMN new_strategy VARCHAR(50);
> 
> -- Both columns exist simultaneously
> ```
>
> **Phase 2: Dual Write**
> ```java
> public void saveSchedule(RecognitionSchedule schedule) {
>     // Write to BOTH old and new format
>     entity.setOldField(schedule.oldValue());
>     entity.setNewField(schedule.newValue());
>     repository.save(entity);
> }
> ```
>
> **Phase 3: Migrate Data**
> ```sql
> -- Background job, process in batches
> UPDATE recognition_schedule 
> SET new_strategy = CASE 
>     WHEN old_strategy = 1 THEN 'STRAIGHT_LINE'
>     WHEN old_strategy = 2 THEN 'MILESTONE'
>   END
> WHERE new_strategy IS NULL
> LIMIT 1000;
> ```
>
> **Phase 4: Switch Reads**
> ```java
> // Start reading from new column
> public RecognitionSchedule load(ScheduleId id) {
>     Entity entity = repository.findById(id);
>     // Read from new_strategy, fall back to old_strategy
>     String strategy = entity.getNewStrategy() != null 
>         ? entity.getNewStrategy() 
>         : convertOld(entity.getOldStrategy());
> }
> ```
>
> **Phase 5: Stop Dual Write**
> ```java
> // Only write new column
> entity.setNewField(schedule.newValue());
> ```
>
> **Phase 6: Contract (Remove Old)**
> ```sql
> -- After all reads switched, remove old column
> ALTER TABLE recognition_schedule 
> DROP COLUMN old_strategy;
> ```
>
> **Key Principles:**
> - **No downtime:** Always keep old and new working simultaneously
> - **Rollback plan:** Can revert at each phase
> - **Monitoring:** Track progress, watch for errors
> - **Testing:** Test on staging with production-size data
>
> For financial data, I'd also:
> - Take full backup before starting
> - Run data quality checks before and after
> - Keep audit log of all migrations
> - Get sign-off from accounting team"

---

### Question: "Design a distributed caching strategy"

**Senior/Staff Answer:**

> "Caching in a hexagonal architecture is interesting because we want to keep domain pure. Let me show you three approaches:
>
> **Approach 1: Decorator Pattern on Port** (My preference)
> ```java
> @Component
> public class CachedAllocateAmount implements AllocateAmount {
>     private final AllocateAmount delegate;  // Real implementation
>     private final Cache cache;
>     
>     public RevenueAllocation execute(RevenueRecognitionRequest request) {
>         String cacheKey = generateKey(request);
>         
>         // Check cache
>         Optional<RevenueAllocation> cached = cache.get(cacheKey);
>         if (cached.isPresent()) {
>             return cached.get();
>         }
>         
>         // Cache miss - delegate to real implementation
>         RevenueAllocation result = delegate.execute(request);
>         
>         // Store in cache
>         cache.put(cacheKey, result, Duration.ofHours(24));
>         
>         return result;
>     }
> }
> ```
>
> **Why decorator?**
> - ✅ Domain stays pure (no caching logic)
> - ✅ Transparent to callers
> - ✅ Can enable/disable via configuration
> - ✅ Easy to test (mock the cache)
>
> **Approach 2: Spring @Cacheable** (Simpler)
> ```java
> @Service
> public class AllocateAmountUseCase {
>     @Cacheable(value = "allocations", key = "#request")
>     public RevenueAllocation execute(RevenueRecognitionRequest request) {
>         // Spring handles caching transparently
>     }
> }
> ```
>
> **Why @Cacheable?**
> - ✅ Less code
> - ✅ Declarative
> - ❌ Couples to Spring
> - ❌ Less control over cache behavior
>
> **Cache Strategy:**
> - **Key:** Hash of (amount, placement, startMonth)
> - **TTL:** 24 hours (calculations don't change)
> - **Invalidation:** On contract amendment
> - **Storage:** Redis (distributed), with local L1 cache
>
> **Monitoring:**
> - Cache hit ratio (target: >80%)
> - Cache latency (target: <10ms)
> - Alert if hit ratio drops (might indicate key problem)
>
> **Trade-offs:**
> - **Caching at application layer:** Simple, but every adapter does own caching
> - **Caching at infrastructure:** Centralized, but need cache warming
> - **My choice:** Decorator pattern - gives control while keeping concerns separated"

---

## 📊 Miro Whiteboarding Techniques

### 1. Start with Context Map

**Always begin here:**
```
Draw boxes for bounded contexts
Show relationships
Add arrows for integration points
Label with integration type (sync/async, events, API)
```

**This shows:** You think in terms of domains and boundaries (DDD)

### 2. Use Hexagonal Template

**For each context, have this ready:**
```
Outer layer (yellow): Infrastructure
Middle layer (green): Application  
Inner layer (blue): Domain

Draw hexagon shape or concentric boxes
Label ports clearly
Show dependency arrows pointing inward
```

### 3. Progressive Detail

**Don't draw everything at once!**

**Step 1:** High-level boxes
**Step 2:** Add key components
**Step 3:** Add details when asked

**Example flow:**
```
"Let me start with bounded contexts..." (broad)
"Now let me zoom into this one..." (focused)
"The domain model here has..." (detailed)
```

### 4. Label Everything

**Use text labels:**
- Component names
- Responsibility (what does it do?)
- Technology choice (Redis, PostgreSQL, etc.)
- Why (add sticky notes with reasoning)

---

## 🎓 Senior/Staff Engineer Talking Points

### Show Strategic Thinking

**Instead of:** "I'd use microservices"

**Say:** 
> "I'd start with a modular monolith to validate domain boundaries, then extract to microservices when we have multiple teams or independent scaling needs. Premature microservices often lead to wrong boundaries and expensive refactoring."

### Show Trade-off Analysis

**Instead of:** "Event-driven architecture is better"

**Say:**
> "Event-driven architecture gives us loose coupling and scalability, but introduces complexity: eventual consistency, event versioning, and distributed debugging. For this use case [explain context], the benefits outweigh the costs because [specific reasoning]. However, if requirements were [different scenario], I'd choose sync communication."

### Show Production Experience

**Instead of:** "Store it in the database"

**Say:**
> "I'd store this in PostgreSQL with these considerations: partition by customer for query performance, point-in-time recovery for financial data, read replicas for reports. We'd need daily backups with 7-year retention for compliance. I'd also implement soft deletes - never physically delete financial records."

### Show You Learn from Experience

**Use phrases like:**
> "In a previous system, we tried X and learned that Y..."
>
> "The mistake I see teams make here is Z, so I'd..."
>
> "This is similar to the revenue recognition challenge - we solved it by..."

---

## 🎯 Common Pitfalls to Avoid

### ❌ Pitfall 1: Jumping to Solution

**Bad:** Immediately start drawing architecture

**Good:** Ask clarifying questions first, understand the problem fully

### ❌ Pitfall 2: Over-Engineering

**Bad:** "We need Kafka, Kubernetes, microservices, event sourcing..."

**Good:** "For this scale, a simple solution works. Here's when we'd need more complexity..."

### ❌ Pitfall 3: Ignoring Non-Functional Requirements

**Bad:** Only focus on features

**Good:** Ask about scale, latency, availability, consistency requirements

### ❌ Pitfall 4: Not Discussing Trade-offs

**Bad:** "This is the best approach"

**Good:** "I'm considering two approaches. Option A is simpler but doesn't scale well. Option B is more complex but handles growth. Given the requirements [X], I'd choose [Y] because..."

### ❌ Pitfall 5: Working in Silence

**Bad:** Draw for 10 minutes quietly

**Good:** Narrate your thinking: "I'm adding this component because... The trade-off here is..."

---

## 📚 Key Concepts Cheat Sheet

### Hexagonal Architecture

| Concept | Definition | Example |
|---------|------------|---------|
| **Port** | Interface defining boundary | `AllocateAmount` interface |
| **Driving Port** | What app offers (use cases) | `CalculateRecognition` |
| **Driven Port** | What app needs | `LoadContract` |
| **Driving Adapter** | Calls into app | REST Controller |
| **Driven Adapter** | Called by app | Database Repository |
| **Dependency Rule** | Always points inward | Infra → App → Domain |

### DDD Patterns

| Pattern | When to Use | Example |
|---------|-------------|---------|
| **Value Object** | No identity needed | MonetaryAmount, Address |
| **Entity** | Has identity, lifecycle | User, Order |
| **Aggregate** | Consistency boundary | Order + OrderLines together |
| **Domain Service** | Operates on multiple objects | PricingCalculator |
| **Domain Event** | Something happened | OrderPlaced, ContractAmended |
| **Repository** | Persist aggregates | OrderRepository |

### SOLID Principles

| Principle | Meaning | Example |
|-----------|---------|---------|
| **S** - Single Responsibility | One reason to change | RecognitionCalculator only calculates |
| **O** - Open/Closed | Extend without modifying | Add new RecognitionStrategy |
| **L** - Liskov Substitution | Subtypes interchangeable | Any impl of AllocateAmount works |
| **I** - Interface Segregation | Small, focused interfaces | AllocateAmount has one method |
| **D** - Dependency Inversion | Depend on abstractions | Controller → AllocateAmount (interface) |

---

## ✅ Final Preparation Checklist

### Technical Knowledge
- [ ] Can explain hexagonal architecture from memory
- [ ] Know the difference between ports and adapters
- [ ] Understand dependency inversion principle
- [ ] Can explain aggregates vs entities vs value objects
- [ ] Know when to use eventual consistency vs strong consistency
- [ ] Understand microservices trade-offs

### Communication Skills
- [ ] Practice explaining your pair programming solution (5 min pitch)
- [ ] Practice whiteboarding a system design (30 min)
- [ ] Record yourself and watch for: clarity, pacing, structure
- [ ] Prepare 5 good questions for the interviewer

### Miro Preparation
- [ ] Familiarize with Miro interface
- [ ] Practice creating boxes, arrows, text
- [ ] Set up a color scheme (domain, application, infrastructure)
- [ ] Have hexagonal architecture template ready

### Day Before
- [ ] Review this guide
- [ ] Skim Domain-Driven Hexagon README once more
- [ ] Draw your revenue recognition architecture from memory
- [ ] Get good sleep!

---

## 🚀 You're Ready!

**You've demonstrated:**
- ✅ Hexagonal architecture implementation
- ✅ Domain-Driven Design patterns
- ✅ Clean code principles
- ✅ TDD practices
- ✅ Strong communication

**Now show them you can:**
- Think at the system level
- Consider trade-offs
- Design for scale and maintainability
- Collaborate on architectural decisions

**Remember:**
- Ask clarifying questions
- Think out loud
- Discuss trade-offs
- Be open to feedback
- Use your pair programming experience as examples

---

## 📖 Key Resources

1. **[Domain-Driven Hexagon](https://github.com/Sairyss/domain-driven-hexagon)** - Your primary reference
2. **[Thoughtworks Architecture](https://www.thoughtworks.com/en-us/insights/blog/architecture/demystify-software-architecture-patterns)** - Pattern overview
3. **Your Implementation** - `coding-challenge-main/java/` - Real example!
4. **[Rillet Careers](https://www.rillet.com/careers)** - Company values and mission

---

**Good luck with your Rillet System Design interview! 🎉**

*Remember: They hired you for your technical skills and your ability to think about architecture. You've got this!* 💪

