# UC-006: Manage Clients

> Admin creates and maintains client information (PAN, contact details, incorporation date, etc.).

---

**Goal:** As an Admin, I want to add and manage client information so that I can organize documents by client and ensure accurate client records.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Admin user
- **Secondary actors:** None

---

## Preconditions

- Admin is logged in
- Admin is on the "Manage Clients" page

---

## Trigger

Admin clicks "Add New Client" or edits an existing client from the client list.

---

## Main Flow (Add New Client)

1. [System] displays the client creation form.
2. [Actor] enters the Client Name and the PAN card number.
3. [Actor] enters the client's Date of Incorporation.
4. [Actor] enters the client's Address.
5. [Actor] enters the client's Email.
6. [Actor] enters the client's Contact Number.
7. [Actor] enters the client's WhatsApp number (optional).
8. [Actor] clicks "Create Client" button.
9. [System] validates that all mandatory fields are filled.
10. [System] validates that the PAN is unique (not already in system).
11. [System] sets the default password for the client account: the PAN, `@` and the Date of Incorporation as DDMMYYYY (e.g. `ABCDE1234F@01042015`).
12. [System] creates a User account with role CLIENT and username as PAN number.
13. [System] creates a Client record with the provided information.
14. [System] displays the default password to the Admin (for Admin to communicate to client).
15. [System] displays a success message: "Client created successfully."
16. [System] redirects to the client details page or client list.

---

## Alternative Flows

### AF-1: PAN Already Exists

**Branches from:** Main Flow step 10
**Condition:** A client with the entered PAN number already exists.

1. [System] displays an error message: "A client with PAN {panNumber} already exists."
2. [Actor] may choose to edit the existing client or cancel.
3. Use case ends or returns to Main Flow step 2.

### AF-2: Missing Mandatory Field

**Branches from:** Main Flow step 9
**Condition:** One or more mandatory fields (Client Name, PAN, Date of Incorporation, Address, Email, Contact Number) are empty.

1. [System] displays a validation error listing all missing fields.
2. [System] highlights the empty field(s) in the form.
3. [Actor] fills in the missing information.
4. Returns to Main Flow step 9.

### AF-3: Invalid Email Format

**Branches from:** Main Flow step 9
**Condition:** The Email field does not contain a valid email address.

1. [System] displays a validation error: "Please enter a valid email address."
2. [System] highlights the Email field.
3. [Actor] corrects the email.
4. Returns to Main Flow step 9.

### AF-4: Invalid Phone Number Format

**Branches from:** Main Flow step 9
**Condition:** Contact Number is not in a valid format.

1. [System] displays a validation error: "Please enter a valid contact number."
2. [System] highlights the Contact Number field.
3. [Actor] corrects the phone number.
4. Returns to Main Flow step 9.

### AF-5: Reset Client Password

**Branches from:** Edit Client (client selected in the list)
**Condition:** The client has lost their password.

1. [Actor] clicks "Reset password" on the client's edit form.
2. [System] asks for confirmation, warning that the current password stops working immediately.
3. [Actor] confirms (or cancels, leaving the password unchanged).
4. [System] sets the password back to the default (PAN@DDMMYYYY of the current Date of Incorporation), stores it hashed, and displays it to the Admin.

---

## Postconditions

- **On success:** Client record is created, User account with CLIENT role is created, default password is set, and Admin receives confirmation with the default password.
- **On failure:** No Client or User record is created; Admin sees error message and can correct the issue.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | All fields except WhatsApp number are mandatory |
| BR-02 | PAN number must be unique in the system |
| BR-03 | PAN must follow valid PAN format |
| BR-04 | Email must be in valid email format |
| BR-05 | Contact Number must be a valid phone number |
| BR-06 | A User account with role CLIENT is automatically created when a Client is added |
| BR-07 | The default password is `{PAN}@{Date of Incorporation as DDMMYYYY}` (e.g. `ABCDE1234F@01042015`); it is set automatically and displayed to Admin for communication to client |
| BR-08 | Only Admin users can create and manage clients |
| BR-09 | Only Admin users can reset a client's password; a reset sets it back to the default password (BR-07) |

---

## Tests

- [x] Main Flow covered (steps 1–16)
- [x] AF-1 (PAN already exists) covered
- [x] AF-2 (missing mandatory field) covered
- [x] AF-3 (invalid email format) covered
- [x] AF-4 (invalid phone number format) covered
- [x] AF-5 (reset client password) covered
- [x] BR-01 through BR-09 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Client Management page with client list (`/admin/clients`) | Authenticated Admin only |
| Add New Client form (side panel on `/admin/clients`) | Authenticated Admin only |
| Edit Client form (select a row in the client list; PAN is read-only) | Authenticated Admin only |
| Default password display (dialog after creation) | Authenticated Admin only |
| Reset password (button on the Edit Client form, with confirmation; default password shown) | Authenticated Admin only |
