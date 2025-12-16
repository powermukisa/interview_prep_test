# Part 2 Interview Script

> **Duration**: 2 hours  
> **Format**: Live coding with screenshare  
> **Goal**: Implement formula support for the spreadsheet engine

---

## Before We Start (Say This)

*"Before I dive in, let me quickly review what we need to implement and share my approach."*

*"Looking at the spec, we need to add formula support in this order:*
1. *Cell references - already done in Part 1*
2. *Literal formulas like `=2.5` or `=2018-01-01`*
3. *Binary operators like `=A1 + B1`*
4. *The `sum()` function with range support*
5. *If time: date arithmetic and `sqrt()`"*

*"Here's the architecture I'm planning. When `get_formatted()` is called, it flows through these methods:"*

```
get_formatted(index)
    │
    └── _resolve_value(index, seen)
            │
            ├── If formula (starts with =):
            │       │
            │       ├── parse_formula(formula_str)  ← provided library
            │       │
            │       └── _evaluate_formula(parsed, seen)
            │               │
            │               ├── String (atom) → _evaluate_atom(atom, seen)
            │               │                       │
            │               │                       ├── Try Index.parse() → _resolve_value() [recursive]
            │               │                       │
            │               │                       └── Else → parse_value() [literal like "42"]
            │               │
            │               └── List (operation) → _evaluate_operation(parsed, seen)
            │                       │
            │                       ├── Binary op (+,-,*,/) → _evaluate_binary_op()
            │                       │
            │                       └── Function call → _evaluate_function()
            │                               │
            │                               ├── 'sum' → _func_sum()
            │                               │               └── _get_indices_from_arg() [handles ranges]
            │                               │
            │                               └── 'sqrt' → _func_sqrt()
            │
            └── Else (plain value):
                    │
                    └── parse_value(raw)  ← existing function
```

*"The key insight is that the formula parser returns either a string for simple values like `'A1'` or `'42'`, or a list for operations like `['+', 'A1', 'B1']`. So `_evaluate_formula` acts as a dispatcher based on the type."*

### Why Use a Tree Structure (AST)?

*"Before I show the examples, let me explain why the formula parser returns a tree structure."*

**What is an AST?**

AST stands for **Abstract Syntax Tree**. It's a tree representation of code/expressions where:
- **Abstract** = we strip away unimportant details (parentheses, whitespace)
- **Syntax** = it represents the structure of the expression
- **Tree** = it's organized hierarchically (operators are parents, operands are children)

For example, `(1 + 2) * 3` and `(  1+2  )*3` have the same AST - the parentheses and spaces are "abstracted away".

**The problem**: We need to evaluate `=1 + 2 * 3`. What's the answer?
- If we evaluate left-to-right: `(1 + 2) * 3 = 9` ❌
- If we respect precedence: `1 + (2 * 3) = 7` ✅

**Option 1: String parsing (bad)**
```python
# Naive approach - just split on operators
"1 + 2 * 3".split("+")  # ['1 ', ' 2 * 3']
# Now what? We'd have to handle precedence ourselves. Messy!
```

**Option 2: Flat list (problematic)**
```python
# Store as flat list: [1, '+', 2, '*', 3]
# Problem: How do we know * comes before +?
# We'd have to scan the list multiple times for precedence.
```

**Option 3: Tree / AST (what we use) ✅**
```python
# Parser returns: ['+', 1, ['*', 2, 3]]
#
#       +
#      / \
#     1   *
#        / \
#       2   3
#
# The STRUCTURE encodes precedence!
# Inner nodes are evaluated first (bottom-up).
```

**Why trees make our lives easier:**

| Benefit | Explanation |
|---------|-------------|
| **Precedence is built-in** | `*` is nested inside `+`, so we evaluate `*` first |
| **Recursive evaluation** | Just evaluate children, then apply operator - simple! |
| **Handles nesting** | `=sqrt(1 + 2)` → `['sqrt', ['+', '1', '2']]` - naturally nested |
| **Uniform structure** | Everything is either an atom (string) or operation (list) |

**How we evaluate a tree (pseudocode):**
```python
def evaluate(node):
    if node is a string:        # Leaf node (atom)
        return parse_value(node)
    else:                       # Internal node (operation)
        op = node[0]
        left = evaluate(node[1])   # Evaluate left subtree
        right = evaluate(node[2])  # Evaluate right subtree
        return apply_op(op, left, right)
```

*"This is why recursion is natural here - trees are recursive data structures. Each subtree is itself a tree that we evaluate the same way."*

---

### Real Example: `3b-ref-to-ref.csv` (Reference Chain)

```
     A         B
1   actual   expected
2   1        1
3   =A2      1
4   =A3      1
```

When we call `get_formatted(A4)`:

```
_resolve_value(A4)
    │ raw = "=A3"
    │ parsed = "A3"  (string - it's an atom!)
    │
    └── _evaluate_formula("A3")
            │
            └── _evaluate_atom("A3")
                    │ Index.parse("A3") succeeds → Index(row=2, col=0)
                    │
                    └── _resolve_value(A3)  ← RECURSIVE CALL
                            │ raw = "=A2"
                            │ parsed = "A2"
                            │
                            └── _evaluate_formula("A2")
                                    │
                                    └── _evaluate_atom("A2")
                                            │
                                            └── _resolve_value(A2)  ← RECURSIVE CALL
                                                    │ raw = "1"
                                                    │ NOT a formula (no =)
                                                    │
                                                    └── parse_value("1") → 1

Result: 1 bubbles back up through all the recursive calls.
```

