# 🎯 The Problem - Explained Simply

## What Is The Actual Problem?

**Imagine you're an accountant at a company.**

Your company just sold an **annual subscription** for **$1,000**.  
The customer paid all $1,000 upfront in January.

But here's the accounting rule:  
**You can't recognize all $1,000 as revenue in January!**

Why? Because you haven't delivered the service yet. The customer paid for **12 months of service**.

So you need to **spread that $1,000 across 12 months** to show revenue as you deliver the service each month.

---

## 🧮 The Math Problem

### Easy Case: $1,200 ÷ 12 months

```
$1,200 ÷ 12 = $100 per month (clean division!)

January:   $100
February:  $100
March:     $100
April:     $100
May:       $100
June:      $100
July:      $100
August:    $100
September: $100
October:   $100
November:  $100
December:  $100
-----------------
TOTAL:     $1,200 ✅ (matches original!)
```

Easy! No problem.

### Hard Case: $1,000 ÷ 12 months

```
$1,000 ÷ 12 = $83.3333333333... (goes on forever!)

But we can only use cents: $83.33 or $83.34
```

If we use $83.33 for every month:
```
$83.33 × 12 = $999.96 ❌ (we lost $0.04!)
```

If we use $83.34 for every month:
```
$83.34 × 12 = $1,000.08 ❌ (we added $0.08!)
```

**The problem:** We have **leftover pennies** (a "rounding difference").

**The solution:** Put those extra pennies in ONE specific month.

---

## 💡 The Solution

### Strategy: Distribute the Remainder

```
Step 1: Calculate base amount
$1,000 ÷ 12 = $83.33 (rounded to cents)

Step 2: Calculate what 11 months would be
$83.33 × 11 = $916.63

Step 3: Give the remainder to one month
$1,000 - $916.63 = $83.37 (goes to the "adjustment month")

Step 4: Result
11 months get $83.33
1 month gets $83.37 (the extra $0.04)
-----------------
TOTAL: $1,000 ✅ (perfect!)
```

### Where to Put the Extra Pennies?

The user should be able to choose:

**Option 1: LAST month (default)**
```
January:   $83.33
February:  $83.33
...
November:  $83.33
December:  $83.37 ← Extra $0.04 here!
```

**Option 2: FIRST month**
```
January:   $83.37 ← Extra $0.04 here!
February:  $83.33
March:     $83.33
...
December:  $83.33
```

**Option 3: MIDDLE month**
```
January:   $83.33
February:  $83.33
...
June:      $83.37 ← Extra $0.04 here!
July:      $83.33
...
December:  $83.33
```

---

## 🖥️ What Currently Exists

### Current UI (Before Any Changes)

When you run `./gradlew bootRun` and open http://localhost:8080, you see:

```
┌─────────────────────────────────────────┐
│  Coding Challenge                       │
├─────────────────────────────────────────┤
│  Total Amount                           │
│  ┌─────────────────────────────────┐   │
│  │ USD [  Enter a decimal number  ]│   │
│  └─────────────────────────────────┘   │
│                                         │
│  [ Calculate ]                          │
│                                         │
│  Results                                │
│  Results will appear here               │
└─────────────────────────────────────────┘
```

**What happens when you click "Calculate"?**

Currently: **NOTHING!** (or null is returned)

The code literally says:
```java
public Object execute(MonetaryAmount amount) {
    //TODO
    return null;  ← Just returns null!
}
```

---

## 🎯 What Needs To Be Built

### 1. The Backend Logic

**File:** `AllocateAmount.java` (currently empty)

**What it needs to do:**
1. Receive an amount (e.g., $1,000)
2. Divide by 12
3. Handle rounding properly
4. Return 12 monthly amounts that add up to exactly $1,000

**Input:**
```json
{
  "amount": {
    "value": 1000,
    "currency": "USD"
  }
}
```

