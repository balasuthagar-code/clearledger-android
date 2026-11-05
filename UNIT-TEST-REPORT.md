# ClearLedger Unit Test Report

**Generated:** 2026-11-05  
**Project:** ClearLedger Android  
**Test command:** `.\gradlew.bat :app:testDebugUnitTest`

## Summary

| Item | Result |
|---|---|
| Test suite | JVM unit tests |
| Test source files | 58 |
| Tests executed | 372 |
| Passed | 372 |
| Failed | 0 |
| Skipped | 0 |
| Exit code | 0 |
| Overall | PASS |

## Coverage Areas

- Core money, currency, date, and validation rules
- Domain use cases for expenses, recurring payments, savings, and summaries
- Repository mappers and in-memory repository behavior
- Authentication, profile, dashboard, income, expense, recurring, crypto, and savings ViewModels
- Merchant suggestion memory and category prefill behavior

## Issue Resolved During Testing

The first run could not compile because savings ViewModel tests did not provide the newly required `ExpenseRepository` dependency. The tests were updated to use the existing in-memory repository test double.

After compilation succeeded, seven merchant suggestion tests failed because JVM tests reached Android-only `Log.d` calls in `ExpenseFormViewModel`. The debug logging was removed from the pure state/data flow and merchant selection path. The full suite then passed.

## Evidence

Gradle XML results are generated at:

`app/build/test-results/testDebugUnitTest/`

The HTML test report is generated at:

`app/build/reports/tests/testDebugUnitTest/index.html`