### Real Example: `4-binop.csv` (Binary Operators)

```
     A           B
1   actual     expected
2   =1 + 2     3
3   =1 - 2     -1
4   =2 * 3     6
5   =4 / 2     2
```

When we call `get_formatted(A2)` for `=1 + 2`:

**Tree structure:**
```
    ['+', '1', '2']
    
         +
        / \
       1   2
```

**Evaluation trace:**
```
_resolve_value(A2)
    │ raw = "=1 + 2"
    │ parsed = ['+', '1', '2']  ← It's a LIST!
    │
    └── _evaluate_formula(['+', '1', '2'])
            │
            └── _evaluate_operation(['+', '1', '2'])
                    │ operator = '+'
                    │
                    └── _evaluate_binary_op('+', '1', '2')
                            │
                            ├── LEFT: _evaluate_formula('1')
                            │           └── _evaluate_atom('1')
                            │                   └── Index.parse('1') fails
                            │                   └── parse_value('1') → 1
                            │
                            ├── RIGHT: _evaluate_formula('2')
                            │           └── _evaluate_atom('2')
                            │                   └── parse_value('2') → 2
                            │
                            └── return 1 + 2 = 3
```

### Real Example: `5-sum.csv` (Function with Range)

```
     A                B
1   actual          expected
2   1               1
3   2               2
4   3               3
5   =sum(A2:A4)     6
```

**What the parser returns:**
```
parse("sum(A2:A4)") → ['sum', 'A2:A4']
```

**Evaluation trace:**
```
_resolve_value(A5)
    │ raw = "=sum(A2:A4)"
    │ parsed = ['sum', 'A2:A4']
    │
    └── _evaluate_formula(['sum', 'A2:A4'])
            │
            └── _evaluate_operation(['sum', 'A2:A4'])
                    │ operator = 'sum'
                    │
                    └── _evaluate_function('sum', ['A2:A4'])
                            │
                            └── _func_sum(['A2:A4'])
                                    │
                                    ├── _get_indices_from_arg('A2:A4')
                                    │       └── Range.parse('A2:A4')
                                    │       └── returns [Index(1,0), Index(2,0), Index(3,0)]
                                    │
                                    ├── _resolve_value(A2) → 1
                                    ├── _resolve_value(A3) → 2
                                    ├── _resolve_value(A4) → 3
                                    │
                                    └── total = 1 + 2 + 3 = 6
```

### Tree for Complex Expression: `=A1 + B1 * C1`

The parser handles operator precedence automatically:

```
parse("A1 + B1 * C1") → ['+', 'A1', ['*', 'B1', 'C1']]
```

**Tree structure (multiplication evaluated first):**
```
              +
             / \
           A1   *
               / \
             B1   C1
```

*"My plan is to integrate the formula parser first, which will give us a clean AST to work with. Then I'll add each feature incrementally, testing as I go."*

---

## Step 1: Update `_resolve_value` to Use Formula Parser (10 minutes)

*"Let me start by updating `_resolve_value` to use the formula parser. First, I need to import it."*

### Add import at the top of `engine.py`:
```python
from .formula import parse as parse_formula, ParseError
```

*"Now I'll update `_resolve_value`. The key change is: instead of my custom reference detection, I'll use the provided parser for ALL formulas."*

### Update `_resolve_value`:

```python
def _resolve_value(self, index: Index, seen: Optional[set] = None) -> tuple[CellValue, str]:
    """Resolve the value at an index, following references and evaluating formulas."""
    if seen is None:
        seen = set()
    
    # Check for circular reference
    if index in seen:
        return None, "#CIRC!"
    
    raw = self.get_raw(index)
    if not raw:
        return None, ""
    
    # Check if this is a formula (starts with =)
    if raw.strip().startswith("="):
        formula_str = raw.strip()[1:].strip()
        try:
            parsed = parse_formula(formula_str)
        except ParseError:
            return None, "#PARSE!"
        
        # Add this cell to seen before evaluating
        seen.add(index)
        return self._evaluate_formula(parsed, seen)
    
    # Plain value - parse and return
    return parse_value(raw), ""
```

*"This is clean and simple:*
- *If it starts with `=`, parse and evaluate the formula*
- *Otherwise, it's a plain value - just parse it*
- *The `seen` set tracks visited cells to detect circular references*
- *We use recursion because formulas naturally form a tree structure"*

---

## Step 2: Add Formula Evaluation Methods (15 minutes)

*"Now I need to add the methods that evaluate the parsed formula. The parser returns either a string for simple values, or a list for operations."*

**Quick reference - what the parser returns:**
```
"=A2"        → parse returns "A2"           (string - cell reference)
"=42"        → parse returns "42"           (string - literal)
"=1 + 2"     → parse returns ['+', '1', '2']  (list - binary operation)
"=sum(A1:A3)" → parse returns ['sum', 'A1:A3'] (list - function call)
```