**Output (what we need to return):**
```json
{
  "allocations": [
    {"month": "JANUARY", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "FEBRUARY", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "MARCH", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "APRIL", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "MAY", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "JUNE", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "JULY", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "AUGUST", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "SEPTEMBER", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "OCTOBER", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "NOVEMBER", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "DECEMBER", "amount": {"value": 83.37, "currency": "USD"}}
  ],
  "baseMonthlyAmount": {"value": 83.33, "currency": "USD"},
  "remainder": {"value": 0.04, "currency": "USD"},
  "total": {"value": 1000, "currency": "USD"}
}
```

### 2. The Enhanced UI

**Current UI:** Just shows JSON blob in "Results will appear here"

**Enhanced UI (what we'll build):**

```
┌─────────────────────────────────────────────────────────────┐
│  Revenue Recognition Calculator                             │
├─────────────────────────────────────────────────────────────┤
│  Annual Amount          Rounding Placement                  │
│  ┌─────────────────┐   ┌───────────────────────────────┐   │
│  │USD [    1000   ]│   │ Last Month (December)     ▼ │   │
│  └─────────────────┘   └───────────────────────────────┘   │
│                                                             │
│  [ Calculate Monthly Allocation ]                          │
│                                                             │
│  Allocation Summary                                        │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Total: $1000.00 | Base: $83.33 | Remainder: $0.04  │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  Monthly Allocations                                       │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Month      │ Amount  │ Status                       │  │
│  │ January    │ $83.33  │ —                            │  │
│  │ February   │ $83.33  │ —                            │  │
│  │ March      │ $83.33  │ —                            │  │
│  │ ...                                                  │  │
│  │ December   │ $83.37  │ 🏷️ Adjusted                 │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 What Code Needs To Change

### Files That Need Work:

#### 1. **Domain Layer** (Core Business Logic)

**NEW FILES TO CREATE:**

**`RoundingPlacement.java`** - Enum for where to put remainder
```java
public enum RoundingPlacement {
    FIRST,   // Extra pennies go to January
    LAST,    // Extra pennies go to December (default)
    MIDDLE   // Extra pennies go to June
}
```

**`MonthlyAllocation.java`** - Represents one month's amount
```java
public record MonthlyAllocation(
    Month month,              // JANUARY, FEBRUARY, etc.
    MonetaryAmount amount,    // $83.33 or $83.37
    boolean hasRemainder      // Is this the adjusted month?
) {}
```

**`RevenueAllocation.java`** - The complete result (aggregate)
```java
public class RevenueAllocation {
    // Holds all 12 monthly allocations
    // Ensures sum = annual amount (business rule!)
}
```

**`AllocationCalculator.java`** - The math logic
```java
public class AllocationCalculator {
    public RevenueAllocation calculate(...) {
        // Step 1: Divide by 12
        // Step 2: Calculate remainder
        // Step 3: Add remainder to specified month
        // Step 4: Return result
    }
}
```

#### 2. **Application Layer** (Use Cases)

**`AllocateAmount.java`** (currently empty - THIS IS THE MAIN FILE)
```java
@Service
public class AllocateAmount {
    public Object execute(MonetaryAmount amount) {
        // TODO ← This is what we need to implement!
        return null;
    }
}
```

**Becomes:**
```java
@Service
public class AllocateRevenueUseCase implements AllocateRevenue {
    private final AllocationCalculator calculator;
    
    public RevenueAllocation execute(RevenueRecognitionRequest request) {
        return calculator.calculate(request);
    }
}
```

#### 3. **Infrastructure Layer** (Web/API)

**`RequestDto.java`** (currently simple)
```java
public record RequestDto(MonetaryAmount amount) {}
```

**Needs to become:**
```java
public record RequestDto(
    MonetaryAmount amount,
    RoundingPlacement roundingPlacement  // NEW: user can choose!
) {}
```

**`ResponseDto.java`** (currently empty)
```java
public record ResponseDto() {
    //TODO: To be defined
}
```

**Needs to become:**
```java
public record ResponseDto(
    List<MonthlyAmountDto> allocations,  // 12 months with amounts
    MonetaryAmount baseMonthlyAmount,     // $83.33
    MonetaryAmount remainder,              // $0.04
    MonetaryAmount total                   // $1000
) {}
```

#### 4. **UI** (HTML)

**`index.html`** - Currently basic, needs:
- Dropdown to select rounding placement
- Table to show 12 months
- Highlight which month got the adjustment
- Summary showing total, base, remainder

---

## 🎬 Step-By-Step Example

Let's walk through what happens when a user enters $1,000:

### Step 1: User Opens Browser
```
http://localhost:8080
```
Sees the form with an input box for amount.

### Step 2: User Types $1,000
```
Amount: [1000]
```

### Step 3: User Clicks "Calculate"
Browser sends:
```json
POST /amounts
{
  "amount": {"value": 1000, "currency": "USD"}
}
```

### Step 4: Backend Receives Request
```java
AmountsController receives RequestDto
  ↓
Calls AllocateAmount.execute(amount)
  ↓
AllocateAmount returns ???  ← THIS IS WHAT WE BUILD!
```

### Step 5: Our Code Runs (What We Build)
```java
1. Calculate: 1000 ÷ 12 = 83.33 (base amount)
2. Calculate: 83.33 × 11 = 916.63
3. Calculate: 1000 - 916.63 = 83.37 (adjusted amount)
4. Build result:
   - 11 months get $83.33
   - 1 month (December) gets $83.37
5. Return JSON response
```

### Step 6: User Sees Result
```
Monthly Allocations:
✓ January:   $83.33
✓ February:  $83.33
✓ March:     $83.33
✓ April:     $83.33
✓ May:       $83.33
✓ June:      $83.33
✓ July:      $83.33
✓ August:    $83.33
✓ September: $83.33
✓ October:   $83.33
✓ November:  $83.33
✓ December:  $83.37 🏷️ Adjusted

Total: $1,000.00 ✅
```

---

## 🎯 The Three Requirements (Simple Version)

### Requirement 1: Handle Perfect Division
```
Input:  $1,200
Output: 12 months × $100 = $1,200
Easy!
```

### Requirement 2: Handle Imperfect Division
```
Input:  $1,000
Output: 11 months × $83.33 + 1 month × $83.37 = $1,000
Tricky! We need to handle the $0.04 remainder.
```

### Requirement 3: Let User Choose Where Remainder Goes
```
User selects "FIRST":
January gets $83.37, rest get $83.33

User selects "LAST" (default):
December gets $83.37, rest get $83.33

User selects "MIDDLE":
June gets $83.37, rest get $83.33
```

---

## 🧪 How Do We Know It Works?

### Test 1: Perfect Division
```java
Input: $1,200
Expected: 12 × $100
Sum: $1,200 ✅
```

### Test 2: Imperfect Division (LAST)
```java
Input: $1,000, placement: LAST
Expected: 
  - Months 1-11: $83.33
  - Month 12: $83.37
Sum: $1,000 ✅
```

### Test 3: Imperfect Division (FIRST)
```java
Input: $1,000, placement: FIRST
Expected: 
  - Month 1: $83.37
  - Months 2-12: $83.33
Sum: $1,000 ✅
```

### Test 4: Edge Case - Very Small Amount
```java
Input: $0.05
Expected: 11 × $0.00 + 1 × $0.05
Sum: $0.05 ✅
```

---

## 📚 Summary: What You're Building

**The Problem:**
Split annual revenue ($1,000) into 12 monthly amounts that:
1. Add up exactly to $1,000 (no rounding errors!)
2. Handle leftover pennies properly
3. Let the user choose where those pennies go

**The Solution:**
Build a revenue recognition calculator that:
1. Takes an annual amount
2. Divides by 12 (with proper rounding)
3. Distributes remainder to one specific month
4. Returns 12 monthly amounts + metadata
5. Shows nice table in UI

**Why This Matters:**
This is a **real accounting problem**. Companies like SaaS businesses need this exact functionality to comply with revenue recognition standards (ASC 606 / IFRS 15).

---

## 🚀 Ready?

Now that you understand the problem, check out:
- **[IMPLEMENTATION_GUIDE.md](OLD_IMPLEMENTATION_GUIDE.md)** for step-by-step code
- **[ARCHITECTURE_AND_DDD_GUIDE.md](ARCHITECTURE_AND_DDD_GUIDE.md)** for how to structure it properly

The guides will walk you through building exactly what's described here! 🎉

