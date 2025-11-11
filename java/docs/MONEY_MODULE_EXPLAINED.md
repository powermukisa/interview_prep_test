# MoneyModule Configuration Explained

## Understanding `.withAmountFieldName("value")`

### The Problem: Default Serialization

The `javax.money.MonetaryAmount` interface (implemented by `org.javamoney.moneta.Money`) has a complex internal structure. Without configuration, Jackson doesn't know how to serialize it.

**Java Code:**
```java
MonetaryAmount amount = Money.of(100, "USD");
```

**Default JSON (without MoneyModule):**
```json
{
  "amount": 100,
  "currency": {
    "currencyCode": "USD",
    "defaultFractionDigits": 2,
    "numericCode": 840
  }
}
```
❌ **Problem:** Too verbose, exposes internal details

### The Solution: `.withAmountFieldName("value")`

```java
new MoneyModule().withAmountFieldName("value")
```

**Now produces:**
```json
{
  "value": 100,
  "currency": "USD"
}
```
✅ **Clean, simple, and matches the UI contract!**

### Why This Matters for Rillet

**Frontend JavaScript expects:**
```javascript
const body = {
  amount: {
    value: 1200,        // ← "value" not "amount"
    currency: "USD"
  }
};
```

**Backend deserializes to:**
```java
MonetaryAmount amount = requestDto.amount();
// amount.getNumber().numberValue(BigDecimal.class) == 1200
// amount.getCurrency().getCurrencyCode() == "USD"
```

### Example Comparison

#### Without `.withAmountFieldName("value")`

**JSON sent by frontend:**
```json
{
  "amount": {
    "amount": 1200,     ← Confusing! "amount.amount"
    "currency": "USD"
  }
}
```

#### With `.withAmountFieldName("value")`

**JSON sent by frontend:**
```json
{
  "amount": {
    "value": 1200,      ← Clear! "amount.value"
    "currency": "USD"
  }
}
```

---

## Understanding `.withQuotedDecimalNumbers()` - Precision Safety

### The Problem: Floating-Point Precision Loss

**Scenario:** You're allocating $1000 / 12 months = $83.333333...

#### Without `.withQuotedDecimalNumbers()` (DANGEROUS ⚠️)

**JSON Response:**
```json
{
  "baseMonthlyAmount": {
    "value": 83.33,               ← JavaScript Number (IEEE 754 float)
    "currency": "USD"
  }
}
```

**What happens in JavaScript:**
```javascript
const value = 83.33;
console.log(value === 83.33);              // true
console.log(value * 100);                  // 8332.999999999999  ← LOSS!
console.log(value + value + value);        // 249.98999999999998 ← WRONG!

// Real example from banking:
0.1 + 0.2 === 0.3                          // false (!!!)
0.1 + 0.2                                  // 0.30000000000000004
```

**Why this is catastrophic in accounting:**
- Sum of 12 months might not equal annual amount
- Rounding errors compound
- Financial calculations become incorrect
- Audit failures

#### With `.withQuotedDecimalNumbers()` (SAFE ✅)

**JSON Response:**
```json
{
  "baseMonthlyAmount": {
    "value": "83.33",             ← String (exact decimal representation)
    "currency": "USD"
  }
}
```

**What happens in JavaScript:**
```javascript
const value = "83.33";
const decimal = new BigDecimal(value);     // Or Decimal.js, big.js, etc.
decimal.times(100);                         // "8333.00" - EXACT!

// Or if using native Number for display only:
parseFloat(value)                           // 83.33 (safe for display)

// For calculations, use decimal libraries:
import Decimal from 'decimal.js';
const total = new Decimal("83.33")
  .times(12)
  .toString();                              // "999.96" - EXACT!
```

### Real-World Example: The $0.01 Bug

**Bad (unquoted):**
```json
{
  "allocations": [
    {"month": "JAN", "amount": {"value": 83.33, "currency": "USD"}},
    {"month": "FEB", "amount": {"value": 83.33, "currency": "USD"}},
    ...
    {"month": "DEC", "amount": {"value": 83.37, "currency": "USD"}}
  ],
  "total": {"value": 1000.00, "currency": "USD"}
}
```

**JavaScript validation:**
```javascript
const sum = allocations.reduce((acc, a) => acc + a.amount.value, 0);
console.log(sum);           // 999.9999999999998 ← BUG!
console.log(sum === 1000);  // false ← AUDIT FAILURE!
```

**Good (quoted):**
```json
{
  "allocations": [
    {"month": "JAN", "amount": {"value": "83.33", "currency": "USD"}},
    ...
  ],
  "total": {"value": "1000.00", "currency": "USD"}
}
```

**JavaScript validation:**
```javascript
import Decimal from 'decimal.js';

const sum = allocations.reduce(
  (acc, a) => acc.plus(new Decimal(a.amount.value)),
  new Decimal(0)
);
console.log(sum.toString());           // "1000.00" ← EXACT!
console.log(sum.equals(new Decimal("1000.00"))); // true ← PASS!
```

---

## Complete Configuration Example

```java
@Configuration
public class Configurations {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            // Handle Java 8 date/time types (LocalDate, Month, etc.)
            .registerModule(new JavaTimeModule())
            
            // Handle javax.money.MonetaryAmount
            .registerModule(new MoneyModule()
                // Serialize as {"value": 100, "currency": "USD"}
                .withAmountFieldName("value")
                
                // Use strings for decimal precision
                // {"value": "83.33"} instead of {"value": 83.33}
                .withQuotedDecimalNumbers()
            );
    }
}
```

---

## Summary

| Configuration | Purpose | Example |
|--------------|---------|---------|
| `.withAmountFieldName("value")` | Clean JSON field names | `{"value": 100}` not `{"amount": 100}` |
| `.withQuotedDecimalNumbers()` | Prevent floating-point errors | `"83.33"` not `83.33` |

**Why Rillet Cares:**
- ✅ **Accounting precision** - No rounding errors
- ✅ **Audit compliance** - Exact sums
- ✅ **API clarity** - Clean, predictable JSON
- ✅ **Cross-platform safety** - Works with any JSON parser