### Add these methods to the Spreadsheet class (after `_resolve_value`):

```python
def _evaluate_formula(self, parsed, seen: set) -> tuple[CellValue, str]:
    """Evaluate a parsed formula expression.
    
    Examples:
        parsed = "A2"           → _evaluate_atom (cell reference)
        parsed = "42"           → _evaluate_atom (literal)
        parsed = ['+', '1', '2'] → _evaluate_operation (binary op)
        parsed = ['sum', 'A1:A3'] → _evaluate_operation (function)
    """
    if isinstance(parsed, str):
        return self._evaluate_atom(parsed, seen)
    elif isinstance(parsed, list):
        return self._evaluate_operation(parsed, seen)
    else:
        return None, "#ERR!"

def _evaluate_atom(self, atom: str, seen: set) -> tuple[CellValue, str]:
    """Evaluate an atom - either a cell reference or a literal value.
    
    Examples from our CSV files:
        atom = "A2" → Index.parse succeeds → _resolve_value(A2) → follows chain
        atom = "42" → Index.parse fails → parse_value("42") → 42
        atom = "3.14" → Index.parse fails → parse_value("3.14") → 3.14
        atom = "2018-01-01" → Index.parse fails → parse_value → datetime
    """
    # First, try to parse as a cell reference
    try:
        index = Index.parse(atom)
        # Resolve the cell (recursive - will follow any reference chains)
        return self._resolve_value(index, seen)
    except ValueError:
        pass
    
    # Not a cell reference - treat as literal (like '42' or '3.14')
    return parse_value(atom), ""
```

*"So `_evaluate_formula` is a dispatcher - it checks if the parsed result is a string or list and routes accordingly.*

*For example, from `3b-ref-to-ref.csv`: when we evaluate `=A3`, the parser returns `"A3"` (a string). We call `_evaluate_atom("A3")`, which successfully parses it as a cell reference and calls `_resolve_value(A3)` to get its value.*

*From `4-binop.csv`: when we evaluate `=1 + 2`, the parser returns `['+', '1', '2']` (a list). We call `_evaluate_operation`, which handles the binary operator."*

---

## Step 3: Test Literal Formulas (3 minutes)

*"Let me add a test to `test_debug.py` to verify literal formulas work. I like using a test file instead of the REPL because I can set breakpoints and build up tests incrementally."*

### Add this test to `test_debug.py` (after the Part 1 tests):

```python
# ==========================================
# Part 2 Tests: Formula Evaluation
# ==========================================

# ------------------------------------------
# Test: Literal formulas (=42, =3.14, =2018-01-01)
# ------------------------------------------
print("\n=== Test: Literal Formulas ===")

s_lit = Spreadsheet()
s_lit.set(Index(0, 0), '=42')
s_lit.set(Index(0, 1), '=3.14')
s_lit.set(Index(0, 2), '=6.02e23')
s_lit.set(Index(1, 0), '=2018-01-01')

print(f"=42: {s_lit.get_formatted(Index(0, 0))}")          # Expected: 42
print(f"=3.14: {s_lit.get_formatted(Index(0, 1))}")        # Expected: 3.14
print(f"=6.02e23: {s_lit.get_formatted(Index(0, 2))}")     # Expected: 6.02e+23
print(f"=2018-01-01: {s_lit.get_formatted(Index(1, 0))}")  # Expected: 2018-01-01T00:00:00
```

### Run the test:
```bash
cd python
source venv/bin/activate
python test_debug.py
```

*"I can also set a breakpoint on `_evaluate_atom` to step through and see how literals are parsed."*

**Expected output:**
```
=== Test: Literal Formulas ===
=42: 42
=3.14: 3.14
=6.02e23: 6.02e+23
=2018-01-01: 2018-01-01T00:00:00
```

*"Great, literal formulas are working. Now let's add binary operators."*

---

## Step 4: Add Binary Operator Support (15 minutes)

*"Now I need to handle operations. Looking at `4-binop.csv`:"*

```
     A           B
1   actual     expected
2   =1 + 2     3
3   =1 - 2     -1
4   =2 * 3     6
5   =4 / 2     2
```

*"When the parser sees `=1 + 2`, it returns `['+', '1', '2']`. The first element is the operator, the rest are operands."*

**Tree for `=1 + 2`:**
```
    ['+', '1', '2']
    
         +
        / \
       1   2
```

### Add these methods after `_evaluate_atom`:

