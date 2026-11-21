# UC-004: View Documents

> Client views all documents associated with their PAN card number, grouped into Filings, Financials, Computations and Misc, and within each group by financial year.

---

**Goal:** As a Client, I want to view all my uploaded documents, organized by kind and financial year, so that I can quickly find the tax records I need.

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
2. [System] shows a side nav with one item per group, in this order: Filings, Financials, Computations, Misc, each with the number of documents in it (BR-04). The first group that has documents is shown.
3. [System] next to the side nav, shows the selected group's documents in one section per financial year, latest first, headed e.g. "FY 2024-25" (BR-05).
4. [System] in each financial year, lists the documents with their Document Type, Assessment Year and Upload Date, most recent first (BR-02), each with a Download link (UC-005).
5. [Actor] picks another group in the side nav to see its documents.

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

### AF-3: Group Has No Documents

**Branches from:** Main Flow step 5
**Condition:** The client picks a group with no documents (its side nav item shows 0).

1. [System] displays "No {group} yet." (e.g. "No computations yet.") under the group title.
2. Returns to Main Flow step 5.

---

## Postconditions

- **On success:** Client sees all their documents, grouped by kind and financial year, with relevant metadata.
- **On failure:** Client sees an appropriate message (empty state, no documents) and can logout or navigate away.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Clients can only view documents tagged with their own PAN number |
| BR-02 | Within a financial year, documents are displayed in reverse chronological order of upload (most recent first) |
| BR-03 | All document metadata must be accurate and match what was uploaded by Admin |
| BR-04 | Each Document Type belongs to one group: **Filings** = IT Acknowledgement; **Financials** = Balance Sheet, Profit & Loss, Audit Report, Tax Audit Report; **Computations** = Tax Computation; **Misc** = Misc Report |
| BR-05 | Within a group, financial years are shown latest first; a financial year is shown as its start year and the next (e.g. stored 2024 → "FY 2024-25") |

---

## Tests

- [x] Main Flow covered (steps 1–5)
- [x] AF-1 (no documents found) covered
- [x] AF-2 (client logs in for first time) covered
- [x] AF-3 (group has no documents) covered
- [x] BR-01 through BR-05 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Client Dashboard / My Documents page (`/documents`; clients land here after login) | Authenticated Client only |
| Group side nav: Filings, Financials, Computations, Misc, with counts; each group has its own page (`/documents/filings`, `/documents/financials`, `/documents/computations`, `/documents/misc`) | Authenticated Client only |
| Financial-year sections for the selected group, with a Download link per document | Authenticated Client only |
| Empty group message (for a group with no documents) | Authenticated Client only |
| Empty state message (`/documents`, shown instead of the side nav) | Authenticated Client (no documents) |

**Implementation notes:**
- The PAN always comes from the signed-in account, never from the request, so a client can't ask for another PAN's documents (BR-01).
- The group of each type is fixed in code (`DocumentType.group()`), so a new document type must be given a group. Groups are not stored in the database.
- Each document shows its type, then "AY 2025-26 · Uploaded 12 Jul 2025, 10:30". The financial year is the section heading, so it isn't repeated per document. Upload Date is in the server's time zone.
- Assessment Year is written the same way as the financial year (stored 2025 → "AY 2025-26").
- AF-1 and AF-2 share one empty state: the AF-1 message plus text suggesting the client contact their consultant (no consultant contact details are stored yet).
- `/documents` (where clients land after login) and unknown group names forward to the first group that has documents, so the address always names the group shown; back/forward and bookmarks work per group.
- On phones (< 640px) the side nav sits above the documents, and the Download link shows only its icon (it keeps its "Download …" label for screen readers).
