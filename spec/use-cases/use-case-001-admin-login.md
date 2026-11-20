# UC-001: Admin Login

> Admin authenticates with the system to access administrative functions.

---

**Goal:** As an Admin, I want to login with my credentials so that I can access the admin panel and manage documents.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Admin user
- **Secondary actors:** None

---

## Preconditions

- Admin user account exists in the system
- User is on the login page

---

## Trigger

Admin clicks the "Login" button after entering credentials.

---

## Main Flow

1. [System] displays the login form with username and password fields.
2. [Actor] enters username and password.
3. [Actor] clicks the "Login" button.
4. [System] validates the username and password against the database.
5. [System] verifies the user has role ADMIN.
6. [System] creates an authenticated session.
7. [System] redirects Admin to the admin dashboard.

---

## Alternative Flows

### AF-1: Invalid Credentials

**Branches from:** Main Flow step 4
**Condition:** Username does not exist or password is incorrect.

1. [System] displays an error message: "Invalid username or password."
2. [System] clears the password field.
3. [Actor] may re-enter credentials or cancel.
4. Use case returns to Main Flow step 2 or ends.

### AF-2: User is Not an Admin

**Branches from:** Main Flow step 5
**Condition:** User account exists but has role CLIENT instead of ADMIN.

1. [System] displays an error message: "Access denied. Admin privileges required."
2. [System] clears the session.
3. Use case ends.

### AF-3: Empty Fields

**Branches from:** Main Flow step 3
**Condition:** Username or password field is empty.

1. [System] displays a validation error: "Username and password are required."
2. [System] highlights the empty field(s).
3. [Actor] enters the missing information.
4. Returns to Main Flow step 3.

---

## Postconditions

- **On success:** Admin is authenticated, session is active, and Admin dashboard is displayed.
- **On failure:** No session is created; login form is redisplayed with an error message.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only users with role ADMIN can access the admin panel |
| BR-02 | Sessions must be validated on each request |
| BR-03 | Passwords must be non-empty and stored securely |

---

## Tests

- [x] Main Flow covered (steps 1–7)
- [x] AF-1 (invalid credentials) covered
- [x] AF-2 (user is not admin) covered
- [x] AF-3 (empty fields) covered
- [x] BR-01, BR-02, BR-03 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Admin login page (`/admin/login`) with username and password fields | Anonymous |
| Admin Dashboard (`/admin`), with a card for each admin section: Dashboard, Upload Documents, Clients, Documents | Authenticated Admin only |
| Admin navigation in the header of every admin page, in the same order: Dashboard, Upload Documents, Clients, Documents (current page highlighted) | Authenticated Admin only |