```python
def _evaluate_operation(self, parsed: list, seen: set) -> tuple[CellValue, str]:
    """Evaluate an operation (binary operator or function call).
    
    Examples from 4-binop.csv:
        ['+', '1', '2']  → binary op, returns 3
        ['-', '1', '2']  → binary op, returns -1
        ['*', '2', '3']  → binary op, returns 6
        ['/', '4', '2']  → binary op, returns 2
        ['sum', 'A1:A3'] → function call
    """
    operator = parsed[0].lower()  # Function names are case-insensitive
    
    # Check if it's a binary operator
    if operator in ('+', '-', '*', '/'):
        return self._evaluate_binary_op(operator, parsed[1], parsed[2], seen)
    
    # Otherwise it's a function call
    return self._evaluate_function(operator, parsed[1:], seen)

def _evaluate_binary_op(self, op: str, left_expr, right_expr, seen: set) -> tuple[CellValue, str]:
    """Evaluate a binary operation (+, -, *, /).
    
    For =1 + 2:
        op = '+'
        left_expr = '1'  → evaluates to 1
        right_expr = '2' → evaluates to 2
        returns 1 + 2 = 3
    
    For =A1 + B1 (where A1=5, B1=3):
        op = '+'
        left_expr = 'A1'  → recursively resolves A1 → 5
        right_expr = 'B1' → recursively resolves B1 → 3
        returns 5 + 3 = 8
    """
    # ============================================================
    # WHY RECURSIVE CALL TO _evaluate_formula?
    # ============================================================
    # The operands (left_expr, right_expr) could be:
    #   - Literals like '1' or '3.14' → _evaluate_formula parses them
    #   - Cell references like 'A1' → _evaluate_formula resolves them
    #   - Nested expressions like ['+', '1', '2'] → _evaluate_formula evaluates them
    #
    # Example: =A1 + B1
    #   left_expr = 'A1'  → call _evaluate_formula('A1') → resolves cell A1
    #   right_expr = 'B1' → call _evaluate_formula('B1') → resolves cell B1
    #
    # Example: =(1 + 2) * 3  (nested operations)
    #   left_expr = ['+', '1', '2'] → call _evaluate_formula recursively → 3
    #   right_expr = '3' → 3
    #   result = 3 * 3 = 9
    #
    # ============================================================
    # WHY seen.copy()?
    # ============================================================
    # The 'seen' set tracks which cells we've visited (for circular ref detection).
    # We COPY it for each branch because the left and right sides are INDEPENDENT.
    #
    # Example: =A1 + A1 (same cell referenced twice - VALID!)
    #   If we didn't copy: left side marks A1 as seen, right side sees A1 
    #   in 'seen' and wrongly returns #CIRC!
    #   With copy: each branch has its own 'seen' set, both can resolve A1.
    #
    # Example: Circular ref =A1 where A1 contains =A1 (INVALID)
    #   The copy doesn't help here - within a SINGLE resolution path,
    #   we correctly detect the cycle.
    # ============================================================
    
    left_val, left_err = self._evaluate_formula(left_expr, seen.copy())
    if left_err:
        return None, left_err
    
    right_val, right_err = self._evaluate_formula(right_expr, seen.copy())
    if right_err:
        return None, right_err
    
    # Handle None values (empty cells)
    if left_val is None or right_val is None:
        return None, "#VALUE!"
    
    # Number operations
    if isinstance(left_val, (int, float)) and isinstance(right_val, (int, float)):
        try:
            if op == '+':
                result = left_val + right_val
            elif op == '-':
                result = left_val - right_val
            elif op == '*':
                result = left_val * right_val
            elif op == '/':
                if right_val == 0:
                    return None, "#DIV/0!"
                result = left_val / right_val
            else:
                return None, "#ERR!"
            
            # UX: [Add after reviewing tests] Return int if result is whole number
            if isinstance(result, float) and result.is_integer():
                return int(result), ""
            return result, "" # [Add this return after tests fail - use break points to find missing return value]
        except Exception:
            return None, "#ERR!"
    
    # Type mismatch for now - we'll add date support later
    return None, "#VALUE!"
```

*"Let me explain the key design decisions:*

1. **Recursive calls to `_evaluate_formula`**: *The operands like `'A1'` or `'1'` need to be evaluated too - they might be cell references that need resolving, or nested expressions. So we recursively call `_evaluate_formula` to handle all cases uniformly.*

2. **Why `seen.copy()`**: *This is subtle but important. Consider `=A1 + A1` - the same cell appears twice, and that's perfectly valid! If we shared the same `seen` set, the left side would mark A1 as visited, and the right side would see it and wrongly report a circular reference. By copying, each branch gets its own independent tracking.*

3. **Division by zero**: *We check for it explicitly and return `#DIV/0!`*

4. **UX polish**: *I convert float results to int when they're whole numbers, so `4/2` shows `2` not `2.0`"*

### Add placeholder for function evaluation:

```python
def _evaluate_function(self, func_name: str, args: list, seen: set) -> tuple[CellValue, str]:
    """Evaluate a function call - placeholder for now."""
    return None, "#NAME?"  # Unknown function
```

---

## Step 5: Test Binary Operators (3 minutes)

*"Let me add tests for binary operators to `test_debug.py`. I'll also verify with the `4-binop.csv` example file."*

### Add this test to `test_debug.py` (continuing from previous tests):

