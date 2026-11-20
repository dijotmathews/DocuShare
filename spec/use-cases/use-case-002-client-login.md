# UC-002: Client Login

> Client authenticates with the system using their PAN card number and password.

---

**Goal:** As a Client, I want to login with my PAN card number and password so that I can access my tax documents.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Client user
- **Secondary actors:** None

---

## Preconditions

- Client user account exists in the system with role CLIENT
- Client has been assigned a default password by Admin
- User is on the login page

---

## Trigger

Client clicks the "Login" button after entering PAN number and password.

---

## Main Flow

1. [System] displays the login form with PAN number and password fields.
2. [Actor] enters their PAN card number as username.
3. [Actor] enters their password.
4. [Actor] clicks the "Login" button.
5. [System] validates the PAN number and password against the database.
6. [System] verifies the user has role CLIENT.
7. [System] creates an authenticated session.
8. [System] redirects Client to their document dashboard.

---

## Alternative Flows

### AF-1: Invalid Credentials

**Branches from:** Main Flow step 5
**Condition:** PAN number does not exist or password is incorrect.

1. [System] displays an error message: "Invalid PAN number or password."
2. [System] clears the password field.
3. [Actor] may re-enter credentials or cancel.
4. Use case returns to Main Flow step 2 or ends.

### AF-2: User is Not a Client

**Branches from:** Main Flow step 6
**Condition:** User account exists but has role ADMIN instead of CLIENT.

1. [System] displays an error message: "Access denied. This account is for admin use only."
2. [System] clears the session.
3. Use case ends.

### AF-3: Empty Fields

**Branches from:** Main Flow step 4
**Condition:** PAN number or password field is empty.

1. [System] displays a validation error: "PAN number and password are required."
2. [System] highlights the empty field(s).
3. [Actor] enters the missing information.
4. Returns to Main Flow step 4.

---

## Postconditions

- **On success:** Client is authenticated, session is active, and Client document dashboard is displayed with their documents.
- **On failure:** No session is created; login form is redisplayed with an error message.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only users with role CLIENT can access the client portal |
| BR-02 | Username must be a valid PAN card number |
| BR-03 | Clients can only see documents associated with their PAN number |
| BR-04 | Sessions must be validated on each request |

---

## Tests

- [x] Main Flow covered (steps 1–8)
- [x] AF-1 (invalid credentials) covered
- [x] AF-2 (user is not client) covered
- [x] AF-3 (empty fields) covered
- [x] BR-01, BR-02, BR-03, BR-04 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Client login page (`/login`) with PAN number and password fields | Anonymous |
| Client Dashboard (Document list, `/documents`) | Authenticated Client only |
