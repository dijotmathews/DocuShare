# UC-007: View All Documents

> Admin views all documents in the system, optionally filtered by client PAN or other criteria.

---

**Goal:** As an Admin, I want to view all documents in the system so that I can monitor uploads, audit records, and manage document storage.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Admin user
- **Secondary actors:** None

---

## Preconditions

- Admin is logged in
- At least one document has been uploaded to the system

---

## Trigger

Admin navigates to the "All Documents" or "Document Management" page in the admin panel.

---

## Main Flow

1. [System] retrieves all Documents from the database.
2. [System] displays a list of documents with columns: PAN, Client Name, Document Type, Assessment Year, Financial Year, Upload Date, and Uploaded By.
3. [System] displays documents in reverse chronological order (most recent first) by default.
4. [System] provides filter and search options.
5. [Actor] views the document list.
6. [Actor] can optionally filter by PAN, Document Type, or date range.
7. [System] updates the list based on applied filters.

---

## Alternative Flows

### AF-1: No Documents Found

**Branches from:** Main Flow step 1
**Condition:** No documents have been uploaded yet.

1. [System] displays an empty state message: "No documents have been uploaded yet."
2. Use case ends.

### AF-2: Filter Applied Returns No Results

**Branches from:** Main Flow step 7
**Condition:** The applied filter criteria match no documents.

1. [System] displays a message: "No documents match your search criteria. Try different filters."
2. [Actor] may clear filters or refine them.
3. Returns to Main Flow step 6.

---

## Postconditions

- **On success:** Admin sees a list of all documents with full metadata and filter options available.
- **On failure:** Admin sees an appropriate empty state message.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only Admin users can view all documents in the system |
| BR-02 | Admin can see documents from all clients, not limited to their own PAN |
| BR-03 | Documents must display complete metadata including uploader information |
| BR-04 | Search and filter operations must be case-insensitive for PAN and client names |
| BR-05 | Deleted documents should not appear in this list (soft delete if applicable) |

---

## Tests

- [x] Main Flow covered (steps 1–7)
- [x] AF-1 (no documents found) covered
- [x] AF-2 (filter returns no results) covered
- [x] BR-01 through BR-05 covered

---

## UI Surface

| Page | Access |
|------|--------|
| All Documents page with filterable table (`/admin/documents`; "Documents" in the admin header, "All Documents" on the dashboard; shown after a successful upload) | Authenticated Admin only |
| Filter controls: "PAN or client name" search, Document Type, Uploaded from / Uploaded to, Clear filters | Authenticated Admin only |
| Empty state messages (AF-1, or AF-2 with a Clear filters button) inside the table | Authenticated Admin only |

**Implementation notes:**
- Filters apply as you type or pick; they combine (AND). The search matches part of a PAN or client name, ignoring case (BR-04); `%` and `_` are matched literally.
- The date range is on the upload date in the server's time zone; both days are included, and the pickers stop the end date from being before the start date.
- An extra "File" column shows where each document is stored (location/file name), for managing storage.
- A result count ("11 documents") is shown above the table. On phones the table collapses to one summary column.
- BR-05: documents can't be deleted yet, so every document is listed; when deletion is added, deleted documents must be excluded from this query.