```python
# ------------------------------------------
# Test: Binary Operators (from 4-binop.csv)
# ------------------------------------------
print("\n=== Test: Binary Operators ===")

s_binop = Spreadsheet()
s_binop.set(Index(0, 0), '=1 + 2')
s_binop.set(Index(0, 1), '=5 - 3')
s_binop.set(Index(0, 2), '=4 * 3')
s_binop.set(Index(0, 3), '=10 / 4')
s_binop.set(Index(1, 0), '=10 / 0')  # Division by zero

print(f"=1 + 2: {s_binop.get_formatted(Index(0, 0))}")   # Expected: 3
print(f"=5 - 3: {s_binop.get_formatted(Index(0, 1))}")   # Expected: 2
print(f"=4 * 3: {s_binop.get_formatted(Index(0, 2))}")   # Expected: 12
print(f"=10 / 4: {s_binop.get_formatted(Index(0, 3))}")  # Expected: 2.5
print(f"=10 / 0: {s_binop.get_formatted(Index(1, 0))}")  # Expected: #DIV/0!

# Test with cell references
s_binop.set(Index(2, 0), '5')   # A3 = 5
s_binop.set(Index(2, 1), '3')   # B3 = 3
s_binop.set(Index(2, 2), '=A3 + B3')  # C3 = A3 + B3
print(f"A3=5, B3=3, =A3 + B3: {s_binop.get_formatted(Index(2, 2))}")  # Expected: 8

# ------------------------------------------
# Test: Verify against 4-binop.csv
# ------------------------------------------
print("\n=== Test: 4-binop.csv ===")

s_csv = Spreadsheet()
read_csv('../examples/4-binop.csv', s_csv)

for row in range(1, 5):  # Rows 2-5 in spreadsheet (index 1-4)
    actual = s_csv.get_formatted(Index(row, 0))
    expected = s_csv.get_raw(Index(row, 1))
    raw = s_csv.get_raw(Index(row, 0))
    status = "✓" if str(actual) == expected else "✗"
    print(f"  {status} {raw} = {actual} (expected {expected})")
```

### Run the test:
```bash
python test_debug.py
```

*"I can set a breakpoint on `_evaluate_binary_op` to step through `=1 + 2` and see how the tree is evaluated."*

**Expected output:**
```
=== Test: Binary Operators ===
=1 + 2: 3
=5 - 3: 2
=4 * 3: 12
=10 / 4: 2.5
=10 / 0: #DIV/0!
A3=5, B3=3, =A3 + B3: 8

=== Test: 4-binop.csv ===
  ✓ =1 + 2 = 3 (expected 3)
  ✓ =1 - 2 = -1 (expected -1)
  ✓ =2 * 3 = 6 (expected 6)
  ✓ =4 / 2 = 2 (expected 2)
```

*"All tests pass! The CSV verification is especially nice because it tests the exact cases from the spec."*

---

## Step 6: Implement sum() Function (20 minutes)

*"Now for `sum()`. I'll need the `Range` class to handle cell ranges like `A2:A4`."*

### Add import at the top of `engine.py`:
```python
from .models import Index, Range  # Add Range to existing import
```

*"Looking at `5-sum.csv`:"*

```
     A                B
1   actual          expected
2   1               1
3   2               2
4   3               3
5   =sum(A2:A4)     6
```

*"The parser returns `['sum', 'A2:A4']` for `=sum(A2:A4)`. The key is handling the range `A2:A4` - I need to iterate over all cells in that range."*

**Tree for `=sum(A2:A4)`:**
```
    ['sum', 'A2:A4']
    
        sum
         |
      'A2:A4' → Range.parse → [A2, A3, A4] → [1, 2, 3] → 6
```

### Replace the placeholder `_evaluate_function` and add helper methods:

```python
def _evaluate_function(self, func_name: str, args: list, seen: set) -> tuple[CellValue, str]:
    """Evaluate a function call.
    
    For =sum(A2:A4):
        func_name = 'sum'
        args = ['A2:A4']
    """
    if func_name == 'sum':
        return self._func_sum(args, seen)
    elif func_name == 'sqrt':
        return self._func_sqrt(args, seen)
    else:
        return None, "#NAME?"  # Unknown function

def _func_sum(self, args: list, seen: set) -> tuple[CellValue, str]:
    """Evaluate sum(arg1, arg2, ...) where args can be ranges or single values.
    
    For =sum(A2:A4) with A2=1, A3=2, A4=3:
        args = ['A2:A4']
        Range.parse('A2:A4').indices → [A2, A3, A4]  # Reuse from models.py!
        Resolve each: 1 + 2 + 3 = 6
    
    For =sum(A1:A3, B1:B2) - multiple ranges:
        args = ['A1:A3', 'B1:B2']
        Process each range, sum all values
    """
    total = 0
    
    for arg in args:
        # ============================================================
        # REUSING models.py: Range.parse() and range.indices
        # ============================================================
        # The Range class already handles:
        #   - Parsing "A1:B3" into a Range object
        #   - Iterating over all cells with .indices property
        # No need to write custom iteration logic!
        # ============================================================
        
        if isinstance(arg, str) and ':' in arg: # [can first ignore the : case and come back to it with the last elif down here]
            # It's a range like "A1:B3" - use Range.parse() from models.py
            try:
                range_obj = Range.parse(arg)
                # range_obj.indices gives us all Index objects in the range
                for index in range_obj.indices:
                    val, err = self._resolve_value(index, seen.copy())
                    if err:
                        return None, err
                    if val is None:
                        continue  # Skip empty cells [another edge case to play with]
                    if not isinstance(val, (int, float)):
                        return None, "#VALUE!"
                    total += val
            except ValueError:
                return None, "#REF!"
        
        elif isinstance(arg, str):
            # Single cell like "A1" or literal like "5"
            val, err = self._evaluate_atom(arg, seen.copy())
            if err:
                return None, err
            if val is None:
                continue
            if not isinstance(val, (int, float)):
                return None, "#VALUE!"
            total += val
        
        elif isinstance(arg, list):
            # Nested expression like sum(1 + 2, A1)
            val, err = self._evaluate_formula(arg, seen.copy())
            if err:
                return None, err
            if val is None:
                continue
            if not isinstance(val, (int, float)):
                return None, "#VALUE!"
            total += val
    
    return total, ""

def _func_sqrt(self, args: list, seen: set) -> tuple[CellValue, str]:
    """Evaluate sqrt(value) - placeholder for later."""
    return None, "#NAME?"
```


