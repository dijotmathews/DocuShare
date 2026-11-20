# UC-004: View Documents

> Client views all documents associated with their PAN card number.

---

**Goal:** As a Client, I want to view all my uploaded documents so that I can access my tax records anytime.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Client user
- **Secondary actors:** None

---

## Preconditions

- Client is logged in
- At least one document has been uploaded for this client's PAN number

---

## Trigger

Client navigates to the "My Documents" or dashboard page after login.

---

## Main Flow

1. [System] retrieves all Documents associated with the logged-in client's PAN number.
2. [System] displays a list of documents with the following columns: Document Type, Assessment Year, Financial Year, and Upload Date.
3. [System] displays document details in a table format.
4. [Actor] views the list of documents.

---

## Alternative Flows

### AF-1: No Documents Found

**Branches from:** Main Flow step 1
**Condition:** No documents have been uploaded for this client's PAN number yet.

1. [System] displays an empty state message: "No documents available yet. Check back soon."
2. Use case ends.

### AF-2: Client Logs In for First Time

**Branches from:** Main Flow step 1
**Condition:** Client account exists but no documents have been created.

1. [System] displays an empty state page with explanatory text.
2. [System] optionally displays contact information or instructions for the client.
3. Use case ends.

---

## Postconditions

- **On success:** Client sees a list of all their documents with relevant metadata.
- **On failure:** Client sees an appropriate message (empty state, no documents) and can logout or navigate away.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Clients can only view documents tagged with their own PAN number |
| BR-02 | Documents must be displayed in reverse chronological order (most recent first) by default |
| BR-03 | All document metadata must be accurate and match what was uploaded by Admin |

---

## Tests

- [x] Main Flow covered (steps 1–4)
- [x] AF-1 (no documents found) covered
- [x] AF-2 (client logs in for first time) covered
- [x] BR-01, BR-02, BR-03 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Client Dashboard / My Documents page (`/documents`; clients land here after login) | Authenticated Client only |
| Empty state message (same page, shown instead of the table) | Authenticated Client (no documents) |

**Implementation notes:**
- The PAN always comes from the signed-in account, never from the request, so a client can't ask for another PAN's documents (BR-01).
- Default order is Upload Date, newest first, shown by the sort indicator; all columns are sortable. Upload Date is shown in the server's time zone as `dd MMM yyyy, HH:mm`.
- AF-1 and AF-2 share one empty state: the AF-1 message plus text suggesting the client contact their consultant (no consultant contact details are stored yet).
- On phones (< 640px) the table collapses to one column: document type with "AY · FY · upload date" beneath.
