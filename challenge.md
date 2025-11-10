# Rillet Coding Challenge

Revenue recognition is a critical accounting principle that determines when and how revenue should be recorded in the
financial statements. The goal is to recognize revenue in a way that accurately reflects the timing of the economic
benefits associated with the revenue-generating activities.

Sometimes that revenue has a recurrent nature which allows recognizing revenue on a periodic basis for recurring
transactions or ongoing services, such as subscriptions, memberships, or long-term contracts. This type of revenue is
generated from customers who make regular payments over time for continuous access to goods or services.

## Table of Contents

1. [Key Concepts in Recurrent Revenue Recognition](#key-concepts-in-recurrent-revenue-recognition)
2. [Problem Statement](#problem-statement)
3. [Example: Subscription-Based Software Service](#example-subscription-based-software-service)
4. [Requirements Summary](#requirements-summary)

## Key Concepts in Recurrent Revenue Recognition

- **Recurring Revenue:** Revenue that is earned on a regular basis, often in the form of subscriptions, service
  contracts, or memberships. This revenue is predictable and usually billed periodically (e.g., monthly, quarterly, or
  annually).

- **Revenue Recognition Over Time**: Revenue from recurring transactions is recognized over the period in which the
  service is provided, or the access is granted, rather than recognizing it all at once.

## Problem Statement

Split a monetary amount into multiple amounts with a frequency over a period of time, spreading the rounding difference,
So, the total should be equal to the initial amount when all the intermediate amounts are added.

**Note:** For all examples assume they all start at the beginning of the month unless told otherwise.

## Example: Subscription-Based Software Service

### Scenario:

Company XYZ provides a cloud-based software service to its customers. The company offers an annual subscription plan,
where customers pay $1,200 at the beginning of each year for access to the platform throughout the year.

### Contract Details:

- Contract Term: 1 year
- Total Contract Value: $1,200
- Performance Obligation: Provide access to the platform for the entire year
- Billing Frequency: Annual, with payment received upfront

### Recognize Revenue Over Time:

- Revenue is recognized evenly over the 12-month subscription period, as the service is provided continuously throughout
  the year.

### Monthly Revenue Recognition Calculation:

- Monthly Revenue = Total Transaction Price / Number of Months
- Monthly Revenue = $1,200 / 12 = $100

## Incremental Steps

For this coding challenge, we will try to implement a recurrent revenue recognition algorithm based on these incremental
steps:

1. Split the yearly amount into monthly amounts given an amount (divisible by 12, e.g., `$1200`).
2. Split the yearly amount into monthly amounts given any amount that when divided by 12 does not return neat amounts
   and might even return a decimal that's eventually periodic (e.g., `1000 / 12`). In an accounting context, the sum of the split amounts must be the same as the initial amount.
3. Given an amount with a rounding difference, we should let the user decide whether that rounding goes to the first
   monthly amount, the last, or in the middle.

### Requirements Summary

Candidates are expected to:

1. Write a function to split an annual amount into monthly amounts, ensuring the total adds up to the initial amount.
2. Handle cases where the division leads to fractional results, ensuring rounding differences are managed correctly.
3. Provide the user an option to decide where the rounding difference should be applied (first, last, or middle month).

### Additional Clarification Examples

1. **Case 1: Neat Division**
    - Total Contract Value: `$1200`
    - Expected Monthly Amount: `$100` each month
      - January -> `$100`
      - February -> `$100`
      - etc.

2. **Case 2: Division with Rounding Difference**
    - Total Contract Value: `$1000`
    - Expected Monthly Amounts (with default rounding): `$83.33` for 11 months, `$83.37` for 1 month