---

## Step 7: Test sum() Function (3 minutes)

*"Let me add tests for sum() to `test_debug.py`. I'll also verify with `5-sum.csv`."*

### Add this test to `test_debug.py` (continuing from previous tests):

```python
# ------------------------------------------
# Test: sum() Function - All Branches
# ------------------------------------------
print("\n=== Test: sum() Function ===")

s_sum = Spreadsheet()
s_sum.set(Index(0, 0), '1')   # A1 = 1
s_sum.set(Index(1, 0), '2')   # A2 = 2
s_sum.set(Index(2, 0), '3')   # A3 = 3
s_sum.set(Index(0, 1), '10')  # B1 = 10
s_sum.set(Index(1, 1), '20')  # B2 = 20

print("Setup: A1=1, A2=2, A3=3, B1=10, B2=20")

# ----------------------------------------
# Branch 1: Range argument (if ':' in arg)
# Tests the `if isinstance(arg, str) and ':' in arg:` branch
# ----------------------------------------
print("\n--- Branch 1: Range argument ---")
s_sum.set(Index(5, 0), '=sum(A1:A3)')
print(f"=sum(A1:A3): {s_sum.get_formatted(Index(5, 0))}")  # Expected: 6

# Multiple ranges
s_sum.set(Index(5, 1), '=sum(A1:A3, B1:B2)')
print(f"=sum(A1:A3, B1:B2): {s_sum.get_formatted(Index(5, 1))}")  # Expected: 36 (1+2+3+10+20)

# ----------------------------------------
# Branch 2: Single cell reference (elif isinstance(arg, str))
# Tests single cell like "A1" that goes through _evaluate_atom
# ----------------------------------------
print("\n--- Branch 2: Single cell reference ---")
s_sum.set(Index(6, 0), '=sum(A1)')
print(f"=sum(A1): {s_sum.get_formatted(Index(6, 0))}")  # Expected: 1

# Multiple single cells
s_sum.set(Index(6, 1), '=sum(A1, A2, A3)')
print(f"=sum(A1, A2, A3): {s_sum.get_formatted(Index(6, 1))}")  # Expected: 6

# ----------------------------------------
# Branch 2b: Literal value (elif isinstance(arg, str))
# Tests literal like "5" that goes through _evaluate_atom → parse_value
# ----------------------------------------
print("\n--- Branch 2b: Literal value ---")
s_sum.set(Index(7, 0), '=sum(5)')
print(f"=sum(5): {s_sum.get_formatted(Index(7, 0))}")  # Expected: 5

# Mix of literals
s_sum.set(Index(7, 1), '=sum(10, 20, 30)')
print(f"=sum(10, 20, 30): {s_sum.get_formatted(Index(7, 1))}")  # Expected: 60

# ----------------------------------------
# Branch 3: Nested expression (elif isinstance(arg, list))
# Tests nested operations like ['+', '1', '2'] from "1 + 2"
# ----------------------------------------
print("\n--- Branch 3: Nested expression ---")
s_sum.set(Index(8, 0), '=sum(1 + 2)')
print(f"=sum(1 + 2): {s_sum.get_formatted(Index(8, 0))}")  # Expected: 3

# Nested with cell reference
s_sum.set(Index(8, 1), '=sum(1 + 2, A1)')
print(f"=sum(1 + 2, A1): {s_sum.get_formatted(Index(8, 1))}")  # Expected: 4 (3 + 1)

# Complex: mix of everything
s_sum.set(Index(9, 0), '=sum(A1:A2, 100, 1 + 1)')
print(f"=sum(A1:A2, 100, 1 + 1): {s_sum.get_formatted(Index(9, 0))}")  # Expected: 105 (1+2+100+2)

# ------------------------------------------
# Test: Verify against 5-sum.csv
# ------------------------------------------
print("\n=== Test: 5-sum.csv ===")

s_csv2 = Spreadsheet()
read_csv('../examples/5-sum.csv', s_csv2)

# Row 5 (index 4) has the sum formula
actual = s_csv2.get_formatted(Index(4, 0))
expected = s_csv2.get_raw(Index(4, 1))
raw = s_csv2.get_raw(Index(4, 0))
status = "✓" if str(actual) == expected else "✗"
print(f"  {status} {raw} = {actual} (expected {expected})")
```

