# Create Exam Use Case - Specification

## Overview
Describe high-level purpose and business value of this use case.

## Actor(s)
- **Primary Actor**: (Who initiates this use case?)
- **Secondary Actor(s)**: (Who else is involved?)

## Preconditions
- What must be true before this use case can begin?
- What data must exist?

## Main Flow

### Scenario 1: Successful Exam Creation
```
Given [Initial state/setup]
When [User performs action]
Then [Expected result]
```

**Steps:**
1. Step description
2. Step description
3. Step description

**Acceptance Criteria:**
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

---

### Scenario 2: Validation Error - [Specific Error Case]
```
Given [Initial state/setup]
When [User performs invalid action]
Then [Error is handled appropriately]
```

**Expected Error:**
- Error Type:
- Error Message:
- Status Code:

**Acceptance Criteria:**
- [ ] Error is caught and validated
- [ ] User receives clear error message
- [ ] System state remains consistent

---

### Scenario 3: Edge Case - [Specific Edge Case]
```
Given [Initial state/setup]
When [Edge case condition occurs]
Then [System handles gracefully]
```

**Acceptance Criteria:**
- [ ] Edge case is handled
- [ ] No unintended side effects occur

---

## Postconditions
- What is state after this use case completes successfully?
- What data has been created/modified?

## Implementation Notes

### API Endpoint(s)
```
POST /api/exams
Content-Type: application/json

Request Body:
{
  "field": "value"
}

Response (200):
{
  "id": "exam-id",
  "field": "value"
}
```

### Database Schema/Model Changes
- Model: `Exam`
- New Fields:
- Modified Fields:
- Relationships:

### Business Rules
1. Rule 1
2. Rule 2
3. Rule 3

## Test Coverage

| Scenario | Unit Test | Integration Test | E2E Test |
|----------|-----------|------------------|----------|
| Successful Creation | ✓ | ✓ | ✓ |
| Validation Error | ✓ | ✓ | ✓ |
| Edge Case | ✓ | ✓ | - |

## Acceptance Criteria (Overall)
- [ ] All scenarios pass
- [ ] All API endpoints working correctly
- [ ] Database changes persisted properly
- [ ] Error handling implemented
- [ ] Documentation updated
