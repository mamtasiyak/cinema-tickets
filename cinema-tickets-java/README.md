# Cinema Tickets - Implementation

## Overview

This is my implementation of the Cinema Tickets coding exercise for the Java & JavaScript Software Engineer position at DWP.

## Running the Tests

```bash
cd cinema-tickets-java
mvn clean test
```

**Expected Result:**
```
Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Implementation Summary

### Business Rules Implemented
- Maximum 25 tickets per purchase
- Three ticket types: Adult (£25), Child (£15), Infant (£0)
- Infants do not require seats
- Child and Infant tickets require at least one Adult ticket

### Validation
- Account ID validation (must be > 0)
- Ticket request validation (non-null, positive quantities)
- Business rule enforcement (max tickets, adult requirement)

### Key Decisions
- Used constructor injection to make the service easier to test with mock objects.
- Fail-fast validation approach
- Immutable TicketTypeRequest (using final fields)
- Added meaningful error messages in InvalidPurchaseException

## Technical Choices

Separated validation into three distinct methods to make each validation 
rule independently testable. The fail-fast approach ensures invalid requests 
are caught before any external service calls are made.

The use of final fields in TicketTypeRequest ensures thread-safety and 
prevents accidental modification after construction.

## Dependencies 

Added `mockito-junit-jupiter` (version 5.14.2) to pom.xml to support JUnit 5 integration with Mockito for the test suite.

---

**Note:** This is my original work completed independently for the DWP coding assessment.