### Run the test:
```bash
python test_debug.py
```

*"I can set a breakpoint on `_func_sum` to step through each branch and see how different argument types are handled."*

**Expected output:**
```
=== Test: sum() Function ===
Setup: A1=1, A2=2, A3=3, B1=10, B2=20

--- Branch 1: Range argument ---
=sum(A1:A3): 6
=sum(A1:A3, B1:B2): 36

--- Branch 2: Single cell reference ---
=sum(A1): 1
=sum(A1, A2, A3): 6

--- Branch 2b: Literal value ---
=sum(5): 5
=sum(10, 20, 30): 60

--- Branch 3: Nested expression ---
=sum(1 + 2): 3
=sum(1 + 2, A1): 4
=sum(A1:A2, 100, 1 + 1): 105

=== Test: 5-sum.csv ===
  ✓ =sum(A2:A4) = 6 (expected 6)
```

*"These tests cover all three branches of `_func_sum`:*
- *Branch 1: Range like `A1:A3` - uses `Range.parse()` and `.indices`*
- *Branch 2: Single string - cell ref `A1` or literal `5` - uses `_evaluate_atom`*
- *Branch 3: Nested list like `1 + 2` - uses `_evaluate_formula` recursively"*

---

## Step 8: Add Date Arithmetic (If Time - 10 minutes)

*"If we have time, let me add date arithmetic. First, I need `timedelta` for date math."*

### Add import at the top of `engine.py`:
```python
from datetime import datetime, timedelta  # Add timedelta to existing import
```

*"The spec says:*
- *date + number = date N days in future*
- *date - number = date N days in past*
- *date - date = difference in days*
- *date + date = error"*

### Add to `_evaluate_binary_op`, after the number operations block:

```python
    # Date + number = date N days in future
    if isinstance(left_val, datetime) and isinstance(right_val, (int, float)):
        if op == '+':
            return left_val + timedelta(days=right_val), ""
        elif op == '-':
            return left_val - timedelta(days=right_val), ""
        else:
            return None, "#VALUE!"  # Can't multiply/divide date by number
    
    # Number + date = date N days in future (only addition)
    if isinstance(left_val, (int, float)) and isinstance(right_val, datetime):
        if op == '+':
            return right_val + timedelta(days=left_val), ""
        else:
            return None, "#VALUE!"
    
    # Date - date = difference in days
    if isinstance(left_val, datetime) and isinstance(right_val, datetime):
        if op == '-':
            delta = left_val - right_val
            return delta.days, ""
        else:
            return None, "#VALUE!"  # Can't add/multiply/divide two dates
    
    # Type mismatch
    return None, "#VALUE!"
```

### Add test to `test_debug.py`:

```python
# ------------------------------------------
# Test: Date Operations (if time)
# ------------------------------------------
print("\n=== Test: Date Operations ===")

s_date = Spreadsheet()
s_date.set(Index(0, 0), '2018-01-01')  # A1 = date
s_date.set(Index(0, 1), '=A1 + 5')     # B1 = date + 5 days
s_date.set(Index(0, 2), '=A1 - 5')     # C1 = date - 5 days
s_date.set(Index(1, 0), '2018-01-10')  # A2 = another date
s_date.set(Index(1, 1), '=A2 - A1')    # B2 = date difference

print(f"A1 = 2018-01-01")
print(f"=A1 + 5 (5 days later): {s_date.get_formatted(Index(0, 1))}")   # Expected: 2018-01-06T00:00:00
print(f"=A1 - 5 (5 days earlier): {s_date.get_formatted(Index(0, 2))}")  # Expected: 2017-12-27T00:00:00
print(f"A2 = 2018-01-10")
print(f"=A2 - A1 (days between): {s_date.get_formatted(Index(1, 1))}")  # Expected: 9
```

### Run the test:
```bash
python test_debug.py
```

**Expected output:**
```
=== Test: Date Operations ===
A1 = 2018-01-01
=A1 + 5 (5 days later): 2018-01-06T00:00:00
=A1 - 5 (5 days earlier): 2017-12-27T00:00:00
A2 = 2018-01-10
=A2 - A1 (days between): 9
```

---

## Step 9: Add sqrt() Function (If Time - 5 minutes)

*"Finally, let me implement sqrt. I need the `math` module."*

### Add import at the top of `engine.py`:
```python
import math
```

### Replace the `_func_sqrt` placeholder:

```python
def _func_sqrt(self, args: list, seen: set) -> tuple[CellValue, str]:
    """Evaluate sqrt(value).
    
    What does `args` look like?
    ============================================================
    The parser extracts function arguments as a list:
    
    Formula             Parser returns              args passed here
    -------             --------------              ----------------
    =sqrt(16)           ['sqrt', '16']              ['16']
    =sqrt(A1)           ['sqrt', 'A1']              ['A1']
    =sqrt(1 + 1)        ['sqrt', ['+', '1', '1']]   [['+', '1', '1']]
    
    So args[0] could be:
      - '16'              (string literal)
      - 'A1'              (string cell reference)
      - ['+', '1', '1']   (list - nested expression)
    
    Why call _evaluate_formula?
    ============================================================
    We don't know what type args[0] is! It could be:
      - A literal string '16' → needs parse_value to become int 16
      - A cell reference 'A1' → needs to resolve the cell's value
      - A nested expression ['+', '1', '1'] → needs to evaluate 1+1=2
    
    _evaluate_formula handles ALL these cases uniformly:
      - String → _evaluate_atom → tries Index.parse, falls back to parse_value
      - List → _evaluate_operation → recursively evaluates the expression
    
    This is the power of the recursive design - we don't need special
    handling for each case, we just call _evaluate_formula and it figures
    it out!
    """
    if len(args) != 1: #[play with this if there's time]
        return None, "#VALUE!"
    
    # ============================================================
    # Evaluate the argument - could be '16', 'A1', or ['+', '1', '1']
    # _evaluate_formula handles all cases uniformly
    # ============================================================
    val, err = self._evaluate_formula(args[0], seen)
    if err:
        return None, err
    
    if val is None:
        return None, "#VALUE!"
    
    if not isinstance(val, (int, float)):
        return None, "#VALUE!"
    
    if val < 0:
        return None, "#NUM!"  # Can't sqrt negative
    
    result = math.sqrt(val)
    if result.is_integer():
        return int(result), ""
    return result, ""
```

### Add test to `test_debug.py`:

```python
# ------------------------------------------
# Test: sqrt() Function (if time)
# ------------------------------------------
print("\n=== Test: sqrt() Function ===")

s_sqrt = Spreadsheet()
s_sqrt.set(Index(0, 0), '=sqrt(16)')
s_sqrt.set(Index(0, 1), '=sqrt(2)')
s_sqrt.set(Index(0, 2), '=sqrt(-1)')  # Error case

print(f"=sqrt(16): {s_sqrt.get_formatted(Index(0, 0))}")  # Expected: 4
print(f"=sqrt(2): {s_sqrt.get_formatted(Index(0, 1))}")   # Expected: 1.414...
print(f"=sqrt(-1): {s_sqrt.get_formatted(Index(0, 2))}")  # Expected: #NUM!

# Nested: sqrt of expression
s_sqrt.set(Index(1, 0), '=sqrt(1 + 1)')
print(f"=sqrt(1 + 1): {s_sqrt.get_formatted(Index(1, 0))}")  # Expected: 1.414...

print("\n✅ All Part 2 tests completed!")
```

### Run the test:
```bash
python test_debug.py
```

**Expected output:**
```
=== Test: sqrt() Function ===
=sqrt(16): 4
=sqrt(2): 1.4142135623730951
=sqrt(-1): #NUM!
=sqrt(1 + 1): 1.4142135623730951

✅ All Part 2 tests completed!
```

---

## Final Test: Run All Tests

*"Now let me run the complete test file to make sure everything works together."*

### Run the full test_debug.py:
```bash
python test_debug.py
```

*"All tests should pass. I can also open the spreadsheet UI with the example CSVs to visually verify:"*

```bash
# Part 1 examples should still work
python -m sheet ../examples/1-basic.csv
python -m sheet ../examples/3a-const-ref.csv
python -m sheet ../examples/3b-ref-to-ref.csv

# Part 2 examples
python -m sheet ../examples/4-binop.csv
python -m sheet ../examples/5-sum.csv
```

---

## Wrap Up (Say This)

*"So to summarize what I implemented:*

1. *Integrated the formula parser to handle all expressions*
2. *Literal formulas like `=42` and `=2018-01-01`*
3. *Binary operators with proper precedence (handled by parser)*
4. *Division by zero detection*
5. *The `sum()` function with range support*
6. *Date arithmetic - adding days, subtracting dates*
7. *The `sqrt()` function*

*For UX, I made sure to:*
- *Return meaningful error codes like `#DIV/0!`, `#CIRC!`, `#VALUE!`*
- *Display whole numbers without decimal points*
- *Skip empty cells in sum instead of erroring*

*If I had more time, I'd add:*
- *More functions like `average`, `min`, `max`*
- *Better error messages with cell locations*
- *Caching evaluated values for performance"*

---

## Quick Reference: Error Codes

| Error | When |
|-------|------|
| `#CIRC!` | Circular reference |
| `#PARSE!` | Invalid formula syntax |
| `#VALUE!` | Wrong type for operation |
| `#DIV/0!` | Division by zero |
| `#NAME?` | Unknown function |
| `#NUM!` | Invalid numeric operation (e.g., sqrt of negative) |
| `#REF!` | Invalid range reference |
| `#ERR!` | Unexpected error |

---

## Quick Reference: Formula Parser Output

```python
parse('A1')           → 'A1'
parse('42')           → '42'
parse('A1 + B1')      → ['+', 'A1', 'B1']
parse('A1 + B1 * C1') → ['+', 'A1', ['*', 'B1', 'C1']]
parse('sum(A1:B2)')   → ['sum', 'A1:B2']
parse('sqrt(1 + 1)')  → ['sqrt', ['+', '1', '1']]
```
